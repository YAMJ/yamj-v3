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

import static org.yamj.plugin.api.common.Constants.SOURCE_IMDB;
import static org.yamj.plugin.api.common.Constants.SOURCE_TVDB;

import com.omertron.thetvdbapi.model.Actor;
import com.omertron.thetvdbapi.model.Episode;
import java.util.*;
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
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.web.apis.TheTVDbApiWrapper;
import org.yamj.plugin.api.metadata.IdMap;
import org.yamj.plugin.api.metadata.tools.MetadataTools;
import org.yamj.plugin.api.type.JobType;

@Service("tvdbScanner")
public class TheTVDbScanner implements ISeriesScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbScanner.class);

    @Autowired
    private TheTVDbApiWrapper tvdbApiWrapper;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private IdentifierService identifierService;
    
    @Override
    public String getScannerName() {
        return SOURCE_TVDB;
    }

    @Override
    public String getSeriesId(Series series) {
        Locale tvdbLocale = localeService.getLocaleForConfig("thetvdb");
        return getSeriesId(series, tvdbLocale, false);
    }

    private String getSeriesId(Series series, Locale tvdbLocale, boolean throwTempError) {
        String tvdbId = series.getSourceDbId(SOURCE_TVDB);
        // search by title
        if (StringUtils.isBlank(tvdbId)) {
            tvdbId = tvdbApiWrapper.getSeriesId(series.getTitle(), series.getStartYear(), tvdbLocale.getLanguage(), throwTempError);
            series.setSourceDbId(SOURCE_TVDB, tvdbId);
        }
        // search by original title
        if (StringUtils.isBlank(tvdbId) && MetadataTools.isOriginalTitleScannable(series.getTitle(), series.getTitleOriginal())) {
            tvdbId = tvdbApiWrapper.getSeriesId(series.getTitleOriginal(), series.getStartYear(), tvdbLocale.getLanguage(), throwTempError);
            series.setSourceDbId(SOURCE_TVDB, tvdbId);
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
        String imdbId = series.getSourceDbId(SOURCE_IMDB);
        if (StringUtils.isBlank(imdbId)) {
            series.setSourceDbId(SOURCE_IMDB, tvdbSeries.getImdbId());
        }

        if (OverrideTools.checkOverwriteTitle(series, SOURCE_TVDB)) {
            series.setTitle(tvdbSeries.getSeriesName(), SOURCE_TVDB);
        }

        if (OverrideTools.checkOverwritePlot(series, SOURCE_TVDB)) {
            series.setPlot(tvdbSeries.getOverview(), SOURCE_TVDB);
        }

        if (OverrideTools.checkOverwriteOutline(series, SOURCE_TVDB)) {
            series.setOutline(tvdbSeries.getOverview(), SOURCE_TVDB);
        }

        series.addRating(SOURCE_TVDB, MetadataTools.parseRating(tvdbSeries.getRating()));

        if (OverrideTools.checkOverwriteYear(series, SOURCE_TVDB)) {
            String faDate = tvdbSeries.getFirstAired();
            if (StringUtils.isNotBlank(faDate) && (faDate.length() >= 4)) {
                try {
                    series.setStartYear(Integer.parseInt(faDate.substring(0, 4)), SOURCE_TVDB);
                } catch (Exception ignore) {
                    // ignore error if year is invalid
                }
            }
        }

        if (OverrideTools.checkOverwriteGenres(series, SOURCE_TVDB)) {
            series.setGenreNames(new LinkedHashSet<>(tvdbSeries.getGenres()), SOURCE_TVDB);
        }

        if (OverrideTools.checkOverwriteStudios(series, SOURCE_TVDB)) {
            String studioName = StringUtils.trimToNull(tvdbSeries.getNetwork());
            if (studioName != null) {
                Set<String> studioNames = Collections.singleton(studioName);
                series.setStudioNames(studioNames, SOURCE_TVDB);
            }
        }

        // ACTORS (to store in episodes)
        Set<CreditDTO> actors = null;
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            List<Actor> tvdbActors = tvdbApiWrapper.getActors(tvdbSeries.getId());
            if (CollectionUtils.isNotEmpty(tvdbActors)) {
                actors = new LinkedHashSet<>(tvdbActors.size());
                for (Actor actor : tvdbActors) {
                    final String sourceId = (actor.getId() > 0 ? Integer.toString(actor.getId()) : null);
                    CreditDTO credit = this.identifierService.createCredit(SOURCE_TVDB,sourceId, JobType.ACTOR, actor.getName(), actor.getRole());
                    if (credit != null) {
                        actors.add(credit);
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

            if (!season.isTvSeasonDone(SOURCE_TVDB)) {
                // same as series id
                final String tvdbId = tvdbSeries.getId();
                season.setSourceDbId(SOURCE_TVDB, tvdbId);
                
                // use values from series
                if (OverrideTools.checkOverwriteTitle(season, SOURCE_TVDB)) {
                    season.setTitle(tvdbSeries.getSeriesName(), SOURCE_TVDB);
                }
                if (OverrideTools.checkOverwritePlot(season, SOURCE_TVDB)) {
                    season.setPlot(tvdbSeries.getOverview(), SOURCE_TVDB);
                }
                if (OverrideTools.checkOverwriteOutline(season, SOURCE_TVDB)) {
                    season.setOutline(tvdbSeries.getOverview(), SOURCE_TVDB);
                }
    
                if (OverrideTools.checkOverwriteYear(season, SOURCE_TVDB)) {
                    // get season year from minimal first aired of episodes
                    String year = tvdbApiWrapper.getSeasonYear(tvdbId, season.getSeason(), tvdbLocale.getLanguage());
                    season.setPublicationYear(MetadataTools.extractYearAsInt(year), SOURCE_TVDB);
                }
    
                // mark season as done
                season.setTvSeasonDone();
            }
            
            // scan episodes
            this.scanEpisodes(season, actors, tvdbLocale);
        }
    }

    private void scanEpisodes(Season season, Set<CreditDTO> actors, Locale tvdbLocale) {
        final String seriesId = season.getSeries().getSourceDbId(SOURCE_TVDB);
        
        for (VideoData videoData : season.getVideoDatas()) {
            if (videoData.isTvEpisodeDone(SOURCE_TVDB)) {
                // nothing to do if already done
                continue;
            }
            
            Episode tvdbEpisode = tvdbApiWrapper.getEpisode(seriesId, season.getSeason(), videoData.getEpisode(), tvdbLocale.getLanguage());
            if (tvdbEpisode == null || StringUtils.isBlank(tvdbEpisode.getId())) {
                // mark episode as not found
                videoData.removeOverrideSource(SOURCE_TVDB);
                videoData.removeSourceDbId(SOURCE_TVDB);
                videoData.setTvEpisodeNotFound();
                continue;
            }
            
            // set episode ID
            videoData.setSourceDbId(SOURCE_TVDB, tvdbEpisode.getId());
            // set IMDb id if not set before
            String imdbId = videoData.getSourceDbId(SOURCE_IMDB);
            if (StringUtils.isBlank(imdbId)) {
                videoData.setSourceDbId(SOURCE_IMDB, tvdbEpisode.getImdbId());
            }

            if (OverrideTools.checkOverwriteTitle(videoData, SOURCE_TVDB)) {
                videoData.setTitle(tvdbEpisode.getEpisodeName(), SOURCE_TVDB);
            }
            
            if (OverrideTools.checkOverwritePlot(videoData, SOURCE_TVDB)) {
                videoData.setPlot(tvdbEpisode.getOverview(), SOURCE_TVDB);
            }
            
            if (OverrideTools.checkOverwriteOutline(videoData, SOURCE_TVDB)) {
                videoData.setOutline(tvdbEpisode.getOverview(), SOURCE_TVDB);
            }

            if (OverrideTools.checkOverwriteReleaseDate(videoData, SOURCE_TVDB)) {
                Date releaseDate = MetadataTools.parseToDate(tvdbEpisode.getFirstAired());
                videoData.setRelease(releaseDate, SOURCE_TVDB);
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
                videoData.addCreditDTO(this.identifierService.createCredit(SOURCE_TVDB, jobType, person));
            }
        }
    }
    
    @Override
    public boolean scanNFO(String nfoContent, IdMap idMap) {
        boolean ignorePresentId = this.configServiceWrapper.getBooleanProperty("thetvdb.nfo.ignore.present.id", false);

        // scan for IMDb ID
        ImdbScanner.scanImdbID(nfoContent, idMap, ignorePresentId);

        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(idMap.getId(SOURCE_TVDB))) {
            return true;
        }

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
                        idMap.addId(SOURCE_TVDB, sourceId);
                        LOG.debug("TheTVDB ID found in NFO: {}", sourceId);
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
