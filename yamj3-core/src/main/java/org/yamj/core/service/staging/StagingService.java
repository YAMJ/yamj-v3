/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.staging;

import org.apache.commons.lang3.StringUtils;

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

            // getById parent stage directory
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
            String baseName = FilenameUtils.getBaseName(stageFileDTO.getFileName());
            String extension = FilenameUtils.getExtension(stageFileDTO.getFileName());
            if (StringUtils.isBlank(baseName) || StringUtils.isBlank(extension)) {
                // no valid baseName or extension
                continue;
            }
            
            StageFile stageFile = stagingDao.getStageFile(baseName, extension, stageDirectory);
            if (stageFile == null) {
                // create new stage file entry
                stageFile = new StageFile();
                stageFile.setBaseName(baseName);
                stageFile.setExtension(extension);
                stageFile.setFileDate(new Date(stageFileDTO.getFileDate()));
                stageFile.setFileSize(stageFileDTO.getFileSize());
                stageFile.setStageDirectory(stageDirectory);
                stageFile.setFileType(filenameScanner.determineFileType(extension));
                stageFile.setFullPath(FilenameUtils.concat(stageDirectoryDTO.getPath(), stageFileDTO.getFileName()));
                stageFile.setStatus(StatusType.NEW);
                stagingDao.saveEntity(stageFile);
            } else {
                Date newDate = new Date(stageFileDTO.getFileDate());
                if ((newDate.compareTo(stageFile.getFileDate()) != 0) || (stageFile.getFileSize() != stageFileDTO.getFileSize())) {
                    stageFile.setFileDate(new Date(stageFileDTO.getFileDate()));
                    stageFile.setFileSize(stageFileDTO.getFileSize());
                    if (!StatusType.DUPLICATE.equals(stageFile.getStatus())) {
                        // mark as updated if no duplicate
                        stageFile.setStatus(StatusType.UPDATED);
                    }
                    stagingDao.updateEntity(stageFile);
                }
            }
        }
    }
}
