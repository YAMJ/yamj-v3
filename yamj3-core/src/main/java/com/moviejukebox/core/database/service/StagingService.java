package com.moviejukebox.core.database.service;

import com.moviejukebox.common.dto.LibraryDTO;
import com.moviejukebox.common.dto.StageDirectoryDTO;
import com.moviejukebox.common.dto.StageFileDTO;
import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.Library;
import com.moviejukebox.core.database.model.StageDirectory;
import com.moviejukebox.core.database.model.StageFile;
import com.moviejukebox.core.database.model.type.FileType;
import com.moviejukebox.core.database.model.type.StatusType;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("stagingService")
public class StagingService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingService.class);

    private List<String> videoTypes = Arrays.asList("avi,mkv".split(","));
    private List<String> imageTypes = Arrays.asList("png,jpg,gif".split(","));
    private List<String> subtitleTypes = Arrays.asList("srt,sub,ass".split(","));
    
    @Autowired
    private StagingDao stagingDao;

    @Transactional(propagation = Propagation.REQUIRED)
    public Library storeLibrary(LibraryDTO libraryDTO) {
        Library library = stagingDao.getLibrary(libraryDTO.getClient(), libraryDTO.getPlayerPath());
        if (library == null) {
            library = new Library();
            library.setClient(libraryDTO.getClient());
            library.setPlayerPath(libraryDTO.getPlayerPath());
        } 
        library.setBaseDirectory(libraryDTO.getBaseDirectory());
        library.setLastScanned(new Date(System.currentTimeMillis()));
        stagingDao.storeEntity(library);
        return library;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public StageDirectory storeStageDirectory(StageDirectoryDTO stageDirectoryDTO, Library library) {
        StageDirectory stageDirectory = stagingDao.getStageDirectory(stageDirectoryDTO.getPath(), library);
        if (stageDirectory == null) {
            stageDirectory = new StageDirectory();
            stageDirectory.setDirectoryPath(stageDirectoryDTO.getPath());
            stageDirectory.setLibrary(library);
            stageDirectory.setStatus(StatusType.NEW);
            stageDirectory.setDirectoryDate(new Date(stageDirectoryDTO.getDate()));
            stagingDao.saveEntity(stageDirectory);
        } else {
            Date newDate = new Date(stageDirectoryDTO.getDate());
            if (newDate.compareTo(stageDirectory.getDirectoryDate()) != 0) {
                stageDirectory.setDirectoryDate(new Date(stageDirectoryDTO.getDate()));
                stageDirectory.setStatus(StatusType.UPDATED);
                stagingDao.updateEntity(stageDirectory);
            }
        }
        return stageDirectory;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public StageFile storeStageFile(StageFileDTO stageFileDTO, StageDirectory stageDirectory) {
        StageFile stageFile = stagingDao.getStageFile(stageFileDTO.getFileName(), stageDirectory);
        if (stageFile == null) {
            // TODO file name scanning HERE
            
            stageFile = new StageFile();
            stageFile.setFileName(stageFileDTO.getFileName());
            stageFile.setFileDate(new Date(stageFileDTO.getFileDate()));
            stageFile.setFileSize(stageFileDTO.getFileSize());
            stageFile.setStageDirectory(stageDirectory);
            stageFile.setFileType(determineFileType(stageFileDTO.getFileName()));
            stageFile.setStatus(StatusType.NEW);
            stagingDao.saveEntity(stageFile);
        } else {
            Date newDate = new Date(stageFileDTO.getFileDate());
            if ((newDate.compareTo(stageFile.getFileDate()) != 0) || (stageFile.getFileSize() != stageFileDTO.getFileSize())) {
                stageFile.setFileDate(new Date(stageFileDTO.getFileDate()));
                stageFile.setFileSize(stageFileDTO.getFileSize());
                stageFile.setStatus(StatusType.UPDATED);
                stagingDao.updateEntity(stageFile);
            }
        }
        return stageFile;
    }
    
    private FileType determineFileType(String fileName) {
        try {
            int index = fileName.lastIndexOf(".");
            if (index < 0) {
                return FileType.UNKNOWN;
            }
            
            String extension = fileName.substring(index + 1).toLowerCase();
            if ("nfo".equals(extension)) {
                return FileType.NFO;
            }
            if (videoTypes.contains(extension)) {
                // BETTER trailer detection; i.e by file name
                String lowerFileName = fileName.toLowerCase();
                if (lowerFileName.equals("trailer."+extension)) {
                    return FileType.TRAILER;
                } else if (lowerFileName.endsWith(".trailer."+extension)) {
                    return FileType.TRAILER;
                }
                return FileType.VIDEO;
            }
            if (subtitleTypes.contains(extension)) {
                return FileType.SUBTITLE;
            }
            if (imageTypes.contains(extension)) {
                // determine exact image type
                String lowerFileName = fileName.toLowerCase();
                if (lowerFileName.equals("fanart."+extension)) {
                    return FileType.FANART;
                } else if (lowerFileName.endsWith(".fanart."+extension)) {
                    return FileType.FANART;
                } else if (lowerFileName.endsWith(".videoimage."+extension)) {
                    // TODO should be a pattern
                    return FileType.VIDEOIMAGE;
                }
                // assume everything else as poster
                return FileType.POSTER;
            }
            
        } catch (Exception error) {
            LOGGER.error("Failed to determine file type for: "+fileName, error);
        }
        return FileType.UNKNOWN;
    }
}
