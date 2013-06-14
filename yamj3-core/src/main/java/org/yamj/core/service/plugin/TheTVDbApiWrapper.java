package org.yamj.core.service.plugin;

import java.util.Collections;

import org.joda.time.DateTime;

import org.apache.commons.collections.CollectionUtils;

import com.omertron.thetvdbapi.model.Episode;

import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Actor;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.Series;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.tools.LRUTimedCache;

@Service("tvdbApiWrapper")
public class TheTVDbApiWrapper {

    private static final String DEFAULT_LANGUAGE = PropertyTools.getProperty("thetvdb.language", "en");
    private static final String ALTERNATE_LANGUAGE = PropertyTools.getProperty("thetvdb.language.alternate", "");
    private static final int YEAR_MIN = 1900;
    private static final int YEAR_MAX = 2050;

    @Autowired
    private TheTVDBApi tvdbApi;

    // make maximal 20 banners objects maximal 30 minutes accessible
    private Lock bannersLock = new ReentrantLock(true);
    private LRUTimedCache<String, Banners> bannersCache = new LRUTimedCache<String, Banners>(20, 1800);
    // make maximal 50 series objects maximal 30 minutes accessible
    private Lock seriesLock = new ReentrantLock(true);
    private LRUTimedCache<String, Series> seriesCache = new LRUTimedCache<String, Series>(50, 1800);
    // make maximal 30 episode lists maximal 30 minutes accessible
    private LRUTimedCache<String, List<Episode>> episodesCache = new LRUTimedCache<String, List<Episode>>(30, 1800);

    
    public Banners getBanners(String id) {
        Banners banners = bannersCache.get(id);
        if (banners == null) {
            bannersLock.lock();
            try {
                // second try cause meanwhile the cache could have been filled
                banners = bannersCache.get(id);
                if (banners == null) {
                    // retrieve banners from TheTVDb
                    banners = tvdbApi.getBanners(id);
                    bannersCache.put(id, banners);
                }
            } finally {
                bannersLock.unlock();
            }
        }
        return banners;
    }

    public Series getSeries(String id) {
        Series series = seriesCache.get(id);
        if (series == null) {
            seriesLock.lock();
            try {
                // second try cause meanwhile the cache could have been filled
                series = seriesCache.get(id);
                if (series == null) {
                    // retrieve series from TheTVDb
                    tvdbApi.getSeries(id, DEFAULT_LANGUAGE);
                    if (series == null && StringUtils.isNotBlank(ALTERNATE_LANGUAGE)) {
                        series = tvdbApi.getSeries(id, ALTERNATE_LANGUAGE);
                    }
                    if (series == null) {
                        // have a valid series object with empty values
                        series = new com.omertron.thetvdbapi.model.Series();
                    }
                    seriesCache.put(id, series);
                }
            } finally {
                seriesLock.unlock();
            }
        }
        return series;
    }

    public String getSeriesId(String title, int year) {
        String id = "";
        if (StringUtils.isNotBlank(title)) {
            List<Series> seriesList = tvdbApi.searchSeries(title, DEFAULT_LANGUAGE);
            if (CollectionUtils.isEmpty(seriesList) && StringUtils.isNotBlank(ALTERNATE_LANGUAGE)) {
                seriesList = tvdbApi.searchSeries(title, ALTERNATE_LANGUAGE);
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
                    id = series.getId();
                    this.seriesCache.put(id, series);
                }
            }
        }
        return id;
    }

    public List<Actor> getActors(String id) {
        return tvdbApi.getActors(id);
    }

    public List<Episode> getSeasonEpisodes(String id, int season) {
        String key = (id + "###" + season);
        
        List<Episode> episodeList = this.episodesCache.get(key);
        if (episodeList == null) {
            episodeList = tvdbApi.getSeasonEpisodes(id, season, DEFAULT_LANGUAGE);
            if (CollectionUtils.isEmpty(episodeList) && StringUtils.isNotBlank(ALTERNATE_LANGUAGE)) {
                episodeList = tvdbApi.getSeasonEpisodes(id, season, ALTERNATE_LANGUAGE);
            }
            if (episodeList == null) {
                episodeList = Collections.emptyList();
            }
            this.episodesCache.put(key, episodeList);
        }
        return episodeList;
    }
}
