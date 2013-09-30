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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.service.ArtworkLocatorService;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.service.artwork.fanart.IMovieFanartScanner;
import org.yamj.core.service.artwork.fanart.ITvShowFanartScanner;
import org.yamj.core.service.artwork.photo.IPhotoScanner;
import org.yamj.core.service.artwork.poster.IMoviePosterScanner;
import org.yamj.core.service.artwork.poster.ITvShowPosterScanner;
import org.yamj.core.service.artwork.tv.ITvShowBannerScanner;
import org.yamj.core.service.artwork.tv.ITvShowVideoImageScanner;

@Service("artworkScannerService")
public class ArtworkScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScannerService.class);
    private static List<String> posterMoviePriorities = Arrays.asList(PropertyTools.getProperty("artwork.scanner.poster.movie.priorities", "tmdb").toLowerCase().split(","));
    private static List<String> fanartMoviePriorities = Arrays.asList(PropertyTools.getProperty("artwork.scanner.fanart.movie.priorities", "tmdb").toLowerCase().split(","));
    private static List<String> posterTvshowPriorities = Arrays.asList(PropertyTools.getProperty("artwork.scanner.poster.tvshow.priorities", "tvdb").toLowerCase().split(","));
    private static List<String> fanartTvshowPriorities = Arrays.asList(PropertyTools.getProperty("artwork.scanner.fanart.tvshow.priorities", "tvdb").toLowerCase().split(","));
    private static List<String> bannerTvshowPriorities = Arrays.asList(PropertyTools.getProperty("artwork.scanner.banner.tvshow.priorities", "tvdb").toLowerCase().split(","));
    private static List<String> videoimageTvshowPriorities = Arrays.asList(PropertyTools.getProperty("artwork.scanner.videoimage.tvshow.priorities", "tvdb").toLowerCase().split(","));
    private static List<String> photoPriorities = Arrays.asList(PropertyTools.getProperty("artwork.scanner.photo.priorities", "tmdb").toLowerCase().split(","));
    private static int posterMovieMaxResults = PropertyTools.getIntProperty("artwork.scanner.poster.movie.maxResults", 5);
    private static int fanartMovieMaxResults = PropertyTools.getIntProperty("artwork.scanner.fanart.movie.maxResults", 5);
    private static int posterTvshowMaxResults = PropertyTools.getIntProperty("artwork.scanner.poster.tvshow.maxResults", 5);
    private static int fanartTvshowMaxResults = PropertyTools.getIntProperty("artwork.scanner.poster.tvshow.maxResults", 5);
    private static int bannerTvshowMaxResults = PropertyTools.getIntProperty("artwork.scanner.banner.tvshow.maxResults", 5);
    private static int videoimageTvshowMaxResults = PropertyTools.getIntProperty("artwork.scanner.videoimage.tvshow.maxResults", 2);
    private static int photoMaxResults = PropertyTools.getIntProperty("artwork.scanner.photo.maxResults", 1);
    private HashMap<String, IMoviePosterScanner> registeredMoviePosterScanner = new HashMap<String, IMoviePosterScanner>();
    private HashMap<String, ITvShowPosterScanner> registeredTvShowPosterScanner = new HashMap<String, ITvShowPosterScanner>();
    private HashMap<String, IMovieFanartScanner> registeredMovieFanartScanner = new HashMap<String, IMovieFanartScanner>();
    private HashMap<String, ITvShowFanartScanner> registeredTvShowFanartScanner = new HashMap<String, ITvShowFanartScanner>();
    private HashMap<String, ITvShowBannerScanner> registeredTvShowBannerScanner = new HashMap<String, ITvShowBannerScanner>();
    private HashMap<String, ITvShowVideoImageScanner> registeredTvShowVideoImageScanner = new HashMap<String, ITvShowVideoImageScanner>();
    private HashMap<String, IPhotoScanner> registeredPhotoScanner = new HashMap<String, IPhotoScanner>();
    @Autowired
    private ArtworkLocatorService artworkLocatorService;
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

    public void registerTvShowVideoImageScanner(ITvShowVideoImageScanner videoImageScanner) {
        LOG.info("Registered TV show episode image scanner: {}", videoImageScanner.getScannerName().toLowerCase());
        registeredTvShowVideoImageScanner.put(videoImageScanner.getScannerName().toLowerCase(), videoImageScanner);
    }

    public void registerPhotoScanner(IPhotoScanner photoScanner) {
        LOG.info("Registered photo scanner: {}", photoScanner.getScannerName().toLowerCase());
        registeredPhotoScanner.put(photoScanner.getScannerName().toLowerCase(), photoScanner);
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
        } else if (ArtworkType.BANNER == artwork.getArtworkType() && !artwork.getMetadata().isMovie()) {
            // banner only for season and series
            located = this.scanBannerLocal(artwork);
            if (CollectionUtils.isEmpty(located)) {
                located = this.scanBannerOnline(artwork);
            }
        } else if (ArtworkType.VIDEOIMAGE == artwork.getArtworkType() && (artwork.getMetadata().getEpisodeNumber() >= 0)) {
            // video image only for episodes
            located = this.scanVideoImageLocal(artwork);
            if (CollectionUtils.isEmpty(located)) {
                located = this.scanVideoImageOnline(artwork);
            }
        } else if (ArtworkType.PHOTO == artwork.getArtworkType()) {
            located = this.scanPhotoLocal(artwork);
            if (CollectionUtils.isEmpty(located)) {
                located = this.scanPhotoOnline(artwork);
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

        List<StageFile> posters = null;

        if (artwork.getMetadata().isMovie()) {
            posters = this.artworkLocatorService.getMoviePosters(artwork.getVideoData());
        }
        // TODO series/season poster scanning

        return createLocatedArtworksLocal(artwork, posters);
    }

    private List<ArtworkLocated> scanPosterOnline(Artwork artwork) {
        LOG.debug("Scan online for poster: {}", artwork);

        List<ArtworkDetailDTO> posters = null;

        if (artwork.getMetadata().isMovie()) {
            // CASE: movie poster scan
            for (String prio : posterMoviePriorities) {
                IMoviePosterScanner scanner = registeredMoviePosterScanner.get(prio);
                if (scanner != null) {
                    LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);
                    posters = scanner.getPosters(artwork.getMetadata());
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

            if (posterMovieMaxResults > 0 && posters.size() > posterMovieMaxResults) {
                LOG.info("Limited movie posters to {}, actually retrieved {} for {}", posterMovieMaxResults, posters.size(), artwork);
                posters = posters.subList(0, posterMovieMaxResults);
            }
        } else {
            // CASE: TV show poster scan
            for (String prio : posterTvshowPriorities) {
                ITvShowPosterScanner scanner = registeredTvShowPosterScanner.get(prio);
                if (scanner != null) {
                    LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);
                    posters = scanner.getPosters(artwork.getMetadata());
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

            if (posterTvshowMaxResults > 0 && posters.size() > posterTvshowMaxResults) {
                LOG.info("Limited TV show posters to {}, actually retrieved {} for {}", posterTvshowMaxResults, posters.size(), artwork);
                posters = posters.subList(0, posterTvshowMaxResults);
            }
        }

        return createLocatedArtworksOnline(artwork, posters);
    }

    private List<ArtworkLocated> scanFanartLocal(Artwork artwork) {
        LOG.trace("Scan local for fanart: {}", artwork);

        List<StageFile> fanarts = null;

        if (artwork.getMetadata().isMovie()) {
            fanarts = this.artworkLocatorService.getMovieFanarts(artwork.getVideoData());
        }
        // TODO series/season poster scanning

        return createLocatedArtworksLocal(artwork, fanarts);
    }

    private List<ArtworkLocated> scanFanartOnline(Artwork artwork) {
        LOG.debug("Scan online for fanart: {}", artwork);

        List<ArtworkDetailDTO> fanarts = null;

        if (artwork.getMetadata().isMovie()) {
            // CASE: movie fanart
            for (String prio : fanartMoviePriorities) {
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

            if (fanartMovieMaxResults > 0 && fanarts.size() > fanartMovieMaxResults) {
                LOG.info("Limited movie fanarts to {}, actually retrieved {} for {}", fanartMovieMaxResults, fanarts.size(), artwork);
                fanarts = fanarts.subList(0, fanartMovieMaxResults);
            }
        } else {
            // CASE: TV show poster scan
            for (String prio : fanartTvshowPriorities) {
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

            if (fanartTvshowMaxResults > 0 && fanarts.size() > fanartTvshowMaxResults) {
                LOG.info("Limited TV show fanart to {}, actually retrieved {} for {}", fanartTvshowMaxResults, fanarts.size(), artwork);
                fanarts = fanarts.subList(0, fanartTvshowMaxResults);
            }
        }

        return createLocatedArtworksOnline(artwork, fanarts);
    }

    private List<ArtworkLocated> scanBannerLocal(Artwork artwork) {
        LOG.trace("Scan local for TV show banner: {}", artwork);

        // TODO local scan
        return null;
    }

    private List<ArtworkLocated> scanBannerOnline(Artwork artwork) {
        LOG.debug("Scan online for TV show banner: {}", artwork);

        List<ArtworkDetailDTO> banners = null;

        for (String prio : bannerTvshowPriorities) {
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

        if (bannerTvshowMaxResults > 0 && banners.size() > bannerTvshowMaxResults) {
            LOG.info("Limited TV show banners to {}, actually retrieved {} for {}", bannerTvshowMaxResults, banners.size(), artwork);
            banners = banners.subList(0, bannerTvshowMaxResults);
        }

        return createLocatedArtworksOnline(artwork, banners);
    }

    private List<ArtworkLocated> scanVideoImageLocal(Artwork artwork) {
        LOG.trace("Scan local for TV show episode image: {}", artwork);

        // TODO local scan
        return null;
    }

    private List<ArtworkLocated> scanVideoImageOnline(Artwork artwork) {
        LOG.debug("Scan online for TV show episode image: {}", artwork);

        List<ArtworkDetailDTO> videoimages = null;

        for (String prio : videoimageTvshowPriorities) {
            ITvShowVideoImageScanner scanner = registeredTvShowVideoImageScanner.get(prio);
            if (scanner != null) {
                LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);
                videoimages = scanner.getVideoImages(artwork.getMetadata());
                if (CollectionUtils.isNotEmpty(videoimages)) {
                    break;
                }
            } else {
                LOG.warn("Desired TV show episode image scanner {} not registerd", prio);
            }
        }

        if (CollectionUtils.isEmpty(videoimages)) {
            LOG.info("No TV show episode image found for: {}", artwork);
            return null;
        }

        if (videoimageTvshowMaxResults > 0 && videoimages.size() > videoimageTvshowMaxResults) {
            LOG.info("Limited Video Images to {}, actually retrieved {} for {}", videoimageTvshowMaxResults, videoimages.size(), artwork);
            videoimages = videoimages.subList(0, videoimageTvshowMaxResults);
        }

        return createLocatedArtworksOnline(artwork, videoimages);
    }

    private List<ArtworkLocated> scanPhotoLocal(Artwork artwork) {
        LOG.trace("Scan local for photo: {}", artwork);
        // TODO: Local photo search
        return Collections.emptyList();
    }

    private List<ArtworkLocated> scanPhotoOnline(Artwork artwork) {
        LOG.debug("Scan online for photo: {}", artwork);

        List<ArtworkDetailDTO> photos = null;

        for (String prio : photoPriorities) {
            IPhotoScanner scanner = registeredPhotoScanner.get(prio);
            if (scanner != null) {
                LOG.debug("Use {} scanner for {}", scanner.getScannerName(), artwork);
                Person person = artwork.getPerson();
                if (person == null) {
                    LOG.warn("No associated person found for artwork: {}", artwork);
                } else {
                    String id = person.getPersonId(prio);
                    LOG.info("Scanning for person ID: {}-{}", prio, id);
                    if (StringUtils.isNumeric(id)) {
                        photos = scanner.getPhotos(Integer.parseInt(id));
                    } else {
                        // Id looks to be invalid, so look it up
                        id = scanner.getPersonId(person);
                        // Could check if the ID is null and then use the IMDB id if available
                        photos = scanner.getPhotos(id);
                    }
                    if (CollectionUtils.isNotEmpty(photos)) {
                        break;
                    }
                }
            } else {
                LOG.warn("Desired photo scanner {} not registerd", prio);
            }
        }

        if (CollectionUtils.isEmpty(photos)) {
            LOG.info("No photos found for: {}", artwork);
            return null;
        }

        if (photoMaxResults > 0 && photos.size() > photoMaxResults) {
            LOG.info("Limited photos to {}, actually retrieved {} for {}", photoMaxResults, photos.size(), artwork);
            photos = photos.subList(0, photoMaxResults);
        }

        return createLocatedArtworksOnline(artwork, photos);
    }

    private List<ArtworkLocated> createLocatedArtworksOnline(Artwork artwork, List<ArtworkDetailDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) {
            return null;
        }

        List<ArtworkLocated> locatedArtworks = new ArrayList<ArtworkLocated>(dtos.size());
        for (ArtworkDetailDTO dto : dtos) {
            ArtworkLocated loc = new ArtworkLocated();
            loc.setArtwork(artwork);
            loc.setSource(dto.getSource());
            loc.setUrl(dto.getUrl());
            loc.setLanguage(dto.getLanguage());
            loc.setRating(dto.getRating());
            loc.setStatus(StatusType.NEW);
            loc.setPriority(10);
            locatedArtworks.add(loc);
        }
        return locatedArtworks;
    }

    private List<ArtworkLocated> createLocatedArtworksLocal(Artwork artwork, List<StageFile> stageFiles) {
        if (CollectionUtils.isEmpty(stageFiles)) {
            return null;
        }

        List<ArtworkLocated> locatedArtworks = new ArrayList<ArtworkLocated>(stageFiles.size());
        for (StageFile stageFile : stageFiles) {
            ArtworkLocated l = new ArtworkLocated();
            l.setArtwork(artwork);
            l.setStageFile(stageFile);
            l.setStatus(StatusType.NEW);
            l.setPriority(1);
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
