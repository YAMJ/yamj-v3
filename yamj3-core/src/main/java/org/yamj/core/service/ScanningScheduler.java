/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.core.service;

import org.yamj.core.database.model.type.StepType;

import java.util.List;
import java.util.concurrent.*;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.database.service.MediaStorageService;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.service.artwork.ArtworkScannerRunner;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.mediainfo.MediaInfoRunner;
import org.yamj.core.service.mediainfo.MediaInfoService;
import org.yamj.core.service.nfo.NfoScannerRunner;
import org.yamj.core.service.nfo.NfoScannerService;
import org.yamj.core.service.plugin.PluginMetadataRunner;
import org.yamj.core.service.plugin.PluginMetadataService;

@Service
public class ScanningScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ScanningScheduler.class);
    
    @Autowired
    private ConfigService configService;
    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private PluginMetadataService pluginMetadataService;
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private MediaStorageService mediaStorageService;
    @Autowired
    private MediaInfoService mediaInfoService;
    @Autowired
    private NfoScannerService nfoScannerService;
    
    private boolean messageDisabledMediaData = Boolean.FALSE;    // Have we already printed the disabled message
    private boolean messageDisabledMediaFiles = Boolean.FALSE;   // Have we already printed the disabled message
    private boolean messageDisabledPeople = Boolean.FALSE;       // Have we already printed the disabled message
    private boolean messageDisabledArtwork = Boolean.FALSE;      // Have we already printed the disabled message
    private boolean messageDisabledNfo = Boolean.FALSE;          // Have we already printed the disabled message


    @Scheduled(initialDelay = 5000, fixedDelay = 45000)
    public void scanMediaFiles() throws Exception {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.mediafilescan.maxThreads", 1);
        if (maxThreads <= 0 || !mediaInfoService.isMediaInfoActivated()) {
            if (!messageDisabledMediaFiles) {
                messageDisabledMediaFiles = Boolean.TRUE;
                LOG.info("Media file scanning is disabled");
            }
            return;
        } else {
            messageDisabledMediaFiles = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.mediafilescan.maxResults", 20);
        List<QueueDTO> queueElements = mediaStorageService.getMediaFileQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No media files found to scan");
            return;
        }

        LOG.info("Found {} media files to process; scan with {} threads", queueElements.size(), maxThreads);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < maxThreads; i++) {
            MediaInfoRunner worker = new MediaInfoRunner(queue, mediaInfoService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignore) {
            }
        }

        LOG.debug("Finished media file scanning");
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 45000)
    public void scanMediaDataNfo() throws Exception {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.nfoscan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledNfo) {
                messageDisabledNfo = Boolean.TRUE;
                LOG.info("NFO scanning is disabled");
            }
            return;
        } else {
            messageDisabledNfo = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.nfoscan.maxResults", 20);
        List<QueueDTO> queueElements = metadataStorageService.getMediaQueueForScanning(maxResults, StepType.NFO);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No media data found for nfo scan");
            return;
        }

        LOG.info("Found {} media objects for nfo scan; scan with {} threads", queueElements.size(), maxThreads);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < maxThreads; i++) {
            NfoScannerRunner worker = new NfoScannerRunner(queue, nfoScannerService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignore) {
            }
        }

        LOG.debug("Finished nfo scanning");
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 45000)
    public void scanMediaDataOnline() throws Exception {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.mediadatascan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledMediaData) {
                messageDisabledMediaData = Boolean.TRUE;
                LOG.info("Media data scanning is disabled");
            }
            return;
        } else {
            messageDisabledMediaData = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.mediadatascan.maxResults", 20);
        List<QueueDTO> queueElements = metadataStorageService.getMediaQueueForScanning(maxResults, StepType.ONLINE);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No media data found for online scan");
            return;
        }

        LOG.info("Found {} media data objects to process; scan with {} threads", queueElements.size(), maxThreads);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < maxThreads; i++) {
            PluginMetadataRunner worker = new PluginMetadataRunner(queue, pluginMetadataService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignore) {
            }
        }

        LOG.debug("Finished media data scanning");
    }

    @Scheduled(initialDelay = 20000, fixedDelay = 45000)
    public void scanPeopleData() throws Exception {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.peoplescan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledPeople) {
                messageDisabledPeople = Boolean.TRUE;
                LOG.info("People scanning is disabled");
            }
            return;
        } else {
            messageDisabledPeople = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.peoplescan.maxResults", 50);
        List<QueueDTO> queueElements = metadataStorageService.getPersonQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No people data found to scan");
            return;
        }

        LOG.info("Found {} people objects to process; scan with {} threads", queueElements.size(), maxThreads);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < maxThreads; i++) {
            PluginMetadataRunner worker = new PluginMetadataRunner(queue, pluginMetadataService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignore) {
            }

        }

        LOG.debug("Finished people data scanning");
    }

    @Scheduled(initialDelay = 30000, fixedDelay = 45000)
    public void scanArtwork() throws Exception {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.artworkscan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabledArtwork) {
                messageDisabledArtwork = Boolean.TRUE;
                LOG.info("Artwork scanning is disabled");
            }
            return;
        } else {
            messageDisabledArtwork = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.artworkscan.maxResults", 30);
        List<QueueDTO> queueElements = artworkStorageService.getArtworkQueueForScanning(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No artwork found to scan");
            return;
        }

        LOG.info("Found {} artwork objects to process; scan with {} threads", queueElements.size(), maxThreads);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < maxThreads; i++) {
            ArtworkScannerRunner worker = new ArtworkScannerRunner(queue, artworkScannerService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignore) {
            }
        }

        LOG.debug("Finished artwork scanning");
    }
}
