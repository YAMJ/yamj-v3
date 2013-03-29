package com.moviejukebox.filescanner;

import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.dto.FileImportDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.remote.service.PingService;
import com.moviejukebox.filescanner.stats.ScannerStatistics;
import com.moviejukebox.filescanner.stats.StatType;
import com.moviejukebox.filescanner.watcher.Watcher;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;

public class WatcherManagementImpl implements ScannerManagement {

    private static final Logger LOG = LoggerFactory.getLogger(WatcherManagementImpl.class);
    private static final String LOG_MESSAGE = "FileScanner: ";
    // Exit status codes
    private static final int EXIT_NORMAL = 0;
    private static final int EXIT_NO_DIRECTORY = 1;
    private static final int EXIT_CONNECT_FAILURE = 2;
    private static final int EXIT_WATCH_FAILURE = 3;
    // List of files
    private static List<File> fileList;
    @Resource(name = "fileImportService")
    private FileImportService fileImportService;
    @Resource(name = "pingService")
    private PingService pingService;

    @Override
    public int runScanner(CmdLineParser parser) {
        fileList = new ArrayList<>();
        String directoryProperty = parser.getParsedOptionValue("d");
        boolean watchEnabled = Boolean.parseBoolean(parser.getParsedOptionValue("w"));
        File directory = new File(directoryProperty);

        int status = scan(directory);

        LOG.info("{}", ScannerStatistics.generateStats());
        LOG.info("{}Scanning completed.", LOG_MESSAGE);

        if (status == EXIT_NORMAL) {
            status = send(directory);
        }

        if (watchEnabled) {
            LOG.info("{}Watching directory '{}' for changes...", LOG_MESSAGE, directoryProperty);
            try {
                Watcher wd = new Watcher(directoryProperty);
                wd.processEvents();
            } catch (IOException ex) {
                LOG.warn("{}Unable to watch directory '{}', error: {}", LOG_MESSAGE, directoryProperty, ex.getMessage());
                status = EXIT_WATCH_FAILURE;
            }
            LOG.info("{}Watching directory '{}' completed", LOG_MESSAGE, directoryProperty);
        }

        LOG.info("{}Exiting with status {}", LOG_MESSAGE, status);

        return status;
    }

    private int scan(File directoryToScan) {
        int status = EXIT_NORMAL;
        LOG.info("{}Scanning directory '{}'...", LOG_MESSAGE, directoryToScan.getName());

        if (directoryToScan == null || !directoryToScan.exists()) {
            LOG.info("{}Failed to read directory '{}'", LOG_MESSAGE, directoryToScan);
            return EXIT_NO_DIRECTORY;
        }

        List<File> currentFileList = Arrays.asList(directoryToScan.listFiles());
        Collections.sort(currentFileList);
        fileList.addAll(currentFileList);

        for (File file : currentFileList) {
            if (file.isDirectory()) {
                ScannerStatistics.inc(StatType.DIRECTORY);
                scan(file);
            } else {
                ScannerStatistics.inc(StatType.FILE);
            }
        }
        return status;
    }

    private int send(File directoryScanned) {
        int status = EXIT_NORMAL;
        LOG.info("{}Starting to send the files to the core server...", LOG_MESSAGE);

        try {
            String pingResponse = pingService.ping();
            LOG.info("{}Ping response: {}", LOG_MESSAGE, pingResponse);
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return EXIT_CONNECT_FAILURE;
        }

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
                    LOG.error("{}{}", LOG_MESSAGE, (dto == null ? "No object found" : dto.toString()));
                }
            }
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return EXIT_CONNECT_FAILURE;
        }

        LOG.info("{}Completed sending of files to core server...", LOG_MESSAGE);

        return status;
    }
}
