package com.moviejukebox.filescanner.tools;

import java.io.*;
import java.util.*;
import name.pachler.nio.file.*;
import static name.pachler.nio.file.StandardWatchEventKind.*;
import static name.pachler.nio.file.ext.ExtendedWatchEventModifier.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Watcher {

    private static final Logger LOG = LoggerFactory.getLogger(Watcher.class);
    private final WatchService watcher = FileSystems.getDefault().newWatchService();
    private final Map<WatchKey, Path> keys = new HashMap<WatchKey, Path>();
    private boolean trace = Boolean.FALSE;
    private static final WatchEvent.Kind[] standardEvents = {ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE};
    private boolean watchEnabled = Boolean.TRUE;    // keep watching the directories

    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Creates a WatchService
     */
    public Watcher() {
    }

    /**
     * Creates a WatchService and registers the given directory
     *
     * @param dir
     */
    public Watcher(Path dir) throws IOException {
        addDirectory(dir);
    }

    /**
     * Creates a WatchService and registers the given directory
     *
     * @param dir
     */
    public Watcher(String dir) throws IOException {
        addDirectory(dir);
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
        register(dir);

        // enable trace after initial registration
        this.trace = Boolean.TRUE;
    }

    /**
     * Register the given directory with the WatchService
     *
     * @param dir
     */
    private void register(Path dir) {
        WatchKey key = null;
        try {
            key = dir.register(watcher, standardEvents, FILE_TREE);
        } catch (UnsupportedOperationException ex) {
            LOG.warn("File watching not supported: {}", ex.getMessage());
        } catch (IOException ex) {
            LOG.error("IO Error: {}", ex.getMessage());
        }

        if (key != null) {
            if (trace) {
                Path prev = keys.get(key);
                if (prev == null) {
                    LOG.info("Register: {}", dir);
                } else {
                    if (!dir.equals(prev)) {
                        LOG.info("Update: {} -> {}", prev, dir);
                    }
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

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException ex) {
                continue;
            } catch (ClosedWatchServiceException ex) {
                LOG.info("Watch service closed, terminating.");
                watchEnabled = Boolean.FALSE;
                break;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                LOG.warn("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
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
                LOG.info("{}: {}", event.kind().name(), child);

            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    public void stopWatching() {
        setWatching(Boolean.FALSE);
    }

    public void startWatching() {
        setWatching(Boolean.TRUE);
    }

    public void setWatching(boolean watchFlag) {
        LOG.info("{} the watch process", (watchFlag ? "Enabling" : "Disabling"));
        this.watchEnabled = watchFlag;
    }
}
