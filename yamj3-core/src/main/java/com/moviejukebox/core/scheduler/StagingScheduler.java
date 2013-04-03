package com.moviejukebox.core.scheduler;

import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.StageFile;
import com.moviejukebox.core.database.model.type.FileType;
import com.moviejukebox.core.database.model.type.StatusType;
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
public class StagingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingScheduler.class);
    
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private MediaImportService mediaImportService;
    @Resource(name = "stagingTaskExecutor")
    private TaskExecutor taskExecutor;

    @Scheduled(initialDelay=10000, fixedDelay=30000)
    public void groupVideos() throws Exception {
        // find new staged videos
        List<StageFile> stageFiles = stagingDao.getStageFiles(10, FileType.VIDEO, StatusType.NEW, StatusType.UPDATED); 
        if (stageFiles.isEmpty()) {
            // nothing to do
            return;
        }
        LOGGER.debug("Found " + stageFiles.size() + " stage files to process");

        // process each staged file with an import runner
        for (StageFile stageFile : stageFiles) {
            LOGGER.info(stageFile.getFile().toString());
//            MediaImportRunner runner = new MediaImportRunner(stageFile, mediaImportService);
//            taskExecutor.execute(runner);
        }
    }
}
