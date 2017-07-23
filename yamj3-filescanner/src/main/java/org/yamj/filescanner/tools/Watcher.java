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
package org.yamj.filescanner.tools;

import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_CREATE;
import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_DELETE;
import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_MODIFY;
import static name.pachler.nio.file.ext.ExtendedWatchEventModifier.FILE_TREE;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import name.pachler.nio.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Callable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutionException;

public class Watcher {

    private static final Logger LOG = LoggerFactory.getLogger(Watcher.class);
    private final WatchService watcherService;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private boolean trace = false;
    @SuppressWarnings("rawtypes")
    private static final WatchEvent.Kind[] STANDARD_EVENTS = {ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE};
    // keep watching the directories
    private boolean watchEnabled = true;
	
    /**
     * Creates a WatchService
     */
    public Watcher() {
       this.watcherService = FileSystems.getDefault().newWatchService();
    }

    /**
     * Creates a WatchService and registers the given directory
     *
     * @param dir
     * @throws java.io.IOException
     */
    public Watcher(Path dir) {
        this();
        addDirectory(dir);
    }

    /**
     * Creates a WatchService and registers the given directory
     *
     * @param dir
     * @throws java.io.IOException
     */
    public Watcher(String dir) {
        this();
        addDirectory(dir);
    }

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Add directory to the watch list
     *
     * @param dir
     */
    public final void addDirectory(String dir) {
        addDirectory(Paths.get(dir));
    }

    /**
     * Add directory to the watch list
     *
     * @param dir
     */
    public final void addDirectory(Path dir) {
		// enable trace for registration
        this.trace = true;
        register(dir);
    }

    /**
     * Register the given directory with the WatchService
     *
     * @param dir
     */
    private void register(Path dir) {
        WatchKey key = null;
		
        try {
            key = dir.register(watcherService, STANDARD_EVENTS, FILE_TREE);
        } catch (UnsupportedOperationException ex) {
            LOG.warn("File watching not supported: {}", ex.getMessage());
            LOG.trace("Exception:", ex);
        } catch (IOException ex) {
            LOG.error("IO Error:", ex);
        }

        if (key != null) {
            if (trace) {
                Path prev = keys.get(key);
                if (prev == null) {
                    LOG.info("Register Watcher for: {}", dir);
                } else if (!dir.equals(prev)) {
                    LOG.info("Update Watcher for: {} -> {}", prev, dir);
                }
            }
            keys.put(key, dir);
        }
    }

    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents() {
        while (watchEnabled) {

            // wait for key to be signaled
			 WatchKey key;
			// just info
			startWatching();
            try {
				key = watcherService.take();
			//	LOG.debug("Watcher start take() key and sleep 20s: " + key);
			// delay action for 20 seconds to let more time to change 
				TimeUnit.MILLISECONDS.sleep(20000);
            } catch (InterruptedException ex) {
				LOG.debug("ProcessEvents watchEnabled InterruptedException: {}", ex);
                continue;
            } catch (ClosedWatchServiceException ex) {
                LOG.info("Watch service closed, terminating.");
                LOG.trace("Watcher ProcessEvents Exception:", ex);
                watchEnabled = false;
                break;
            }

            Path dir = keys.get(key);
			LOG.debug("Watcher : " + dir);
            if (dir == null) {
                LOG.warn("WatchKey not recognized!!");
                continue;
            }
			
            for (WatchEvent<?> event : key.pollEvents()) {
                @SuppressWarnings("rawtypes")
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == StandardWatchEventKind.OVERFLOW) {
                    LOG.info("Too many watched events!");
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                LOG.info("{}: {} ", event.kind().name(), child); // runningCount.incrementAndGet());
				
            }
			
            // reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			LOG.debug("Watcher reset key.");
            if (!valid) {
               keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
					LOG.debug("Watcher keys.isEmpty()");
                   break;
                }
            }
			// now stop watcher to exit loop and treat what is modified
			 stopWatching();
        }
    }

    public void stopWatching() {
        setWatching(false);
    }

    public void startWatching() {
        setWatching(true);
    }

    public void setWatching(boolean watchFlag) {
        LOG.info("{} the watch process", watchFlag ? "Enabling" : "Disabling");
        this.watchEnabled = watchFlag;
    }
}
