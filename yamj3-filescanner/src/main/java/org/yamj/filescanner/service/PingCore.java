package org.yamj.filescanner.service;

import org.yamj.common.remote.service.PingService;
import org.yamj.common.tools.DateTimeTools;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Service;

/**
 * Wait for the core server to be available or timeout
 *
 * @author Stuart
 */
@Service("pingCore")
public final class PingCore {

    private static final Logger LOG = LoggerFactory.getLogger(PingCore.class);
    private static final String DATETIME_FORMAT = "yyyy-MM-dd hh:mm:ss";
    private static final long DIFF_CHECK_SECONDS = 5;
    private int timeoutSeconds;
    private int numberOfRetries;
    private DateTime lastCheck;
    private boolean connected;
    // Spring service(s)
    @Autowired
    private PingService pingService;

    public PingCore() {
    }

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
     * Attempt to connect to the core server using the default retries and timeout
     *
     * @return
     */
    public boolean check() {
        return check(numberOfRetries, timeoutSeconds);
    }

    /**
     * Attempt to connect to the core server using the specified retries and timeout
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
        Boolean status;
        try {
            String pingResponse = pingService.ping();
            LOG.info("Ping response: {}", pingResponse);
            status = Boolean.TRUE;
        } catch (RemoteConnectFailureException ex) {
            LOG.error("Failed to connect to the core server: {}", ex.getMessage());
            status = Boolean.FALSE;
        } catch (Exception ex) {
            // Hate catching general exceptions, but should determine how this is thrown
            LOG.error("General failure to connect to the core server: {}", ex.getMessage());
            status = Boolean.FALSE;
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
        if (diff > DIFF_CHECK_SECONDS * 1000) {
            // Only add the difference if the time was longer than 5 seconds
            status.append(", ");
            status.append(DateTimeTools.formatDurationText(diff));
            status.append(" ago");
        }
        status.append(" and connection status was ").append(connected ? "connected" : "not connected");
        return status.toString();
    }
}
