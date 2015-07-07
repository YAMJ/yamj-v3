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
package org.yamj.core.service.tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.service.CommonStorageService;
import org.yamj.core.service.file.FileStorageService;

/**
 * Task for checking if video, series or person is older than x days and marks
 * those data entries as updated in order to force a rescan.
 */
@Component
public class DeleteTask implements ITask {

    private static final Logger LOG = LoggerFactory.getLogger(RecheckTask.class);

    @Autowired
    private ExecutionTaskService executionTaskService;
    @Autowired
    private CommonStorageService commonStorageService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public String getTaskName() {
        return "delete";
    }

    @PostConstruct
    public void init() {
        executionTaskService.registerTask(this);
    }

    @Override
    public void execute(String options) throws Exception {
        LOG.debug("Execute delete task");
        Set<String> filesToDelete = new HashSet<>();

        try {
            List<Long> ids = this.commonStorageService.getStageFilesToDelete();
            if (CollectionUtils.isEmpty(ids)) {
                LOG.trace("No stage files found to delete");
            } else {
                // delete stage files
                for (Long id : ids) {
                    try {
                        filesToDelete.addAll(this.commonStorageService.deleteStageFile(id));
                    } catch (Exception ex) {
                        LOG.warn("Failed to delete stage file ID: {}", id);
                        LOG.error("Deletion error", ex);
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warn("Failed to retrieve stage files to delete", ex);
        }

        try {
            List<Long> ids = this.commonStorageService.getArtworkLocatedToDelete();
            if (CollectionUtils.isEmpty(ids)) {
                LOG.trace("No located artwork found to delete");
            } else {
                // delete stage files
                for (Long id : ids) {
                    try {
                        filesToDelete.addAll(this.commonStorageService.deleteArtworkLocated(id));
                    } catch (Exception ex) {
                        LOG.warn("Failed to delete located artwork ID: {}", id);
                        LOG.error("Deletion error", ex);
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warn("Failed to retrieve located artworks to delete", ex);
        }

        // delete orphan persons if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.person", Boolean.TRUE)) {
            try {
                List<Long> ids = this.commonStorageService.getOrphanPersons();
                for (Long id : ids) {
                    try {
                        filesToDelete.addAll(this.commonStorageService.deletePerson(id));
                    } catch (Exception ex) {
                        LOG.warn("Failed to delete person ID: {}", id);
                        LOG.error("Deletion error", ex);
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
                        LOG.warn("Failed to delete boxed set ID: " + id, ex);
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
