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
package org.yamj.core.service.artwork;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.ImageType;
import org.yamj.core.database.service.ArtworkLocatorService;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.scheduling.IQueueProcessService;
import org.yamj.core.service.artwork.online.*;
import org.yamj.core.service.attachment.Attachment;
import org.yamj.core.service.attachment.AttachmentScannerService;
import org.yamj.core.service.file.FileTools;

@Service("artworkScannerService")
public class ArtworkScannerService implements IQueueProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScannerService.class);
    private static final String USE_SCANNER_FOR = "Use {} scanner for {}";
    
    private final HashMap<String, IMoviePosterScanner> registeredMoviePosterScanner = new HashMap<>();
    private final HashMap<String, ITvShowPosterScanner> registeredTvShowPosterScanner = new HashMap<>();
    private final HashMap<String, IMovieFanartScanner> registeredMovieFanartScanner = new HashMap<>();
    private final HashMap<String, ITvShowFanartScanner> registeredTvShowFanartScanner = new HashMap<>();
    private final HashMap<String, ITvShowBannerScanner> registeredTvShowBannerScanner = new HashMap<>();
    private final HashMap<String, ITvShowVideoImageScanner> registeredTvShowVideoImageScanner = new HashMap<>();
    private final HashMap<String, IPhotoScanner> registeredPhotoScanner = new HashMap<>();
    private final HashMap<String, IBoxedSetPosterScanner> registeredBoxedSetPosterScanner = new HashMap<>();
    private final HashMap<String, IBoxedSetFanartScanner> registeredBoxedSetFanartScanner = new HashMap<>();
    private final HashMap<String, IBoxedSetBannerScanner> registeredBoxedSetBannerScanner = new HashMap<>();
    
    @Autowired
    private ArtworkLocatorService artworkLocatorService;
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private AttachmentScannerService attachmentScannerService;
    
    public void registerArtworkScanner(IArtworkScanner artworkScanner) {
        final String scannerName = artworkScanner.getScannerName().toLowerCase();
        
        if (artworkScanner instanceof IMoviePosterScanner) {
            LOG.trace("Registered movie poster scanner: {}", scannerName);
            registeredMoviePosterScanner.put(scannerName, (IMoviePosterScanner)artworkScanner);
        }
        if (artworkScanner instanceof ITvShowPosterScanner) {
            LOG.trace("Registered TV show poster scanner: {}", scannerName);
            registeredTvShowPosterScanner.put(scannerName, (ITvShowPosterScanner)artworkScanner);
        }
        if (artworkScanner instanceof IMovieFanartScanner) {
            LOG.trace("Registered movie fanart scanner: {}", scannerName);
            registeredMovieFanartScanner.put(scannerName, (IMovieFanartScanner)artworkScanner);
        }
        if (artworkScanner instanceof ITvShowFanartScanner) {
            LOG.trace("Registered TV show fanart scanner: {}", scannerName);
            registeredTvShowFanartScanner.put(scannerName, (ITvShowFanartScanner)artworkScanner);
        }
        if (artworkScanner instanceof ITvShowBannerScanner) {
            LOG.trace("Registered TV show banner scanner: {}", scannerName);
            registeredTvShowBannerScanner.put(scannerName, (ITvShowBannerScanner)artworkScanner);
        }
        if (artworkScanner instanceof ITvShowVideoImageScanner) {
            LOG.trace("Registered TV show episode image scanner: {}", scannerName);
            registeredTvShowVideoImageScanner.put(scannerName, (ITvShowVideoImageScanner)artworkScanner);
        }
        if (artworkScanner instanceof IPhotoScanner) {
            LOG.trace("Registered photo scanner: {}", scannerName);
            registeredPhotoScanner.put(scannerName, (IPhotoScanner)artworkScanner);
        }
        if (artworkScanner instanceof IBoxedSetPosterScanner) {
            LOG.trace("Registered boxed set poster scanner: {}", scannerName);
            registeredBoxedSetPosterScanner.put(scannerName, (IBoxedSetPosterScanner)artworkScanner);
        }
        if (artworkScanner instanceof IBoxedSetFanartScanner) {
            LOG.trace("Registered boxed set fanart scanner: {}", scannerName);
            registeredBoxedSetFanartScanner.put(scannerName, (IBoxedSetFanartScanner)artworkScanner);
        }
        if (artworkScanner instanceof IBoxedSetBannerScanner) {
            LOG.trace("Registered boxed set banner scanner: {}", scannerName);
            registeredBoxedSetBannerScanner.put(scannerName, (IBoxedSetBannerScanner)artworkScanner);
        }
    }

    @Override
    public void processQueueElement(QueueDTO queueElement) {
        // get unique required artwork
        Artwork artwork = artworkStorageService.getRequiredArtwork(queueElement.getId());

        // holds the located artwork
        List<ArtworkLocated> locatedArtworks = new LinkedList<>();

        if (ArtworkType.POSTER == artwork.getArtworkType()) {
            // poster only for movie, season, series and boxed sets
            this.scanPosterLocal(artwork, locatedArtworks);
            this.scanPosterAttached(artwork, locatedArtworks);
            this.scanPosterOnline(artwork, locatedArtworks);
        } else if (ArtworkType.FANART == artwork.getArtworkType()) {
            // fanart only for movie, season, series and boxed sets
            this.scanFanartLocal(artwork, locatedArtworks);
            this.scanFanartAttached(artwork, locatedArtworks);
            this.scanFanartOnline(artwork, locatedArtworks);
        } else if (ArtworkType.BANNER == artwork.getArtworkType()) {
            // banner only for season, series and boxed sets
            this.scanBannerLocal(artwork, locatedArtworks);
            this.scanBannerAttached(artwork, locatedArtworks);
            this.scanBannerOnline(artwork, locatedArtworks);
        } else if (ArtworkType.VIDEOIMAGE == artwork.getArtworkType()) {
            // video image only for episodes
            this.scanVideoImageLocal(artwork, locatedArtworks);
            this.scanVideoImageAttached(artwork, locatedArtworks);
            this.scanVideoImageOnline(artwork, locatedArtworks);
        } else if (ArtworkType.PHOTO == artwork.getArtworkType()) {
            this.scanPhotoLocal(artwork, locatedArtworks);
            this.scanPhotoOnline(artwork, locatedArtworks);
        } else {
            // Don't throw an exception here, just a debug message for now
            LOG.debug("Artwork scan not implemented for {}", artwork);
        }

        // storage
        try {
            artworkStorageService.updateArtwork(artwork, locatedArtworks);
        } catch (Exception error) {
            // NOTE: status will not be changed
            LOG.error("Failed storing artwork {}-{}", queueElement.getId(), artwork.getArtworkType().toString());
            LOG.warn("Storage error", error);
        }
    }

    @Override
    public void processErrorOccurred(QueueDTO queueElement, Exception error) {
        LOG.error("Failed scan for artwork "+queueElement.getId(), error);

        artworkStorageService.errorArtwork(queueElement.getId());
    }
    
    private void scanPosterLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local poster scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for poster: {}", artwork);
        List<StageFile> posters = Collections.emptyList();

        if (artwork.getVideoData() != null) {
            // scan movie poster
            posters = this.artworkLocatorService.getMatchingArtwork(ArtworkType.POSTER, artwork.getVideoData());
        } else if (artwork.getSeason() != null) {
            // scan season poster
            posters = this.artworkLocatorService.getMatchingArtwork(ArtworkType.POSTER, artwork.getSeason());
        } else if (artwork.getSeries() != null) {
            // scan series poster
            posters = this.artworkLocatorService.getMatchingArtwork(ArtworkType.POSTER, artwork.getSeries());
        } else if (artwork.getBoxedSet() != null) {
            // scan boxed set poster
            posters = this.artworkLocatorService.getMatchingArtwork(ArtworkType.POSTER, artwork.getBoxedSet());
        }

        createLocatedArtworksLocal(artwork, posters, locatedArtworks);
    }

    private void scanPosterAttached(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isAttachedArtworkScanEnabled(artwork)) {
            LOG.trace("Attached poster scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan attachments for poster: {}", artwork);
        List<Attachment> attachments = attachmentScannerService.scan(artwork);
        createLocatedArtworksAttached(artwork, attachments, locatedArtworks);
    }

    private void scanPosterOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online poster scan disabled: {}", artwork);
            return;
        }
        
        LOG.debug("Scan online for poster: {}", artwork);
        List<ArtworkDetailDTO> posters = Collections.emptyList();
        int maxResults = 0;
        
        if (artwork.getBoxedSet() != null) {
            // CASE: boxed set poster
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.poster.boxset.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.poster.boxset.priorities", registeredBoxedSetPosterScanner.keySet())) {
                IBoxedSetPosterScanner scanner = registeredBoxedSetPosterScanner.get(prio);
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                posters = scanner.getPosters(artwork.getBoxedSet());
                if (!posters.isEmpty()) {
                    break;
                }
            }
        } else if (artwork.getVideoData() != null && artwork.getVideoData().isMovie()) {
            // CASE: movie poster
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.poster.movie.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.poster.movie.priorities", registeredMoviePosterScanner.keySet())) {
                IMoviePosterScanner scanner = registeredMoviePosterScanner.get(prio);
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                posters = scanner.getPosters(artwork.getVideoData());
                if (!posters.isEmpty()) {
                    break;
                }
            }
        } else if (artwork.getSeason() != null || artwork.getSeries() != null) {
            // CASE: TV show poster scan
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.poster.tvshow.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.poster.tvshow.priorities", registeredTvShowPosterScanner.keySet())) {
                ITvShowPosterScanner scanner = registeredTvShowPosterScanner.get(prio);
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                if (artwork.getSeries() != null) {
                    posters = scanner.getPosters(artwork.getSeries());
                } else {
                    posters = scanner.getPosters(artwork.getSeason());
                }
                if (!posters.isEmpty()) {
                    break;
                }
            }
        }

        if (posters.isEmpty()) {
            LOG.info("No poster found for: {}", artwork);
            return;
        }

        if (maxResults > 0 && posters.size() > maxResults) {
            LOG.info("Limited posters to {}, actually retrieved {} for {}", maxResults, posters.size(), artwork);
            posters = posters.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, posters, locatedArtworks);
    }

    private void scanFanartLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local fanart scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for fanart: {}", artwork);
        List<StageFile> fanarts  = Collections.emptyList();

        if (artwork.getVideoData() != null) {
            // scan movie fanart
            fanarts = this.artworkLocatorService.getMatchingArtwork(ArtworkType.FANART, artwork.getVideoData());
        } else if (artwork.getSeason() != null) {
            // scan season fanart
            fanarts = this.artworkLocatorService.getMatchingArtwork(ArtworkType.FANART, artwork.getSeason());
        } else if (artwork.getSeries() != null) {
            // scan series fanart
            fanarts = this.artworkLocatorService.getMatchingArtwork(ArtworkType.FANART, artwork.getSeries());
        } else if (artwork.getBoxedSet() != null) {
            // scan boxed set fanart
            fanarts = this.artworkLocatorService.getMatchingArtwork(ArtworkType.FANART, artwork.getBoxedSet());
        }

        createLocatedArtworksLocal(artwork, fanarts, locatedArtworks);
    }

    private void scanFanartAttached(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isAttachedArtworkScanEnabled(artwork)) {
            LOG.trace("Attached fanart scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan attachments for fanart: {}", artwork);
        List<Attachment> attachments = attachmentScannerService.scan(artwork);
        createLocatedArtworksAttached(artwork, attachments, locatedArtworks);
    }

    private void scanFanartOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online fanart scan disabled: {}", artwork);
            return;
        }

        LOG.debug("Scan online for fanart: {}", artwork);
        List<ArtworkDetailDTO> fanarts = Collections.emptyList();
        int maxResults = 0;
        
        if (artwork.getBoxedSet() != null) {
            // CASE: boxed set fanart
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.fanart.boxset.maxResults", 5);
            
            for (String prio : determinePriorities("yamj3.artwork.scanner.fanart.boxset.priorities", registeredBoxedSetFanartScanner.keySet())) {
                IBoxedSetFanartScanner scanner = registeredBoxedSetFanartScanner.get(prio);
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                fanarts = scanner.getFanarts(artwork.getBoxedSet());
                if (!fanarts.isEmpty()) {
                    break;
                }
            }
        } else if (artwork.getVideoData() != null && artwork.getVideoData().isMovie()) {
            // CASE: movie fanart
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.fanart.movie.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.fanart.movie.priorities", registeredMovieFanartScanner.keySet())) {
                IMovieFanartScanner scanner = registeredMovieFanartScanner.get(prio);
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                fanarts = scanner.getFanarts(artwork.getVideoData());
                if (!fanarts.isEmpty()) {
                    break;
                }
            }
        } else if (artwork.getSeason() != null || artwork.getSeries() != null) {
            // CASE: TV show fanart
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.fanart.tvshow.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.fanart.tvshow.priorities", registeredTvShowFanartScanner.keySet())) {
                ITvShowFanartScanner scanner = registeredTvShowFanartScanner.get(prio);
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                if (artwork.getSeries() != null) {
                    fanarts = scanner.getFanarts(artwork.getSeries());
                } else {
                    fanarts = scanner.getFanarts(artwork.getSeason());
                }
                if (!fanarts.isEmpty()) {
                    break;
                }
            }
        }


        if (fanarts.isEmpty()) {
            LOG.info("No fanart found for: {}", artwork);
            return;
        }

        if (maxResults > 0 && fanarts.size() > maxResults) {
            LOG.info("Limited fanarts to {}, actually retrieved {} for {}", maxResults, fanarts.size(), artwork);
            fanarts = fanarts.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, fanarts, locatedArtworks);
    }

    private void scanBannerLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local banner scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for TV show banner: {}", artwork);
        List<StageFile> banners = Collections.emptyList();

        if (artwork.getSeason() != null) {
            // scan season banner
            banners = this.artworkLocatorService.getMatchingArtwork(ArtworkType.BANNER, artwork.getSeason());
        } else if (artwork.getSeries() != null) {
            // scan series banner
            banners = this.artworkLocatorService.getMatchingArtwork(ArtworkType.BANNER, artwork.getSeries());
        } else if (artwork.getBoxedSet() != null) {
            // scan boxed set banner
            banners = this.artworkLocatorService.getMatchingArtwork(ArtworkType.BANNER, artwork.getBoxedSet());
        }

        createLocatedArtworksLocal(artwork, banners, locatedArtworks);
    }

    private void scanBannerAttached(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isAttachedArtworkScanEnabled(artwork)) {
            LOG.trace("Attached banner scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan attachments for banner: {}", artwork);
        List<Attachment> attachments = attachmentScannerService.scan(artwork);
        createLocatedArtworksAttached(artwork, attachments, locatedArtworks);
    }

    private void scanBannerOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online banner scan disabled: {}", artwork);
            return;
        }

        LOG.debug("Scan online for banner: {}", artwork);
        List<ArtworkDetailDTO> banners = Collections.emptyList();
        int maxResults = 0;
        
        if (artwork.getBoxedSet() != null) {
            // CASE: boxed set banner
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.banner.boxset.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.banner.boxset.priorities", registeredBoxedSetBannerScanner.keySet())) {
                IBoxedSetBannerScanner scanner = registeredBoxedSetBannerScanner.get(prio);
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                banners = scanner.getBanners(artwork.getBoxedSet());
                if (!banners.isEmpty()) {
                    break;
                }
            }
        } else if (artwork.getSeason() != null || artwork.getSeries() != null) {
            // CASE: TV show banner
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.banner.tvshow.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.banner.tvshow.priorities", registeredTvShowBannerScanner.keySet())) {
                ITvShowBannerScanner scanner = registeredTvShowBannerScanner.get(prio);
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                if (artwork.getSeries() != null) {
                    banners = scanner.getBanners(artwork.getSeries());
                } else {
                    banners = scanner.getBanners(artwork.getSeason());
                }
                if (!banners.isEmpty()) {
                    break;
                }
            }
        }

        if (banners.isEmpty()) {
            LOG.info("No banner found for: {}", artwork);
            return;
        }

        if (maxResults > 0 && banners.size() > maxResults) {
            LOG.info("Limited banner to {}, actually retrieved {} for {}", maxResults, banners.size(), artwork);
            banners = banners.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, banners, locatedArtworks);
    }

    private void scanVideoImageLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local episode image scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for TV show episode image: {}", artwork);

        List<StageFile> videoimages = Collections.emptyList();
        
        if (artwork.getVideoData() != null) {
            // TODO local scan for video images
        }
        
        createLocatedArtworksLocal(artwork, videoimages, locatedArtworks);
    }

    private void scanVideoImageAttached(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isAttachedArtworkScanEnabled(artwork)) {
            LOG.trace("Attached episode image scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan attachments for TV show episode image: {}", artwork);
        List<Attachment> attachments = attachmentScannerService.scan(artwork);
        createLocatedArtworksAttached(artwork, attachments, locatedArtworks);
    }
    
    private void scanVideoImageOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online episode image scan disabled: {}", artwork);
            return;
        }

        VideoData videoData = artwork.getVideoData();
        if (videoData == null || videoData.isMovie()) {
            LOG.warn("No associated episode found for artwork: {}", artwork);
            return;
        }

        LOG.debug("Scan online for TV show episode image: {}", artwork);
        List<ArtworkDetailDTO> videoimages = Collections.emptyList();
        
        for (String prio : determinePriorities("yamj3.artwork.scanner.videoimage.priorities", registeredTvShowVideoImageScanner.keySet())) {
            ITvShowVideoImageScanner scanner = registeredTvShowVideoImageScanner.get(prio);
            LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
            videoimages = scanner.getVideoImages(videoData);
            if (!videoimages.isEmpty()) {
                break;
            }
        }

        if (videoimages.isEmpty()) {
            LOG.info("No TV show episode image found for: {}", artwork);
            return;
        }

        int maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.videoimage.maxResults", 2);
        if (maxResults > 0 && videoimages.size() > maxResults) {
            LOG.info("Limited TV show episode images to {}, actually retrieved {} for {}", maxResults, videoimages.size(), artwork);
            videoimages = videoimages.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, videoimages, locatedArtworks);
    }

    private void scanPhotoLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local photo scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for photo: {}", artwork);
        List<StageFile> photos = Collections.emptyList();

        if (artwork.getPerson() != null) {
            // scan person photo
            photos = this.artworkLocatorService.getPhotos(artwork.getPerson());
        }

        createLocatedArtworksLocal(artwork, photos, locatedArtworks);
    }

    private void scanPhotoOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online photo scan disabled: {}", artwork);
            return;
        }

        Person person = artwork.getPerson();
        if (person == null) {
            LOG.warn("No associated person found for artwork: {}", artwork);
            return;
        }

        LOG.debug("Scan online for photo: {}", artwork);
        List<ArtworkDetailDTO> photos = Collections.emptyList();

        for (String prio : determinePriorities("yamj3.artwork.scanner.photo.priorities", registeredPhotoScanner.keySet())) {
            IPhotoScanner scanner = registeredPhotoScanner.get(prio);
            LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), person);
            photos = scanner.getPhotos(person);
            if (!photos.isEmpty()) {
                break;
            }
        }

        if (photos.isEmpty()) {
            LOG.info("No photos found for: {}", artwork);
            return;
        }

        int maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.photo.maxResults", 1);
        if (maxResults > 0 && photos.size() > maxResults) {
            LOG.info("Limited photos to {}, actually retrieved {} for {}", maxResults, photos.size(), artwork);
            photos = photos.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, photos, locatedArtworks);
    }

    private static void createLocatedArtworksOnline(Artwork artwork, List<ArtworkDetailDTO> dtos, List<ArtworkLocated> locatedArtworks) {
        for (ArtworkDetailDTO dto : dtos) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource(dto.getSource());
            located.setUrl(dto.getUrl());
            located.setHashCode(dto.getHashCode());
            located.setImageType(dto.getImageType());
            located.setLanguageCode(dto.getLanguageCode());
            located.setRating(dto.getRating());
            located.setStatus(StatusType.NEW);
            located.setPriority(10);
            locatedArtworks.add(located);
        }
    }

    private static void createLocatedArtworksLocal(Artwork artwork, List<StageFile> stageFiles, List<ArtworkLocated> locatedArtworks) {
        for (StageFile stageFile : stageFiles) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource("file");
            located.setPriority(1);
            located.setStageFile(stageFile);
            located.setHashCode(stageFile.getHashCode());
            located.setImageType(ImageType.fromString(stageFile.getExtension()));
            
            if (FileTools.isFileReadable(stageFile)) {
                located.setStatus(StatusType.NEW);
            } else {
                located.setStatus(StatusType.INVALID);
            }
            
            locatedArtworks.add(located);
        }
    }

    private static void createLocatedArtworksAttached(Artwork artwork, List<Attachment> attachments, List<ArtworkLocated> locatedArtworks) {
        for (Attachment attachment : attachments) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource("attachment#"+attachment.getAttachmentId());
            located.setPriority(8);
            located.setStageFile(attachment.getStageFile());
            located.setHashCode(attachment.getStageFile().getHashCode(attachment.getAttachmentId()));
            located.setImageType(attachment.getImageType());
            
            if (FileTools.isFileReadable(attachment.getStageFile())) {
                located.setStatus(StatusType.NEW);
            } else {
                located.setStatus(StatusType.INVALID);
            }
            
            locatedArtworks.add(located);
        }
    }

    private Set<String> determinePriorities(String configkey, Set<String> possibleScanners) {
        final String configValue = this.configServiceWrapper.getProperty(configkey, "");
        Set<String> result = ArtworkTools.determinePriorities(configValue, possibleScanners);
        LOG.trace("{} --> {}", configkey, result);
        return result;
    }
}
