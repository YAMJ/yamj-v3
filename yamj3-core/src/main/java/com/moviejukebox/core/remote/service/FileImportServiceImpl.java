package com.moviejukebox.core.remote.service;

import com.moviejukebox.core.service.StagingService;

import com.moviejukebox.common.dto.ImportDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.core.database.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("fileImportService")
public class FileImportServiceImpl implements FileImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileImportServiceImpl.class);

    @Autowired
    private StagingService stagingService;

    @Override
    public void importScanned(ImportDTO importDTO) {
        Library library = stagingService.storeLibrary(importDTO);
        stagingService.storeStageDirectory(importDTO.getStageDirectory(), library);
        LOGGER.debug("Imported scanned directory: " + importDTO.getStageDirectory().getPath());
    }
}
