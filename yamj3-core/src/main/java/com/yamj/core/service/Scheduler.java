package com.yamj.core.service;

import com.yamj.common.tools.PropertyTools;
import com.yamj.common.type.StatusType;
import com.yamj.core.database.dao.ArtworkDao;
import com.yamj.core.database.dao.MediaDao;
import com.yamj.core.database.dao.PersonDao;
import com.yamj.core.database.dao.StagingDao;
import com.yamj.core.database.model.dto.QueueDTO;
import com.yamj.core.database.model.type.FileType;
import com.yamj.core.service.artwork.ArtworkScannerRunner;
import com.yamj.core.service.artwork.ArtworkScannerService;
import com.yamj.core.service.mediaimport.MediaImportService;
import com.yamj.core.service.plugin.PluginDatabaseRunner;
import com.yamj.core.service.plugin.PluginDatabaseService;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class Scheduler {

    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private static final int MEDIA_SCANNER_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.mediascan.maxThreads", 5);
    private static final int PEOPLE_SCANNER_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.peoplescan.maxThreads", 5);
    private static final int ARTWORK_SCANNER_THREADS = PropertyTools.getIntProperty("yamj3.scheduler.artworkscan.maxThreads", 3);

    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private ArtworkDao artworkDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private MediaImportService mediaImportService;
    @Autowired
    private PluginDatabaseService pluginDatabaseService;
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private PersonDao personDao;

    static {
        // Configure the ToStringBuilder to use the short prefix by default
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Async
    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    public void processStageFiles() throws Exception {
        Long id = null;

        // PROCESS VIDEOS
        do {
            try {
                // find next stage file to process
                id = stagingDao.getNextStageFileId(FileType.VIDEO, StatusType.NEW, StatusType.UPDATED);
                if (id != null) {
                    LOG.debug("Process stage file: {}", id);
                    this.mediaImportService.processVideo(id);
                }
            } catch (Exception error) {
                LOG.error("Failed to process stage file", error);
                try {
                    this.mediaImportService.processingError(id);
                } catch (Exception ignore) {
                }
            }
        } while (id != null);

        // PROCESS IMAGES

        // PROCESS NFOS

        // PROCESS SUBTITLES

        // PROCESS PEOPLE
    }

    @Async
    @Scheduled(initialDelay = 15000, fixedDelay = 45000)
    public void scanMediaData() throws Exception {
        List<QueueDTO> queueElements = mediaDao.getMediaQueueForScanning();
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No media data found to scan");
            return;
        }

        LOG.info("Found {} media data objects to process", queueElements.size());
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(MEDIA_SCANNER_THREADS);
        for (int i = 0; i < MEDIA_SCANNER_THREADS; i++) {
            PluginDatabaseRunner worker = new PluginDatabaseRunner(queue, pluginDatabaseService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {}
        }

        LOG.debug("Finished media data scanning");
    }

    @Async
    @Scheduled(initialDelay = 15000, fixedDelay = 45000)
    public void scanPeopleData() throws Exception {
        List<QueueDTO> queueElements = personDao.getPersonQueueForScanning();
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No people data found to scan");
            return;
        }

        LOG.info("Found {} people objects to process", queueElements.size());
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(PEOPLE_SCANNER_THREADS);
        for (int i = 0; i < PEOPLE_SCANNER_THREADS; i++) {
            PluginDatabaseRunner worker = new PluginDatabaseRunner(queue, pluginDatabaseService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {}
           
        }

        LOG.debug("Finished person data scanning");
    }
    
    @Async
    @Scheduled(initialDelay = 15000, fixedDelay = 45000)
    public void scanArtwork() throws Exception {
        List<QueueDTO> queueElements = artworkDao.getArtworkQueueForScanning();
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.debug("No artwork found to scan");
            return;
        }

        LOG.info("Found {} artwork objects to process", queueElements.size());
        BlockingQueue<QueueDTO> queue = new LinkedBlockingQueue<QueueDTO>(queueElements);

        ExecutorService executor = Executors.newFixedThreadPool(ARTWORK_SCANNER_THREADS);
        for (int i = 0; i < ARTWORK_SCANNER_THREADS; i++) {
            ArtworkScannerRunner worker = new ArtworkScannerRunner(queue, artworkScannerService);
            executor.execute(worker);
        }
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {}
        }

        LOG.debug("Finished artwork scanning");
    }

}
