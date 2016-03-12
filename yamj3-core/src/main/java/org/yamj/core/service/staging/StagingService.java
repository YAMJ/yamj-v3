/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import java.io.File;
import java.util.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.dto.ImportDTO;
import org.yamj.common.dto.StageDirectoryDTO;
import org.yamj.common.dto.StageFileDTO;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.mediaimport.FilenameScanner;

@Transactional(readOnly = true)
@Service("stagingService")
public class StagingService {

    private static final Logger LOG = LoggerFactory.getLogger(StagingService.class);

    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private FilenameScanner filenameScanner;
    @Autowired
    private ConfigService configService;

    @Value("${yamj3.folder.name.watched:null}")
    private String watchedFolderName;
    @Value("${yamj3.folder.name.subtitle:null}")
    private String subtitleFolderName;

    @Transactional
    public Library storeLibrary(ImportDTO libraryDTO) {
        Library library = stagingDao.getLibrary(libraryDTO.getClient(), libraryDTO.getPlayerPath());
        if (library == null) {
            library = new Library();
            library.setClient(libraryDTO.getClient());
            library.setPlayerPath(libraryDTO.getPlayerPath());
        }
        library.setBaseDirectory(FilenameUtils.normalizeNoEndSeparator(libraryDTO.getBaseDirectory(), true));
        library.setLastScanned(new Date());
        stagingDao.storeEntity(library);
        return library;
    }

    @Transactional
    public void storeStageDirectory(StageDirectoryDTO stageDirectoryDTO, Library library) {
        // normalize the directory path
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

            LOG.debug("New directory: {}", stageDirectory.getDirectoryPath());
            stagingDao.saveEntity(stageDirectory);
        } else {
            Date newDate = getDateWithoutMilliseconds(stageDirectoryDTO.getDate());
            if (newDate.compareTo(stageDirectory.getDirectoryDate()) != 0) {
                stageDirectory.setDirectoryDate(newDate);

                LOG.debug("Updated directory: {}", stageDirectory.getDirectoryPath());
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

            FileType fileType = filenameScanner.determineFileType(extension);
            if (fileType == FileType.UNKNOWN) {
                LOG.info("Skipped file cause unknown extension: {}", stageFileDTO.getFileName());
                // unknown file type
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
                stageFile.setStatus(StatusType.NEW);

                // set changeable values in stage file
                setChangeableValues(stageFile, stageFileDTO);

                LOG.debug("New {} file: {}", stageFile.getFileType().name().toLowerCase(), stageFile.getFullPath());
                stagingDao.saveEntity(stageFile);
            } else {
                Date newDate = getDateWithoutMilliseconds(stageFileDTO.getFileDate());
                if ((newDate.compareTo(stageFile.getFileDate()) != 0) || (stageFile.getFileSize() != stageFileDTO.getFileSize())) {

                    // set changeable values in stage file
                    setChangeableValues(stageFile, stageFileDTO);

                    if (stageFile.isNew()) {
                        // leave NEW status as NEW
                    }  else if (stageFile.isDuplicate()) {
                        // leave DUPLICATE status as DUPLICATE
                        // Note: duplicate is only set for videos with same name
                    } else {
                        // mark stage file as updated
                        stageFile.setStatus(StatusType.UPDATED);
                    }

                    LOG.debug("Updated {} file: {}", stageFile.getFileType().name().toLowerCase(), stageFile.getFullPath());
                    stagingDao.updateEntity(stageFile);
                }
            }
        }
    }

    private static void setChangeableValues(StageFile stageFile, StageFileDTO stageFileDTO) {
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

    private static Date getDateWithoutMilliseconds(long millis) {
        // strip milliseconds cause mostly not stored in database
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public List<StageFile> getValidNFOFiles(VideoData videoData) {
        // read NFO files for movies
        return this.stagingDao.getValidNFOFilesForVideo(videoData.getId());
    }

    public List<StageFile> getValidNFOFiles(Series series) {
        // read NFO files for series
        return this.stagingDao.getValidNFOFilesForSeries(series.getId());
    }

    public void updateStageFile(StageFile stageFile) {
        this.stagingDao.updateEntity(stageFile);
    }

    @Transactional
    public boolean deleteStageFile(long id) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("status", StatusType.DELETED);
        return this.stagingDao.executeUpdate("update StageFile set status=:status where id=:id", params)>0;
    }

    @Transactional
    public boolean updateStageFile(long id) {
        StageFile stageFile = stagingDao.getStageFile(id);
        if (stageFile == null || stageFile.isDuplicate()) {
            // stage file not found or duplicate
            return false;
        }
        stageFile.setStatus(StatusType.UPDATED);
        this.stagingDao.updateEntity(stageFile);
        return true;
    }

    public void updateWatchedFile(MediaFile mediaFile, StageFile stageFile) {
        // reset watched file date
        Date maxWatchedFileDate = this.maxWatchedFileDate(stageFile);
        if (maxWatchedFileDate != null) {
            // just update last date if max watched file date has been found
            mediaFile.setWatchedFile(true, maxWatchedFileDate);
        } else if (mediaFile.isWatchedFile()) {
            // set watched date to actual date if watch-change detected
            mediaFile.setWatchedFile(false, new Date());
        }
    }
    
    public Date maxWatchedFileDate(StageFile videoFile) {
        boolean checkLibrary = this.configService.getBooleanProperty("yamj3.librarycheck.folder.watched", Boolean.TRUE);
        return this.stagingDao.maxWatchedFileDate(videoFile, watchedFolderName, checkLibrary);
    }

    public List<StageFile> findWatchedVideoFiles(StageFile watchedFile) {
        String videoBaseName = FilenameUtils.getBaseName(watchedFile.getBaseName());
        String videoExtension = FilenameUtils.getExtension(watchedFile.getBaseName());
        if (filenameScanner.determineFileType(videoExtension) != FileType.VIDEO) {
            // extension is no video, so use the full base name
            videoBaseName = watchedFile.getBaseName();
            videoExtension = null;
        }

        List<StageFile> videoFiles;
        if (FileTools.isWithinSpecialFolder(watchedFile, watchedFolderName)) {

            Library library = null;
            if (this.configService.getBooleanProperty("yamj3.librarycheck.folder.watched", Boolean.TRUE)) {
                library = watchedFile.getStageDirectory().getLibrary();
            }

            // search in all directories of the library
            videoFiles = this.stagingDao.findStageFiles(FileType.VIDEO, videoBaseName, videoExtension, library);
        } else {
            // search in just this directory
            videoFiles = this.stagingDao.findStageFiles(FileType.VIDEO, videoBaseName, videoExtension, watchedFile.getStageDirectory());
        }

        return videoFiles;
    }

    public List<StageFile> findSubtitleVideoFiles(StageFile subtitleFile, String language) {
        String videoBaseName = subtitleFile.getBaseName();
        if (StringUtils.isNotBlank(language)) {
            // remove extension cause that was the language
            videoBaseName = FilenameUtils.removeExtension(videoBaseName);
        }

        List<StageFile> videoFiles;
        if (FileTools.isWithinSpecialFolder(subtitleFile, subtitleFolderName)) {

            Library library = null;
            if (this.configService.getBooleanProperty("yamj3.librarycheck.folder.subtitle", Boolean.TRUE)) {
                library = subtitleFile.getStageDirectory().getLibrary();
            }

            // search in all directories of the library
            videoFiles = this.stagingDao.findStageFiles(FileType.VIDEO, videoBaseName, null, library);
        } else {
            // search in just this directory
            videoFiles = this.stagingDao.findStageFiles(FileType.VIDEO, videoBaseName, null, subtitleFile.getStageDirectory());
        }

        return videoFiles;
    }
    
    public List<StageFile> findVideoStageFiles(Artwork artwork) {
        return this.stagingDao.findVideoStageFiles(artwork);
    }
}
