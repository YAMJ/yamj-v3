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
import org.yamj.core.service.artwork.ArtworkProcessRunner;
import org.yamj.core.service.artwork.ArtworkProcessorService;

@Service
public class ArtworkProcessScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkProcessScheduler.class);
    private static final int ARTWORK_PROCESSOR_MAX_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.artworkprocess.maxThreads", 1);
    private static final int ARTWORK_PROCESSOR_MAX_RESULTS = PropertyTools.getIntProperty("yamj3.scheduler.artworkprocess.maxResults", 20);

    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ArtworkProcessorService artworkProcessorService;

    @Scheduled(initialDelay = 30000, fixedDelay = 60000)
    public void processArtwork() throws Exception {
        if (ARTWORK_PROCESSOR_MAX_THREADS <= 0) {
            LOG.info("Artwork processing is disabled");
            return;
        }
        
        List<QueueDTO> queueElements = artworkStorageService.getArtworLocatedQueue(ARTWORK_PROCESSOR_MAX_RESULTS);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No artwork found to process");
            return;
        }

        LOG.info("Found {} artwork objects to process; process with {} threads", queueElements.size(), ARTWORK_PROCESSOR_MAX_THREADS);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(ARTWORK_PROCESSOR_MAX_THREADS);
        for (int i = 0; i < ARTWORK_PROCESSOR_MAX_THREADS; i++) {
            ArtworkProcessRunner worker = new ArtworkProcessRunner(queue, artworkProcessorService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {}
        }

        LOG.debug("Finished artwork processing");
    }
}
