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
 * Task for checking if video, series or person is older than x days
 * and marks those data entries as updated in order to force a rescan. 
 */
/**
 * Task for checking if video, series or person is older than x days
 * and marks those data entries as updated in order to force a rescan. 
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
    public void init() throws Exception {
        executionTaskService.registerTask(this);
    }

    @Override
    public void execute(String options) throws Exception {
        LOG.debug("Execute delete task");
        Set<String> filesToDelete = new HashSet<String>();

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

        // delete orphan certifications if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.certification", Boolean.TRUE)) {
            try {
                this.commonStorageService.deleteOrphanCertifications();
            } catch (Exception ex) {
                LOG.warn("Failed to delete orphan certifications", ex);
            }
        }

        // delete orphan certifications if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.boxedset", Boolean.TRUE)) {
            try {
                List<Long> ids = this.commonStorageService.getOrphanBoxedSets();
                for (Long id : ids) {
                    try {
                        filesToDelete.addAll(this.commonStorageService.deleteBoxedSet(id));
                    } catch (Exception ex) {
                        LOG.warn("Failed to delete boxed set ID: {}", id);
                        LOG.error("Deletion error", ex);
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