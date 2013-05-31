package org.yamj.common.remote.service;

import org.yamj.common.dto.ImportDTO;

public interface FileImportService {

    void importScanned(ImportDTO importDTO);
}
