/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.filescanner.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.yamj.common.dto.ImportDTO;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.type.StatusType;
import org.yamj.filescanner.ApplicationContextProvider;
import org.yamj.filescanner.model.Library;
import org.yamj.filescanner.model.LibraryCollection;
import org.yamj.filescanner.model.TimeType;

@Service
public class LibrarySendScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(LibrarySendScheduler.class);
    private AtomicInteger runningCount = new AtomicInteger(0);
    private AtomicInteger retryCount = new AtomicInteger(0);
    @Autowired
    private LibraryCollection libraryCollection;
    @Autowired
    private ThreadPoolTaskExecutor yamjExecutor;

    @Async
    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    public void sendLibraries() {
        LOG.info("There are {} libraries to process, this is attempt {} to complete sending.", libraryCollection.size(), retryCount.incrementAndGet());
        LOG.info("There are {} items currently queued to be sent to core.", runningCount.get());

        for (Library library : libraryCollection.getLibraries()) {
            LOG.info("  {} has {} directories and the file scanner has {} scanning.",
                    library.getImportDTO().getBaseDirectory(),
                    library.getDirectories().size(),
                    library.isScanningComplete() ? "finished" : "not finished");
            try {
                for (Map.Entry<String, Future<StatusType>> entry : library.getDirectoryStatus().entrySet()) {
                    LOG.info("    {}: {}", entry.getKey(), entry.getValue().isDone() ? entry.getValue().get() : "Being processed");
                    boolean sendStatus = Boolean.FALSE;
                    if (entry.getValue().isDone()) {
                        StatusType processingStatus = entry.getValue().get();
                        if (processingStatus == StatusType.NEW) {
                            LOG.info("    Sending '{}' to core for processing.", entry.getKey());
                            sendStatus = sendToCore(library, entry.getKey());
                        } else if (processingStatus == StatusType.UPDATED) {
                            LOG.info("    Sending updated '{}' to core for processing.", entry.getKey());
                            sendStatus = sendToCore(library, entry.getKey());
                        } else if (processingStatus == StatusType.ERROR) {
                            LOG.info("    Resending '{}' to core for processing (was in error status).", entry.getKey());
                            sendStatus = sendToCore(library, entry.getKey());
                        } else if (processingStatus == StatusType.DONE) {
                            LOG.info("    Completed: '{}'", entry.getKey());
                            sendStatus = Boolean.TRUE;
                        } else {
                            LOG.warn("    Unknown processing status {} for {}", processingStatus, entry.getKey());
                        }
                    } else {
                        LOG.warn("    Still being procesed {}", entry.getKey());
                        sendStatus = Boolean.FALSE;
                    }

                    if (!sendStatus) {
                        // Make sure this is set to false
                        library.setSendingComplete(Boolean.FALSE);
                        LOG.warn("Failed to send a file. Waiting until next run...");
                        return;
                    }
                }

                // When we reach this point we should have completed the library sending
                LOG.info("Sending complete for {}",library.getImportDTO().getBaseDirectory());
                library.setSendingComplete(Boolean.TRUE);
                library.getStatistics().setTime(TimeType.SENDING_END, DateTime.now().getMillis());
            } catch (InterruptedException ex) {
                LOG.info("InterruptedException: {}", ex.getMessage());
            } catch (ExecutionException ex) {
                LOG.info("ExecutionException: {}", ex.getMessage());
            }
        }
    }

    /**
     * Check that the library has all the directories sent to the core server
     *
     * If there are entries that have not been sent, or need resending, they should be done as well
     *
     * @param library
     */
    private void checkLibraryAllSent(Library library) {
        int retryCount2 = 3; // The number of times to retry sending the files to core
        List<String> resendDirs = new ArrayList<String>();

        int processedDone, processedError, unprocessed;
        do {
            // Clear the directories to resend.
            resendDirs.clear();
            processedDone = 0;
            processedError = 0;
            unprocessed = 0;
            LOG.info("There are {} items remaining to be sent to core.", runningCount.get());
            for (Map.Entry<String, Future<StatusType>> entry : library.getDirectoryStatus().entrySet()) {
                try {
                    if (entry.getValue().isDone()) {
                        LOG.info("{} - Status: {}", entry.getKey(), entry.getValue().get());
                        if (entry.getValue().get() == StatusType.ERROR) {
                            processedError++;
                            // Add the directory to the list to resend.
                            resendDirs.add(entry.getKey());
                        } else {
                            processedDone++;
                        }
                    } else {
                        LOG.info("{} - Not yet processed", entry.getKey());
                        unprocessed++;
                    }
                } catch (InterruptedException ex) {
                    LOG.trace("Interrupted checking '{}'", entry.getKey());
                } catch (ExecutionException ex) {
                    LOG.trace("Execution exception with '{}', error: {}", entry.getKey(), ex.getMessage());
                }
            }

            LOG.info("Done: {}, Error: {}, Unprocessed: {}", processedDone, processedError, unprocessed);

            if (processedError > 0) {
                LOG.info("There were {} errors sending to the server. Will attempt to send {} more times.", processedError, retryCount2--);

                for (String errorDir : resendDirs) {
                    LOG.info("Resending '{}' to the core", errorDir);
                    sendToCore(library, errorDir);
                    // Add one to the unprocessed count
                    unprocessed++;
                }
            }

            if (unprocessed > 0) {
                try {
                    LOG.info("Waiting for threads to complete...");
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    LOG.trace("Interrupted whilst waiting for threads to complete.");
                }
            }
        } while (unprocessed > 0 && retryCount2 >= 0);

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
        if (stageDto == null) {
            LOG.warn("StageDirectoryDTO for '{}' is null!", sendDir);
            // We do not want to send this again.
            library.addDirectoryStatus(sendDir, ConcurrentUtils.constantFuture(StatusType.INVALID));
            return Boolean.TRUE;
        } else {
            return sendToCore(library, stageDto);
        }
    }

    /**
     * Send an ImportDTO to the core
     *
     * Increment the running count
     *
     * @param importDto
     */
    private boolean sendToCore(Library library, StageDirectoryDTO stageDir) {
        boolean sentOk = Boolean.FALSE;
        ImportDTO dto = library.getImportDTO(stageDir);

        LOG.debug("Sending #{}: {}", runningCount.incrementAndGet(), dto.getBaseDirectory());

        ApplicationContext appContext = ApplicationContextProvider.getApplicationContext();
        SendToCore stc = (SendToCore) appContext.getBean("sendToCore");
        stc.setImportDto(dto);
        stc.setCounter(runningCount);
        FutureTask<StatusType> task = new FutureTask<StatusType>(stc);

        try {
            yamjExecutor.submit(task);
            library.addDirectoryStatus(stageDir.getPath(), task);
            sentOk = Boolean.TRUE;
        } catch (TaskRejectedException ex) {
            LOG.warn("Send queue full. '{}' will be sent later.", stageDir.getPath());
            library.addDirectoryStatus(stageDir.getPath(), ConcurrentUtils.constantFuture(StatusType.NEW));
        }
        return sentOk;
    }

    public ThreadPoolTaskExecutor getYamjExecutor() {
        return yamjExecutor;
    }

    public void setYamjExecutor(ThreadPoolTaskExecutor yamjExecutor) {
        this.yamjExecutor = yamjExecutor;
    }
}
