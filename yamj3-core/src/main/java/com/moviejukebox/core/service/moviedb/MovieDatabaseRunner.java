package com.moviejukebox.core.service.moviedb;

import com.moviejukebox.core.database.model.dto.QueueDTO;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieDatabaseRunner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieDatabaseRunner.class);

    private final BlockingQueue<QueueDTO> queue;
    private final MovieDatabaseService controller;

    public MovieDatabaseRunner(BlockingQueue<QueueDTO> queue, MovieDatabaseService controller) {
        this.queue = queue;
        this.controller = controller;
    }

    @Override
    public void run() {
        QueueDTO queueElement = queue.poll();
        while (queueElement != null) {
            try {
                controller.scanMetadata(queueElement);
            } catch (Exception error) {
                LOGGER.error("Failed to process media data", error);
                try {
                    controller.processingError(queueElement);
                } catch (Exception ignore) {
                    // ignore this error;
                }
            }
            queueElement = queue.poll();
        }
    }
}
