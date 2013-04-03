package com.moviejukebox.filescanner;

import static com.moviejukebox.common.type.ExitType.CONNECT_FAILURE;
import static com.moviejukebox.common.type.ExitType.NO_DIRECTORY;
import static com.moviejukebox.common.type.ExitType.SUCCESS;
import static com.moviejukebox.common.type.ExitType.WATCH_FAILURE;

import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.remote.service.PingService;
import com.moviejukebox.common.type.ExitType;
import com.moviejukebox.core.database.model.type.DirectoryType;
import com.moviejukebox.filescanner.stats.ScannerStatistics;
import com.moviejukebox.filescanner.stats.StatType;
import com.moviejukebox.filescanner.watcher.Watcher;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteConnectFailureException;

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
    // List of directories to scan and whether to keep watching them
    private static Map<String, Boolean> directoryList = new HashMap<String, Boolean>();
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

        Map<String, Boolean> library = processLibrary(libraryFilename, directoryProperty, watchEnabled);
        LOG.info("{}Found {} libraries to process.", LOG_MESSAGE, library.size());

        ExitType status = SUCCESS;
        for (String directoryString : library.keySet()) {
            File directory = new File(directoryString);
            status = scan(directory);
            LOG.info("{}", ScannerStatistics.generateStats());
            LOG.info("{}Scanning completed.", LOG_MESSAGE);

            if (status == SUCCESS) {
                status = send(directory);
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

    private ExitType scan(File directoryToScan) {
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
                    ScannerStatistics.inc(StatType.BLURAY);
                    // Don't scan BLURAY structures
                    LOG.info("{}Skipping {} directory type", LOG_MESSAGE, dirEnd);
                    continue;
                } else if (dirEnd == DirectoryType.DVD) {
                    ScannerStatistics.inc(StatType.DVD);
                    // Don't scan DVD structures
                    LOG.info("{}Skipping {} directory type", LOG_MESSAGE, dirEnd);
                    continue;
                } else {
                    ScannerStatistics.inc(StatType.DIRECTORY);
                    scan(file);
                }
            } else {
                ScannerStatistics.inc(StatType.FILE);
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

        /* use ImportDTO
        FileImportDTO dto;
        try {
            for (File file : fileList) {
                dto = new FileImportDTO();
                dto.setScanPath(directoryScanned.getAbsolutePath());
                dto.setFilePath(file.getAbsolutePath());
                if (file.isFile()) {
                    dto.setFileDate(file.lastModified());
                    dto.setFileSize(file.length());
                } else {
                    dto.setFileDate(0);
                    dto.setFileSize(0);
                }

                LOG.info("{}Sending '{}' to the server...", LOG_MESSAGE, file.getName());
                try {
                    fileImportService.importFile(dto);
                } catch (RemoteAccessException ex) {
                    LOG.error("{}Failed to send object to the core server: {}", LOG_MESSAGE, ex.getMessage());
                    LOG.error("{}{}", LOG_MESSAGE, dto.toString());
                }
            }
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return CONNECT_FAILURE;
        }
		*/
        
        LOG.info("{}Completed sending of files to core server...", LOG_MESSAGE);

        return status;
    }

    /**
     * Read the library files (multiple) / command line properties and set up the library
     *
     * @param libraryList The name and location of the library files to process
     * @param directoryProperty A single directory from the command line
     * @param watchEnabled The default watch status
     */
    private Map<String, Boolean> processLibrary(List<String> libraryList, String directoryProperty, boolean watchEnabled) {
        LOG.info("{}Library file: {}", LOG_MESSAGE, libraryList);
        LOG.info("{}Directory   : {}", LOG_MESSAGE, directoryProperty);
        LOG.info("{}Watch status: {}", LOG_MESSAGE, watchEnabled);

        Map<String, Boolean> processedLibrary = new HashMap<String, Boolean>();

        // Process the library files
        for (String singleLibrary : libraryList) {
            processedLibrary.putAll(processLibraryFile(singleLibrary, watchEnabled));
        }

        // Add the single command line library, if specified
        if (StringUtils.isNotBlank(directoryProperty)) {
            processedLibrary.put(directoryProperty, watchEnabled);
        }

        return processedLibrary;
    }

    /**
     * Read the library file (single) / command line properties and set up the library
     *
     * @param libraryFile
     * @param directoryProperty
     * @param watchEnabled
     * @return
     */
    private Map<String, Boolean> processLibrary(String libraryFile, String directoryProperty, boolean watchEnabled) {
        List<String> libraryList = new ArrayList<String>();
        libraryList.add(libraryFile);
        return processLibrary(libraryList, directoryProperty, watchEnabled);
    }

    /**
     * Read the library file from disk and return the library object
     *
     * @param libraryFile the file to read
     * @param defaultWatchState the default watched state if not provided in the file
     * @return
     */
    private Map<String, Boolean> processLibraryFile(String libraryFile, boolean defaultWatchState) {
        LOG.warn("{}processLibraryFile - Not supported yet.", LOG_MESSAGE);
        LOG.warn("{}Library File: {}", LOG_MESSAGE, libraryFile);
        LOG.warn("{}Watch state : {}", LOG_MESSAGE, defaultWatchState);
        return new HashMap<String, Boolean>();
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
