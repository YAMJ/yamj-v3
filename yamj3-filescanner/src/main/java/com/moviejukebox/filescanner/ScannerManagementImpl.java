package com.moviejukebox.filescanner;

import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.dto.FileImportDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.remote.service.PingService;
import com.moviejukebox.common.type.ExitType;
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
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import static com.moviejukebox.common.type.ExitType.*;

public class ScannerManagementImpl implements ScannerManagement {

    private static final Logger LOG = LoggerFactory.getLogger(ScannerManagementImpl.class);
    private static final String LOG_MESSAGE = "FileScanner: ";
    // List of files
    private static List<File> fileList;
    @Resource(name = "fileImportService")
    private FileImportService fileImportService;
    @Resource(name = "pingService")
    private PingService pingService;

    @Override
    public ExitType runScanner(CmdLineParser parser) {
        fileList = new ArrayList<File>();
        String directoryProperty = parser.getParsedOptionValue("d");
        File directory = new File(directoryProperty);

        ExitType status = scan(directory);

        LOG.info("{}", ScannerStatistics.generateStats());
        LOG.info("{}Scanning completed.", LOG_MESSAGE);

        if (status == SUCCESS) {
            status = send(directory);
        }

        LOG.info("{}Exiting with status {}", LOG_MESSAGE, status);

        return status;
    }

    private ExitType scan(File directoryToScan) {
        LOG.info("{}Scanning directory '{}'...", LOG_MESSAGE, directoryToScan.getName());

        if (!directoryToScan.exists()) {
            LOG.info("{}Failed to read directory '{}'", LOG_MESSAGE, directoryToScan);
            return NO_DIRECTORY;
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
        return SUCCESS;
    }

    private ExitType send(File directoryScanned) {
        LOG.info("{}Starting to send the files to the core server...", LOG_MESSAGE);

        try {
            String pingResponse = pingService.ping();
            LOG.info("{}Ping response: {}", LOG_MESSAGE, pingResponse);
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return CONNECT_FAILURE;
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
                    LOG.error("{}{}", LOG_MESSAGE, dto.toString());
                }
            }
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return CONNECT_FAILURE;
        }

        LOG.info("{}Completed sending of files to core server...", LOG_MESSAGE);

        return SUCCESS;
    }
}
