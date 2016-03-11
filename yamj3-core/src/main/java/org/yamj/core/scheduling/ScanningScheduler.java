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
package org.yamj.core.scheduling;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.database.service.TrailerStorageService;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.metadata.MetadataScannerService;
import org.yamj.core.service.trailer.TrailerScannerService;

@Component
public class ScanningScheduler extends AbstractQueueScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ScanningScheduler.class);
    private static final ReentrantLock SCANNING_LOCK = new ReentrantLock();

    @Autowired
    private ConfigService configService;
    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private MetadataScannerService metadataScannerService;
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ArtworkProcessScheduler artworkProcessScheduler;
    @Autowired
    private TrailerScannerService trailerScannerService;
    @Autowired
    private TrailerStorageService trailerStorageService;
    @Autowired
    private TrailerProcessScheduler trailerProcessScheduler;
    
    private boolean messageDisabledMetaData = Boolean.FALSE;     // Have we already printed the disabled message
    private boolean messageDisabledPeople = Boolean.FALSE;       // Have we already printed the disabled message
    private boolean messageDisabledFilmography = Boolean.FALSE;  // Have we already printed the disabled message
    private boolean messageDisabledArtwork = Boolean.FALSE;      // Have we already printed the disabled message
    private boolean messageDisabledTrailer = Boolean.FALSE;      // Have we already printed the disabled message

    private final AtomicBoolean watchScanMetaData = new AtomicBoolean(false);
    private final AtomicBoolean watchScanPeopleData = new AtomicBoolean(false);
    private final AtomicBoolean watchScanFilmography = new AtomicBoolean(false);
    private final AtomicBoolean watchScanArtwork = new AtomicBoolean(false);
    private final AtomicBoolean watchScanTrailer = new AtomicBoolean(false);

    @Scheduled(initialDelay = 1000, fixedDelay = 300000)
    public void triggerAllScans() {
        LOG.trace("Trigger scan for all");
        watchScanMetaData.set(true);
        watchScanPeopleData.set(true);
        watchScanFilmography.set(true);
        watchScanArtwork.set(true);
        watchScanTrailer.set(true);
    }
    
    public void triggerScanMetaData() {
        LOG.trace("Trigger scan of meta data");
        watchScanMetaData.set(true);
    }
    
    public void triggerScanPeopleData() {
        LOG.trace("Trigger scan of people data");
        watchScanPeopleData.set(true);
    }

    public void triggerScanFilmography() {
        LOG.trace("Trigger scan of filmogprahy");
        watchScanFilmography.set(true);
    }

    public void triggerScanArtwork() {
        LOG.trace("Trigger scan of artwork");
        watchScanArtwork.set(true);
    }

    public void triggerScanTrailer() {
        LOG.trace("Trigger scan of trailer");
        watchScanTrailer.set(true);
    }

    @Scheduled(initialDelay = 2000, fixedDelay = 1000)
    public void runAllScans() {
        if (SCANNING_LOCK.tryLock()) {
            try {
                if (watchScanMetaData.get()) {
                    scanMetaData();
                }
                if (watchScanPeopleData.get()) {
                    scanPeopleData();
                }
                if (watchScanFilmography.get()) {
                    scanFilmography();
                }
                if (watchScanArtwork.get()) {
                    scanArtwork();
                }
                if (watchScanTrailer.get()) {
                    scanTrailer();
                }
            } finally {
                SCANNING_LOCK.unlock();
            }
        }
    }

    private void scanMetaData() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.metadatascan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledMetaData) {
                messageDisabledMetaData = Boolean.TRUE;
                LOG.info("Metadata scanning is disabled");
            }
            watchScanMetaData.set(false);
            watchScanPeopleData.set(true);
            watchScanTrailer.set(true);
            return;
        }
        
        if (messageDisabledMetaData) {
            LOG.info("Metadata scanning is enabled");
            messageDisabledMetaData = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.metadatascan.maxResults", 30);
        List<QueueDTO> queueElements = metadataStorageService.getMetaDataQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No metadata found to scan");
            watchScanMetaData.set(false);
            watchScanPeopleData.set(true);
            watchScanTrailer.set(true);
            return;
        }

        LOG.info("Found {} metadata objects to process; scan with {} threads", queueElements.size(), maxThreads);
        threadedProcessing(queueElements, maxThreads, metadataScannerService);

        // trigger scan for people data and trailer
        watchScanPeopleData.set(true);
        watchScanTrailer.set(true);
        
        LOG.debug("Finished metadata scanning");
    }

    private void scanPeopleData() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.peoplescan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledPeople) {
                messageDisabledPeople = Boolean.TRUE;
                LOG.info("People scanning is disabled");
            }
            watchScanPeopleData.set(false);
            watchScanFilmography.set(true);
            return;
        }
        
        if (messageDisabledPeople) {
            LOG.info("People scanning is enabled");
            messageDisabledPeople = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.peoplescan.maxResults", 50);
        List<QueueDTO> queueElements = metadataStorageService.getPersonQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No people data found to scan");
            watchScanPeopleData.set(false);
            watchScanFilmography.set(true);
            return;
        }

        LOG.info("Found {} people objects to process; scan with {} threads", queueElements.size(), maxThreads);
        threadedProcessing(queueElements, maxThreads, metadataScannerService);

        // trigger scan for filmography
        watchScanFilmography.set(true);
        
        LOG.debug("Finished people data scanning");
    }

    private void scanFilmography() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.filmographyscan.maxThreads", 1);
        if (maxThreads <= 0) { 
            if (!messageDisabledFilmography) {
                messageDisabledFilmography = Boolean.TRUE;
                LOG.info("Filmography scanning is disabled");
            }
            watchScanFilmography.set(false);
            watchScanArtwork.set(true);
            return;
        }
        
        if (messageDisabledFilmography) {
            LOG.info("Filmography scanning is enabled");
            messageDisabledFilmography = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.filmographyscan.maxResults", 50);
        List<QueueDTO> queueElements = metadataStorageService.getFilmographyQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No filmography data found to scan");
            watchScanFilmography.set(false);
            watchScanArtwork.set(true);
            return;
        }

        LOG.info("Found {} filmography objects to process; scan with {} threads", queueElements.size(), maxThreads);
        threadedProcessing(queueElements, maxThreads, metadataScannerService);

        // trigger scan for artwork
        watchScanArtwork.set(true);
        
        LOG.debug("Finished filmography data scanning");
    }

    private void scanArtwork() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.artworkscan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledArtwork) {
                messageDisabledArtwork = Boolean.TRUE;
                LOG.info("Artwork scanning is disabled");
            }
            watchScanArtwork.set(false);
            return;
        }
        
        if (messageDisabledArtwork) {
            LOG.info("Artwork scanning is enabled");
            messageDisabledArtwork = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.artworkscan.maxResults", 60);
        List<QueueDTO> queueElements = artworkStorageService.getArtworkQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No artwork found to scan");
            watchScanArtwork.set(false);
            return;
        }

        LOG.info("Found {} artwork objects to process; scan with {} threads", queueElements.size(), maxThreads);
        threadedProcessing(queueElements, maxThreads, artworkScannerService);

        // trigger artwork processing
        this.artworkProcessScheduler.trigger();
        
        LOG.debug("Finished artwork scanning");
    }

    private void scanTrailer() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.trailerscan.maxThreads", 0);
        if (maxThreads <= 0) {
            if (!messageDisabledTrailer) {
                messageDisabledTrailer = Boolean.TRUE;
                LOG.info("Trailer scanning is disabled");
            }
            watchScanTrailer.set(false);
            return;
        }
        
        if (messageDisabledTrailer) {
            LOG.info("Trailer scanning is enabled");
            messageDisabledTrailer = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.trailerscan.maxResults", 30);
        List<QueueDTO> queueElements = trailerStorageService.getTrailerQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No trailer found to scan");
            watchScanTrailer.set(false);
            return;
        }

        LOG.info("Found {} trailer objects to process; scan with {} threads", queueElements.size(), maxThreads);
        threadedProcessing(queueElements, maxThreads, trailerScannerService);

        // trigger trailer processing
        this.trailerProcessScheduler.trigger();
        
        LOG.debug("Finished trailer scanning");
    }
}
