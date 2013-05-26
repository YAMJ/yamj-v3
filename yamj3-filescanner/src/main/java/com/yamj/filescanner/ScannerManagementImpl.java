package com.yamj.filescanner;

import com.yamj.common.cmdline.CmdLineParser;
import com.yamj.common.dto.ImportDTO;
import com.yamj.common.dto.StageDirectoryDTO;
import com.yamj.common.dto.StageFileDTO;
import com.yamj.common.remote.service.FileImportService;
import com.yamj.common.tools.PropertyTools;
import com.yamj.common.type.DirectoryType;
import com.yamj.common.type.ExitType;
import com.yamj.common.type.StatusType;
import com.yamj.filescanner.comparator.FileTypeComparator;
import com.yamj.filescanner.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yamj.filescanner.model.LibraryCollection;
import com.yamj.filescanner.model.StatType;
import com.yamj.filescanner.service.PingCore;
import com.yamj.filescanner.service.SendToCore;
import com.yamj.filescanner.tools.DirectoryEnding;
import com.yamj.filescanner.tools.Watcher;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Performs an initial scan of the library location and then updates when changes occur.
 *
 * @author Stuart
 */
public class ScannerManagementImpl implements ScannerManagement {

    /*
     * TODO: choose between watcher process and simple re-scan
     * TODO: determine what files have changed between scans
     * TODO: add library file reader
     * TODO: add multiple directory location support
     */
    private static final Logger LOG = LoggerFactory.getLogger(ScannerManagementImpl.class);
    // The default watched status
    private static final Boolean DEFAULT_WATCH_STATE = PropertyTools.getBooleanProperty("filescanner.watch.default", Boolean.FALSE);
    private AtomicInteger runningCount = new AtomicInteger(0);
    // Spring service(s)
    @Autowired
    private FileImportService fileImportService;
    @Autowired
    private LibraryCollection libraryCollection;
    @Autowired
    private PingCore pingCore;
    @Autowired
    private ThreadPoolTaskExecutor yamjExecutor;
    // ImportDTO constants
    private static final String DEFAULT_CLIENT = PropertyTools.getProperty("filescanner.default.client", "FileScanner");
    private static final String DEFAULT_PLAYER_PATH = PropertyTools.getProperty("filescanner.default.playerpath", "");

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
        pingCore.check(0, 0);   // Do a quick check of the status of the connection

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
            return ExitType.NO_DIRECTORY;
        }

        ExitType status = ExitType.SUCCESS;
        for (Library library : libraryCollection.getLibraries()) {
            library.getStatistics().setTimeStart(System.currentTimeMillis());
            status = scan(library);
            library.getStatistics().setTimeEnd(System.currentTimeMillis());
            LOG.info("{}", library.getStatistics().generateStatistics(Boolean.TRUE));
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
        ExitType status = ExitType.SUCCESS;
        File baseDirectory = new File(library.getImportDTO().getBaseDirectory());
        LOG.info("Scanning library '{}'...", baseDirectory.getAbsolutePath());

        if (!baseDirectory.exists()) {
            LOG.info("Failed to read directory '{}'", baseDirectory.getAbsolutePath());
            return ExitType.NO_DIRECTORY;
        }

        scanDir(library, baseDirectory);

        checkLibraryAllSent(library);
        LOG.info("Completed.");

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
        DirectoryType dirType = DirectoryEnding.check(directory);
        StageDirectoryDTO stageDir;

        LOG.info("Scanning directory '{}', detected type - {}", library.getRelativeDir(directory), dirType);

        if (dirType == DirectoryType.BLURAY || dirType == DirectoryType.DVD) {
            // Don't scan BLURAY or DVD structures
            LOG.info("Skipping directory '{}' as its a {} type", directory.getAbsolutePath(), dirType);
            library.getStatistics().increment(dirType == DirectoryType.BLURAY ? StatType.BLURAY : StatType.DVD);
            stageDir = null;
        } else {
            stageDir = new StageDirectoryDTO();
            stageDir.setPath(directory.getAbsolutePath());
            stageDir.setDate(directory.lastModified());

            library.getStatistics().increment(StatType.DIRECTORY);

            List<File> currentFileList = Arrays.asList(directory.listFiles());
            FileTypeComparator comp = new FileTypeComparator(Boolean.FALSE);
            Collections.sort(currentFileList, comp);

            for (File file : currentFileList) {
                if (file.isFile()) {
                    stageDir.addStageFile(scanFile(file));
                    library.getStatistics().increment(StatType.FILE);
                } else {
                    // First directory we find, we can stop (because we are sorted files first)
                    break;
                }
            }

            library.addDirectory(stageDir);
            // Now send the directory files before processing the directories
//            ExitType sendStatus = send(library.getImportDTO(stageDir));
//            if (sendStatus == ExitType.SUCCESS) {
//                library.addDirectoryStatus(stageDir.getPath(), StatusType.DONE);
//            }
            sendToCore(library, stageDir);

            // Resort the files with directories first
            comp.setDirectoriesFirst(Boolean.TRUE);
            Collections.sort(currentFileList, comp);

            // Now scan the directories
            for (File scanDir : currentFileList) {
                if (scanDir.isDirectory()) {
                    if (scanDir(library, scanDir) == null) {
                        LOG.info("Not adding directory '{}', no files found", scanDir.getAbsolutePath());
                    }
                } else {
                    // First file we find, we can stop (because we are sorted directories first)
                    break;
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
     * Increment the running count
     *
     * @param importDto
     */
    private void sendToCore(Library library, StageDirectoryDTO stageDir) {
        ImportDTO dto = library.getImportDTO(stageDir);

        LOG.debug("Sending #{}: {}", runningCount.incrementAndGet(), dto.getBaseDirectory());
        ApplicationContext cntx = ApplicationContextProvider.getApplicationContext();
        SendToCore stc = (SendToCore) cntx.getBean("sendToCore");
        stc.setImportDto(dto);
        stc.setCounter(runningCount);
        FutureTask<StatusType> task = new FutureTask<StatusType>(stc);
        yamjExecutor.submit(task);
        library.addDirectoryStatus(stageDir.getPath(), ConcurrentUtils.constantFuture(StatusType.DONE));
    }

    /**
     * Check that the library has all the directories sent to the core server
     *
     * If there are entries that have not been sent, or need resending, they should be done as well
     *
     * @param library
     */
    private void checkLibraryAllSent(Library library) {
        while (yamjExecutor.getActiveCount() > 0) {
            LOG.info("Remaining: {}, Active count: {}, Core Pool: {}, Max Pool: {}, Pool Size: {}",
                    runningCount.get(),
                    yamjExecutor.getActiveCount(),
                    yamjExecutor.getCorePoolSize(),
                    yamjExecutor.getMaxPoolSize(),
                    yamjExecutor.getPoolSize());
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ex) {
                //
            }
        }

        LOG.info("Completed");
        yamjExecutor.shutdown();

        LOG.info("Checking status of running threads.");
        boolean allDone = Boolean.FALSE;
        do {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ex) {
                //
            }

            for (Entry<String, Future<StatusType>> entry : library.getDirectoryStatus().entrySet()) {
                try {
                    LOG.info("{}: {}", entry.getKey(), entry.getValue().get());
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                }
                allDone = allDone || entry.getValue().isDone();
            }
            LOG.info("All Done?: {}\n\n", allDone);
        } while (!allDone);
    }

    /**
     * Check to see if the library entries need sending to the core server
     */
    private void sendLibrary2(Library library) {
        LOG.info("Checking directory status for library {}", library.getImportDTO().getBaseDirectory());

        LOG.info(pingCore.status());
        pingCore.check(10, 30);
        for (Entry<String, Future<StatusType>> entry : library.getDirectoryStatus().entrySet()) {
            Future<StatusType> fStatus = entry.getValue();
            String relDir = library.getRelativeDir(entry.getKey());
            LOG.info("  {} = Done: {}", relDir, fStatus.isDone());
            if (pingCore.isConnected()) {
                if (fStatus.isDone()) {
                    StatusType status;
                    try {
                        status = fStatus.get();
                    } catch (InterruptedException ex) {
                        status = StatusType.ERROR;
                    } catch (ExecutionException ex) {
                        status = StatusType.ERROR;
                    }

                    if (status != StatusType.DONE) {
                        LOG.info("    {}: Sending to core...", relDir);
                        sendToCore(library, library.getDirectory(entry.getKey()));
                    } else {
                        LOG.info("    {}: Already sent to core, status: {}", relDir, status);
                    }
                } else {
                    LOG.info("  {}: Not completed yet", relDir);
                }
            } else {
                LOG.info("  {}: Unable to send to core because of connection issues.", relDir);
            }
        }
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

    public ThreadPoolTaskExecutor getYamjExecutor() {
        return yamjExecutor;
    }

    public void setYamjExecutor(ThreadPoolTaskExecutor yamjExecutor) {
        this.yamjExecutor = yamjExecutor;
    }
}
