package org.yamj.core.service.tasks;

import java.io.File;
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
        
        List<Long> ids = this.commonStorageService.getStageFilesToDelete();
        if (CollectionUtils.isEmpty(ids)) {
            LOG.debug("No stage files found to delete");
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
        
        // delete orphan persons if allowed
        if (this.configService.getBooleanProperty("yamj3.delete.orphan.person", Boolean.TRUE)) {
            try {
                ids = this.commonStorageService.getOrphanPersons();
                for (Long id : ids) {
                    try {
                        filesToDelete.addAll(this.commonStorageService.deletePerson(id));
                    } catch (Exception ex) {
                        LOG.warn("Failed to delete person ID: {}", id);
                        LOG.error("Deletion error", ex);
                    }
                }
            } catch (Exception ex) {
                LOG.warn("Failed to retrieve orphan person", ex);
            }
        }
        
        if (CollectionUtils.isEmpty(filesToDelete)) {
            LOG.debug("No files to delete in cache directories");
            return;
        }

        // delete files on disk
        for (String filename : filesToDelete) {
            try {
                LOG.debug("Delete file: {}", filename);
                File file = new File(filename);
                if (!file.exists()) {
                    LOG.info("File '{}' does not exist", filename);
                } else if (!file.delete()) {
                    LOG.warn("File '{}' could not be deleted", filename);
                }
            } catch (Exception ex) {
                LOG.error("Deletion error for file: '"+ filename+"'", ex);
            }
        }
    }
}