package com.moviejukebox.filescanner;

import com.moviejukebox.common.dto.ImportDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.type.ExitType;
import static com.moviejukebox.common.type.ExitType.CONNECT_FAILURE;
import com.moviejukebox.filescanner.model.Library;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;

public class SendToCore implements Callable<ExitType> {

    private static final Logger LOG = LoggerFactory.getLogger(SendToCore.class);
    FileImportService fileImportService;
    ImportDTO dto;

    public SendToCore(FileImportService fileImportService, ImportDTO dto) {
        this.fileImportService = fileImportService;
        this.dto = dto;
    }

    public FileImportService getFileImportService() {
        return fileImportService;
    }

    public void setFileImportService(FileImportService fileImportService) {
        this.fileImportService = fileImportService;
    }

    public ImportDTO getDto() {
        return dto;
    }

    public void setDto(ImportDTO dto) {
        this.dto = dto;
    }

    @Override
    public ExitType call() {
        ExitType status;
        try {
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
}
