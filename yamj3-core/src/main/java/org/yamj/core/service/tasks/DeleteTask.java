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
import org.yamj.core.database.model.StageFile;
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
        
        List<StageFile> stageFiles = this.commonStorageService.getStageFilesToDelete();
        if (CollectionUtils.isEmpty(stageFiles)) {
            LOG.trace("No stage files found to delete");
            return;
        }
        
        // delete stage files
        Set<String> filesToDelete = new HashSet<String>();
        for (StageFile stageFile : stageFiles) {
            try {
                filesToDelete.addAll(this.commonStorageService.deleteStageFile(stageFile));
            } catch (Exception ex) {
                LOG.warn("Failed to delete stage file: {})", stageFile);
                LOG.error("Deletion error", ex);
            }
        }

        if (CollectionUtils.isEmpty(filesToDelete)) {
            LOG.trace("No files to delete on disc");
            return;
        }

        // delete files on disk
        for (String fileName : filesToDelete) {
            try {
                File file = new File(fileName);
                if (!file.delete()) {
                    LOG.info("File '{}' could not be deleted", fileName);
                }
            } catch (Exception ex) {
                LOG.warn("File '{}' could not be deleted", fileName);
                LOG.error("Deletion error", ex);
            }
        }
    }
}