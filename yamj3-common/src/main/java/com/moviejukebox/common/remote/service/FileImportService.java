package com.moviejukebox.common.remote.service;

import com.moviejukebox.common.dto.FileDeletionDTO;
import com.moviejukebox.common.dto.FileImportDTO;
import com.moviejukebox.common.dto.ImportDTO;

public interface FileImportService {

    @Deprecated
    void importFile(FileImportDTO fileImportDTO);

    @Deprecated
    void deleteFile(FileDeletionDTO fileDeletionDTO);
    
    void importScanned(ImportDTO importDTO);
}
