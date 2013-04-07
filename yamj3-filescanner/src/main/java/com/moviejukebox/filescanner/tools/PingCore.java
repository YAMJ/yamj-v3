package com.moviejukebox.filescanner.tools;

import com.moviejukebox.common.remote.service.PingService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteConnectFailureException;

/**
 * Wait for the core server to be available or timeout
 *
 * @author Stuart
 */
public class PingCore {

    private static Logger LOG = LoggerFactory.getLogger(PingCore.class);
    private long timeoutSeconds;
    // Spring service(s)
//    @Resource(name = "pingService")   - Cant work out how to get this to work
    private PingService pingService;

    public PingCore(PingService pingService) {
        this.pingService = pingService;
        this.timeoutSeconds = 30;
    }

    public PingCore(PingService pingService, long timeout) {
        this.pingService = pingService;
        this.timeoutSeconds = timeout;
    }

    public boolean check() {
        boolean connected = pingCore();
        if (!connected) {
            LOG.info("Waiting {} seconds for server to become available.", timeoutSeconds);
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
