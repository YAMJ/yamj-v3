package org.yamj.filescanner.service;

import org.yamj.common.dto.ImportDTO;
import org.yamj.common.remote.service.FileImportService;
import org.yamj.common.type.StatusType;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;

public class SendToCore implements Callable<StatusType> {

    private static final Logger LOG = LoggerFactory.getLogger(SendToCore.class);
    @Autowired
    private FileImportService fileImportService;
//    @Autowired
//    private PingCore pingCore;
    private ImportDTO importDto;
    private int timeoutSeconds;
    private int numberOfRetries;
    private AtomicInteger runningCount;

    public SendToCore(ImportDTO importDto) {
        this.importDto = importDto;
    }

    public SendToCore() {
    }

    public void setImportDto(ImportDTO importDto) {
        this.importDto = importDto;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setNumberOfRetries(int numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
    }

    public void setCounter(AtomicInteger runningCount) {
        this.runningCount = runningCount;
    }

    @Override
    public StatusType call() {
        StatusType status;
        int currentTry = 1;

        do {
            LOG.debug("SendToCore try {}/{}", currentTry++, numberOfRetries);
            status = send();
            // Only sleep if there was an error
            if (status == StatusType.ERROR) {
                try {
                    LOG.debug("Error sending to core, waiting {} seconds to retry", timeoutSeconds);
                    TimeUnit.SECONDS.sleep(timeoutSeconds);
                } catch (InterruptedException ex) {
                    LOG.trace("Interrupted whilst waiting {} seconds for the send to complete", timeoutSeconds);
                }
            }
        } while (status == StatusType.ERROR && currentTry <= numberOfRetries);

        // Whether or not the message was sent, quit
        LOG.info("Exiting with status {}, remaining threads: {}", status, runningCount.decrementAndGet());
        return status;
    }

    private StatusType send() {
        StatusType status;
        try {
            LOG.debug("Sending: {}", importDto.getBaseDirectory());
            fileImportService.importScanned(importDto);
            LOG.debug("Successfully queued");
            status = StatusType.DONE;
        } catch (RemoteConnectFailureException ex) {
            LOG.error("Failed to connect to the core server: {}", ex.getMessage());
            status = StatusType.ERROR;
        } catch (RemoteAccessException ex) {
            LOG.error("Failed to send object to the core server: {}", ex.getMessage());
            status = StatusType.ERROR;
        }
        return status;
    }
}
