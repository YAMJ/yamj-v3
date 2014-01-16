/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.plugin;

import com.omertron.thetvdbapi.model.Actor;
import com.omertron.thetvdbapi.model.Episode;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.tools.OverrideTools;

@Service("tvdbScanner")
public class TheTVDbScanner implements ISeriesScanner, InitializingBean {

    public static final String SCANNER_ID = "tvdb";
    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbScanner.class);

    @Autowired
    private PluginMetadataService pluginMetadataService;
    @Autowired
    private TheTVDbApiWrapper tvdbApiWrapper;

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // register this scanner
        pluginMetadataService.registerSeriesScanner(this);
    }

    @Override
    public String getSeriesId(Series series) {
        String id = series.getSourceDbId(SCANNER_ID);

        if (StringUtils.isBlank(id)) {
            return getSeriesId(series.getTitle(), series.getStartYear());
        }

        return id;
    }

    @Override
    public String getSeriesId(String title, int year) {
        return tvdbApiWrapper.getSeriesId(title, year);
    }

    @Override
    public ScanResult scan(Series series) {
        String id = getSeriesId(series);

        if (StringUtils.isBlank(id)) {
            return ScanResult.MISSING_ID;
        }

        com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApiWrapper.getSeries(id);

        series.setSourceDbId(SCANNER_ID, tvdbSeries.getId());
        series.setSourceDbId(ImdbScanner.SCANNER_ID, tvdbSeries.getImdbId());

        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            series.setTitle(StringUtils.trim(tvdbSeries.getSeriesName()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(StringUtils.trim(tvdbSeries.getOverview()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(StringUtils.trim(tvdbSeries.getOverview()), SCANNER_ID);
        }

        // Add the genres
        series.setGenreNames(new HashSet<String>(tvdbSeries.getGenres()), SCANNER_ID);

        // TODO more values
        if (StringUtils.isNumeric(tvdbSeries.getRating())) {
            try {
                series.addRating(SCANNER_ID, (int) (Float.parseFloat(tvdbSeries.getRating()) * 10));
            } catch (NumberFormatException nfe) {
                LOG.warn("Failed to convert TVDB rating '{}' to an integer, error: {}", tvdbSeries.getRating(), nfe.getMessage());
            }
        }

        String faDate = tvdbSeries.getFirstAired();
        if (StringUtils.isNotBlank(faDate) && (faDate.length() >= 4)) {
            series.setStartYear(Integer.parseInt(faDate.substring(0, 4)));
        }

        // CAST & CREW
        Set<CreditDTO> actors = new LinkedHashSet<CreditDTO>();
        for (Actor actor : tvdbApiWrapper.getActors(id)) {
            actors.add(new CreditDTO(JobType.ACTOR, actor.getName(), actor.getRole()));
        }

        // SCAN SEASONS
        this.scanSeasons(series, tvdbSeries, actors);

        return ScanResult.OK;
    }

    private void scanSeasons(Series series, com.omertron.thetvdbapi.model.Series tvdbSeries, Set<CreditDTO> actors) {

        for (Season season : series.getSeasons()) {

            // update season values if not done before
            if (season.isScannableTvSeason()) {

                // use values from series
                if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                    season.setTitle(StringUtils.trim(tvdbSeries.getSeriesName()), SCANNER_ID);
                }

                if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                    season.setPlot(StringUtils.trim(tvdbSeries.getOverview()), SCANNER_ID);
                }

                if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                    season.setOutline(StringUtils.trim(tvdbSeries.getOverview()), SCANNER_ID);
                }

                // TODO common usable format
                season.setFirstAired(StringUtils.trimToNull(tvdbSeries.getFirstAired()));

                // mark as scanned
                season.setTvSeasonScanned();
            }

            // scan episodes
            this.scanEpisodes(season, actors);
        }
    }

    private void scanEpisodes(Season season, Set<CreditDTO> actors) {
        if (CollectionUtils.isEmpty(season.getVideoDatas())) {
            return;
        }

        // get episodes to scan
        List<VideoData> videoDatas = season.getScannableTvEpisodes();
        if (CollectionUtils.isEmpty(videoDatas)) {
            // nothing to do anymore
            return;
        }

        String seriesId = season.getSeries().getSourceDbId(SCANNER_ID);
        List<Episode> episodeList = tvdbApiWrapper.getSeasonEpisodes(seriesId, season.getSeason());

        for (VideoData videoData : videoDatas) {

            Episode episode = this.findEpisode(episodeList, season.getSeason(), videoData.getEpisode());
            if (episode == null) {
                // mark episode as not found
                videoData.setTvEpisodeNotFound();
            } else {

                if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                    videoData.setTitle(StringUtils.trim(episode.getEpisodeName()), SCANNER_ID);
                }

                if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                    videoData.setPlot(StringUtils.trim(episode.getOverview()), SCANNER_ID);
                }

                // cast and crew
                videoData.addCreditDTOS(actors);

                for (String director : episode.getDirectors()) {
                    videoData.addCreditDTO(new CreditDTO(JobType.DIRECTOR, director));
                }
                for (String writer : episode.getWriters()) {
                    videoData.addCreditDTO(new CreditDTO(JobType.WRITER, writer));
                }
                for (String guestStar : episode.getGuestStars()) {
                    videoData.addCreditDTO(new CreditDTO(JobType.GUEST_STAR, guestStar));
                }

                // TODO more values
                // mark episode as missing
                videoData.setTvEpisodeScanned();
            }
        }
    }

    /**
     * Locate the specific episode from the list of episodes
     *
     * @param episodeList
     * @param seasonNumber
     * @param episodeNumber
     * @return
     */
    private Episode findEpisode(List<Episode> episodeList, int seasonNumber, int episodeNumber) {
        if (CollectionUtils.isEmpty(episodeList)) {
            return null;
        }

        for (Episode episode : episodeList) {
            if (episode.getSeasonNumber() == seasonNumber && episode.getEpisodeNumber() == episodeNumber) {
                return episode;
            }
        }
        return null;
    }
}
