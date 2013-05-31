package org.yamj.core.service.artwork;

import org.yamj.core.database.model.dto.QueueDTO;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtworkScannerRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScannerRunner.class);
    private final BlockingQueue<QueueDTO> queue;
    private final ArtworkScannerService service;

    public ArtworkScannerRunner(BlockingQueue<QueueDTO> queue, ArtworkScannerService service) {
        this.queue = queue;
        this.service = service;
    }

    @Override
    public void run() {
        QueueDTO queueElement = queue.poll();
        while (queueElement != null) {
            try {
                service.scanArtwork(queueElement);
            } catch (Exception error) {
                LOG.error("Failed to process artwork", error);
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
