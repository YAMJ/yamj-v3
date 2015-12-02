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

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;

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
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.web.apis.TVRageApiWrapper;

import com.omertron.tvrageapi.model.CountryDetail;
import com.omertron.tvrageapi.model.Episode;
import com.omertron.tvrageapi.model.EpisodeList;
import com.omertron.tvrageapi.model.ShowInfo;

@Service("tvRageScanner")
public class TVRageScanner implements ISeriesScanner {

    public static final String SCANNER_ID = "tvrage";
    private static final Logger LOG = LoggerFactory.getLogger(TVRageScanner.class);

    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private TVRageApiWrapper tvRageApiWrapper;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize TVRage scanner");

        // register this scanner
        onlineScannerService.registerMetadataScanner(this);
    }

    @Override
    public String getSeriesId(Series series) {
        return getSeriesId(series, false);
    }

    private String getSeriesId(Series series, boolean throwTempError) {
        String tvRageId = series.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(tvRageId)) {
            return tvRageId;
        }

        ShowInfo showInfo = null;
        if (StringUtils.isNotBlank(tvRageId)) {
            // try by vanity URL
            showInfo = tvRageApiWrapper.getShowInfoByVanityURL(tvRageId, throwTempError);
        }
        
        // try by title
        if (showInfo == null || !showInfo.isValid()) {
            showInfo = tvRageApiWrapper.getShowInfoByTitle(series.getTitle(), throwTempError);
        }

        // try by original title
        if ((showInfo == null || !showInfo.isValid()) && series.isTitleOriginalScannable()) {
            showInfo = tvRageApiWrapper.getShowInfoByTitle(series.getTitleOriginal(), throwTempError);
        }

        if (showInfo != null && showInfo.isValid() && showInfo.getShowID()>0) {
            tvRageId = Integer.toString(showInfo.getShowID());
            series.setSourceDbId(SCANNER_ID, tvRageId);
            return tvRageId;
        }
        
        return null;
    }

    @Override
    public ScanResult scanSeries(Series series) {
        final Locale tvRageLocale = localeService.getLocaleForConfig("tvrage");
        ShowInfo showInfo = null;
        EpisodeList episodeList = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("tvrage.throwError.tempUnavailable", Boolean.TRUE);
            String tvRageId = getSeriesId(series, throwTempError); 

            if (!StringUtils.isNumeric(tvRageId)) {
                LOG.debug("TVRage id not available '{}'", series.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            showInfo = tvRageApiWrapper.getShowInfo(tvRageId, throwTempError);
            episodeList = tvRageApiWrapper.getEpisodeList(tvRageId, throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = this.configServiceWrapper.getIntProperty("tvrage.maxRetries.tvshow", 0);
            if (series.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }

        if (showInfo == null || !showInfo.isValid()) {
            LOG.error("Can't find informations for series '{}'", series.getIdentifier());
            return ScanResult.NO_RESULT;
        }

        String title = showInfo.getShowName();
        if (showInfo.getAkas() != null) {
            // try AKAs for title in another country
            loop: for (CountryDetail cd : showInfo.getAkas()) {
                if (tvRageLocale.getCountry().equals(cd.getCountry())) {
                    title = cd.getDetail();
                    break loop;
                }
            }
        }
        
        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            series.setTitle(title, SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(series, SCANNER_ID)) {
            series.setTitleOriginal(showInfo.getShowName(), SCANNER_ID);
        }
        
        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(showInfo.getSummary(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(showInfo.getSummary(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            series.setGenreNames(showInfo.getGenres(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
            series.setStartYear(showInfo.getStarted(), SCANNER_ID);
            series.setEndYear(MetadataTools.extractYearAsInt(showInfo.getEnded()), SCANNER_ID);
        }

        if (showInfo.getNetwork() != null && OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            Set<String> studioNames = new HashSet<>();
            for (CountryDetail cd : showInfo.getNetwork()) {
                if (StringUtils.isNotBlank(cd.getDetail())) {
                    studioNames.add(cd.getDetail());
                }
            }
            series.setStudioNames(studioNames, SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteCountries(series, SCANNER_ID)) {
            String countryCode = localeService.findCountryCode(showInfo.getOriginCountry());
            if (countryCode != null) {
                Set<String> countryCodes = Collections.singleton(countryCode);
                series.setCountryCodes(countryCodes, SCANNER_ID);
            }
        }
        
        scanSeasons(series, showInfo, title, episodeList);
        
        return ScanResult.OK;
    }
    
    private static void scanSeasons(Series series, ShowInfo showInfo, String title, EpisodeList episodeList) {
        
        for (Season season : series.getSeasons()) {
            
            if (!season.isTvSeasonDone(SCANNER_ID)) {
                // same as series id
                final String tvRageId = Integer.toString(showInfo.getShowID());
                season.setSourceDbId(SCANNER_ID, tvRageId);
                
                // use values from series
                if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                    season.setTitle(title, SCANNER_ID);
                }
                if (OverrideTools.checkOverwriteOriginalTitle(series, SCANNER_ID)) {
                    series.setTitleOriginal(showInfo.getShowName(), SCANNER_ID);
                }
                if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                    season.setPlot(showInfo.getSummary(), SCANNER_ID);
                }
                if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                    season.setOutline(showInfo.getSummary(), SCANNER_ID);
                }
    
                if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                    // get season year from minimal first aired of episodes
                    Episode episode = episodeList.getEpisode(season.getSeason(), 1);
                    if (episode != null && episode.getAirDate() != null) {
                        season.setPublicationYear(MetadataTools.extractYearAsInt(episode.getAirDate()), SCANNER_ID);
                    }
                }
    
                // mark season as done
                season.setTvSeasonDone();
            }
            
            // scan episodes
            scanEpisodes(season, episodeList);
        }
    }

    private static void scanEpisodes(Season season, EpisodeList episodeList) {
        for (VideoData videoData : season.getVideoDatas()) {
            
            if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                // nothing to do if already done
                continue;
            }

            // get the episode
            Episode episode = episodeList.getEpisode(season.getSeason(), videoData.getEpisode());
            if (episode == null || !episode.isValid()) {
                // mark episode as not found
                videoData.removeOverrideSource(SCANNER_ID);
                videoData.removeSourceDbId(SCANNER_ID);
                videoData.setTvEpisodeNotFound();
                continue;
            }
            
            try {
                int lastIdx = StringUtils.lastIndexOf(episode.getLink(), "/");
                if (lastIdx > 0) {
                    String tvRageId = episode.getLink().substring(lastIdx+1);
                    videoData.setSourceDbId(SCANNER_ID, tvRageId);
                }   
            } catch (Exception ex) {/*ignore*/}
            
            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                videoData.setTitle(episode.getTitle(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                videoData.setPlot(episode.getSummary(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                videoData.setRelease(episode.getAirDate(), SCANNER_ID);
            }
            
            videoData.addRating(SCANNER_ID, MetadataTools.parseRating(episode.getRating()));
            
            // mark episode as done
            videoData.setTvEpisodeDone();
        }
    }
    
    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNumeric(dto.getId(SCANNER_ID))) {
            return Boolean.TRUE;
        }

        // There are two formats for the URL. The first is a vanity URL with the show name in it,
        // http://www.tvrage.com/House
        // the second is an id based URL
        // http://www.tvrage.com/shows/id-22771

        LOG.trace("Scanning NFO for TVRage ID");
        
        try {
            String text = "/shows/";
            int beginIndex = nfoContent.indexOf(text);
            if (beginIndex > -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + text.length()), "/ \n,:!&é\"'(è_çà)=$");
                // Remove the "id-" from the front of the ID
                String id = st.nextToken().substring("id-".length());
                LOG.debug("TVRage ID found in NFO: {}", id);
                dto.addId(SCANNER_ID, id);
                return Boolean.TRUE;
            }

            text = "tvrage.com/";
            beginIndex = nfoContent.indexOf(text);
            if (beginIndex > -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + text.length()), "/ \n,:!&\"'=$");
                String id = st.nextToken();
                LOG.debug("TVRage vanity ID found in NFO: {}", id);
                dto.addId(SCANNER_ID, id);
                return Boolean.TRUE;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No TVRage ID found in NFO");
        return Boolean.FALSE;
    }
}
