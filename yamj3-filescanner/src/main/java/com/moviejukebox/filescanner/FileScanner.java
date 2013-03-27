package com.moviejukebox.filescanner;

import com.moviejukebox.filescanner.stats.ScannerStatistics;
import com.moviejukebox.filescanner.stats.StatType;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileScanner {

    private static final Logger LOG = LoggerFactory.getLogger(FileScanner.class);
    private static final String LOG_MESSAGE = "FileScanner: ";
    // DEBUG ONLY - Get it from the command line or properties
    private static final String DIRECTORY_TO_SCAN = "T:/Films/";
    // Exit status codes
    private static final int EXIT_NORMAL = 0;
    private static final int EXIT_NO_DIRECTORY = 1;

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        // Perhaps add some command line options in here using the cmdline parsing.

        FileScanner main = new FileScanner();
        int status = main.scan(DIRECTORY_TO_SCAN);

        LOG.info("{}",ScannerStatistics.generateStats());
        LOG.info("{}Scanning completed.", LOG_MESSAGE);
        LOG.info("{}Exiting with status {}", LOG_MESSAGE, status);
        System.exit(status);
    }

    private int scan(String directoryToScan) {
        File directory = new File(directoryToScan);
        return scan(directory);
    }

    private int scan(File directoryToScan) {
        int status = EXIT_NORMAL;
        LOG.info("{}Scanning directory {}...", LOG_MESSAGE,directoryToScan);

        if (directoryToScan == null || !directoryToScan.exists()) {
            LOG.info("{}Failed to read directory '{}'", LOG_MESSAGE, directoryToScan);
            return EXIT_NO_DIRECTORY;
        }

        List<File> fileList = Arrays.asList(directoryToScan.listFiles());
        Collections.sort(fileList);

        for (File file : fileList) {
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
