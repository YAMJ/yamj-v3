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
    // The Collection of libraries
    private LibraryCollection libraryCollection;
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
        libraryCollection = new LibraryCollection();

        String directoryProperty = parser.getParsedOptionValue("d");
        boolean watchEnabled = parseWatchStatus(parser.getParsedOptionValue("w"));
        String libraryFilename = parser.getParsedOptionValue("l");

        if (StringUtils.isNotBlank(libraryFilename)) {
            libraryCollection.processLibraryFile(libraryFilename, watchEnabled);
        }

        if (StringUtils.isNotBlank(directoryProperty)) {
            LOG.info("Adding directory from command line: {}", directoryProperty);
            libraryCollection.addLibraryDirectory(directoryProperty, watchEnabled);
        }

        LOG.info("Found {} libraries to process.", libraryCollection.size());
        if (libraryCollection.size() == 0) {
            return NO_DIRECTORY;
        }

        ExitType status = SUCCESS;
        for (Library library : libraryCollection.getLibraries()) {
            status = scan(library);
            LOG.info("{}", library.getStatistics().generateStats());
            LOG.info("Scanning completed.");

            if (status == SUCCESS) {
                status = send(library);
            }
        }

        if (watchEnabled) {
            LOG.info("Watching directory '{}' for changes...", directoryProperty);
            try {
                Watcher wd = new Watcher(directoryProperty);
                wd.processEvents();
            } catch (IOException ex) {
                LOG.warn("Unable to watch directory '{}', error: {}", directoryProperty, ex.getMessage());
                status = WATCH_FAILURE;
            }
            LOG.info("Watching directory '{}' completed", directoryProperty);
        }

        LOG.info("Exiting with status {}", status);

        return status;
    }

    /**
     * Start scanning a library.
     *
     * @param library
     * @return
     */
    private ExitType scan(Library library) {
        ExitType status = SUCCESS;
        File baseDirectory = new File(library.getBaseDirectory());
        LOG.info("Scanning library '{}'...", baseDirectory.getName());

        if (!baseDirectory.exists()) {
            LOG.info("Failed to read directory '{}'", baseDirectory);
            return NO_DIRECTORY;
        }

        StageDirectoryDTO sd = new StageDirectoryDTO();
        sd.setPath(baseDirectory.getAbsolutePath());
        sd.setDate(baseDirectory.lastModified());
        library.setStageDirectory(sd);

        List<File> currentFileList = Arrays.asList(baseDirectory.listFiles());
        for (File file : currentFileList) {
            if (file.isDirectory()) {
                scanDir(library, sd, file);
            } else {
                scanFile(library, sd, file);
            }
        }

        return status;
    }

    /**
     * Scan a directory (and recursively any other directories contained
     *
     * @param library
     * @param parentDto
     * @param directory
     */
    private void scanDir(Library library, StageDirectoryDTO parentDto, File directory) {
        DirectoryType dirEnd = checkDirectoryEnding(directory);

        LOG.info("Scanning directory '{}', detected type - {}", directory.getAbsolutePath(), dirEnd);

        if (dirEnd == DirectoryType.BLURAY || dirEnd == DirectoryType.DVD) {
            // Don't scan BLURAY or DVD structures
            LOG.info("Skipping directory '{}' because its a {} type", directory.getAbsolutePath(), dirEnd);
            library.getStatistics().inc(dirEnd == DirectoryType.BLURAY ? StatType.BLURAY : StatType.DVD);
        } else {
            library.getStatistics().inc(StatType.DIRECTORY);

            StageDirectoryDTO sd = new StageDirectoryDTO();
            parentDto.addStageDir(sd);

            List<File> currentFileList = Arrays.asList(directory.listFiles());
            for (File file : currentFileList) {
                if (file.isDirectory()) {
                    scanDir(library, sd, file);
                } else {
                    scanFile(library, sd, file);
                }
            }
        }
    }

    /**
     * Scan an individual file
     *
     * @param library
     * @param parentDto
     * @param file
     */
    private void scanFile(Library library, StageDirectoryDTO parentDto, File file) {
        LOG.info("Scanning file '{}'", file.getName());
        library.getStatistics().inc(StatType.FILE);
        StageFileDTO sf = new StageFileDTO(file);
        parentDto.addStageFile(sf);
    }

    /**
     * Send an entire library to the core
     *
     * @param library
     * @return
     */
    private ExitType send(Library library) {
        ExitType status = SUCCESS;
        LOG.info("Starting to send the files to the core server...");

        try {
            String pingResponse = pingService.ping();
            LOG.info("Ping response: {}", pingResponse);
        } catch (RemoteConnectFailureException ex) {
            LOG.error("Failed to connect to the core server: {}", ex.getMessage());
            return CONNECT_FAILURE;
        }

        try {
            LOG.info("Sending library '{}' to the server...", library.getBaseDirectory());
            try {
                ImportDTO dto = new ImportDTO();
                dto.setBaseDirectory(library.getBaseDirectory());
                dto.setClient("FileScanner");
                dto.setPlayerPath(library.getBaseDirectory());
                dto.setStageDirectory(library.getStageDirectory());
                fileImportService.importScanned(dto);
            } catch (RemoteAccessException ex) {
                LOG.error("Failed to send object to the core server: {}", ex.getMessage());
                return CONNECT_FAILURE;
            }
        } catch (RemoteConnectFailureException ex) {
            LOG.error("Failed to connect to the core server: {}", ex.getMessage());
            return CONNECT_FAILURE;
        }

        LOG.info("Completed sending of files to core server...");

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
