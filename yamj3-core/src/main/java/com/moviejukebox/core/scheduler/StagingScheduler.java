package com.moviejukebox.core.scheduler;

import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.type.FileType;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.scanner.moviedb.MovieDatabaseController;
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
    @Autowired
    private MovieDatabaseController movieDatabaseController;

    @Scheduled(initialDelay=10000, fixedDelay=30000)
    public void processStageFiles() throws Exception {
        Long id = null;
        
        // PROCESS VIDEOS
        do {
            try {
    	        // find next stage file  to process
                id =  stagingDao.getNextStageFileId(FileType.VIDEO, StatusType.NEW, StatusType.UPDATED); 
                if (id != null) {
                    this.mediaImportService.processVideo(id);
    	        } else {
    	            LOGGER.info("No video found to process");
    	        }
            } catch (Exception error) {
                LOGGER.error("Failed to process stage file", error);
                mediaImportService.processingError(id);
            }
	    } while (id != null);

        // PROCESS IMAGES

        // PROCESS NFOS
        
        // PROCESS SUBTITLES
    }
    
    
    @Scheduled(initialDelay=10000, fixedDelay=30000)
    public void scanMovieMetadata() throws Exception {
        
    }
}
