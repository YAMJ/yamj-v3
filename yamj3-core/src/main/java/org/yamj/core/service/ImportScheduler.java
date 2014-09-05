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

import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.service.mediaimport.MediaImportService;
import org.yamj.core.tools.ExceptionTools;

@Service
public class ImportScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ImportScheduler.class);
    private static final ReentrantLock PROCESS_LOCK = new ReentrantLock();
    
    @Autowired
    private MediaImportService mediaImportService;

    @Async
    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    public void processStageFiles() throws Exception {
        Long id = null;

        // PROCESS VIDEOS
        do {
            PROCESS_LOCK.lock();
            
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(FileType.VIDEO, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.debug("Process video stage file: {}", id);
                    mediaImportService.processVideo(id);
                    LOG.info("Processed video stage file: {}", id);
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of video stage file {}", id);
                } else {
                    LOG.error("Failed to process video stage file {}", id);
                    LOG.error("Staging error", error);
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ignore) {}
                }
            } finally {
                PROCESS_LOCK.unlock();
            }
        } while (id != null);

        // PROCESS NFOS
        do {
            PROCESS_LOCK.lock();
            
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(FileType.NFO, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.debug("Process nfo stage file: {}", id);
                    mediaImportService.processNfo(id);
                    LOG.info("Processed nfo stage file: {}", id);
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of nfo stage file {}", id);
                } else {
                    LOG.error("Failed to process nfo stage file {}", id);
                    LOG.warn("Staging error", error);
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ignore) {}
                }
            } finally {
                PROCESS_LOCK.unlock();
            }
        } while (id != null);

        // PROCESS IMAGES

        // PROCESS SUBTITLES

        // PROCESS PEOPLE
    }
}
