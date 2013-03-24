package com.moviejukebox.common.remote.service;

import com.moviejukebox.common.dto.FileImportDTO;

public interface FileImportService {

    void importFile(FileImportDTO fileImportDTO);
    
    void deleteFile(FileImportDTO fileImportDTO);
}
