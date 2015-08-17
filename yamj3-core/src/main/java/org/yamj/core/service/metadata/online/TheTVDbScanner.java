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
import org.yamj.core.database.model.type.JobType;
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
        LOG.info("Initialize TheTVDb scanner");
        
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
        if (StringUtils.isBlank(tvdbId)) {
            tvdbId = tvdbApiWrapper.getSeriesId(series.getTitle(), series.getStartYear(), tvdbLocale.getLanguage(), throwTempError);
            series.setSourceDbId(SCANNER_ID, tvdbId);
        }
        return tvdbId;
    }

    @Override
    public String getSeasonId(Season season) {
        String tvdbId = season.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(tvdbId)) {
            // same as series id
            tvdbId = season.getSeries().getSourceDbId(SCANNER_ID);
            season.setSourceDbId(SCANNER_ID, tvdbId);
        }
        return  tvdbId;
    }

    @Override
    public String getEpisodeId(VideoData videoData) {
        Locale tvdbLocale = localeService.getLocaleForConfig("thetvdb");
        return getEpisodeId(videoData, tvdbLocale, false);
    }

    private String getEpisodeId(VideoData videoData, Locale tvdbLocale, boolean throwTempError) {
        String allocineId = videoData.getSourceDbId(SCANNER_ID);
        
        if (StringUtils.isBlank(allocineId)) {
            // NOTE: seriesId = seasonId
            String seasonId = videoData.getSeason().getSourceDbId(SCANNER_ID);
            if (StringUtils.isNotBlank(seasonId)) {
                final int seasonNumber = videoData.getSeason().getSeason();
                final int episodeNumber = videoData.getEpisode();
                Episode tvdbEpisode = tvdbApiWrapper.getEpisode(seasonId, seasonNumber, episodeNumber, tvdbLocale.getLanguage(), throwTempError);
                allocineId = tvdbEpisode.getId();
                videoData.setSourceDbId(SCANNER_ID, allocineId);
            }
          
        }

        return allocineId;
    }
    
    @Override
    public ScanResult scanSeries(Series series) {
        Locale tvdbLocale = localeService.getLocaleForConfig("thetvdb");
        com.omertron.thetvdbapi.model.Series tvdbSeries = null;
        List<Actor> tvdbActors = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("thetvdb.throwError.tempUnavailable", Boolean.TRUE);
            String tvdbId = getSeriesId(series, tvdbLocale, throwTempError); 

            if (StringUtils.isBlank(tvdbId)) {
                LOG.debug("TVDb id not available '{}'", series.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            tvdbSeries = tvdbApiWrapper.getSeries(tvdbId, tvdbLocale.getLanguage(), throwTempError);
            tvdbActors = tvdbApiWrapper.getActors(tvdbSeries.getId(), throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("thetvdb.maxRetries.tvshow", 0);
            if (series.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        
        if (StringUtils.isBlank(tvdbSeries.getId())) {
            LOG.error("Can't find informations for series '{}'", series.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        series.setSourceDbId(SCANNER_ID, tvdbSeries.getId());
        series.setSourceDbId(ImdbScanner.SCANNER_ID, tvdbSeries.getImdbId());

        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            series.setTitle(tvdbSeries.getSeriesName(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(tvdbSeries.getOverview(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(tvdbSeries.getOverview(), SCANNER_ID);
        }

        if (StringUtils.isNumeric(tvdbSeries.getRating())) {
            try {
                series.addRating(SCANNER_ID, (int) (Float.parseFloat(tvdbSeries.getRating()) * 10));
            } catch (NumberFormatException nfe) {
                LOG.warn("Failed to convert TVDB rating '{}' to an integer: {}", tvdbSeries.getRating(), nfe.getMessage());
            }
        }

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

        // CAST & CREW
        Set<CreditDTO> actors = new LinkedHashSet<>();
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            for (Actor actor : tvdbActors) {
                actors.add(new CreditDTO(SCANNER_ID, JobType.ACTOR, actor.getName(), actor.getRole()));
            }
        }
        
        // SCAN SEASONS
        this.scanSeasons(series, tvdbSeries, actors, tvdbLocale);

        return ScanResult.OK;
    }

    @Override
    public ScanResult scanSeason(Season season) {
        Locale tvdbLocale = localeService.getLocaleForConfig("thetvdb");
        com.omertron.thetvdbapi.model.Series tvdbSeries = null;
        Episode tvdbEpisode = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("thetvdb.throwError.tempUnavailable", Boolean.TRUE);
            // NOTE: same as seriesId
            String seasonId = this.getSeasonId(season);

            if (StringUtils.isBlank(seasonId)) {
                LOG.debug("TVDb id not available '{}'", season.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            tvdbSeries = tvdbApiWrapper.getSeries(seasonId, tvdbLocale.getLanguage(), throwTempError);
            tvdbEpisode = tvdbApiWrapper.getEpisode(tvdbSeries.getId(), season.getSeason(), 1, tvdbLocale.getLanguage(), throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("thetvdb.maxRetries.tvshow", 0);
            if (season.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }

        if (StringUtils.isBlank(tvdbSeries.getId())) {
            LOG.error("Can't find informations for season '{}'", season.getIdentifier());
            return ScanResult.ERROR;
        }

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

        if (tvdbEpisode != null && OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
            int year = MetadataTools.extractYearAsInt(tvdbEpisode.getFirstAired());
            season.setPublicationYear(year, SCANNER_ID);
        }

        return ScanResult.OK;
    }

    @Override
    public ScanResult scanEpisode(VideoData videoData) {
        final Locale tvdbLocale = localeService.getLocaleForConfig("thetvdb");
        Episode tvdbEpisode = null;
        List<Actor> actors = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("thetvdb.throwError.tempUnavailable", Boolean.TRUE);
            // NOTE: seriesId = seasonId
            String seasonId = videoData.getSeason().getSourceDbId(SCANNER_ID);

            if (StringUtils.isBlank(seasonId)) {
                LOG.debug("TVDb id not available '{}'", videoData.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            String tvdbId = videoData.getSourceDbId(SCANNER_ID);
            if (StringUtils.isBlank(tvdbId)) {
                final int seasonNumber = videoData.getSeason().getSeason();
                final int episodeNumber = videoData.getEpisode();
                tvdbEpisode = tvdbApiWrapper.getEpisode(seasonId, seasonNumber, episodeNumber, tvdbLocale.getLanguage(), throwTempError);
            } else {
                tvdbEpisode = tvdbApiWrapper.getEpisode(tvdbId,  tvdbLocale.getLanguage(), throwTempError);
            }
            
            // get season actors
            actors = tvdbApiWrapper.getActors(seasonId, throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("thetvdb.maxRetries.tvshow", 0);
            if (videoData.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }

        if (tvdbEpisode == null || StringUtils.isBlank(tvdbEpisode.getId())) {
            LOG.error("Can't find informations for episode '{}'", videoData.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        videoData.setSourceDbId(SCANNER_ID, tvdbEpisode.getId());

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

        // actors
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            for (Actor actor : actors) {
                videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.ACTOR, actor.getName(), actor.getRole()));
            }
        }

        // directors
        if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
            for (String director : tvdbEpisode.getDirectors()) {
                videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.DIRECTOR, director));
            }
        }
        
        // writers
        if (this.configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
            for (String writer : tvdbEpisode.getWriters()) {
                videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.WRITER, writer));
            }
        }

        // guest stars
        if (this.configServiceWrapper.isCastScanEnabled(JobType.GUEST_STAR)) {
            for (String guestStar : tvdbEpisode.getGuestStars()) {
                videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.GUEST_STAR, guestStar));
            }
        }

        return ScanResult.OK;
    }

    @Deprecated
    private void scanSeasons(Series series, com.omertron.thetvdbapi.model.Series tvdbSeries, Set<CreditDTO> actors, Locale tvdbLocale) {

        for (Season season : series.getSeasons()) {

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
                String seriesId = season.getSeries().getSourceDbId(SCANNER_ID);
                Date year = this.getSeasonYear(seriesId, season.getSeason(), tvdbLocale);
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
            this.scanEpisodes(season, actors, tvdbLocale);
        }
    }

    @Deprecated
    private Date getSeasonYear(String seriesId, int season, Locale tvdbLocale) {
        List<Episode> episodeList = tvdbApiWrapper.getSeasonEpisodes(seriesId, season, tvdbLocale.getLanguage(), false);
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
    
    @Deprecated
    private void scanEpisodes(Season season, Set<CreditDTO> actors, Locale tvdbLocale) {
        if (season.isTvEpisodesScanned(SCANNER_ID)) {
            // nothing to do anymore
            return;
        }

        String seriesId = season.getSeries().getSourceDbId(SCANNER_ID);
        List<Episode> episodeList = tvdbApiWrapper.getSeasonEpisodes(seriesId, season.getSeason(), tvdbLocale.getLanguage(), false);

        for (VideoData videoData : season.getVideoDatas()) {
            if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                // nothing to do if already done
                continue;
            }
            
            Episode episode = findEpisode(episodeList, season.getSeason(), videoData.getEpisode());
            if (episode == null) {
                // mark episode as not found
                videoData.setTvEpisodeNotFound();
            } else {
                videoData.setSourceDbId(SCANNER_ID, episode.getId());

                if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                    videoData.setTitle(episode.getEpisodeName(), SCANNER_ID);
                }
                
                if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                    videoData.setPlot(episode.getOverview(), SCANNER_ID);
                }
                
                if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                    videoData.setOutline(episode.getOverview(), SCANNER_ID);
                }

                if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                    Date releaseDate = MetadataTools.parseToDate(episode.getFirstAired());
                    videoData.setRelease(releaseDate, SCANNER_ID);
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
    @Deprecated
    private static Episode findEpisode(List<Episode> episodeList, int seasonNumber, int episodeNumber) {
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
