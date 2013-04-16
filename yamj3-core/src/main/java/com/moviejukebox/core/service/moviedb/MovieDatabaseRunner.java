package com.moviejukebox.core.service.moviedb;

import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieDatabaseRunner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieDatabaseRunner.class);

    private final BlockingQueue<Long> queue;
    private final MovieDatabaseService controller;

    public MovieDatabaseRunner(BlockingQueue<Long> queue, MovieDatabaseService controller) {
        this.queue = queue;
        this.controller = controller;
    }

    @Override
    public void run() {
        Long id = queue.poll();
        while (id != null) {
            try {
                controller.scanMetadata(id);
            } catch (Exception error) {
                LOGGER.error("Failed to process video data", error);
                try {
                    controller.processingError(id);
                } catch (Exception ignore) {
                    // ignore this error;
                }
            }
            id = queue.poll();
        }
    }
}
