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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.service.artwork.ArtworkProcessRunner;
import org.yamj.core.service.artwork.ArtworkProcessorService;
import org.yamj.core.tools.ThreadTools;

@Component
public class ArtworkProcessScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkProcessScheduler.class);
    private static final ReentrantLock ARTWORK_PROCESS_LOCK = new ReentrantLock();

    @Autowired
    private ConfigService configService;
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ArtworkProcessorService artworkProcessorService;

    private boolean messageDisabled = Boolean.FALSE;    // Have we already printed the disabled message
    private final AtomicBoolean watchProcess = new AtomicBoolean(false);

    @Scheduled(initialDelay = 5000, fixedDelay = 300000)
    public void triggerProcess() {
        LOG.trace("Trigger process");
        watchProcess.set(true);
    }

    @Async
    @Scheduled(initialDelay = 6000, fixedDelay = 1000)
    public void runProcess() {
        if (watchProcess.get() && ARTWORK_PROCESS_LOCK.tryLock()) {
            try {
                processArtwork();
            } finally {
                ARTWORK_PROCESS_LOCK.unlock();
            }
        }
    }
    
    private void processArtwork() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.artworkprocess.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabled) {
                messageDisabled = Boolean.TRUE;
                LOG.info("Artwork processing is disabled");
            }
            watchProcess.set(false);
            return;
        }
        
        if (messageDisabled) {
            LOG.info("Artwork processing is enabled");
            messageDisabled = Boolean.FALSE;
        }

        int maxResults = configService.getIntProperty("yamj3.scheduler.artworkprocess.maxResults", 20);
        List<QueueDTO> queueElements = artworkStorageService.getArtworLocatedQueue(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No artwork found to process");
            watchProcess.set(false);
            return;
        }

        LOG.info("Found {} artwork objects to process; process with {} threads", queueElements.size(), maxThreads);
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < maxThreads; i++) {
            executor.execute(new ArtworkProcessRunner(queue, artworkProcessorService));
        }
        ThreadTools.waitForTermination(executor);

        LOG.debug("Finished artwork processing");
    }
}
