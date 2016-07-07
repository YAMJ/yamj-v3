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

import static org.yamj.core.database.model.type.FileType.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yamj.core.service.mediaimport.MediaImportService;
import org.yamj.core.tools.ExceptionTools;

@Component
public class ImportScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ImportScheduler.class);
    private static final ReentrantLock IMPORT_LOCK = new ReentrantLock();
    private static final String STAGING_ERROR = "Staging Error";
    private static final String DATABASE_ERROR = "Database Error";

    // start with an initial media import
    private final AtomicBoolean watchProcess = new AtomicBoolean(true);

    @Autowired
    private MediaImportService mediaImportService;
    @Autowired
    private MetadataScanScheduler metadataScanScheduler;
    @Autowired
    private MediaFileScanScheduler mediaFileScanScheduler;
    @Autowired
    private ArtworkProcessScheduler artworkProcessScheduler;
    
    public void trigger() {
        LOG.trace("Trigger media import");
        watchProcess.set(true);
    }

    @Scheduled(initialDelay = 2000, fixedDelay = 1000)
    public void run() {
        if (watchProcess.get() && IMPORT_LOCK.tryLock()) {
            try {
                processStageFiles();
            } finally {
                IMPORT_LOCK.unlock();
            }
            watchProcess.set(false);
        }
    }

    private void processStageFiles() { //NOSONAR
        Long id = null;
        int counter = 0;
        
        // PROCESS VIDEOS
        do {
            id = null;
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(VIDEO);
                if (id != null) {
                    LOG.trace("Process video stage file: {}", id);
                    mediaImportService.processVideo(id);
                    LOG.info("Processed video stage file: {}", id);
                    counter++;

                    // trigger media file scan after 20 processed videos
                    if ((counter % 20) == 0) { 
                        mediaFileScanScheduler.trigger();
                    }
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of video stage file {}", id);
                } else if (id == null) {
                    LOG.error("Failed to get next video stage file", error);
                } else {
                    LOG.error("Failed to process video stage file {}", id);
                    LOG.error(STAGING_ERROR, error);
                    
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ex) {
                        // leave status as it is in any error case
                        LOG.trace(DATABASE_ERROR, ex);
                    }
                }
            }
        } while (id != null);
        if (counter > 0) {
            // trigger scan of media files and meta data if video files has been processed
            mediaFileScanScheduler.trigger();
            metadataScanScheduler.triggerScanVideo();
            counter = 0;
        }
        
        // PROCESS NFOS
        do {
            id = null;
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(NFO);
                if (id != null) {
                    LOG.trace("Process nfo stage file: {}", id);
                    mediaImportService.processNfo(id);
                    LOG.info("Processed nfo stage file: {}", id);
                    counter++;
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of nfo stage file {}", id);
                } else if (id == null) {
                    LOG.error("Failed to get next nfo stage file", error);
                } else {
                    LOG.error("Failed to process nfo stage file {}", id);
                    LOG.warn(STAGING_ERROR, error);
                    
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ex) {
                        // leave status as it is in any error case
                        LOG.trace(DATABASE_ERROR, ex);
                    }
                }
            }
        } while (id != null);
        if (counter > 0) {
            // trigger scan of meta data when NFOs has been processed
            metadataScanScheduler.triggerScanVideo();
            counter = 0;
        }

        // PROCESS IMAGES
        do {
            id = null;
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(IMAGE);
                if (id != null) {
                    LOG.trace("Process image stage file: {}", id);
                    mediaImportService.processImage(id);
                    LOG.info("Processed image stage file: {}", id);
                    counter++;
                    
                    // trigger artwork process after 20 processed images
                    if ((counter % 20) == 0) { 
                        artworkProcessScheduler.trigger();
                    }
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of image stage file {}", id);
                } else if (id == null) {
                    LOG.error("Failed to get next image stage file", error);
                } else {
                    LOG.error("Failed to process image stage file {}", id);
                    LOG.warn(STAGING_ERROR, error);
                    
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ex) {
                        // leave status as it is in any error case
                        LOG.trace(DATABASE_ERROR, ex);
                    }
                }
            }
        } while (id != null);
        if (counter > 0) {
            // trigger artwork process if images has been processed
            artworkProcessScheduler.trigger();
            counter = 0;
        }

        // PROCESS WATCHED
        do {
            id = null;
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(WATCHED);
                if (id != null) {
                    LOG.trace("Process watched stage file: {}", id);
                    mediaImportService.processWatched(id);
                    LOG.info("Processed watched stage file: {}", id);
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of watched stage file {}", id);
                } else if (id == null) {
                    LOG.error("Failed to get next watched stage file", error);
                } else {
                    LOG.error("Failed to process watched stage file {}", id);
                    LOG.warn(STAGING_ERROR, error);
                    
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ex) {
                        // leave status as it is in any error case
                        LOG.trace(DATABASE_ERROR, ex);
                    }
                }
            }
        } while (id != null);

        // PROCESS SUBTITLE
        do {
            id = null;
            try {
                // find next stage file to process
                id = mediaImportService.getNextStageFileId(SUBTITLE);
                if (id != null) {
                    LOG.trace("Process subtitle stage file: {}", id);
                    mediaImportService.processSubtitle(id);
                    LOG.info("Processed subtitle stage file: {}", id);
                }
            } catch (Exception error) {
                if (ExceptionTools.isLockingError(error)) {
                    LOG.warn("Locking error during import of subtitle stage file {}", id);
                } else if (id == null) {
                    LOG.error("Failed to get next subtitle stage file", error);
                } else {
                    LOG.error("Failed to process subtitle stage file {}", id);
                    LOG.warn(STAGING_ERROR, error);
                    
                    try {
                        mediaImportService.processingError(id);
                    } catch (Exception ex) {
                        // leave status as it is in any error case
                        LOG.trace(DATABASE_ERROR, ex);
                    }
                }
            }
        } while (id != null);
    }
}
