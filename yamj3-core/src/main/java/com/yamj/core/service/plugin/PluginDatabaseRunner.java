package com.yamj.core.service.plugin;

import com.yamj.core.database.model.dto.QueueDTO;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginDatabaseRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PluginDatabaseRunner.class);
    private final BlockingQueue<QueueDTO> queue;
    private final PluginDatabaseService controller;

    public PluginDatabaseRunner(BlockingQueue<QueueDTO> queue, PluginDatabaseService controller) {
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
                LOG.error("Failed to process meta data", error);
                try {
                    controller.processingError(queueElement);
                } catch (Exception ignore) {
                    // ignore this error
                }
            }
            queueElement = queue.poll();
        }
    }
}
