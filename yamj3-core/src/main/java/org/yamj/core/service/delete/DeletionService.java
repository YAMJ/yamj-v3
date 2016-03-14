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
package org.yamj.core.service.delete;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.dto.DeletionDTO;
import org.yamj.core.database.service.CommonStorageService;
import org.yamj.core.scheduling.ArtworkScanScheduler;
import org.yamj.core.service.file.FileStorageService;

/**
 * Task for checking if video, series or person is older than x days and marks
 * those data entries as updated in order to force a rescan.
 */
@Service("deletionService")
public class DeletionService {

    private static final Logger LOG = LoggerFactory.getLogger(DeletionService.class);
    private static final ReentrantLock DELETION_LOCK = new ReentrantLock();

    @Autowired
    private CommonStorageService commonStorageService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ArtworkScanScheduler artworkScanScheduler;
    
    public void executeAllDeletions() {
        if (DELETION_LOCK.tryLock()) {
            try {
                this.doExecuteAllDeletions();
            } finally {
                DELETION_LOCK.unlock();
            }
        }
    }
    
    private void doExecuteAllDeletions() {
        Set<String> filesToDelete = new HashSet<>();

        try {
            List<Long> ids = this.commonStorageService.getStageFilesForDeletion();
            // delete stage files
            for (Long id : ids) {
                try {
                    filesToDelete.addAll(this.commonStorageService.deleteStageFile(id));
                } catch (Exception ex) {
                    LOG.error("Failed to delete stage file ID: "+id, ex);
                }
            }
        } catch (Exception ex) {
            LOG.warn("Failed to retrieve stage files to delete", ex);
        }

        try {
            List<Long> ids = this.commonStorageService.getArtworkLocatedForDeletion();
            boolean updateTrigger = false;
                
            // delete stage files
            for (Long id : ids) {
                try {
                    DeletionDTO dto = this.commonStorageService.deleteArtworkLocated(id);
                    filesToDelete.addAll(dto.getFilesToDelete());
                    updateTrigger = updateTrigger || dto.isUpdateTrigger();
                } catch (Exception ex) {
                    LOG.error("Failed to delete located artwork ID: "+id, ex);
                }
            }
            
            // trigger artwork scan
            if (updateTrigger) {
                artworkScanScheduler.trigger();
            }
        } catch (Exception ex) {
            LOG.warn("Failed to retrieve located artworks to delete", ex);
        }

        try {
            List<Long> ids = this.commonStorageService.getTrailersToDelete();

            // delete trailers
            for (Long id : ids) {
                try {
                    String fileToDelete  = this.commonStorageService.deleteTrailer(id);
                    if (fileToDelete != null) {
                       filesToDelete.add(fileToDelete);
                    }
                } catch (Exception ex) {
                    LOG.error("Failed to delete trailer ID: "+id, ex);
                }
            }
        } catch (Exception ex) {
            LOG.warn("Failed to retrieve trailers to delete", ex);
        }

        // delete orphan persons if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.person", Boolean.TRUE)) {
            try {
                List<Long> ids = this.commonStorageService.getOrphanPersons();
                for (Long id : ids) {
                    try {
                        filesToDelete.addAll(this.commonStorageService.deletePerson(id));
                    } catch (Exception ex) {
                        LOG.error("Failed to delete person ID: "+id, ex);
                    }
                }
            } catch (Exception ex) {
                LOG.warn("Failed to retrieve orphan persons", ex);
            }
        }

        // delete orphan genres if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.genre", Boolean.TRUE)) {
            try {
                this.commonStorageService.deleteOrphanGenres();
            } catch (Exception ex) {
                LOG.warn("Failed to delete orphan genres", ex);
            }
        }

        // delete orphan studios if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.studio", Boolean.TRUE)) {
            try {
                this.commonStorageService.deleteOrphanStudios();
            } catch (Exception ex) {
                LOG.warn("Failed to delete orphan studios", ex);
            }
        }

        // delete orphan countries if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.country", Boolean.TRUE)) {
            try {
                this.commonStorageService.deleteOrphanCountries();
            } catch (Exception ex) {
                LOG.warn("Failed to delete orphan countries", ex);
            }
        }

        // delete orphan certifications if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.certification", Boolean.TRUE)) {
            try {
                this.commonStorageService.deleteOrphanCertifications();
            } catch (Exception ex) {
                LOG.warn("Failed to delete orphan certifications", ex);
            }
        }

        // delete orphan boxed sets if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.boxedset", Boolean.TRUE)) {
            try {
                List<Long> ids = this.commonStorageService.getOrphanBoxedSets();
                for (Long id : ids) {
                    try {
                        filesToDelete.addAll(this.commonStorageService.deleteBoxedSet(id));
                    } catch (Exception ex) {
                        LOG.error("Failed to delete boxed set ID: "+id, ex);
                    }
                }
            } catch (Exception ex) {
                LOG.warn("Failed to retrieve orphan boxed sets", ex);
            }
        }

        // delete storage files
        this.fileStorageService.deleteStorageFiles(filesToDelete);
    }
}
