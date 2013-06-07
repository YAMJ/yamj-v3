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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.service.artwork.ArtworkScannerRunner;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.plugin.PluginMetadataRunner;
import org.yamj.core.service.plugin.PluginMetadataService;

@Service
public class MetadataScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataScheduler.class);
    private static final int MEDIA_SCANNER_MAX_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.mediascan.maxThreads", 5);
    private static final int MEDIA_SCANNER_MAX_RESULTS = PropertyTools.getIntProperty("yamj3.scheduler.mediascan.maxResults", 20);
    private static final int PEOPLE_SCANNER_MAX_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.peoplescan.maxThreads", 5);
    private static final int PEOPLE_SCANNER_MAX_RESULTS = PropertyTools.getIntProperty("yamj3.scheduler.peoplescan.maxResults", 40);
    private static final int ARTWORK_SCANNER_MAX_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.artworkscan.maxThreads", 3);
    private static final int ARTWORK_SCANNER_MAX_RESULTS = PropertyTools.getIntProperty("yamj3.scheduler.artworkscan.maxResults", 30);

    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private PluginMetadataService pluginMetadataService;
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ArtworkScannerService artworkScannerService;

    @Scheduled(initialDelay = 5000, fixedDelay = 45000)
    public void scanMediaData() throws Exception {
        List<QueueDTO> queueElements = metadataStorageService.getMediaQueueForScanning(MEDIA_SCANNER_MAX_RESULTS);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No media data found to scan");
            return;
        }

        LOG.info("Found {} media data objects to process", queueElements.size());
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(MEDIA_SCANNER_MAX_THREADS);
        for (int i = 0; i < MEDIA_SCANNER_MAX_THREADS; i++) {
            PluginMetadataRunner worker = new PluginMetadataRunner(queue, pluginMetadataService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {}
        }

        LOG.debug("Finished media data scanning");
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 45000)
    public void scanPeopleData() throws Exception {
        List<QueueDTO> queueElements = metadataStorageService.getPersonQueueForScanning(PEOPLE_SCANNER_MAX_RESULTS);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No people data found to scan");
            return;
        }

        LOG.info("Found {} people objects to process", queueElements.size());
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
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {}

        }

        LOG.debug("Finished person data scanning");
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 45000)
    public void scanArtwork() throws Exception {
        List<QueueDTO> queueElements = artworkStorageService.getArtworkQueueForScanning(ARTWORK_SCANNER_MAX_RESULTS);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No artwork found to scan");
            return;
        }

        LOG.info("Found {} artwork objects to process", queueElements.size());
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
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {}
        }

        LOG.debug("Finished artwork scanning");
    }

}
