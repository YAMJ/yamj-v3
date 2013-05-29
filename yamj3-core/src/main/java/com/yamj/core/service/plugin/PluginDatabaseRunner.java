package com.yamj.core.service.plugin;

import com.yamj.core.database.model.dto.QueueDTO;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginDatabaseRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PluginDatabaseRunner.class);
    private final BlockingQueue<QueueDTO> queue;
    private final PluginDatabaseService service;

    public PluginDatabaseRunner(BlockingQueue<QueueDTO> queue, PluginDatabaseService service) {
        this.queue = queue;
        this.service = service;
    }

    @Override
    public void run() {
        QueueDTO queueElement = queue.poll();
        while (queueElement != null) {
            try {
                service.scanMetadata(queueElement);
            } catch (Exception error) {
                LOG.error("Failed to process meta data", error);
                try {
                    service.processingError(queueElement);
                } catch (Exception ignore) {
                    // ignore this error
                }
            }
            queueElement = queue.poll();
        }
    }
}
