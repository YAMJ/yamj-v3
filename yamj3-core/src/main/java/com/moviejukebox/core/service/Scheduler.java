package com.moviejukebox.core.service;

import com.moviejukebox.core.service.mediaimport.MediaImportService;

import com.moviejukebox.core.service.moviedb.MovieDatabaseService;
import com.moviejukebox.core.service.moviedb.MovieDatabaseRunner;

import com.moviejukebox.core.database.dao.MediaDao;
import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.type.FileType;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.tools.PropertyTools;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private MediaImportService mediaImportService;
    @Autowired
    private MovieDatabaseService movieDatabaseController;

    @Scheduled(initialDelay=10000, fixedDelay=30000)
    public void processStageFiles() throws Exception {
        Long id = null;
        
        // PROCESS VIDEOS
        do {
            try {
    	        // find next stage file to process
                id =  stagingDao.getNextStageFileId(FileType.VIDEO, StatusType.NEW, StatusType.UPDATED); 
                if (id != null) {
                    LOGGER.debug("Process stage file: " + id);
                    this.mediaImportService.processVideo(id);
    	        }
            } catch (Exception error) {
                LOGGER.error("Failed to process stage file", error);
                try {
                    mediaImportService.processingError(id);
                } catch (Exception ignore) {}
            }
	    } while (id != null);

        // PROCESS IMAGES

        // PROCESS NFOS
        
        // PROCESS SUBTITLES
    }
    
    
    @Scheduled(initialDelay=10000, fixedDelay=45000)
    public void processVideoData() throws Exception {
        List<Long> ids = mediaDao.getVideoDataIds(StatusType.NEW, StatusType.UPDATED);
        if (CollectionUtils.isEmpty(ids)) {
            LOGGER.debug("No video data to process");
            return;
        }
        
        LOGGER.info("Found {} video data objects to process", ids.size());
        BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>(ids);

        int maxScannerThreads = PropertyTools.getIntProperty("yamj3.scheduler.metadata.maxThreads", 5);
        ExecutorService executor = Executors.newFixedThreadPool(maxScannerThreads);
        for (int i=0; i<maxScannerThreads; i++) {
            MovieDatabaseRunner worker = new MovieDatabaseRunner(queue, movieDatabaseController);
            executor.execute(worker);
        }
        executor.shutdown();
        
        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {}
        }
        
        LOGGER.debug("Finished video data processing");
    }
}
