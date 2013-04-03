package com.moviejukebox.filescanner;


import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.dto.ImportDTO;
import com.moviejukebox.common.dto.StageDirectoryDTO;
import com.moviejukebox.common.dto.StageFileDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.remote.service.PingService;
import com.moviejukebox.common.type.ExitType;
import com.moviejukebox.filescanner.watcher.Watcher;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteConnectFailureException;
import static com.moviejukebox.common.type.ExitType.*;
import com.moviejukebox.core.database.model.type.DirectoryType;
import com.moviejukebox.filescanner.model.Library;
import com.moviejukebox.filescanner.model.LibraryCollection;
import com.moviejukebox.filescanner.model.LibraryStatistics;
import com.moviejukebox.filescanner.model.StatType;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.remoting.RemoteAccessException;

/**
 * Performs an initial scan of the library location and then updates when changes occur.
 *
 * @author Stuart
 */
public class WatcherManagementImpl implements ScannerManagement {

    /*
     * TODO: choose between watcher process and simple re-scan
     * TODO: determine what files have changed between scans
     * TODO: add library file reader
     * TODO: add multiple directory location support
     *
     */
    private static final Logger LOG = LoggerFactory.getLogger(WatcherManagementImpl.class);
    private static final String LOG_MESSAGE = "FileScanner: ";
    // List of files
    private static List<File> fileList = new ArrayList<File>();
    // The default watched status
    private static final Boolean DEFAULT_WATCH_STATE = Boolean.FALSE;    // TODO: Should be a property
    // Directory endings for DVD and Blurays
    private static final Map<String, DirectoryType> DIR_ENDINGS = new HashMap<String, DirectoryType>(2);
    // Spring services
    @Resource(name = "fileImportService")
    private FileImportService fileImportService;
    @Resource(name = "pingService")
    private PingService pingService;

    static {
        // The ending of the directory & Type
        DIR_ENDINGS.put("BDMV", DirectoryType.BLURAY);
        DIR_ENDINGS.put("AUDIO_TS", DirectoryType.DVD);
        DIR_ENDINGS.put("VIDEO_TS", DirectoryType.DVD);
    }

    /**
     * Start the scanner and process the command line properties.
     *
     * @param parser
     * @return
     */
    @Override
    public ExitType runScanner(CmdLineParser parser) {
        String directoryProperty = parser.getParsedOptionValue("d");
        boolean watchEnabled = parseWatchStatus(parser.getParsedOptionValue("w"));
        String libraryFilename = parser.getParsedOptionValue("l");

        if (StringUtils.isNotBlank(libraryFilename)) {
            LibraryCollection.processLibraryFile(libraryFilename, watchEnabled);
        }

        if (StringUtils.isNotBlank(directoryProperty)) {
            LibraryCollection.processLibraryFile(directoryProperty, watchEnabled);
        }

        LOG.info("{}Found {} libraries to process.", LOG_MESSAGE, LibraryCollection.size());
        if (LibraryCollection.size() == 0) {
            return NO_DIRECTORY;
        }

        ExitType status = SUCCESS;
        for (Library library : LibraryCollection.getLibraries()) {
            File libraryDirectory = new File(library.getPath());
            status = scan(libraryDirectory);
            LOG.info("{}", library.getStatistics().generateStats());
            LOG.info("{}Scanning completed.", LOG_MESSAGE);

            if (status == SUCCESS) {
                status = send(libraryDirectory);
            }
        }

        if (watchEnabled) {
            LOG.info("{}Watching directory '{}' for changes...", LOG_MESSAGE, directoryProperty);
            try {
                Watcher wd = new Watcher(directoryProperty);
                wd.processEvents();
            } catch (IOException ex) {
                LOG.warn("{}Unable to watch directory '{}', error: {}", LOG_MESSAGE, directoryProperty, ex.getMessage());
                status = WATCH_FAILURE;
            }
            LOG.info("{}Watching directory '{}' completed", LOG_MESSAGE, directoryProperty);
        }

        LOG.info("{}Exiting with status {}", LOG_MESSAGE, status);

        return status;
    }

    private void scan(Library library) {

    }

    private ExitType scan(File directoryToScan) {
        LibraryStatistics stats = new LibraryStatistics();
        ExitType status = SUCCESS;
        LOG.info("{}Scanning directory '{}'...", LOG_MESSAGE, directoryToScan.getName());

        if (!directoryToScan.exists()) {
            LOG.info("{}Failed to read directory '{}'", LOG_MESSAGE, directoryToScan);
            return NO_DIRECTORY;
        }

        List<File> currentFileList = Arrays.asList(directoryToScan.listFiles());
        fileList.addAll(currentFileList);

        for (File file : currentFileList) {
            if (file.isDirectory()) {
                DirectoryType dirEnd = checkDirectoryEnding(file);

                if (dirEnd == DirectoryType.BLURAY) {
                    stats.inc(StatType.BLURAY);
                    // Don't scan BLURAY structures
                    LOG.info("{}Skipping {} directory type", LOG_MESSAGE, dirEnd);
                    continue;
                } else if (dirEnd == DirectoryType.DVD) {
                    stats.inc(StatType.DVD);
                    // Don't scan DVD structures
                    LOG.info("{}Skipping {} directory type", LOG_MESSAGE, dirEnd);
                    continue;
                } else {
                    stats.inc(StatType.DIRECTORY);
                    scan(file);
                }
            } else {
                stats.inc(StatType.FILE);
            }
        }
        return status;
    }

    private ExitType send(File directoryScanned) {
        ExitType status = SUCCESS;
        LOG.info("{}Starting to send the files to the core server...", LOG_MESSAGE);

        try {
            String pingResponse = pingService.ping();
            LOG.info("{}Ping response: {}", LOG_MESSAGE, pingResponse);
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return CONNECT_FAILURE;
        }

        ImportDTO importDto = new ImportDTO();
        StageDirectoryDTO dirDto;
        StageFileDTO fileDto;

        try {
            for (File file : fileList) {
                fileDto = new StageFileDTO();
                fileDto.setFileName(directoryScanned.getName());
                if (file.isFile()) {
                    fileDto.setFileDate(file.lastModified());
                    fileDto.setFileSize(file.length());
                } else {
                    fileDto.setFileDate(0);
                    fileDto.setFileSize(0);
                }

                LOG.info("{}Sending '{}' to the server...", LOG_MESSAGE, file.getName());
                try {
                    fileImportService.importScanned(importDto);
                } catch (RemoteAccessException ex) {
                    LOG.error("{}Failed to send object to the core server: {}", LOG_MESSAGE, ex.getMessage());
                    LOG.error("{}{}", LOG_MESSAGE, importDto.toString());
                }
            }
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return CONNECT_FAILURE;
        }

        LOG.info("{}Completed sending of files to core server...", LOG_MESSAGE);

        return status;
    }

    /**
     * Get the watched status from the command line property or return the default value.
     *
     * @param parsedOptionValue the property from the command line
     * @return
     */
    private boolean parseWatchStatus(String parsedOptionValue) {
        if (StringUtils.isBlank(parsedOptionValue)) {
            return DEFAULT_WATCH_STATE;
        }
        return Boolean.parseBoolean(parsedOptionValue);
    }

    /**
     * Return the DirectoryType of the directory
     *
     * @param directory
     * @return
     */
    private DirectoryType checkDirectoryEnding(File directory) {
        if (DIR_ENDINGS.containsKey(directory.getName())) {
            return DIR_ENDINGS.get(directory.getName());
        }
        return DirectoryType.STANDARD;
    }
}
