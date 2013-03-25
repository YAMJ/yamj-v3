package com.moviejukebox.core.scheduler;

import com.moviejukebox.core.importer.MediaImportService;

import com.moviejukebox.core.importer.MediaImportRunner;

import com.moviejukebox.core.database.dao.FileStageDao;
import com.moviejukebox.core.database.model.FileStage;
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

    @Autowired
    private FileStageDao fileStageDao;
    @Autowired
    private MediaImportService mediaImportService;
    
    @Resource(name = "stagingTaskExecutor")
    private TaskExecutor taskExecutor;
    
    @Scheduled(initialDelay=10000, fixedDelay=30000)
    public void checkFileStage() {
        LOGGER.info("Running file stage scheduler");
        
        // find file stags
        List<FileStage> fileStages = fileStageDao.getFileStages(10);
        if (fileStages.isEmpty()) {
            LOGGER.debug("No file stage objects found; nothing to do");
            return;
        }

        // process each staged file with an import runner
        for (FileStage fileStage : fileStages) {
            MediaImportRunner runner = new MediaImportRunner(fileStage, mediaImportService);
            taskExecutor.execute(runner);
        }
    }
}
