package com.moviejukebox.filescanner;

import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.dto.ImportDTO;
import com.moviejukebox.common.dto.StageDirectoryDTO;
import com.moviejukebox.common.dto.StageFileDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.type.DirectoryType;
import com.moviejukebox.common.type.ExitType;
import com.moviejukebox.filescanner.tools.Watcher;
import java.io.File;
import java.util.*;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.moviejukebox.common.type.ExitType.*;
import com.moviejukebox.filescanner.model.Library;
import com.moviejukebox.filescanner.model.LibraryCollection;
import com.moviejukebox.filescanner.model.StatType;
import com.moviejukebox.filescanner.tools.DirectoryEnding;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.springframework.remoting.RemoteAccessException;
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
    // The default watched status
    private static final Boolean DEFAULT_WATCH_STATE = Boolean.FALSE;    // TODO: Should be a property
    // Spring service(s)
    @Resource(name = "fileImportService")
    private FileImportService fileImportService;
    @Resource(name = "libraryCollection")
    private LibraryCollection libraryCollection;
    // Thread executers
    private static final int NUM_THREADS = 2;
    ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    List<Future<ExitType>> list = new ArrayList<Future<ExitType>>();
    // ImportDTO constants
    private static final String DEFAULT_CLIENT = "FileScanner";
    private static final String DEFAULT_PLAYER_PATH = "";

    /**
     * Start the scanner and process the command line properties.
     *
     * @param parser
     * @return
     */
    @Override
    public ExitType runScanner(CmdLineParser parser) {
        libraryCollection.setDefaultClient(DEFAULT_CLIENT);
        libraryCollection.setDefaultPlayerPath(DEFAULT_PLAYER_PATH);

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
        int count = 1;
        for (Library library : libraryCollection.getLibraries()) {
            libraryCollection.saveLibraryToFile("testLibrary_" + count++ + ".xml", library);

            status = scan(library);
            LOG.info("{}", library.getStatistics().generateStats());
            LOG.info("Scanning completed.");
        }

        if (watchEnabled) {
            Watcher wd = new Watcher();
            Boolean directoriesToWatch = Boolean.TRUE;

            for (Library library : libraryCollection.getLibraries()) {
                String dirToWatch = library.getImportDTO().getBaseDirectory();
                if (library.isWatch()) {
                    LOG.info("Watching directory '{}' for changes...", dirToWatch);
                    wd.addDirectory(dirToWatch);
                    directoriesToWatch = Boolean.TRUE;
                } else {
                    LOG.info("Watching skipped for directory '{}'", dirToWatch);
                }
            }

            if (directoriesToWatch) {
                wd.processEvents();
                LOG.info("Watching directory '{}' completed", directoryProperty);
            } else {
                LOG.info("No directories marked for watching.");
            }
        } else {
            LOG.info("Watching not enabled.");
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
        File baseDirectory = new File(library.getImportDTO().getBaseDirectory());
        LOG.info("Scanning library '{}'...", baseDirectory.getAbsolutePath());

        if (!baseDirectory.exists()) {
            LOG.info("Failed to read directory '{}'", baseDirectory.getAbsolutePath());
            return NO_DIRECTORY;
        }

        StageDirectoryDTO stageDir = new StageDirectoryDTO();
        stageDir.setPath(baseDirectory.getAbsolutePath());
        stageDir.setDate(baseDirectory.lastModified());
        library.addDirectory(stageDir);

        List<File> currentFileList = Arrays.asList(baseDirectory.listFiles());
        for (File file : currentFileList) {
            if (file.isDirectory()) {
                StageDirectoryDTO sd = scanDir(library, file);
                if (sd != null) {
                    library.addDirectory(sd);
                    send(library.getImportDTO(sd));
                } else {
                    LOG.info("Not adding directory '{}'", file.getAbsolutePath());
                }
            } else {
                stageDir.addStageFile(scanFile(file));
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
    private StageDirectoryDTO scanDir(Library library, File directory) {
        DirectoryType dirEnd = DirectoryEnding.check(directory);
        StageDirectoryDTO stageDir;

        LOG.info("Scanning directory '{}', detected type - {}", library.getRelativeDir(directory), dirEnd);

        if (dirEnd == DirectoryType.BLURAY || dirEnd == DirectoryType.DVD) {
            // Don't scan BLURAY or DVD structures
            LOG.info("Skipping directory '{}' as its a {} type", directory.getAbsolutePath(), dirEnd);
            library.getStatistics().inc(dirEnd == DirectoryType.BLURAY ? StatType.BLURAY : StatType.DVD);
            stageDir = null;
        } else {
            stageDir = new StageDirectoryDTO();
            stageDir.setPath(directory.getAbsolutePath());
            stageDir.setDate(directory.lastModified());

            library.getStatistics().inc(StatType.DIRECTORY);

            List<File> currentFileList = Arrays.asList(directory.listFiles());
            for (File file : currentFileList) {
                if (file.isDirectory()) {
                    StageDirectoryDTO scanSD = scanDir(library, file);
                    if (scanSD != null) {
                        library.addDirectory(scanSD);
                        send(library.getImportDTO(scanSD));
                    } else {
                        LOG.info("Not adding directory '{}'", file.getAbsolutePath());
                    }
                } else {
                    stageDir.addStageFile(scanFile(file));
                    library.getStatistics().inc(StatType.FILE);
                }
            }
        }
        return stageDir;
    }

    /**
     * Scan an individual file
     *
     * @param library
     * @param parentDto
     * @param file
     */
    private StageFileDTO scanFile(File file) {
        LOG.info("Scanning file '{}'", file.getName());
        return new StageFileDTO(file);
    }

    /**
     * Send an ImportDTO to the core
     *
     * @param importDto
     */
    private ExitType send(ImportDTO dto) {
        ExitType status;
        try {
//            LOG.info("Sending: {}", dto.toString());
            fileImportService.importScanned(dto);
            status = ExitType.SUCCESS;
        } catch (RemoteConnectFailureException ex) {
            LOG.error("Failed to connect to the core server: {}", ex.getMessage());
            status = CONNECT_FAILURE;
        } catch (RemoteAccessException ex) {
            LOG.error("Failed to send object to the core server: {}", ex.getMessage());
            status = CONNECT_FAILURE;
        }
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
}
