/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.filescanner.service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.filescanner.ApplicationContextProvider;
import org.yamj.filescanner.model.Library;
import org.yamj.filescanner.model.LibraryCollection;
import org.yamj.filescanner.model.TimeType;

@Service
public class LibrarySendScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(LibrarySendScheduler.class);
    private static final int RETRY_MAX = PropertyTools.getIntProperty("filescanner.send.retry", 5);
    private final AtomicInteger runningCount = new AtomicInteger(0);
    private final AtomicInteger retryCount = new AtomicInteger(0);

    @Autowired
    private LibraryCollection libraryCollection;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor yamjExecutor;

    /**
     * Set some executor properties
     */
    @PostConstruct
    public void init() {
        yamjExecutor.setWaitForTasksToCompleteOnShutdown(true);
        yamjExecutor.setThreadNamePrefix("LibrarySendTask-");
    }

    /**
     * Clean up the service before exiting
     */
    @PreDestroy
    public void cleanUp() {
        LOG.info("LibrarySendScheduler is exiting.");
        yamjExecutor.shutdown();
    }

    @Async
    @Scheduled(initialDelay = 10000, fixedDelay = 15000)
    public void sendLibraries() { //NOSONAR
        if (retryCount.get() > RETRY_MAX) {
            LOG.info("Maximum number of retries ({}) exceeded. No further processing attempted.", Integer.valueOf(RETRY_MAX));
            for (Library library : libraryCollection.getLibraries()) {
                library.setSendingComplete(true);
            }
            return;
        }

        LOG.info("There are {} libraries to process, there have been {} consecutive failed attempts to send.", libraryCollection.size(), retryCount.get());
        LOG.info("There are {} items currently queued to be sent to core.", runningCount.get());

        for (Library library : libraryCollection.getLibraries()) {
            library.getStatistics().setTime(TimeType.SENDING_START);
            LOG.info("  {} has {} directories and the file scanner has {} scanning.",
                    library.getImportDTO().getBaseDirectory(),
                    library.getDirectories().size(),
                    library.isScanningComplete() ? "finished" : "not finished");

            try {
                for (Map.Entry<String, Future<StatusType>> entry : library.getDirectoryStatus().entrySet()) {
                    LOG.info("    {}: {}", entry.getKey(), entry.getValue().isDone() ? entry.getValue().get() : "Being processed");

                    if (checkStatus(library, entry.getValue(), entry.getKey())) {
                        if (retryCount.get() > 0) {
                            LOG.debug("Successfully sent file to server, resetting retry count to 0 from {}.", retryCount.getAndSet(0));
                        } else {
                            LOG.debug("Successfully sent file to server.");
                            retryCount.set(0);
                        }
                    } else {
                        // Make sure this is set to false
                        library.setSendingComplete(false);
                        LOG.warn("Failed to send a file, this was failed attempt #{}. Waiting until next run...", retryCount.incrementAndGet());
                        return;
                    }
                }

                // Don't stop sending until the scanning is completed and there are no running tasks
                if (library.isScanningComplete() && runningCount.get() <= 0) {
                    // When we reach this point we should have completed the library sending
                    LOG.info("Sending complete for {}", library.getImportDTO().getBaseDirectory());
                    library.setSendingComplete(true);
                    library.getStatistics().setTime(TimeType.SENDING_END);
                } else {
                    LOG.info("  {}: Scanning and/or sending ({} left) is not complete. Waiting for more files to send.", library.getImportDTO().getBaseDirectory(), runningCount.get());
                }
            } catch (InterruptedException ex) { //NOSONAR
                LOG.info("Interrupted error: {}", ex.getMessage());
            } catch (ExecutionException ex) {
                LOG.warn("Execution error", ex);
            }
        }
    }

    private boolean checkStatus(Library library, Future<StatusType> statusType, String directory) throws InterruptedException, ExecutionException {
        boolean sendStatus;

        
        if (statusType.isDone()) {
            StatusType processingStatus = statusType.get();
            
            if (processingStatus == StatusType.NEW) {
                LOG.info("    Sending '{}' to core for processing.", directory);
                sendStatus = sendToCore(library, directory);
            } else if (processingStatus == StatusType.UPDATED) {
                LOG.info("    Sending updated '{}' to core for processing.", directory);
                sendStatus = sendToCore(library, directory);
            } else if (processingStatus == StatusType.ERROR) {
                LOG.info("    Resending '{}' to core for processing (was in error status).", directory);
                sendStatus = sendToCore(library, directory);
            } else if (processingStatus == StatusType.DONE) {
                LOG.info("    Completed: '{}'", directory);
                sendStatus = true;
            } else {
                LOG.warn("    Unknown processing status {} for {}", processingStatus, directory);
                // Assume this is correct, so we don't get stuck
                sendStatus = true;
            }
        } else {
            LOG.warn("    Still being procesed {}", directory);
            sendStatus = false;
        }
        return sendStatus;
    }

    /**
     * Send the directory to the core.
     *
     * Will get the StageDirectoryDTO from the library for sending.
     *
     * @param library
     * @param sendDir
     */
    private boolean sendToCore(Library library, String sendDir) {
        StageDirectoryDTO stageDto = library.getDirectory(sendDir);
        boolean sentOk = false;

        if (stageDto == null) {
            LOG.warn("StageDirectoryDTO for '{}' is null!", sendDir);
            // We do not want to send this again.
            library.addDirectoryStatus(sendDir, ConcurrentUtils.constantFuture(StatusType.INVALID));
            return true;
        }

        LOG.info("Sending #{}: {}", runningCount.incrementAndGet(), sendDir);

        ApplicationContext appContext = ApplicationContextProvider.getApplicationContext();
        SendToCore stc = (SendToCore) appContext.getBean("sendToCore");
        stc.setImportDto(library.getImportDTO(stageDto));
        stc.setCounter(runningCount);
        FutureTask<StatusType> task = new FutureTask<>(stc);

        try {
            yamjExecutor.submit(task);
            library.addDirectoryStatus(stageDto.getPath(), task);
            sentOk = true;
        } catch (TaskRejectedException ex) {
            LOG.warn("Send queue full. '{}' will be sent later.", stageDto.getPath());
            LOG.trace("Exception: ", ex);
            library.addDirectoryStatus(stageDto.getPath(), ConcurrentUtils.constantFuture(StatusType.NEW));
        }
        
        return sentOk;
    }
}
