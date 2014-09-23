/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import java.util.Calendar;

import java.io.File;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.dto.ImportDTO;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.dto.StageFileDTO;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.service.mediaimport.FilenameScanner;

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
            // used to set the directory name
            File dirFile = new File(normalized);

            stageDirectory = new StageDirectory();
            stageDirectory.setDirectoryPath(normalized);
            stageDirectory.setDirectoryName(dirFile.getName());
            stageDirectory.setLibrary(library);
            stageDirectory.setDirectoryDate(getDateWithoutMilliseconds(stageDirectoryDTO.getDate()));

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
            Date newDate = getDateWithoutMilliseconds(stageDirectoryDTO.getDate());
            if (newDate.compareTo(stageDirectory.getDirectoryDate()) != 0) {
                stageDirectory.setDirectoryDate(newDate);
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
                stageFile.setStageDirectory(stageDirectory);
                stageFile.setFileType(filenameScanner.determineFileType(extension));
                stageFile.setFullPath(FilenameUtils.concat(stageDirectoryDTO.getPath(), stageFileDTO.getFileName()));

                // set changeable values in stage file
                setChangeableValues(stageFile, stageFileDTO);

                stageFile.setStatus(StatusType.NEW);
                stagingDao.saveEntity(stageFile);
            } else {
                Date newDate = getDateWithoutMilliseconds(stageFileDTO.getFileDate());
                if ((newDate.compareTo(stageFile.getFileDate()) != 0) || (stageFile.getFileSize() != stageFileDTO.getFileSize())) {

                    // set changeable values in stage file
                    setChangeableValues(stageFile, stageFileDTO);
                    
                    if (StatusType.NEW.equals(stageFile.getStatus())) { 
                        // leave NEW status as NEW
                    }  else if (StatusType.DUPLICATE.equals(stageFile.getStatus())) {
                        // leave DUPLICATE status as DUPLICATE
                        // Note: duplicate is only set for videos with same name
                    } else {
                        // mark stage file as updated
                        stageFile.setStatus(StatusType.UPDATED);
                    }
                    
                    stagingDao.updateEntity(stageFile);
                }
            }
        }
    }
    
    private void setChangeableValues(StageFile stageFile, StageFileDTO stageFileDTO) {
        stageFile.setFileDate(getDateWithoutMilliseconds(stageFileDTO.getFileDate()));
        stageFile.setFileSize(stageFileDTO.getFileSize());

        if (FileType.VIDEO.equals(stageFile.getFileType())) {
            // media info scan content
            stageFile.setContent(stageFileDTO.getContent());
        } else if (FileType.NFO.equals(stageFile.getFileType())) {
            // NFO XML content
            stageFile.setContent(stageFileDTO.getContent());
        }
    }

    private Date getDateWithoutMilliseconds(long millis) {
        // strip milliseconds cause mostly not stored in database
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    
    }
    @Transactional
    public List<StageFile> getValidNFOFiles(VideoData videoData) {
        // read NFO files for movies
        return this.stagingDao.getValidNFOFilesForVideo(videoData.getId());
    }

    @Transactional
    public List<StageFile> getValidNFOFiles(Series series) {
        // read NFO files for series
        return this.stagingDao.getValidNFOFilesForSeries(series.getId());
    }

    @Transactional
    public void updateStageFile(StageFile stageFile) {
        this.stagingDao.updateEntity(stageFile);
    }
}
