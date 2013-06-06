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
            LOG.debug("{}: SendToCore try {}/{}", importDto.getBaseDirectory(), currentTry++, numberOfRetries);
            status = send();
            // Only sleep if there was an error
            if (status == StatusType.ERROR) {
                try {
                    LOG.debug("{}: Error sending to core, waiting {} seconds to retry", importDto.getBaseDirectory(), timeoutSeconds);
                    TimeUnit.SECONDS.sleep(timeoutSeconds);
                } catch (InterruptedException ex) {
                    LOG.trace("{}: Interrupted whilst waiting {} seconds for the send to complete", importDto.getBaseDirectory(), timeoutSeconds);
                }
            }
        } while (status == StatusType.ERROR && currentTry <= numberOfRetries);

        // Whether or not the message was sent, quit
        LOG.info("{}: Exiting with status {}, remaining threads: {}", importDto.getBaseDirectory(), status, runningCount.decrementAndGet());
        return status;
    }

    private StatusType send() {
        StatusType status;
        try {
            LOG.debug("Sending: {}", importDto.getBaseDirectory());
            fileImportService.importScanned(importDto);
            LOG.debug("{}: Successfully queued", importDto.getBaseDirectory());
            status = StatusType.DONE;
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}: Failed to connect to the core server: {}", importDto.getBaseDirectory(), ex.getMessage());
            status = StatusType.ERROR;
        } catch (RemoteAccessException ex) {
            LOG.error("{}: Failed to send object to the core server: {}", importDto.getBaseDirectory(), ex.getMessage());
            status = StatusType.ERROR;
        }
        return status;
    }
}
