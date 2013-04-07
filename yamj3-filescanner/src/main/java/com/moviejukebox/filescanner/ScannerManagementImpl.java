package com.moviejukebox.filescanner;

import static com.moviejukebox.common.type.ExitType.CONNECT_FAILURE;
import static com.moviejukebox.common.type.ExitType.NO_DIRECTORY;
import static com.moviejukebox.common.type.ExitType.SUCCESS;

import com.moviejukebox.common.cmdline.CmdLineParser;
import com.moviejukebox.common.dto.ImportDTO;
import com.moviejukebox.common.dto.StageDirectoryDTO;
import com.moviejukebox.common.dto.StageFileDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.remote.service.PingService;
import com.moviejukebox.common.type.ExitType;
import com.moviejukebox.filescanner.model.LibraryStatistics;
import com.moviejukebox.filescanner.model.StatType;
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

/**
 * This is a simple scanner class.
 *
 * It does not support any of the more advanced functions (such as watching an rescanning.
 *
 * It will simple scan the directory specified, send the file list to the server and quit.
 *
 * @author Stuart
 */
public class ScannerManagementImpl implements ScannerManagement {

    private static final Logger LOG = LoggerFactory.getLogger(ScannerManagementImpl.class);
    private static final String LOG_MESSAGE = "FileScanner: ";
    // List of files
    private static List<File> fileList;
    // Statistics
    private static LibraryStatistics stats = new LibraryStatistics();
    @Resource(name = "fileImportService")
    private FileImportService fileImportService;
    @Resource(name = "pingService")
    private PingService pingService;
    // Constants
    private static final String DEFAULT_CLIENT = "FileScanner";

    @Override
    public ExitType runScanner(CmdLineParser parser) {
        fileList = new ArrayList<File>();
        String directoryProperty = parser.getParsedOptionValue("d");
        File directory = new File(directoryProperty);

        ExitType status = scan(directory);

        LOG.info("{}", stats.generateStats());
        LOG.info("{}Scanning completed.", LOG_MESSAGE);

        LOG.info("{}Exiting with status {}", LOG_MESSAGE, status);

        return status;
    }

    private ExitType scan(File directoryToScan) {
        LOG.info("{}Scanning directory '{}'...", LOG_MESSAGE, directoryToScan.getName());

        if (!directoryToScan.exists()) {
            LOG.info("{}Failed to read directory '{}'", LOG_MESSAGE, directoryToScan);
            return NO_DIRECTORY;
        }

        ImportDTO importDto = new ImportDTO();
        importDto.setClient(DEFAULT_CLIENT);
        importDto.setBaseDirectory(directoryToScan.getParent());
        importDto.setPlayerPath(directoryToScan.getParent());

        StageDirectoryDTO sdDto = new StageDirectoryDTO();
        sdDto.setDate(directoryToScan.lastModified());
        sdDto.setPath(directoryToScan.getAbsolutePath());

        importDto.setStageDirectory(sdDto);

        List<File> currentFileList = Arrays.asList(directoryToScan.listFiles());
        Collections.sort(currentFileList);
        fileList.addAll(currentFileList);

        for (File file : currentFileList) {
            if (file.isDirectory()) {
                stats.inc(StatType.DIRECTORY);
                scan(file);
            } else {
                stats.inc(StatType.FILE);
                sdDto.addStageFile(new StageFileDTO(file));
            }
        }
        return send(importDto);
    }

    private ExitType send(ImportDTO importDto) {
        LOG.info("{}Sending files to the core server...", LOG_MESSAGE);

        try {
            String pingResponse = pingService.ping();
            LOG.info("{}Ping response: {}", LOG_MESSAGE, pingResponse);
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return CONNECT_FAILURE;
        }

        try {
            LOG.info("{}Sending '{}' to the server...", LOG_MESSAGE, importDto.getBaseDirectory());
            try {
                fileImportService.importScanned(importDto);
            } catch (RemoteAccessException ex) {
                LOG.error("{}Failed to send object to the core server: {}", LOG_MESSAGE, ex.getMessage());
            }
        } catch (RemoteConnectFailureException ex) {
            LOG.error("{}Failed to connect to the core server: {}", LOG_MESSAGE, ex.getMessage());
            return CONNECT_FAILURE;
        }

        LOG.info("{}Completed sending of files to core server...", LOG_MESSAGE);

        return SUCCESS;
    }
}
