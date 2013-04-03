package com.moviejukebox.common.remote.service;

import com.moviejukebox.common.dto.ImportDTO;

public interface FileImportService {

    void importScanned(ImportDTO importDTO);
}
