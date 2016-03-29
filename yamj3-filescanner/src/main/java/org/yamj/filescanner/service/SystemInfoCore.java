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

import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Service;
import org.yamj.common.remote.service.SystemInfoService;
import org.yamj.common.tools.DateTimeTools;

/**
 * Wait for the core server to be available or timeout
 *
 * @author Stuart
 */
@Service
public final class SystemInfoCore {

    private static final Logger LOG = LoggerFactory.getLogger(SystemInfoCore.class);
    private static final String DATETIME_FORMAT = "yyyy-MM-dd hh:mm:ss";
    private static final long DIFF_CHECK_SECONDS = 5;
    private int timeoutSeconds;
    private int numberOfRetries;
    private DateTime lastCheck;
    private boolean connected;

    @Autowired
    private SystemInfoService pingService;

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setNumberOfRetries(int numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
    }

    public DateTime getLastCheck() {
        return lastCheck;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Attempt to connect to the core server using the default retries and
     * timeout
     *
     * @return
     */
    public boolean check() {
        return check(numberOfRetries, timeoutSeconds);
    }

    /**
     * Attempt to connect to the core server using the specified retries and
     * timeout
     *
     * @param retries
     * @param timeout
     * @return
     */
    public boolean check(int retries, int timeout) {
        boolean checkConnected = pingCore();
        int retryCount = 0;
        while (!checkConnected && (retryCount++ < retries)) {
            LOG.info("Attempt #{}/{}: Waiting {} seconds for server to become available.", retryCount, retries, timeout);
            try {
                TimeUnit.SECONDS.sleep(timeout);
                checkConnected = pingCore();
            } catch (InterruptedException ex) {
                LOG.info("Interrupted whilst waiting for connection to core server: {}", ex.getMessage());
                break;
            }
        }

        LOG.info(status());
        return checkConnected;
    }

    /**
     * Attempt to connect to the core server and get a response
     *
     * @return
     */
    private boolean pingCore() {
        lastCheck = new DateTime();
        boolean status;
        try {
            String pingResponse = pingService.ping();
            LOG.info("Ping response: {}", pingResponse);
            status = true;
        } catch (RemoteConnectFailureException ex) {
            LOG.error("Failed to connect to the core server: {}", ex.getMessage());
            LOG.trace("Exception:", ex);
            status = false;
        } catch (Exception ex) {
            // Hate catching general exceptions, but should determine how this is thrown
            LOG.error("General failure to connect to the core server: {}", ex.getMessage());
            LOG.trace("Exception:", ex);
            status = false;
        }
        connected = status;
        return status;
    }

    /**
     * Get a string representation of the status
     *
     * @return
     */
    public String status() {
        long diff = DateTimeTools.getDuration(lastCheck, new DateTime());

        StringBuilder status = new StringBuilder("Core server last checked at ");
        status.append(DateTimeTools.convertDateToString(lastCheck, DATETIME_FORMAT));
        if (diff > TimeUnit.SECONDS.toMillis(DIFF_CHECK_SECONDS)) {
            // Only add the difference if the time was longer than 5 seconds
            status.append(", ");
            status.append(DateTimeTools.formatDurationText(diff));
            status.append(" ago");
        }
        status.append(" and connection status was ").append(connected ? "connected" : "not connected");
        return status.toString();
    }
}
