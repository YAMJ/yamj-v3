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
import org.yamj.core.database.service.MediaStorageService;
import org.yamj.core.service.mediainfo.MediaInfoService;

@Component
public class MediaFileScanScheduler extends AbstractQueueScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(MediaFileScanScheduler.class);
    private static final ReentrantLock PROCESS_LOCK = new ReentrantLock();

    @Autowired
    private ConfigService configService;
    @Autowired
    private MediaStorageService mediaStorageService;
    @Autowired
    private MediaInfoService mediaInfoService;
    
    private boolean messageDisabled = Boolean.FALSE;    // Have we already printed the disabled message
    private final AtomicBoolean watchProcess = new AtomicBoolean(false);

    @Scheduled(initialDelay = 5000, fixedDelay = 3600000)
    public void trigger() {
        LOG.trace("Trigger media file scan");
        watchProcess.set(true);
    }

    @Scheduled(initialDelay = 6000, fixedDelay = 1000)
    public void run() {
        if (watchProcess.get() && PROCESS_LOCK.tryLock()) {
            try {
                scanMediaFiles();
            } finally {
                PROCESS_LOCK.unlock();
            }
        }
    }
    
    private void scanMediaFiles() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.mediafilescan.maxThreads", 1);
        if (maxThreads <= 0) {
            if (!messageDisabled) {
                messageDisabled = Boolean.TRUE;
                LOG.info("Media file scanning is disabled");
            }
            watchProcess.set(false);
            return;
        }
        
        if (messageDisabled) {
            LOG.info("Media file scanning is enabled");
            messageDisabled = Boolean.FALSE;
        }

        int maxResults = Math.max(1,configService.getIntProperty("yamj3.scheduler.mediafilescan.maxResults", 50));
        List<QueueDTO> queueElements = mediaStorageService.getMediaFileQueue(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No media files found to scan");
            watchProcess.set(false);
            return;
        }

        LOG.info("Found {} media files to process; scan with {} threads", queueElements.size(), maxThreads);
        threadedProcessing(queueElements, maxThreads, mediaInfoService);

        LOG.debug("Finished media file scanning");
    }
}
