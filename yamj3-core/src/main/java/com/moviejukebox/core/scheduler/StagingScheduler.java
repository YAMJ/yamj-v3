package com.moviejukebox.core.scheduler;

import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.StageFile;
import com.moviejukebox.core.database.model.type.FileType;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.service.MediaImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StagingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingScheduler.class);
    
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private MediaImportService mediaImportService;

    @Scheduled(initialDelay=10000, fixedDelay=30000)
    public void processStageFiles() throws Exception {
        StageFile stageFile = null;
        
        // PROCESS VIDEOS
        do {
            try {
    	        // find next stage file  to process
                stageFile =  stagingDao.getNextStageFile(FileType.VIDEO, StatusType.NEW, StatusType.UPDATED); 
                if (stageFile != null) {
                    if (StatusType.NEW.equals(stageFile.getStatus())) {
                        mediaImportService.processNewVideo(stageFile);
                    } else {
                        mediaImportService.processUpdatedVideo(stageFile);
                    }
    	        } else {
    	            LOGGER.info("No video found to process");
    	        }
            } catch (Exception error) {
                LOGGER.error("Failed to process stage file", error);
                mediaImportService.processingError(stageFile);
            }
	    } while (stageFile != null);

        // PROCESS IMAGES

        // PROCESS NFOS
        
        // PROCESS SUBTITLES
    }
}
