package com.moviejukebox.core.remote.service;

import com.moviejukebox.common.dto.*;
import com.moviejukebox.common.remote.service.FileImportService;
import com.moviejukebox.core.database.dao.FileStageDao;
import com.moviejukebox.core.database.model.FileStage;
import com.moviejukebox.core.database.model.Library;
import com.moviejukebox.core.database.model.StageDirectory;
import com.moviejukebox.core.database.model.StageFile;
import com.moviejukebox.core.database.model.type.FileStageType;
import com.moviejukebox.core.database.service.StagingService;
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
    @Autowired
    private StagingService stagingService;
    
    @Override
    public void importFile(FileImportDTO fileImportDTO) {
        // TODO exception handing if saving the entity failed
        
        FileStage fileStage = new FileStage();
        fileStage.setScanPath(fileImportDTO.getScanPath());
        fileStage.setFilePath(fileImportDTO.getFilePath());
        fileStage.setFileDate(new Date(fileImportDTO.getFileDate()));
        fileStage.setFileSize(fileImportDTO.getFileSize());
        fileStage.setFileStageType(FileStageType.NEW);
        fileStageDao.saveEntity(fileStage);
        
        LOGGER.debug("Stored stage file: " + fileStage);
    }

    @Override
    public void deleteFile(FileDeletionDTO fileDeletionDTO) {
        // TODO exception handing if saving the entity failed
        
        FileStage fileStage = new FileStage();
        fileStage.setScanPath(fileDeletionDTO.getScanPath());
        fileStage.setFilePath(fileDeletionDTO.getFilePath());
        fileStage.setFileDate(new Date(0));
        fileStage.setFileSize(0);
        fileStage.setFileStageType(FileStageType.DELETION);
        fileStageDao.saveEntity(fileStage);
        
        LOGGER.debug("Stored stage file for deletion: " + fileStage);
    }

    @Override
    public void importLibrary(LibraryDTO libraryDTO) {
        Library library = stagingService.storeLibrary(libraryDTO);
        for (StageDirectoryDTO stageDirectoryDTO : libraryDTO.getStageDirectories()) {
            StageDirectory stageDirectory = stagingService.storeStageDirectory(stageDirectoryDTO, library);
            for (StageFileDTO stageFileDTO : stageDirectoryDTO.getStageFiles()) {
                StageFile stageFile = stagingService.storeStageFile(stageFileDTO, stageDirectory);
                LOGGER.debug("Stored stage file: " + stageDirectory.getDirectoryPath() + stageFile.getFileName());
            }
        }
    }
}
