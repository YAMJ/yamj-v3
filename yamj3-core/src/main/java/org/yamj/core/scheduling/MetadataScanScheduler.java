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
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.ExecutionTask;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ExecutionTaskStorageService;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.service.metadata.MetadataScannerService;

@Component
public class MetadataScanScheduler extends AbstractQueueScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataScanScheduler.class);
    private static final ReentrantLock SCANNING_LOCK = new ReentrantLock();

    @Autowired
    private ConfigService configService;
    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private MetadataScannerService metadataScannerService;
    @Autowired 
    private ArtworkScanScheduler artworkScanScheduler;
    @Autowired 
    private TrailerScanScheduler trailerScanScheduler;
    @Autowired
    private ExecutionTaskStorageService executionTaskStorageService;
    
    private boolean messageDisabledVideo = false;       // Have we already printed the disabled message
    private boolean messageDisabledPeople = false;       // Have we already printed the disabled message
    private boolean messageDisabledFilmography = false;  // Have we already printed the disabled message
    private boolean videosHasBeenScanned = false;
    
    private final AtomicBoolean watchScanVideo = new AtomicBoolean(false);
    private final AtomicBoolean watchScanPeople = new AtomicBoolean(false);
    private final AtomicBoolean watchScanFilmography = new AtomicBoolean(false);

    @Scheduled(initialDelay = 1000, fixedDelay = 30000)
    public void trigger() {
        LOG.trace("Trigger metadata scan");
        watchScanVideo.set(true);
        watchScanPeople.set(true);
        watchScanFilmography.set(true);
    }
    
    public void triggerScanVideo() {
        LOG.trace("Trigger video scan");
        watchScanVideo.set(true);
    }
    
    public void triggerScanPeople() {
        LOG.trace("Trigger people scan");
        watchScanPeople.set(true);
    }

    public void triggerScanFilmography() {
        LOG.trace("Trigger filmogprahy scan");
        watchScanFilmography.set(true);
    }

    @Scheduled(initialDelay = 2000, fixedDelay = 1000)
    public void runAllScans() {
        if (SCANNING_LOCK.tryLock()) {
            try {
                if (watchScanVideo.get()) {
                    scanVideo();
                }
                if (watchScanPeople.get()) {
                    scanPeople();
                }
                if (watchScanFilmography.get()) {
                    scanFilmography();
                }
            } finally {
                SCANNING_LOCK.unlock();
            }
        }
    }

    private void scanVideo() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.metadatascan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledVideo) {
                messageDisabledVideo = true;
                LOG.info("Metadata scanning is disabled");
            }
            watchScanVideo.set(false);
            videosHasBeenScanned = false;
        } else {
            
            if (messageDisabledVideo) {
                LOG.info("Metadata scanning is enabled");
                messageDisabledVideo = false;
            }
    
            int maxResults = Math.max(1,configService.getIntProperty("yamj3.scheduler.metadatascan.maxResults", 30));
            List<QueueDTO> queueElements = metadataStorageService.getMetaDataQueueForScanning(maxResults);
            if (CollectionUtils.isEmpty(queueElements)) {
                LOG.trace("No metadata found to scan");
                watchScanVideo.set(false);
                
                if (videosHasBeenScanned) {
                    // this indicated that in previous runs videos has been scanned
                    // so force run of Trakt.TV task
                    try {
                        ExecutionTask task = executionTaskStorageService.getExecutionTask("trakttv");
                        if (task != null) {
                            task.setNextExecution(LocalDateTime.fromDateFields(task.getNextExecution()).minusYears(2).toDate());
                            this.executionTaskStorageService.updateEntity(task);                            
                        }
                    } catch (Exception ignore) {
                        // ignore any exception
                    }
                    
                    // now all vidoes has been scanned again
                    videosHasBeenScanned = false;
                }
                
            } else {
                LOG.info("Found {} metadata objects to process; scan with {} threads", queueElements.size(), maxThreads);
                threadedProcessing(queueElements, maxThreads, metadataScannerService);
                LOG.debug("Finished metadata scanning");
                videosHasBeenScanned = true;
            }
        }
        
        // trigger people scan
        watchScanPeople.set(true);
        // trigger artwork scan
        artworkScanScheduler.trigger();
        // trigger trailer scan
        trailerScanScheduler.trigger();
    }

    private void scanPeople() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.peoplescan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledPeople) {
                messageDisabledPeople = true;
                LOG.info("People scanning is disabled");
            }
            watchScanPeople.set(false);
        } else {
            
            if (messageDisabledPeople) {
                LOG.info("People scanning is enabled");
                messageDisabledPeople = false;
            }
    
            int maxResults = Math.max(1,configService.getIntProperty("yamj3.scheduler.peoplescan.maxResults", 50));
            List<QueueDTO> queueElements = metadataStorageService.getPersonQueueForScanning(maxResults);
            if (CollectionUtils.isEmpty(queueElements)) {
                LOG.trace("No people data found to scan");
                watchScanPeople.set(false);
                watchScanFilmography.set(true);
            } else {
                LOG.info("Found {} people objects to process; scan with {} threads", queueElements.size(), maxThreads);
                threadedProcessing(queueElements, maxThreads, metadataScannerService);
                LOG.debug("Finished people data scanning");
            }
        }
        
        // trigger filmography scan
        watchScanFilmography.set(true);
        // trigger artwork scan
        artworkScanScheduler.trigger();
    }

    private void scanFilmography() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.filmographyscan.maxThreads", 1);
        if (maxThreads <= 0) { 
            if (!messageDisabledFilmography) {
                messageDisabledFilmography = true;
                LOG.info("Filmography scanning is disabled");
            }
            watchScanFilmography.set(false);
            return;
        }
        
        if (messageDisabledFilmography) {
            LOG.info("Filmography scanning is enabled");
            messageDisabledFilmography = false;
        }

        int maxResults = Math.max(1,configService.getIntProperty("yamj3.scheduler.filmographyscan.maxResults", 50));
        List<QueueDTO> queueElements = metadataStorageService.getFilmographyQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No filmography data found to scan");
            watchScanFilmography.set(false);
            return;
        }

        LOG.info("Found {} filmography objects to process; scan with {} threads", queueElements.size(), maxThreads);
        threadedProcessing(queueElements, maxThreads, metadataScannerService);
        LOG.debug("Finished filmography data scanning");
    }
}
