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

import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.TvDbException;
import com.omertron.thetvdbapi.model.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigService;
import org.yamj.core.config.LocaleService;
import org.yamj.core.web.ResponseTools;

@Service
public class TheTVDbApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbApiWrapper.class);
    private static final int YEAR_MIN = 1900;
    private static final int YEAR_MAX = 2050;
    private final Lock seriesLock = new ReentrantLock(true);
    private final Lock bannersLock = new ReentrantLock(true);

    @Autowired
    private Cache tvdbCache;
    @Autowired
    private ConfigService configService;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private TheTVDBApi tvdbApi;
    
    public Banners getBanners(String id) {
        String cacheKey = "banners###"+id;
        Banners banners = tvdbCache.get(cacheKey, Banners.class);
        
        if (banners == null) {
            bannersLock.lock();
            try {
                // retrieve banners from TheTVDb
                banners = tvdbApi.getBanners(id);
                tvdbCache.putIfAbsent(cacheKey, banners);
            } catch (TvDbException ex) {
                LOG.error("Failed to get banners using TVDb ID {}: {}", id, ex.getMessage());
                LOG.trace("TheTVDb error" , ex);
            } finally {
                bannersLock.unlock();
            }
        }
        
        return (banners == null ? new Banners() : banners);
    }

    /**
     * Get series information using the ID
     *
     * @param id
     * @return
     */
    public Series getSeries(String id) {
        return getSeries(id, false);
    }
        
    /**
     * Get series information using the ID
     *
     * @param throwTempError
     * @return
     */
    public Series getSeries(String id, boolean throwTempError) {
        String cacheKey = "series###"+id;
        Series series = tvdbCache.get(cacheKey, Series.class);

        if (series == null) {
            seriesLock.lock();
            try {
                String defaultLanguage = localeService.getLocaleForConfig("thetvdb").getLanguage();
                String altLanguage = configService.getProperty("thetvdb.language.alternate", StringUtils.EMPTY);

                // retrieve series from TheTVDb
                series = tvdbApi.getSeries(id, defaultLanguage);
                if (series == null && StringUtils.isNotBlank(altLanguage)) {
                    series = tvdbApi.getSeries(id, altLanguage);
                }
                if (series == null) {
                    // build just for caching if nothing found
                    series = new Series();
                }

                tvdbCache.putIfAbsent(cacheKey, series);
            } catch (TvDbException ex) {
                if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                    throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
                }
                LOG.error("Failed to get series using TVDb ID {}: {}", id, ex.getMessage());
                LOG.trace("TheTVDb error" , ex);
                series = new Series();
            } finally {
                seriesLock.unlock();
            }
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
    public String getSeriesId(String title, int year, boolean throwTempError) {
        String tvdbId = null;
        if (StringUtils.isNotBlank(title)) {
            seriesLock.lock();
            try {
                String defaultLanguage = localeService.getLocaleForConfig("thetvdb").getLanguage();
                String altLanguage = configService.getProperty("thetvdb.language.alternate", StringUtils.EMPTY);

                boolean usedDefault = true;
                List<Series> seriesList = tvdbApi.searchSeries(title, defaultLanguage);

                if (CollectionUtils.isEmpty(seriesList) && StringUtils.isNotBlank(altLanguage)) {
                    seriesList = tvdbApi.searchSeries(title, altLanguage);
                    usedDefault = false;
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
                        Series gotSeries = tvdbApi.getSeries(tvdbId, usedDefault ? defaultLanguage : altLanguage);
                        if (gotSeries != null) {
                            tvdbCache.put("series###"+tvdbId, gotSeries);
                        }
                    }
                }
            } catch (TvDbException ex) {
                if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                    throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
                }
                LOG.error("Failed retrieving TVDb id for series '{}': {}", title, ex.getMessage());
                LOG.trace("TheTVDb error" , ex);
            } finally {
                seriesLock.unlock();
            }
        }
        return tvdbId;
    }

    public List<Actor> getActors(String id, boolean throwTempError) {
        try {
            return tvdbApi.getActors(id);
        } catch (TvDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheTVDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get actors using TVDb ID {}: {}", id, ex.getMessage());
            LOG.trace("TheTVDb error" , ex);
        }
        return null;
    }

    public List<Episode> getSeasonEpisodes(String id, int season) {
        String cacheKey = ("episodes###" + id + "###" + season);
        List<Episode> episodeList = tvdbCache.get(cacheKey, List.class);
        
        if (episodeList == null) {
            try {
                String defaultLanguage = localeService.getLocaleForConfig("thetvdb").getLanguage();
                String altLanguage = configService.getProperty("thetvdb.language.alternate", StringUtils.EMPTY);
    
                episodeList = tvdbApi.getSeasonEpisodes(id, season, defaultLanguage);
    
                if (CollectionUtils.isEmpty(episodeList) && StringUtils.isNotBlank(altLanguage)) {
                    episodeList = tvdbApi.getSeasonEpisodes(id, season, altLanguage);
                }
                
                if (CollectionUtils.isNotEmpty(episodeList)) {
                    this.tvdbCache.put(cacheKey, episodeList);
                }
            } catch (TvDbException ex) {
                LOG.error("Failed to get episodes for TVDb ID {} and season {}: {}", id, season, ex.getMessage());
                LOG.trace("TheTVDb error" , ex);
            }
        }
        
        return episodeList;
    }
}
