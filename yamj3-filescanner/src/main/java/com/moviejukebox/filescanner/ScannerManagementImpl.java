package com.moviejukebox.filescanner;

import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.filescanner.stats.ScannerStatistics;
import com.moviejukebox.filescanner.stats.StatType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScannerManagementImpl implements ScannerManagement {

    private static final Logger LOG = LoggerFactory.getLogger(FileScanner.class);
    private static final String LOG_MESSAGE = "FileScanner: ";
    // Exit status codes
    private static final int EXIT_NORMAL = 0;
    private static final int EXIT_NO_DIRECTORY = 1;
    // DEBUG ONLY - Get it from the command line or properties
    private static final String DIRECTORY_TO_SCAN = "T:/Films/";
    // List of files
    private static List<File> fileList;
    @Resource(name = "fileImportService")
    private FileImportService fileImportService;

    @Override
    public int runScanner(CmdLineParser parser) {
        fileList = new ArrayList<>();
        String directoryProperty = parser.getParsedOptionValue("d");
        File directory = new File(directoryProperty);

        int status = scan(directory);

        LOG.info("{}",ScannerStatistics.generateStats());
        LOG.info("{}Scanning completed.", LOG_MESSAGE);
        LOG.info("{}Exiting with status {}", LOG_MESSAGE, status);

        return status;
    }

    private int scan(File directoryToScan) {
        int status = EXIT_NORMAL;
        LOG.info("{}Scanning directory {}...", LOG_MESSAGE, directoryToScan.getName());

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
}
