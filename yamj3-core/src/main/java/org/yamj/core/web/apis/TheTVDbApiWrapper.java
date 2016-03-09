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
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.CachingNames;
import org.yamj.core.config.ConfigService;
import org.yamj.core.web.TemporaryUnavailableException;

@Service
public class TheTVDbApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbApiWrapper.class);
    private static final int YEAR_MIN = 1900;
    private static final int YEAR_MAX = 2100;
    private static final String API_ERROR = "TheTVDb error";
    
    @Autowired
    private ConfigService configService;
    @Autowired
    private TheTVDBApi tvdbApi;
    
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id}", unless="#result==null")
    public Banners getBanners(String id) {
        Banners banners = null;
        
        try {
            // retrieve banners from TheTVDb
            banners = tvdbApi.getBanners(id);
        } catch (Exception ex) {
            LOG.error("Failed to get banners using TVDb ID {}: {}", id, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        
        return banners;
    }

    /**
     * Get series information using the ID
     *
     * @param id
     * @return
     */
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id, #language}", unless="#result==null")
    public Series getSeries(String id, String language) {
        return getSeries(id, language, false);
    }
    
    /**
     * Get series information using the ID
     *
     * @param throwTempError
     * @return
     */
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id, #language}", unless="#result==null")
    public Series getSeries(String id, String language, boolean throwTempError) {
        Series series = null;

        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", language);

            // retrieve series from TheTVDb
            series = tvdbApi.getSeries(id, language);
            if (series == null && !altLanguage.equalsIgnoreCase(language)) {
                series = tvdbApi.getSeries(id, altLanguage);
            }
        } catch (TvDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get series using TVDb ID {}: {}", id, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        
        return series;
    }

    /**
     * Get the Series ID by title and year
     *
     * @param title
     * @param year
     * @return
     */
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #title.toLowerCase(), #year, #language}")
    public String getSeriesId(String title, int year, String language, boolean throwTempError) {
        String tvdbId = null;

        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", language);
            
            List<Series> seriesList = tvdbApi.searchSeries(title, language);
            if (CollectionUtils.isEmpty(seriesList) && !altLanguage.equalsIgnoreCase(language)) {
                seriesList = tvdbApi.searchSeries(title, altLanguage);
            }

            if (CollectionUtils.isEmpty(seriesList)) {
                return StringUtils.EMPTY;
            }
            
            tvdbId = getMatchingSeries(seriesList, year).getId();
        } catch (TvDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving TVDb id for series '{}': {}", title, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        
        return (tvdbId == null) ? StringUtils.EMPTY : tvdbId;
    }

    private static Series getMatchingSeries(List<Series> seriesList, int year) {
        for (Series s : seriesList) {
            if (s.getFirstAired() != null && !s.getFirstAired().isEmpty() && (year > YEAR_MIN && year < YEAR_MAX)) {
                DateTime firstAired = DateTime.parse(s.getFirstAired());
                firstAired.getYear();
                if (firstAired.getYear() == year) {
                    return s;
                }
            } else {
                return s;
            }
        }
        return new Series();
    }
    
    public List<Actor> getActors(String id) {
        try {
            return tvdbApi.getActors(id);
        } catch (Exception ex) {
            LOG.error("Failed to get actors using TVDb ID {}: {}", id, ex.getMessage());
            LOG.trace(API_ERROR, ex);
            return null;
        }
    }

    public String getSeasonYear(String id, int season, String language) {
        String year = null;
        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", language);

            year = tvdbApi.getSeasonYear(id, season, language);
            if (StringUtils.isBlank(year) && !altLanguage.equalsIgnoreCase(language)) {
                year = tvdbApi.getSeasonYear(id, season, altLanguage);
            }
        } catch (Exception ex) {
            LOG.error("Failed to get season year for TVDb ID {} and season {}: {}", id, season, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        
        return year;
    }
        
    @Cacheable(value=CachingNames.API_TVDB, key="{#root.methodName, #id, #season, #episode, #language}", unless="#result==null")
    public Episode getEpisode(String id, int season, int episode, String language) {
        Episode tvdbEpisode = null;
        
        try {
            String altLanguage = configService.getProperty("thetvdb.language.alternate", language);

            tvdbEpisode = tvdbApi.getEpisode(id, season, episode, language);
            if (tvdbEpisode == null && !altLanguage.equalsIgnoreCase(language)) {
                tvdbEpisode = tvdbApi.getEpisode(id, season, episode, altLanguage);
            }
        } catch (Exception ex) {
            LOG.error("Failed to get episode {} for TVDb ID {} and season {}: {}", episode, id, season, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        
        return tvdbEpisode;
    }

    private static void checkTempError(boolean throwTempError, TvDbException ex) {
        if (throwTempError && ResponseTools.isTemporaryError(ex)) {
            throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
        }
    }
}
