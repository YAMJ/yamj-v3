package com.moviejukebox.core.remote.service;

import com.moviejukebox.common.dto.FileImportDTO;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.core.database.dao.FileStageDao;
import com.moviejukebox.core.database.model.FileStage;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("fileImportService")
public class FileImportServiceImpl implements FileImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileImportServiceImpl.class);

    @Autowired
    private FileStageDao fileStageDao;
    
    @Override
    public void importFile(FileImportDTO fileImportDTO) {
        // TODO exception handing if saving the entity failed
        
        FileStage fileStage = new FileStage();
        fileStage.setScanPath(fileImportDTO.getScanPath());
        fileStage.setFilePath(fileImportDTO.getFilePath());
        fileStage.setFileDate(new Date(fileImportDTO.getFileDate()));
        fileStage.setFileSize(fileImportDTO.getFileSize());
        fileStageDao.saveEntity(fileStage);
        
        LOGGER.debug("Stored stage file: " + fileStage);
    }

    @Override
    public void deleteFile(FileImportDTO fileImportDTO) {
        // TODO delete the file
    }
}
