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
import org.yamj.core.service.artwork.fanart.IFanartScanner;
import org.yamj.core.service.artwork.fanart.IMovieFanartScanner;
import org.yamj.core.service.artwork.poster.IMoviePosterScanner;
import org.yamj.core.service.artwork.poster.IPosterScanner;

@Service("artworkScannerService")
public class ArtworkScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScannerService.class);
    private static List<String> POSTER_MOVIE_PRIORITIES = Arrays.asList(PropertyTools.getProperty("artwork.scanner.poster.movie.priorities", "tmdb").toLowerCase().split(","));
    private static List<String> FANART_MOVIE_PRIORITIES = Arrays.asList(PropertyTools.getProperty("artwork.scanner.fanart.movie.priorities", "tmdb").toLowerCase().split(","));
    private HashMap<String, IMoviePosterScanner> registeredMoviePosterScanner = new HashMap<String, IMoviePosterScanner>();
    private HashMap<String, IMovieFanartScanner> registeredMovieFanartScanner = new HashMap<String, IMovieFanartScanner>();
    @Autowired
    private ArtworkStorageService artworkStorageService;

    public void registerMoviePosterScanner(IMoviePosterScanner posterScanner) {
        registeredMoviePosterScanner.put(posterScanner.getScannerName().toLowerCase(), posterScanner);
    }

    public void registerMovieFanartScanner(IMovieFanartScanner fanartScanner) {
        registeredMovieFanartScanner.put(fanartScanner.getScannerName().toLowerCase(), fanartScanner);
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
        
        if (ArtworkType.POSTER.equals(artwork.getArtworkType())) {
            located = this.scanPosterLocal(artwork);
            if (CollectionUtils.isEmpty(located)) {
                located = this.scanPosterOnline(artwork);
            }
        } else if (ArtworkType.FANART.equals(artwork.getArtworkType())) {
            located = this.scanFanartLocal(artwork);
            if (CollectionUtils.isEmpty(located)) {
                located = this.scanFanartOnline(artwork);
            }
        } else {
            // Don't throw an exception here, just a debug message for now
            LOG.debug("Artwork scan not implemented for {}", artwork);
        }

        // storage
        try {
            if (CollectionUtils.isEmpty(located)) {
                artwork.setStatus(StatusType.NOTFOUND);
            } else {
                artwork.setStatus(StatusType.DONE);
            }

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
        LOG.trace("Scan online for poster: {}", artwork);

        List<String> posterURLs = null;

        if (artwork.getVideoData() != null) {
            // CASE: movie poster scan
            for (String prio : POSTER_MOVIE_PRIORITIES) {
                IPosterScanner scanner = registeredMoviePosterScanner.get(prio);
                if (scanner != null) {
                    posterURLs = scanner.getPosterURLs(artwork.getVideoData());
                    if (CollectionUtils.isNotEmpty(posterURLs)) {
                        break;
                    }
                }
            }
        } else {
            // Don't throw an exception here, just a debug message for now
            LOG.debug("Artwork scan not implemented for {}", artwork);
        }
        
        if (CollectionUtils.isEmpty(posterURLs)) {
            return null;
        }
        return createLocatedArtworks(artwork, posterURLs);
    }

    private List<ArtworkLocated> scanFanartLocal(Artwork artwork) {
        LOG.trace("Scan local for fanart: {}", artwork);

        // TODO local scan
        return null;
    }

    private List<ArtworkLocated> scanFanartOnline(Artwork artwork) {
        LOG.trace("Scan online for fanart: {}", artwork);

        List<String> fanartURLs = null;

        if (artwork.getVideoData() != null) {
            // CASE: movie fanart
            for (String prio : FANART_MOVIE_PRIORITIES) {
                IFanartScanner scanner = registeredMovieFanartScanner.get(prio);
                if (scanner != null) {
                    fanartURLs = scanner.getFanartURLs(artwork.getVideoData());
                    if (CollectionUtils.isNotEmpty(fanartURLs)) {
                        break;
                    }
                }
            }
        } else {
            // Don't throw an exception here, just a debug message for now
            LOG.debug("Artwork search not implemented for {}", artwork);
        }
        
        if (CollectionUtils.isEmpty(fanartURLs)) {
            LOG.debug("No artwork found for: {}", artwork);
            return null;
        }
        return createLocatedArtworks(artwork, fanartURLs);
    }

    private List<ArtworkLocated> createLocatedArtworks(Artwork artwork, List<String> urls) {
        List<ArtworkLocated> located = new ArrayList<ArtworkLocated>(urls.size());
        for (String url : urls) {
            ArtworkLocated l = new ArtworkLocated();
            l.setArtwork(artwork);
            l.setUrl(url);
            l.setStatus(StatusType.NEW);
            located.add(l);
        }
        return located;
    }
    
    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        artworkStorageService.errorArtwork(queueElement.getId());
    }
}
