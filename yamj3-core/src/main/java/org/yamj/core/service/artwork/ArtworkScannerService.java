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
package org.yamj.core.service.artwork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.service.artwork.fanart.IMovieFanartScanner;
import org.yamj.core.service.artwork.fanart.ITvShowFanartScanner;
import org.yamj.core.service.artwork.poster.IMoviePosterScanner;
import org.yamj.core.service.artwork.poster.ITvShowPosterScanner;
import org.yamj.core.service.artwork.tv.ITvShowBannerScanner;

@Service("artworkScannerService")
public class ArtworkScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScannerService.class);
    private static List<String> POSTER_MOVIE_PRIORITIES = Arrays.asList(PropertyTools.getProperty("artwork.scanner.poster.movie.priorities", "tmdb").toLowerCase().split(","));
    private static List<String> FANART_MOVIE_PRIORITIES = Arrays.asList(PropertyTools.getProperty("artwork.scanner.fanart.movie.priorities", "tmdb").toLowerCase().split(","));
    private static List<String> POSTER_TVSHOW_PRIORITIES = Arrays.asList(PropertyTools.getProperty("artwork.scanner.poster.tvshow.priorities", "tvdb").toLowerCase().split(","));
    private static List<String> FANART_TVSHOW_PRIORITIES = Arrays.asList(PropertyTools.getProperty("artwork.scanner.fanart.tvshow.priorities", "tvdb").toLowerCase().split(","));
    private static List<String> BANNER_TVSHOW_PRIORITIES = Arrays.asList(PropertyTools.getProperty("artwork.scanner.banner.tvshow.priorities", "tvdb").toLowerCase().split(","));
    private static int POSTER_MOVIE_MAXRESULTS = PropertyTools.getIntProperty("artwork.scanner.poster.movie.maxResults", 5);
    private static int FANART_MOVIE_MAXRESULTS = PropertyTools.getIntProperty("artwork.scanner.fanart.movie.maxResults", 5);
    private static int POSTER_TVSHOW_MAXRESULTS = PropertyTools.getIntProperty("artwork.scanner.poster.tvshow.maxResults", 5);
    private static int FANART_TVSHOW_MAXRESULTS = PropertyTools.getIntProperty("artwork.scanner.poster.tvshow.maxResults", 5);
    private static int BANNER_TVSHOW_MAXRESULTS = PropertyTools.getIntProperty("artwork.scanner.banner.tvshow.maxResults", 5);

    private HashMap<String, IMoviePosterScanner> registeredMoviePosterScanner = new HashMap<String, IMoviePosterScanner>();
    private HashMap<String, ITvShowPosterScanner> registeredTvShowPosterScanner = new HashMap<String, ITvShowPosterScanner>();
    private HashMap<String, IMovieFanartScanner> registeredMovieFanartScanner = new HashMap<String, IMovieFanartScanner>();
    private HashMap<String, ITvShowFanartScanner> registeredTvShowFanartScanner = new HashMap<String, ITvShowFanartScanner>();
    private HashMap<String, ITvShowBannerScanner> registeredTvShowBannerScanner = new HashMap<String, ITvShowBannerScanner>();

    @Autowired
    private ArtworkStorageService artworkStorageService;

    public void registerMoviePosterScanner(IMoviePosterScanner posterScanner) {
        LOG.info("Registered movie poster scanner: {}", posterScanner.getScannerName().toLowerCase());
        registeredMoviePosterScanner.put(posterScanner.getScannerName().toLowerCase(), posterScanner);
    }

    public void registerTvShowPosterScanner(ITvShowPosterScanner posterScanner) {
        LOG.info("Registered TV show poster scanner: {}", posterScanner.getScannerName().toLowerCase());
        registeredTvShowPosterScanner.put(posterScanner.getScannerName().toLowerCase(), posterScanner);
    }

    public void registerMovieFanartScanner(IMovieFanartScanner fanartScanner) {
        LOG.info("Registered movie fanart scanner: {}", fanartScanner.getScannerName().toLowerCase());
        registeredMovieFanartScanner.put(fanartScanner.getScannerName().toLowerCase(), fanartScanner);
    }

    public void registerTvShowFanartScanner(ITvShowFanartScanner fanartScanner) {
        LOG.info("Registered TV show fanart scanner: {}", fanartScanner.getScannerName().toLowerCase());
        registeredTvShowFanartScanner.put(fanartScanner.getScannerName().toLowerCase(), fanartScanner);
    }

    public void registerTvShowBannerScanner(ITvShowBannerScanner bannerScanner) {
        LOG.info("Registered TV show banner scanner: {}", bannerScanner.getScannerName().toLowerCase());
        registeredTvShowBannerScanner.put(bannerScanner.getScannerName().toLowerCase(), bannerScanner);
    }

    public void scanArtwork(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        // get unique required artwork
        Artwork artwork = artworkStorageService.getRequiredArtwork(queueElement.getId());

        // holds the located artwork
        List<ArtworkLocated> located = null;
        
        if (ArtworkType.POSTER == artwork.getArtworkType()) {
            located = this.scanPosterLocal(artwork);
            if (CollectionUtils.isEmpty(located)) {
                located = this.scanPosterOnline(artwork);
            }
        } else if (ArtworkType.FANART == artwork.getArtworkType()) {
            located = this.scanFanartLocal(artwork);
            if (CollectionUtils.isEmpty(located)) {
                located = this.scanFanartOnline(artwork);
            }
        } else if (ArtworkType.BANNER == artwork.getArtworkType() && artwork.getVideoData() == null) {
            // banner only for season and series
            located = this.scanBannerLocal(artwork);
            if (CollectionUtils.isEmpty(located)) {
                located = this.scanBannerOnline(artwork);
            }
        } else {
            // Don't throw an exception here, just a debug message for now
            LOG.debug("Artwork scan not implemented for {}", artwork);
        }

        // storage
        try {
            artworkStorageService.updateArtwork(artwork, located);
        } catch (Exception error) {
            // NOTE: status will not be changed
            LOG.error("Failed storing artwork {}-{}", queueElement.getId(), artwork.getArtworkType().toString());
            LOG.warn("Storage error", error);
        }
    }

    private List<ArtworkLocated> scanPosterLocal(Artwork artwork) {
        LOG.trace("Scan local for poster: {}", artwork);

        // TODO local scan
        return null;
    }

    private List<ArtworkLocated> scanPosterOnline(Artwork artwork) {
        LOG.debug("Scan online for poster: {}", artwork);

        List<ArtworkDetailDTO> posters = null;

        if (artwork.getMetadata().isMovie()) {
            // CASE: movie poster scan
            for (String prio : POSTER_MOVIE_PRIORITIES) {
                IMoviePosterScanner scanner = registeredMoviePosterScanner.get(prio);
                if (scanner != null) {
                    LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);  
                    posters= scanner.getPosters(artwork.getMetadata());
                    if (CollectionUtils.isNotEmpty(posters)) {
                        break;
                    }
                } else {
                    LOG.warn("Desired movie poster scanner {} not registerd", prio);
                }
            }

            if (CollectionUtils.isEmpty(posters)) {
                LOG.info("No movie poster found for: {}", artwork);
                return null;
            }

            if (POSTER_MOVIE_MAXRESULTS > 0 && posters.size() > POSTER_MOVIE_MAXRESULTS) {
                LOG.info("Limited movie posters to {} where retrieved {}: {}", POSTER_MOVIE_MAXRESULTS, posters.size(), artwork);
                posters = posters.subList(0, POSTER_MOVIE_MAXRESULTS);
            }

        } else {
            // CASE: TV show poster scan
            for (String prio : POSTER_TVSHOW_PRIORITIES) {
                ITvShowPosterScanner scanner = registeredTvShowPosterScanner.get(prio);
                if (scanner != null) {
                    LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);
                    posters= scanner.getPosters(artwork.getMetadata());
                    if (CollectionUtils.isNotEmpty(posters)) {
                        break;
                    }
                } else {
                    LOG.warn("Desired TV show poster scanner {} not registerd", prio);
                }
            }

            if (CollectionUtils.isEmpty(posters)) {
                LOG.info("No TV show poster found for: {}", artwork);
                return null;
            }

            if (POSTER_TVSHOW_MAXRESULTS > 0 && posters.size() > POSTER_TVSHOW_MAXRESULTS) {
                LOG.info("Limited TV show posters to {} where retrieved {}: {}", POSTER_TVSHOW_MAXRESULTS, posters.size(), artwork);
                posters = posters.subList(0, POSTER_TVSHOW_MAXRESULTS);
            }
        }
        
        return createLocatedArtworks(artwork, posters);
    }

    private List<ArtworkLocated> scanFanartLocal(Artwork artwork) {
        LOG.trace("Scan local for fanart: {}", artwork);

        // TODO local scan
        return null;
    }

    private List<ArtworkLocated> scanFanartOnline(Artwork artwork) {
        LOG.debug("Scan online for fanart: {}", artwork);

        List<ArtworkDetailDTO> fanarts = null;

        if (artwork.getMetadata().isMovie()) {
            // CASE: movie fanart
            for (String prio : FANART_MOVIE_PRIORITIES) {
                IMovieFanartScanner scanner = registeredMovieFanartScanner.get(prio);
                if (scanner != null) {
                    LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);  
                    fanarts = scanner.getFanarts(artwork.getMetadata());
                    if (CollectionUtils.isNotEmpty(fanarts)) {
                        break;
                    }
                } else {
                    LOG.warn("Desired movie fanart scanner {} not registerd", prio);
                }
            }

            if (CollectionUtils.isEmpty(fanarts)) {
                LOG.info("No movie fanart found for: {}", artwork);
                return null;
            }

            if (FANART_MOVIE_MAXRESULTS > 0 && fanarts.size() > FANART_MOVIE_MAXRESULTS) {
                LOG.info("Limited movie fanarts to {} where retrieved {}: {}", FANART_MOVIE_MAXRESULTS, fanarts.size(), artwork);
                fanarts = fanarts.subList(0, FANART_MOVIE_MAXRESULTS);
            }
        } else {
            // CASE: TV show poster scan
            for (String prio : FANART_TVSHOW_PRIORITIES) {
                ITvShowFanartScanner scanner = registeredTvShowFanartScanner.get(prio);
                if (scanner != null) {
                    LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);  
                    fanarts = scanner.getFanarts(artwork.getMetadata());
                } else {
                    LOG.warn("Desired TV show fanart scanner {} not registerd", prio);
                }
            }

            if (CollectionUtils.isEmpty(fanarts)) {
                LOG.info("No TV show fanarts found for: {}", artwork);
                return null;
            }

            if (FANART_TVSHOW_MAXRESULTS > 0 && fanarts.size() > FANART_TVSHOW_MAXRESULTS) {
                LOG.info("Limited TV show fanart to {} where retrieved {}: {}", FANART_TVSHOW_MAXRESULTS, fanarts.size(), artwork);
                fanarts = fanarts.subList(0, FANART_TVSHOW_MAXRESULTS);
            }
        }
        
        return createLocatedArtworks(artwork, fanarts);
    }

    private List<ArtworkLocated> scanBannerLocal(Artwork artwork) {
        LOG.trace("Scan local for TV show banner: {}", artwork);

        // TODO local scan
        return null;
    }

    private List<ArtworkLocated> scanBannerOnline(Artwork artwork) {
        LOG.debug("Scan online for TV show banner: {}", artwork);

        List<ArtworkDetailDTO> banners = null;

        for (String prio : BANNER_TVSHOW_PRIORITIES) {
            ITvShowBannerScanner scanner = registeredTvShowBannerScanner.get(prio);
            if (scanner != null) {
                LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);  
                banners = scanner.getBanners(artwork.getMetadata());
                if (CollectionUtils.isNotEmpty(banners)) {
                    break;
                }
            } else {
                LOG.warn("Desired TV show banner scanner {} not registerd", prio);
            }
        }
        
        if (CollectionUtils.isEmpty(banners)) {
            LOG.info("No TV show banner found for: {}", artwork);
            return null;
        }
        
        if (BANNER_TVSHOW_MAXRESULTS > 0 && banners.size() > BANNER_TVSHOW_MAXRESULTS) {
            LOG.info("Limited TV show banners to {} where retrieved {}: {}", BANNER_TVSHOW_MAXRESULTS, banners.size(), artwork);
            banners = banners.subList(0, BANNER_TVSHOW_MAXRESULTS);
        }
        
        return createLocatedArtworks(artwork, banners);
    }

    private List<ArtworkLocated> createLocatedArtworks(Artwork artwork, List<ArtworkDetailDTO> dtos) {
        List<ArtworkLocated> locatedArtworks = new ArrayList<ArtworkLocated>(dtos.size());
        for (ArtworkDetailDTO dto : dtos) {
            ArtworkLocated l = new ArtworkLocated();
            l.setArtwork(artwork);
            l.setSource(dto.getSource());
            l.setUrl(dto.getUrl());
            l.setLanguage(dto.getLanguage());
            l.setRating(dto.getRating());
            l.setStatus(StatusType.NEW);
            locatedArtworks.add(l);
        }
        return locatedArtworks;
    }
    
    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        artworkStorageService.errorArtwork(queueElement.getId());
    }
}
