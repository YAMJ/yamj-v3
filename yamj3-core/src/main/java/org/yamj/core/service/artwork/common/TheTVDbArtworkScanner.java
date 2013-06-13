/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.artwork.common;

import com.omertron.thetvdbapi.TheTVDBApi;
import com.omertron.thetvdbapi.model.Banner;
import com.omertron.thetvdbapi.model.BannerType;
import com.omertron.thetvdbapi.model.Banners;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.IMetadata;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.artwork.fanart.ITvShowFanartScanner;
import org.yamj.core.service.artwork.poster.ITvShowPosterScanner;
import org.yamj.core.service.artwork.tv.ITvShowBannerScanner;
import org.yamj.core.service.plugin.TheTVDbScanner;
import org.yamj.core.tools.LRUTimedCache;

@Service("tvdbArtworkScanner")
public class TheTVDbArtworkScanner implements
    ITvShowPosterScanner, ITvShowFanartScanner, ITvShowBannerScanner, 
    InitializingBean
{

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbArtworkScanner.class);
    private static final String DEFAULT_LANGUAGE = PropertyTools.getProperty("thetvdb.language", "en");
    private static final String ALTERNATE_LANGUAGE = PropertyTools.getProperty("thetvdb.language.alternate", "");

    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private TheTVDBApi tvdbApi;
    @Autowired
    private TheTVDbScanner tvdbScanner;

    // make maximal 20 banners objects maximal 30 minutes accessible
    private Lock bannersLock = new ReentrantLock(true);
    private LRUTimedCache<String,Banners> bannersCache = new LRUTimedCache<String,Banners>(20, 1800); 
    // make maximal 50 series objects maximal 30 minutes accessible
    private Lock seriesLock = new ReentrantLock(true);
    private LRUTimedCache<String,com.omertron.thetvdbapi.model.Series> seriesCache = new LRUTimedCache<String,com.omertron.thetvdbapi.model.Series>(50, 1800); 
    
    @Override
    public String getScannerName() {
        return TheTVDbScanner.TVDB_SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() {
        // register this scanner
        artworkScannerService.registerTvShowPosterScanner(this);
        artworkScannerService.registerTvShowFanartScanner(this);
    }

    @Override
    public String getId(String title, int year, int season) {
        return tvdbScanner.getSeriesId(title, year);
    }
    
    @Override
    public List<ArtworkDetailDTO> getPosters(String title, int year, int season) {
        String id = this.getId(title, year, season);
        return this.getPosters(id, season);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String id, int season) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        
        List<ArtworkDetailDTO> dtos;
        if (season < 0) {
            // series
            dtos = this.getSeriesPosters(id);
        } else {
            // season
            dtos = this.getSeasonPosters(id, season);
        }
        return dtos;
    }

    private List<ArtworkDetailDTO> getSeriesPosters(String id) {
        List<ArtworkDetailDTO> langDTOs = new ArrayList<ArtworkDetailDTO>();
        List<ArtworkDetailDTO> altLangDTOs = new ArrayList<ArtworkDetailDTO>();
        List<ArtworkDetailDTO> noLangDTOs = new ArrayList<ArtworkDetailDTO>();

        // get series artwork
        Banners bannerList = getTVDbBanners(id);
        
        // find posters
        for (Banner banner : bannerList.getPosterList()) {
            if (banner.getBannerType2() == BannerType.Poster) {
                if (banner.getLanguage().equalsIgnoreCase(DEFAULT_LANGUAGE)) {
                    langDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isNotBlank(ALTERNATE_LANGUAGE) && banner.getLanguage().equalsIgnoreCase(ALTERNATE_LANGUAGE)) {
                    altLangDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isBlank(banner.getLanguage())) {
                    noLangDTOs.add(createArtworDetail(banner));
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Series {}: Found {} posters for language '{}'", id, langDTOs.size(), DEFAULT_LANGUAGE);
            if (StringUtils.isNotBlank(ALTERNATE_LANGUAGE)) {
                LOG.debug("Series {}: Found {} posters for alternate language '{}'", id, altLangDTOs.size(), ALTERNATE_LANGUAGE);
            }
            LOG.debug("Series {}: Found {} posters without language", id, noLangDTOs.size());
        }

        List<ArtworkDetailDTO> returnDTOs = null;
        if (langDTOs.size()>0) {
            LOG.info("Series {}: Using posters with language '{}'", id, DEFAULT_LANGUAGE);
            returnDTOs = langDTOs;
        } else if (altLangDTOs.size()>0) {
            LOG.info("Series {}: No poster found for language '{}', using posters with language '{}'", id, DEFAULT_LANGUAGE, ALTERNATE_LANGUAGE);
            returnDTOs = altLangDTOs;
        } else if (noLangDTOs.size()>0) {
            LOG.info("Series {}: No poster found for language '{}', using posters with no language", id, DEFAULT_LANGUAGE);
            returnDTOs = noLangDTOs;
        } else {
            String url = this.getTVDbSeries(id).getPoster();
            if (StringUtils.isNotBlank(url)) {
                LOG.info("Series {}: Using default series poster", id);
                returnDTOs = new ArrayList<ArtworkDetailDTO>(1);
                returnDTOs.add(new ArtworkDetailDTO(getScannerName(), url));
            }
        }

        return returnDTOs;
    }
        
    private List<ArtworkDetailDTO> getSeasonPosters(String id, int season) {
        List<ArtworkDetailDTO> langDTOs = new ArrayList<ArtworkDetailDTO>();
        List<ArtworkDetailDTO> altLangDTOs = new ArrayList<ArtworkDetailDTO>();
        List<ArtworkDetailDTO> noLangDTOs = new ArrayList<ArtworkDetailDTO>();

        // get series artwork
        Banners bannerList = getTVDbBanners(id);
        
        // find posters
        for (Banner banner : bannerList.getSeasonList()) {
            if ((banner.getSeason() == season) && (banner.getBannerType2() == BannerType.Season)) {
                if (banner.getLanguage().equalsIgnoreCase(DEFAULT_LANGUAGE)) {
                    langDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isNotBlank(ALTERNATE_LANGUAGE) && banner.getLanguage().equalsIgnoreCase(ALTERNATE_LANGUAGE)) {
                    altLangDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isBlank(banner.getLanguage())) {
                    noLangDTOs.add(createArtworDetail(banner));
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Season {}-{}: Found {} posters for language '{}'", id, season, langDTOs.size(), DEFAULT_LANGUAGE);
            if (StringUtils.isNotBlank(ALTERNATE_LANGUAGE)) {
                LOG.debug("Season {}-{}: Found {} posters for alternate language '{}'", id, season, altLangDTOs.size(), ALTERNATE_LANGUAGE);
            }
            LOG.debug("Season {}-{}: Found {} posters without language", id, season, noLangDTOs.size());
        }

        List<ArtworkDetailDTO> returnDTOs = null;
        if (langDTOs.size()>0) {
            LOG.info("Season {}-{}: Using posters with language '{}'", id, season, DEFAULT_LANGUAGE);
            returnDTOs = langDTOs;
        } else if (altLangDTOs.size()>0) {
            LOG.info("Season {}-{}: No poster found for language '{}', using posters with language '{}'", id, season, DEFAULT_LANGUAGE, ALTERNATE_LANGUAGE);
            returnDTOs = altLangDTOs;
        } else if (noLangDTOs.size()>0) {
            LOG.info("Season {}-{}: No poster found for language '{}', using posters with no language", id, season, DEFAULT_LANGUAGE);
            returnDTOs = noLangDTOs;
        } else if (CollectionUtils.isNotEmpty(bannerList.getPosterList())) {
            LOG.info("Season {}-{}: No poster found by language, using first series poster found", id, season);
            returnDTOs = new ArrayList<ArtworkDetailDTO>(1);
            Banner banner = bannerList.getPosterList().get(0);
            returnDTOs.add(createArtworDetail(banner));
        } else {
            String url = this.getTVDbSeries(id).getPoster();
            if (StringUtils.isNotBlank(url)) {
                LOG.info("Season {}-{}: Using default series poster", id, season);
                returnDTOs = new ArrayList<ArtworkDetailDTO>(1);
                returnDTOs.add(new ArtworkDetailDTO(getScannerName(), url));
            }
        }

        return returnDTOs;
    }
    
    @Override
    public List<ArtworkDetailDTO> getFanarts(String title, int year, int season) {
        String id = this.getId(title, year, season);
        return this.getFanarts(id, season);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(String id, int season) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        
        List<ArtworkDetailDTO> dtos;
        if (season < 0) {
            // series
            dtos = this.getSeriesFanarts(id);
        } else {
            // season
            dtos = this.getSeasonFanarts(id, season);
        }
        return dtos;
    }

    private List<ArtworkDetailDTO> getSeriesFanarts(String id) {
        List<ArtworkDetailDTO> hdDTOs = new ArrayList<ArtworkDetailDTO>();
        List<ArtworkDetailDTO> sdDTOs = new ArrayList<ArtworkDetailDTO>();

        // get series artwork
        Banners bannerList = getTVDbBanners(id);
        
        // find fanarts
        for (Banner banner : bannerList.getFanartList()) {
            if (banner.getBannerType2() == BannerType.FanartHD) {
                // HD fanart
                hdDTOs.add(createArtworDetail(banner));
            } else {
                // SD fanart
                sdDTOs.add(createArtworDetail(banner));
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Series {}: Found {} HD fanarts", id, hdDTOs.size());
            LOG.debug("Series {}: Found {} SD fanarts", id, sdDTOs.size());
        }
        
        List<ArtworkDetailDTO> returnDTOs = null;
        if (hdDTOs.size()>0) {
            LOG.info("Series {}: Using HD fanarts", id);
            returnDTOs = hdDTOs;
        } else if (sdDTOs.size()>0) {
            LOG.info("Series {}: No HD fanarts found; using SD fanarts", id);
            returnDTOs = sdDTOs;
        } else {
            String url = this.getTVDbSeries(id).getFanart();
            if (StringUtils.isNotBlank(url)) {
                LOG.info("Series {}: Using default series fanart", id);
                returnDTOs = new ArrayList<ArtworkDetailDTO>(1);
                returnDTOs.add(new ArtworkDetailDTO(getScannerName(), url));
            }
        }

        return returnDTOs;
    }

    private List<ArtworkDetailDTO> getSeasonFanarts(String id, int season) {
        // NOTE: no explicit season fanarts in TheTVDb
        
        List<ArtworkDetailDTO> hdDTOs = new ArrayList<ArtworkDetailDTO>();
        List<ArtworkDetailDTO> sdDTOs = new ArrayList<ArtworkDetailDTO>();

        // get series artwork
        Banners bannerList = getTVDbBanners(id);
        
        // find fanarts
        for (Banner banner : bannerList.getFanartList()) {
            if (banner.getBannerType2() == BannerType.FanartHD) {
                // HD fanart
                hdDTOs.add(createArtworDetail(banner));
            } else {
                // SD fanart
                sdDTOs.add(createArtworDetail(banner));
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Season {}-{}: Found {} HD fanarts", id, hdDTOs.size());
            LOG.debug("Season {}-{}: Found {} SD fanarts", id, sdDTOs.size());
        }
        
        List<ArtworkDetailDTO> returnDTOs = null;
        if (hdDTOs.size()>0) {
            LOG.debug("Season {}-{}: Using HD fanarts", id, season);
            returnDTOs = hdDTOs;
        } else if (sdDTOs.size()>0) {
            LOG.debug("Season {}-{}: No HD fanarts found; using SD fanarts", id, season);
            returnDTOs = sdDTOs;
        } else {
            String url = this.getTVDbSeries(id).getFanart();
            if (StringUtils.isNotBlank(url)) {
                LOG.debug("Season {}-{}: Using default series fanart", id, season);
                returnDTOs = new ArrayList<ArtworkDetailDTO>(1);
                returnDTOs.add(new ArtworkDetailDTO(getScannerName(), url));
            }
        }

        return returnDTOs;
    }

    private ArtworkDetailDTO createArtworDetail(Banner banner) {
        ArtworkDetailDTO dto = new ArtworkDetailDTO(getScannerName(), banner.getUrl(), banner.getLanguage());

        if (banner.getRating() != null) {
            try {
                dto.setRating((int)(banner.getRating() * 10));
            } catch (Exception ignore) {}
        }
        
        return dto;
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(String title, int year, int season) {
        String id = this.getId(title, year, season);
        return this.getBanners(id, season);
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(String id, int season) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        
        List<ArtworkDetailDTO> dtos;
        if (season < 0) {
            // series
            dtos = this.getSeriesBanners(id);
        } else {
            // season
            dtos = this.getSeasonBanners(id, season);
        }
        return dtos;
    }
    
    private List<ArtworkDetailDTO> getSeriesBanners(String id) {
        return null;
    }

    private List<ArtworkDetailDTO> getSeasonBanners(String id, int season) {
        return null;
    }
    
    private synchronized Banners getTVDbBanners(String id) {
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

    private com.omertron.thetvdbapi.model.Series getTVDbSeries(String id) {
        com.omertron.thetvdbapi.model.Series tvdbSeries = seriesCache.get(id);
        if (tvdbSeries == null) {
            seriesLock.lock();
            try {
                // second try cause meanwhile the cache could have been filled
                tvdbSeries = seriesCache.get(id);
                if (tvdbSeries == null) {
                    // retrieve series from TheTVDb
                    tvdbApi.getSeries(id, DEFAULT_LANGUAGE);
                    if (tvdbSeries == null && StringUtils.isNotBlank(ALTERNATE_LANGUAGE)) {
                        tvdbSeries = tvdbApi.getSeries(id, ALTERNATE_LANGUAGE);
                    }
                    if (tvdbSeries == null) {
                        // have a valid series object with empty values
                        tvdbSeries = new com.omertron.thetvdbapi.model.Series();
                    }
                    seriesCache.put(id, tvdbSeries);
                }
            } finally {
                seriesLock.unlock();
            }
        }
        return tvdbSeries;
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(IMetadata metadata) {
        String id = getId(metadata);
        return getPosters(id, metadata.getSeasonNumber());
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(IMetadata metadata) {
        String id = getId(metadata);
        return getFanarts(id, metadata.getSeasonNumber());
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(IMetadata metadata) {
        String id = getId(metadata);
        return getBanners(id, metadata.getSeasonNumber());
    }

    @Override
    public String getId(IMetadata metadata) {
        // first get the series, cause TheTVDb has just one id
        // per series, not season based
        Series series;
        if (metadata instanceof Series) {
            series = (Series)metadata;
        } else if (metadata instanceof Season) {
            series = ((Season)metadata).getSeries();
        } else if (metadata instanceof VideoData) {
            series = ((VideoData)metadata).getSeason().getSeries();
        } else {
            // no valid object for scanning artwork from TheTVDb
            return null;
        }
        
        // get the series id
        return tvdbScanner.getSeriesId(series);
    }
}
