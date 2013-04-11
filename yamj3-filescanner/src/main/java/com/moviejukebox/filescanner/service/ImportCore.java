package com.moviejukebox.filescanner.service;

import com.moviejukebox.common.dto.ImportDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.common.type.ExitType;
import static com.moviejukebox.common.type.ExitType.CONNECT_FAILURE;
import java.util.concurrent.Callable;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.stereotype.Service;

@Service("importCore")
public class ImportCore implements Callable<ExitType> {

    private static final Logger LOG = LoggerFactory.getLogger(ImportCore.class);
    // Spring service(s)
    @Resource(name = "fileImportService")
    FileImportService fileImportService;
    ImportDTO dto;

    public ImportCore() {
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
