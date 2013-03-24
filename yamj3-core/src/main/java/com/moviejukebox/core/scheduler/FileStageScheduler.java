package com.moviejukebox.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FileStageScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStageScheduler.class);

    @Scheduled(initialDelay=10000, fixedDelay=30000)
    public void checkFileStage() {
        LOGGER.info("Running file stage scheduler");
    }
}
