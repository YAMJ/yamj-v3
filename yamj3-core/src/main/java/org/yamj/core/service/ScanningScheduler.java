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
package org.yamj.core.service;

import java.util.List;
import java.util.concurrent.*;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.database.service.MediaStorageService;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.service.artwork.ArtworkScannerRunner;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.mediainfo.MediaInfoRunner;
import org.yamj.core.service.mediainfo.MediaInfoService;
import org.yamj.core.service.plugin.PluginMetadataRunner;
import org.yamj.core.service.plugin.PluginMetadataService;

@Service
public class ScanningScheduler implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ScanningScheduler.class);
    private static final int MEDIAFILE_SCANNER_MAX_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.mediafilescan.maxThreads", 1);
    private static final int MEDIAFILE_SCANNER_MAX_RESULTS = PropertyTools.getIntProperty("yamj3.scheduler.mediafilescan.maxResults", 20);
    private static final int MEDIADATA_SCANNER_MAX_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.mediadatascan.maxThreads", 1);
    private static final int MEDIADATA_SCANNER_MAX_RESULTS = PropertyTools.getIntProperty("yamj3.scheduler.mediadatascan.maxResults", 20);
    private static final int PEOPLE_SCANNER_MAX_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.peoplescan.maxThreads", 1);
    private static final int PEOPLE_SCANNER_MAX_RESULTS = PropertyTools.getIntProperty("yamj3.scheduler.peoplescan.maxResults", 50);
    private static final int ARTWORK_SCANNER_MAX_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.artworkscan.maxThreads", 1);
    private static final int ARTWORK_SCANNER_MAX_RESULTS = PropertyTools.getIntProperty("yamj3.scheduler.artworkscan.maxResults", 30);
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
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (MEDIADATA_SCANNER_MAX_THREADS <= 0) {
            LOG.info("Media data scanning is disabled");
        }
        if (MEDIAFILE_SCANNER_MAX_THREADS <= 0 || !mediaInfoService.isMediaInfoActivated()) {
            LOG.info("Media file scanning is disabled");
        }
        if (PEOPLE_SCANNER_MAX_THREADS <= 0) {
            LOG.info("People scanning is disabled");
        }
        if (ARTWORK_SCANNER_MAX_THREADS <= 0) {
            LOG.info("Artwork scanning is disabled");
        }
    }
    
    @Scheduled(initialDelay = 5000, fixedDelay = 45000)
    public void scanMediaData() throws Exception {
        if (MEDIADATA_SCANNER_MAX_THREADS <= 0) {
            return;
        }

        List<QueueDTO> queueElements = metadataStorageService.getMediaQueueForScanning(MEDIADATA_SCANNER_MAX_RESULTS);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No media data found to scan");
            return;
        }

        LOG.info("Found {} media data objects to process; scan with {} threads", queueElements.size(), MEDIADATA_SCANNER_MAX_THREADS);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(MEDIADATA_SCANNER_MAX_THREADS);
        for (int i = 0; i < MEDIADATA_SCANNER_MAX_THREADS; i++) {
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

    @Scheduled(initialDelay = 5000, fixedDelay = 45000)
    public void scanMediaFiles() throws Exception {
        if (MEDIAFILE_SCANNER_MAX_THREADS <= 0 || !mediaInfoService.isMediaInfoActivated()) {
            return;
        }

        List<QueueDTO> queueElements = mediaStorageService.getMediaFileQueueForScanning(MEDIAFILE_SCANNER_MAX_RESULTS);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No media files found to scan");
            return;
        }

        LOG.info("Found {} media files to process; scan with {} threads", queueElements.size(), MEDIAFILE_SCANNER_MAX_THREADS);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(MEDIAFILE_SCANNER_MAX_THREADS);
        for (int i = 0; i < MEDIAFILE_SCANNER_MAX_THREADS; i++) {
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
    public void scanPeopleData() throws Exception {
        if (PEOPLE_SCANNER_MAX_THREADS <= 0) {
            return;
        }

        List<QueueDTO> queueElements = metadataStorageService.getPersonQueueForScanning(PEOPLE_SCANNER_MAX_RESULTS);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No people data found to scan");
            return;
        }

        LOG.info("Found {} people objects to process; scan with {} threads", queueElements.size(), PEOPLE_SCANNER_MAX_THREADS);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(PEOPLE_SCANNER_MAX_THREADS);
        for (int i = 0; i < PEOPLE_SCANNER_MAX_THREADS; i++) {
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

    @Scheduled(initialDelay = 15000, fixedDelay = 45000)
    public void scanArtwork() throws Exception {
        if (ARTWORK_SCANNER_MAX_THREADS <= 0) {
            return;
        }

        List<QueueDTO> queueElements = artworkStorageService.getArtworkQueueForScanning(ARTWORK_SCANNER_MAX_RESULTS);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No artwork found to scan");
            return;
        }

        LOG.info("Found {} artwork objects to process; scan with {} threads", queueElements.size(), ARTWORK_SCANNER_MAX_THREADS);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(ARTWORK_SCANNER_MAX_THREADS);
        for (int i = 0; i < ARTWORK_SCANNER_MAX_THREADS; i++) {
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
