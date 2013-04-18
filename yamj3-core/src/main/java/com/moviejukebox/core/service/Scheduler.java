package com.moviejukebox.core.service;

import com.moviejukebox.core.database.dao.MediaDao;
import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.dto.QueueDTO;
import com.moviejukebox.core.database.model.type.FileType;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.service.mediaimport.MediaImportService;
import com.moviejukebox.core.service.moviedb.MovieDatabaseRunner;
import com.moviejukebox.core.service.moviedb.MovieDatabaseService;
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
    
    
    @Scheduled(initialDelay=15000, fixedDelay=45000)
    public void scanMediaData() throws Exception {
        List<QueueDTO> queueElements = mediaDao.getMediaQueueForScanning();
        if (CollectionUtils.isEmpty(queueElements)) {
            LOGGER.debug("No media data found to scan");
            return;
        }
        
        LOGGER.info("Found {} media data objects to process", queueElements.size());
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        int maxScannerThreads = PropertyTools.getIntProperty("yamj3.scheduler.mediascan.maxThreads", 5);
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
        
        LOGGER.debug("Finished media data scanning");
    }
}
