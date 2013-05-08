package com.yamj.core.service;

import com.yamj.core.database.dao.MediaDao;
import com.yamj.core.database.dao.StagingDao;
import com.yamj.core.database.model.dto.QueueDTO;
import com.yamj.core.database.model.type.FileType;
import com.yamj.common.type.StatusType;
import com.yamj.core.service.mediaimport.MediaImportService;
import com.yamj.core.service.plugin.PluginDatabaseRunner;
import com.yamj.core.service.plugin.PluginDatabaseService;
import com.yamj.common.tools.PropertyTools;
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

    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private MediaImportService mediaImportService;
    @Autowired
    private PluginDatabaseService movieDatabaseController;

    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    public void processStageFiles() throws Exception {
        Long id = null;

        // PROCESS VIDEOS
        do {
            try {
                // find next stage file to process
                id = stagingDao.getNextStageFileId(FileType.VIDEO, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.debug("Process stage file: {}" , id);
                    this.mediaImportService.processVideo(id);
                }
            } catch (Exception error) {
                LOG.error("Failed to process stage file", error);
                try {
                    mediaImportService.processingError(id);
                } catch (Exception ignore) {
                }
            }
        } while (id != null);

        // PROCESS IMAGES

        // PROCESS NFOS

        // PROCESS SUBTITLES
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 45000)
    public void scanMediaData() throws Exception {
        List<QueueDTO> queueElements = mediaDao.getMediaQueueForScanning();
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No media data found to scan");
            return;
        }

        LOG.info("Found {} media data objects to process", queueElements.size());
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        int maxScannerThreads = PropertyTools.getIntProperty("yamj3.scheduler.mediascan.maxThreads", 5);
        ExecutorService executor = Executors.newFixedThreadPool(maxScannerThreads);
        for (int i = 0; i < maxScannerThreads; i++) {
            PluginDatabaseRunner worker = new PluginDatabaseRunner(queue, movieDatabaseController);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {
            }
        }

        LOG.debug("Finished media data scanning");
    }
}
