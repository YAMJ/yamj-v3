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
package org.yamj.core.web.apis;

import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.TvDbException;
import com.omertron.thetvdbapi.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.core.CachingNames;
import org.yamj.core.config.ConfigService;
import org.yamj.core.service.metadata.online.TemporaryUnavailableException;
import org.yamj.core.web.ResponseTools;

@Service
public class TheTVDbApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbApiWrapper.class);
    private static final int YEAR_MIN = 1900;
    private static final int YEAR_MAX = 2050;
    private final Lock bannersLock = new ReentrantLock(true);

    @Autowired
    private ConfigService configService;
    @Autowired
    private TheTVDBApi tvdbApi;
    
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id}")
    public Banners getBanners(String id) {
        Banners banners = null;
        bannersLock.lock();
        
        try {
            // retrieve banners from TheTVDb
            banners = tvdbApi.getBanners(id);
        } catch (TvDbException ex) {
            LOG.error("Failed to get banners using TVDb ID {}: {}", id, ex.getMessage());
            LOG.trace("TheTVDb error" , ex);
        } finally {
            bannersLock.unlock();
        }
        
        return (banners == null ? new Banners() : banners);
    }

    /**
     * Get series information using the ID
     *
     * @param id
     * @return
     */
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id, #language}")
    public Series getSeries(String id, String language) {
        return getSeries(id, language, false);
    }
    
    /**
     * Get series information using the ID
     *
     * @param throwTempError
     * @return
     */
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id, #language}")
    public Series getSeries(String id, String language, boolean throwTempError) {
        Series series = null;

        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", StringUtils.EMPTY);
            if (altLanguage.equalsIgnoreCase(language)) altLanguage = null;

            // retrieve series from TheTVDb
            series = tvdbApi.getSeries(id, language);
            if (series == null && StringUtils.isNotBlank(altLanguage)) {
                series = tvdbApi.getSeries(id, altLanguage);
            }
        } catch (TvDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get series using TVDb ID {}: {}", id, ex.getMessage());
            LOG.trace("TheTVDb error" , ex);
        }
        
        return (series == null ? new Series() : series);
    }

    /**
     * Get the Series ID by title and year
     *
     * @param title
     * @param year
     * @return
     */
    public String getSeriesId(String title, int year, String language, boolean throwTempError) {
        String tvdbId = null;

        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", StringUtils.EMPTY);
            if (altLanguage.equalsIgnoreCase(language)) altLanguage = null;
            
            List<Series> seriesList = tvdbApi.searchSeries(title, language);
            if (CollectionUtils.isEmpty(seriesList) && StringUtils.isNotBlank(altLanguage)) {
                seriesList = tvdbApi.searchSeries(title, altLanguage);
            }

            if (CollectionUtils.isNotEmpty(seriesList)) {
                Series series = null;
                for (Series s : seriesList) {
                    if (s.getFirstAired() != null && !s.getFirstAired().isEmpty() && (year > YEAR_MIN && year < YEAR_MAX)) {
                        DateTime firstAired = DateTime.parse(s.getFirstAired());
                        firstAired.getYear();
                        if (firstAired.getYear() == year) {
                            series = s;
                            break;
                        }
                    } else {
                        series = s;
                        break;
                    }
                }

                if (series != null) {
                    tvdbId = series.getId();
                }
            }
        } catch (TvDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving TVDb id for series '{}': {}", title, ex.getMessage());
            LOG.trace("TheTVDb error" , ex);
        }
        
        return (tvdbId == null ? StringUtils.EMPTY : tvdbId);
    }

    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id}")
    public List<Actor> getActors(String id, boolean throwTempError) {
        List<Actor> actorList = null;
        
        try {
            actorList = tvdbApi.getActors(id);
        } catch (TvDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get actors using TVDb ID {}: {}", id, ex.getMessage());
            LOG.trace("TheTVDb error" , ex);
        }
        
        return (actorList == null ? new ArrayList<Actor>() : actorList);
    }

    @Deprecated
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id, #season, #language}")
    public List<Episode> getSeasonEpisodes(String id, int season, String language, boolean throwTempError) {
        List<Episode> episodeList = null;

        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", StringUtils.EMPTY);
            if (altLanguage.equalsIgnoreCase(language)) altLanguage = null;

            episodeList = tvdbApi.getSeasonEpisodes(id, season, language);
            if (CollectionUtils.isEmpty(episodeList) && StringUtils.isNotBlank(altLanguage)) {
                episodeList = tvdbApi.getSeasonEpisodes(id, season, altLanguage);
            }
        } catch (TvDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get episodes for TVDb ID {} and season {}: {}", id, season, ex.getMessage());
            LOG.trace("TheTVDb error" , ex);
        }
        
        return (episodeList == null ? new ArrayList<Episode>() : episodeList);
    }

    public Episode getEpisode(String id, int season, int episode, String language, boolean throwTempError) {
        Episode tvdbEpisode = null;
        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", StringUtils.EMPTY);
            if (altLanguage.equalsIgnoreCase(language)) altLanguage = null;

            tvdbEpisode = tvdbApi.getEpisode(id, season, episode, language);
            if (tvdbEpisode == null && StringUtils.isNotBlank(altLanguage)) {
                tvdbEpisode = tvdbApi.getEpisode(id, season, episode, altLanguage);
            }
        } catch (TvDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get episode {} for TVDb ID {} and season {} : {}", episode, id, season, ex.getMessage());
            LOG.trace("TheTVDb error" , ex);
        }
        
        return tvdbEpisode;
    }

    public Episode getEpisode(String id, String language, boolean throwTempError) {
        Episode tvdbEpisode = null;
        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", StringUtils.EMPTY);
            if (altLanguage.equalsIgnoreCase(language)) altLanguage = null;

            tvdbEpisode = tvdbApi.getEpisodeById(id, language);
            if (tvdbEpisode == null && StringUtils.isNotBlank(altLanguage)) {
                tvdbEpisode = tvdbApi.getEpisodeById(id, altLanguage);
            }
        } catch (TvDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get episode for TVDb ID {}: {}", id, ex.getMessage());
            LOG.trace("TheTVDb error" , ex);
        }
        
        return (tvdbEpisode == null ? new Episode() : tvdbEpisode);
    }
}
