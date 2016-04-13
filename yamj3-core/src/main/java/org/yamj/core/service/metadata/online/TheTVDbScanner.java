/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import org.yamj.plugin.api.type.JobType;

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
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.web.apis.TheTVDbApiWrapper;

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
    @Autowired
    private LocaleService localeService;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.trace("Initialize TheTVDb scanner");
        
        // register this scanner
        onlineScannerService.registerMetadataScanner(this);
    }

    @Override
    public String getSeriesId(Series series) {
        Locale tvdbLocale = localeService.getLocaleForConfig("thetvdb");
        return getSeriesId(series, tvdbLocale, false);
    }

    private String getSeriesId(Series series, Locale tvdbLocale, boolean throwTempError) {
        String tvdbId = series.getSourceDbId(SCANNER_ID);
        // search by title
        if (StringUtils.isBlank(tvdbId)) {
            tvdbId = tvdbApiWrapper.getSeriesId(series.getTitle(), series.getStartYear(), tvdbLocale.getLanguage(), throwTempError);
            series.setSourceDbId(SCANNER_ID, tvdbId);
        }
        // search by original title
        if (StringUtils.isBlank(tvdbId) && series.isTitleOriginalScannable()) {
            tvdbId = tvdbApiWrapper.getSeriesId(series.getTitleOriginal(), series.getStartYear(), tvdbLocale.getLanguage(), throwTempError);
            series.setSourceDbId(SCANNER_ID, tvdbId);
        }
        return tvdbId;
    }
    
    @Override
    public ScanResult scanSeries(Series series, boolean throwTempError) {
        Locale tvdbLocale = localeService.getLocaleForConfig("thetvdb");
        
        // get series id
        String tvdbId = getSeriesId(series, tvdbLocale, throwTempError); 
        if (StringUtils.isBlank(tvdbId)) {
            LOG.debug("TVDb id not available '{}'", series.getIdentifier());
            return ScanResult.MISSING_ID;
        }

        // get series info
        com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApiWrapper.getSeries(tvdbId, tvdbLocale.getLanguage(), throwTempError);
        if (tvdbSeries == null || StringUtils.isBlank(tvdbSeries.getId())) {
            LOG.error("Can't find informations for series '{}'", series.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        // set IMDb id if not set before
        String imdbId = series.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            series.setSourceDbId(ImdbScanner.SCANNER_ID, tvdbSeries.getImdbId());
        }

        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            series.setTitle(tvdbSeries.getSeriesName(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(tvdbSeries.getOverview(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(tvdbSeries.getOverview(), SCANNER_ID);
        }

        series.addRating(SCANNER_ID, MetadataTools.parseRating(tvdbSeries.getRating()));

        if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
            String faDate = tvdbSeries.getFirstAired();
            if (StringUtils.isNotBlank(faDate) && (faDate.length() >= 4)) {
                try {
                    series.setStartYear(Integer.parseInt(faDate.substring(0, 4)), SCANNER_ID);
                } catch (Exception ignore) {
                    // ignore error if year is invalid
                }
            }
        }

        if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            series.setGenreNames(new LinkedHashSet<>(tvdbSeries.getGenres()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            String studioName = StringUtils.trimToNull(tvdbSeries.getNetwork());
            if (studioName != null) {
                Set<String> studioNames = Collections.singleton(studioName);
                series.setStudioNames(studioNames, SCANNER_ID);
            }
        }

        // ACTORS (to store in episodes)
        Set<CreditDTO> actors = null;
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            List<Actor> tvdbActors = tvdbApiWrapper.getActors(tvdbSeries.getId());
            if (CollectionUtils.isNotEmpty(tvdbActors)) {
                actors = new LinkedHashSet<>(tvdbActors.size());
                for (Actor actor : tvdbActors) {
                    if (StringUtils.isNotBlank(actor.getName())) {
                        actors.add(new CreditDTO(SCANNER_ID, JobType.ACTOR, actor.getName(), actor.getRole()));
                    }
                }
            }
        }
        
        // SCAN SEASONS
        this.scanSeasons(series, tvdbSeries, actors, tvdbLocale);

        return ScanResult.OK;
    }

    private void scanSeasons(Series series, com.omertron.thetvdbapi.model.Series tvdbSeries, Set<CreditDTO> actors, Locale tvdbLocale) {

        for (Season season : series.getSeasons()) {

            if (!season.isTvSeasonDone(SCANNER_ID)) {
                // same as series id
                final String tvdbId = tvdbSeries.getId();
                season.setSourceDbId(SCANNER_ID, tvdbId);
                
                // use values from series
                if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                    season.setTitle(tvdbSeries.getSeriesName(), SCANNER_ID);
                }
                if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                    season.setPlot(tvdbSeries.getOverview(), SCANNER_ID);
                }
                if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                    season.setOutline(tvdbSeries.getOverview(), SCANNER_ID);
                }
    
                if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                    // get season year from minimal first aired of episodes
                    String year = tvdbApiWrapper.getSeasonYear(tvdbId, season.getSeason(), tvdbLocale.getLanguage());
                    season.setPublicationYear(MetadataTools.extractYearAsInt(year), SCANNER_ID);
                }
    
                // mark season as done
                season.setTvSeasonDone();
            }
            
            // scan episodes
            this.scanEpisodes(season, actors, tvdbLocale);
        }
    }

    private void scanEpisodes(Season season, Set<CreditDTO> actors, Locale tvdbLocale) {
        final String seriesId = season.getSeries().getSourceDbId(SCANNER_ID);
        
        for (VideoData videoData : season.getVideoDatas()) {
            if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                // nothing to do if already done
                continue;
            }
            
            Episode tvdbEpisode = tvdbApiWrapper.getEpisode(seriesId, season.getSeason(), videoData.getEpisode(), tvdbLocale.getLanguage());
            if (tvdbEpisode == null || StringUtils.isBlank(tvdbEpisode.getId())) {
                // mark episode as not found
                videoData.removeOverrideSource(SCANNER_ID);
                videoData.removeSourceDbId(SCANNER_ID);
                videoData.setTvEpisodeNotFound();
                continue;
            }
            
            // set episode ID
            videoData.setSourceDbId(SCANNER_ID, tvdbEpisode.getId());
            // set IMDb id if not set before
            String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
            if (StringUtils.isBlank(imdbId)) {
                videoData.setSourceDbId(ImdbScanner.SCANNER_ID, tvdbEpisode.getImdbId());
            }

            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                videoData.setTitle(tvdbEpisode.getEpisodeName(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                videoData.setPlot(tvdbEpisode.getOverview(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                videoData.setOutline(tvdbEpisode.getOverview(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                Date releaseDate = MetadataTools.parseToDate(tvdbEpisode.getFirstAired());
                videoData.setRelease(releaseDate, SCANNER_ID);
            }

            // directors
            addCredits(videoData, JobType.DIRECTOR, tvdbEpisode.getDirectors());
            // writers
            addCredits(videoData, JobType.WRITER, tvdbEpisode.getWriters());
            // actors
            videoData.addCreditDTOS(actors);
            // guest stars
            addCredits(videoData, JobType.GUEST_STAR, tvdbEpisode.getGuestStars());
            
            // mark episode as done
            videoData.setTvEpisodeDone();
        }
    }

    private void addCredits(VideoData videoData, JobType jobType, Collection<String> persons) {
        if (persons != null && this.configServiceWrapper.isCastScanEnabled(jobType)) {
            for (String person : persons) {
                if (StringUtils.isNotBlank(person)) {
                    videoData.addCreditDTO(new CreditDTO(SCANNER_ID, jobType, person));
                }
            }
        }
    }
    
    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return true;
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
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }
        
        LOG.debug("No TheTVDB ID found in NFO");
        return false;
    }
}
