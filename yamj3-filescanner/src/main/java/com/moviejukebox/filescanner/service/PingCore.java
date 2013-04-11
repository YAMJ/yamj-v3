package com.moviejukebox.filescanner.service;

import com.moviejukebox.common.remote.service.PingService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Resource;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Service;

/**
 * Wait for the core server to be available or timeout
 *
 * @author Stuart
 */
@Service("pingCore")
public class PingCore {

    private static Logger LOG = LoggerFactory.getLogger(PingCore.class);
    private long timeoutSeconds;
    private int numberOfRetries;
    // Spring service(s)
    @Resource(name = "pingService")
    private PingService pingService;

    public PingCore() {
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setNumberOfRetries(int numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
    }

    public boolean check() {
        boolean connected = pingCore();
        int retryCount = 1;
        while (!connected && (retryCount++ <= numberOfRetries)) {
            LOG.info("Attempt #{}: Waiting {} seconds for server to become available.", retryCount, timeoutSeconds);
            try {
                TimeUnit.SECONDS.sleep(timeoutSeconds);
                connected = pingCore();
            } catch (InterruptedException ex) {
                LOG.info("Interrupted whilst waiting for connection to core server: {}", ex.getMessage());
            }
        }

        return connected;
    }

    private boolean pingCore() {
        Boolean status;
        try {
            String pingResponse = pingService.ping();
            LOG.info("Ping response: {}", pingResponse);
            status = Boolean.TRUE;
        } catch (RemoteConnectFailureException ex) {
            LOG.error("Failed to connect to the core server: {}", ex.getMessage());
            status = Boolean.FALSE;
        }
        return status;
    }
}
