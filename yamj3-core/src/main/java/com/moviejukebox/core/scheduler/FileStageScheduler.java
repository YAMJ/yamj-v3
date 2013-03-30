package com.moviejukebox.core.scheduler;

import com.moviejukebox.core.database.dao.FileStageDao;
import com.moviejukebox.core.database.model.FileStage;
import com.moviejukebox.core.database.model.type.FileStageType;
import com.moviejukebox.core.importer.MediaImportRunner;
import com.moviejukebox.core.importer.MediaImportService;
import java.util.List;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FileStageScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStageScheduler.class);

    // trigger for import
    private boolean trigger = false;
    
    @Autowired
    private FileStageDao fileStageDao;
    @Autowired
    private MediaImportService mediaImportService;
    @Resource(name = "stagingTaskExecutor")
    private TaskExecutor taskExecutor;

    public synchronized void triggerProcess() {
        this.trigger = true;
    }
    
    @Scheduled(initialDelay=10000, fixedDelay=30000)
    public void process() {
        if (trigger == false) {
            // nothing to process
            return;
        }
        
        // find new file stages
        List<FileStage> fileStages = fileStageDao.getFileStages(FileStageType.NEW, 10);
        if (fileStages.isEmpty()) {
            LOGGER.debug("No file stage objects found; nothing to do");
            // reset the trigger
            trigger = false;
            // nothing to do, so leave
            return;
        }

        // process each staged file with an import runner
        for (FileStage fileStage : fileStages) {
            MediaImportRunner runner = new MediaImportRunner(fileStage, mediaImportService);
            taskExecutor.execute(runner);
        }

        // reset the trigger
        trigger = false;
    }
}
