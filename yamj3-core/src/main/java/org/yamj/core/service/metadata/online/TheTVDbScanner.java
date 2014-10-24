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
package org.yamj.core.service.metadata.online;

import com.omertron.thetvdbapi.model.Actor;
import com.omertron.thetvdbapi.model.Episode;
import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.configuration.ConfigServiceWrapper;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.service.metadata.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;

@Service("tvdbScanner")
public class TheTVDbScanner implements ISeriesScanner {

    public static final String SCANNER_ID = "tvdb";
    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbScanner.class);

    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private TheTVDbApiWrapper tvdbApiWrapper;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() throws Exception {
        LOG.info("Initialize TheTVDb scanner");
        
        // register this scanner
        onlineScannerService.registerSeriesScanner(this);
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

        if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            series.setGenreNames(new HashSet<String>(tvdbSeries.getGenres()), SCANNER_ID);
        }

        if (StringUtils.isNumeric(tvdbSeries.getRating())) {
            try {
                series.addRating(SCANNER_ID, (int) (Float.parseFloat(tvdbSeries.getRating()) * 10));
            } catch (NumberFormatException nfe) {
                LOG.warn("Failed to convert TVDB rating '{}' to an integer, error: {}", tvdbSeries.getRating(), nfe.getMessage());
            }
        }

        if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
            String faDate = tvdbSeries.getFirstAired();
            if (StringUtils.isNotBlank(faDate) && (faDate.length() >= 4)) {
                try {
                    series.setStartYear(Integer.parseInt(faDate.substring(0, 4)), SCANNER_ID);
                } catch (Exception ignore) {}
            }
        }

        if (OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            String studioName = StringUtils.trimToNull(tvdbSeries.getNetwork());
            if (studioName != null) {
                Set<String> studioNames = Collections.singleton(studioName);
                series.setStudioNames(studioNames, SCANNER_ID);
            }
        }

        // CAST & CREW
        Set<CreditDTO> actors = new LinkedHashSet<CreditDTO>();
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            for (Actor actor : tvdbApiWrapper.getActors(id)) {
                actors.add(new CreditDTO(SCANNER_ID, JobType.ACTOR, actor.getName(), actor.getRole()));
            }
        }
        
        // SCAN SEASONS
        this.scanSeasons(series, tvdbSeries, actors);

        return ScanResult.OK;
    }

    private void scanSeasons(Series series, com.omertron.thetvdbapi.model.Series tvdbSeries, Set<CreditDTO> actors) {

        for (Season season : series.getSeasons()) {

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

            if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                // get season year from minimal first aired of episodes
                String seriesId = season.getSeries().getSourceDbId(SCANNER_ID);
                Date year = this.getSeasonYear(seriesId, season.getSeasonNumber());
                if (year == null) {
                    // try first aired from series as fall-back
                    if (StringUtils.isNotBlank(tvdbSeries.getFirstAired())) {
                        year = MetadataTools.parseToDate(tvdbSeries.getFirstAired().trim());
                    }
                }
                season.setPublicationYear(MetadataTools.extractYearAsInt(year), SCANNER_ID);
            }

            // mark season as done
            season.setTvSeasonDone();
            
            // scan episodes
            this.scanEpisodes(season, actors);
        }
    }

    private Date getSeasonYear(String seriesId, int season) {
        List<Episode> episodeList = tvdbApiWrapper.getSeasonEpisodes(seriesId, season);
        if (CollectionUtils.isEmpty(episodeList)) {
            return null;
        }
        
        Date yearDate = null;
        for (Episode episode : episodeList) {
            if (StringUtils.isNotBlank(episode.getFirstAired())) {
                Date parsedDate = MetadataTools.parseToDate(episode.getFirstAired().trim());
                if (parsedDate != null) {
                    if (yearDate == null) {
                        yearDate = parsedDate;
                    } else if (parsedDate.before(yearDate)) {
                        yearDate = parsedDate;
                    }
                }
            }
        }
        return yearDate;
    }
    
    private void scanEpisodes(Season season, Set<CreditDTO> actors) {
        if (season.isTvEpisodesScanned(SCANNER_ID)) {
            // nothing to do anymore
            return;
        }

        String seriesId = season.getSeries().getSourceDbId(SCANNER_ID);
        List<Episode> episodeList = tvdbApiWrapper.getSeasonEpisodes(seriesId, season.getSeason());

        for (VideoData videoData : season.getVideoDatas()) {
            if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                // nothing to do if already done
                continue;
            }
            
            Episode episode = this.findEpisode(episodeList, season.getSeason(), videoData.getEpisode());
            if (episode == null) {
                // mark episode as not found
                videoData.setTvEpisodeNotFound();
            } else {
                videoData.setSourceDbId(SCANNER_ID, episode.getId());

                if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                    videoData.setTitle(StringUtils.trim(episode.getEpisodeName()), SCANNER_ID);
                }

                if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                    videoData.setPlot(StringUtils.trim(episode.getOverview()), SCANNER_ID);
                }

                if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                    videoData.setOutline(StringUtils.trim(episode.getOverview()), SCANNER_ID);
                }

                if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                    Date releaseDate = MetadataTools.parseToDate(episode.getFirstAired());
                    videoData.setReleaseDate(releaseDate, SCANNER_ID);
                }

                // directors
                if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
                    for (String director : episode.getDirectors()) {
                        videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.DIRECTOR, director));
                    }
                }
                
                // writers
                if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
                    for (String writer : episode.getWriters()) {
                        videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.WRITER, writer));
                    }
                }

                // actors
                videoData.addCreditDTOS(actors);

                // guest stars
                if (this.configServiceWrapper.isCastScanEnabled(JobType.GUEST_STAR)) {
                    for (String guestStar : episode.getGuestStars()) {
                        videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.GUEST_STAR, guestStar));
                    }
                }
                
                // mark episode as done
                videoData.setTvEpisodeDone();
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

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return Boolean.TRUE;
        }
    
        // scan for IMDb ID
        ImdbScanner.scanImdbID(nfoContent, dto, ignorePresentId);

        LOG.trace("Scanning NFO for TheTVDB ID");
        
        // http://www.allocine.fr/...=XXXXX.html
        try {
            String compareString = nfoContent.toUpperCase();
            int idx = compareString.indexOf("THETVDB.COM");
            if (idx > -1) {
                int beginIdx = compareString.indexOf("&ID=");
                int length = 4;
                if (beginIdx < idx) {
                    beginIdx = compareString.indexOf("?ID=");
                }
                if (beginIdx < idx) {
                    beginIdx = compareString.indexOf("&SERIESID=");
                    length = 10;
                }
                if (beginIdx < idx) {
                    beginIdx = compareString.indexOf("?SERIESID=");
                    length = 10;
                }

                if (beginIdx > idx) {
                    int endIdx = compareString.indexOf("&", beginIdx + 1);
                    String id;
                    if (endIdx > -1) {
                        id = compareString.substring(beginIdx + length, endIdx);
                    } else {
                        id = compareString.substring(beginIdx + length);
                    }

                    if (StringUtils.isNotBlank(id)) {
                        String sourceId = id.trim();
                        dto.addId(SCANNER_ID, sourceId);
                        LOG.debug("TheTVDB ID found in NFO: {}", sourceId);
                        dto.addId(SCANNER_ID, sourceId);
                        return Boolean.TRUE;
                    }
                }
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }
        
        LOG.debug("No TheTVDB ID found in NFO");
        return Boolean.FALSE;
    }
}
