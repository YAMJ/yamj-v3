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
package org.yamj.core.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.service.mediaimport.MediaImportService;
import org.yamj.core.tools.ExceptionTools;

@Component
public class ImportScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ImportScheduler.class);
    private static final ReentrantLock IMPORT_LOCK = new ReentrantLock();
    
    @Autowired
    private MediaImportService mediaImportService;
    @Autowired
    private ScanningScheduler scanningScheduler;
    
    private final AtomicBoolean watchProcess = new AtomicBoolean(false);

    @Scheduled(initialDelay = 1000, fixedDelay = 300000)
    public void triggerProcess() {
        LOG.trace("Trigger process");
        watchProcess.set(true);
    }

    @Async
    @Scheduled(initialDelay = 2000, fixedDelay = 1000)
    public void runProcess() {
        if (IMPORT_LOCK.isLocked()) {
            // do nothing if locked
            return;
        }

        IMPORT_LOCK.lock();
        try {
            if (watchProcess.getAndSet(false)) processStageFiles();
        } finally {
            IMPORT_LOCK.unlock();
        }
    }

    private void processStageFiles() {
        Long id = null;

        // PROCESS VIDEOS
        do {
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(FileType.VIDEO, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.trace("Process video stage file: {}", id);
                    mediaImportService.processVideo(id);
                    LOG.info("Processed video stage file: {}", id);
                } else {
                    // trigger scan of media files and meta data
                    scanningScheduler.triggerScanMediaFiles();
                    scanningScheduler.triggerScanMetaData();
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of video stage file {}", id);
                } else {
                    LOG.error("Failed to process video stage file {}", id);
                    LOG.error("Staging error", error);
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ignore) {
                        // leave status as it is in any error case
                    }
                }
            }
        } while (id != null);

        // PROCESS NFOS
        do {
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(FileType.NFO, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.trace("Process nfo stage file: {}", id);
                    mediaImportService.processNfo(id);
                    LOG.info("Processed nfo stage file: {}", id);
                } else {
                    // trigger scan of meta data
                    scanningScheduler.triggerScanMetaData();
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of nfo stage file {}", id);
                } else {
                    LOG.error("Failed to process nfo stage file {}", id);
                    LOG.warn("Staging error", error);
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ignore) {
                        // leave status as it is in any error case
                    }
                }
            }
        } while (id != null);

        // PROCESS IMAGES
        do {
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(FileType.IMAGE, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.trace("Process image stage file: {}", id);
                    mediaImportService.processImage(id);
                    LOG.info("Processed image stage file: {}", id);
                } else {
                    // trigger scan of artwork
                    scanningScheduler.triggerScanArtwork();
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of image stage file {}", id);
                } else {
                    LOG.error("Failed to process image stage file {}", id);
                    LOG.warn("Staging error", error);
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ignore) {
                        // leave status as it is in any error case
                    }
                }
            }
        } while (id != null);

        // PROCESS WATCHED
        do {
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(FileType.WATCHED, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.trace("Process watched stage file: {}", id);
                    mediaImportService.processWatched(id);
                    LOG.info("Processed watched stage file: {}", id);
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of watched stage file {}", id);
                } else {
                    LOG.error("Failed to process watched stage file {}", id);
                    LOG.warn("Staging error", error);
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ignore) {
                        // leave status as it is in any error case
                    }
                }
            }
        } while (id != null);

        // PROCESS SUBTITLE
        do {
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(FileType.SUBTITLE, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.trace("Process subtitle stage file: {}", id);
                    mediaImportService.processSubtitle(id);
                    LOG.info("Processed subtitle stage file: {}", id);
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of subtitle stage file {}", id);
                } else {
                    LOG.error("Failed to process subtitle stage file {}", id);
                    LOG.warn("Staging error", error);
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ignore) {
                        // leave status as it is in any error case
                    }
                }
            }
        } while (id != null);
    }
}
