package org.yamj.core.service.staging;

import org.yamj.core.service.mediaimport.FilenameScanner;

import org.yamj.common.dto.ImportDTO;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.dto.StageFileDTO;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.Library;
import org.yamj.core.database.model.StageDirectory;
import org.yamj.core.database.model.StageFile;
import org.yamj.common.type.StatusType;
import java.util.Date;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("stagingService")
public class StagingService {

    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private FilenameScanner filenameScanner;

    @Transactional(propagation = Propagation.REQUIRED)
    public Library storeLibrary(ImportDTO libraryDTO) {
        Library library = stagingDao.getLibrary(libraryDTO.getClient(), libraryDTO.getPlayerPath());
        if (library == null) {
            library = new Library();
            library.setClient(libraryDTO.getClient());
            library.setPlayerPath(libraryDTO.getPlayerPath());
        }
        library.setBaseDirectory(FilenameUtils.normalizeNoEndSeparator(libraryDTO.getBaseDirectory(), true));
        library.setLastScanned(new Date(System.currentTimeMillis()));
        stagingDao.storeEntity(library);
        return library;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void storeStageDirectory(StageDirectoryDTO stageDirectoryDTO, Library library) {
        // normalize the directory path by using URI
        String normalized = FilenameUtils.normalizeNoEndSeparator(stageDirectoryDTO.getPath(), true);

        StageDirectory stageDirectory = stagingDao.getStageDirectory(normalized, library);
        if (stageDirectory == null) {
            stageDirectory = new StageDirectory();
            stageDirectory.setDirectoryPath(normalized);
            stageDirectory.setLibrary(library);
            stageDirectory.setStatus(StatusType.NEW);
            stageDirectory.setDirectoryDate(new Date(stageDirectoryDTO.getDate()));

            // get parent stage directory
            int lastIndex = normalized.lastIndexOf('/');
            if (lastIndex > 0) {
                String parentPath = normalized.substring(0, lastIndex);
                StageDirectory parent = stagingDao.getStageDirectory(parentPath, library);
                if (parent != null) {
                    stageDirectory.setParentDirectory(parent);
                }
            }

            stagingDao.saveEntity(stageDirectory);
        } else {
            Date newDate = new Date(stageDirectoryDTO.getDate());
            if (newDate.compareTo(stageDirectory.getDirectoryDate()) != 0) {
                stageDirectory.setDirectoryDate(new Date(stageDirectoryDTO.getDate()));
                stageDirectory.setStatus(StatusType.UPDATED);
                stagingDao.updateEntity(stageDirectory);
            }
        }

        for (StageFileDTO stageFileDTO : stageDirectoryDTO.getStageFiles()) {
            StageFile stageFile = stagingDao.getStageFile(stageFileDTO.getFileName(), stageDirectory);
            if (stageFile == null) {

                stageFile = new StageFile();
                stageFile.setFileName(stageFileDTO.getFileName());
                stageFile.setFileDate(new Date(stageFileDTO.getFileDate()));
                stageFile.setFileSize(stageFileDTO.getFileSize());
                stageFile.setStageDirectory(stageDirectory);
                stageFile.setFileType(filenameScanner.determineFileType(stageFileDTO.getFileName()));
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
        }
    }
}
