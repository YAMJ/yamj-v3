package com.yamj.core.remote.service;

import com.yamj.core.service.staging.StagingService;

import com.yamj.common.dto.ImportDTO;
import com.yamj.common.remote.service.FileImportService;
import com.yamj.core.database.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("fileImportService")
public class FileImportServiceImpl implements FileImportService {

    private static final Logger LOG = LoggerFactory.getLogger(FileImportServiceImpl.class);
    @Autowired
    private StagingService stagingService;

    @Override
    public void importScanned(ImportDTO importDTO) {
        Library library;
        try {
            library = stagingService.storeLibrary(importDTO);
            stagingService.storeStageDirectory(importDTO.getStageDirectory(), library);
            LOG.debug("Imported scanned directory: {}", importDTO.getStageDirectory().getPath());
        } catch (Exception error) {
            LOG.error("Failed to import scanned directory: {}", importDTO.getStageDirectory().getPath(), error);
            throw new RuntimeException("Failed to import scanned directory: "+importDTO.getStageDirectory().getPath());
        }
    }
}
