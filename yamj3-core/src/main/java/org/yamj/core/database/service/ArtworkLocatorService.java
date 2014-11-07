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
package org.yamj.core.database.service;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.FileType;

@Service("artworkLocatorService")
public class ArtworkLocatorService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkLocatorService.class);

    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private ConfigService configService;

    private Set<String> buildSearchMap(ArtworkType artworkType, List<StageFile> videoFiles, Set<StageDirectory> directories) {
        Set<String> artworkNames = new HashSet<String>();

        // generic names (placed in folder)
        if (ArtworkType.POSTER == artworkType) {
            artworkNames.add("poster");
            artworkNames.add("cover");
            artworkNames.add("folder");
        } else if (ArtworkType.FANART == artworkType) {
            artworkNames.add("fanart");
            artworkNames.add("backdrop");
            artworkNames.add("background");
        } else if (ArtworkType.BANNER == artworkType) {
            artworkNames.add("banner");
        } else {
            // no artwork names for this type
            return artworkNames;
        }
        
        for (StageFile videoFile : videoFiles) {
            directories.add(videoFile.getStageDirectory());
            
            // same name than video file
            if (ArtworkType.POSTER == artworkType) {
                artworkNames.add(videoFile.getBaseName().toLowerCase());
                artworkNames.add(videoFile.getBaseName().toLowerCase() + ".poster");
                artworkNames.add(videoFile.getBaseName().toLowerCase() + "-poster");
            } else if (ArtworkType.FANART == artworkType) {
                artworkNames.add(videoFile.getBaseName().toLowerCase() + ".fanart");
                artworkNames.add(videoFile.getBaseName().toLowerCase() + "-fanart");
            } else if (ArtworkType.BANNER == artworkType) {
                artworkNames.add(videoFile.getBaseName().toLowerCase() + ".banner");
                artworkNames.add(videoFile.getBaseName().toLowerCase() + "-banner");
            }
            
            // same name as directory
            String directoryName = videoFile.getStageDirectory().getDirectoryName().toLowerCase();
            if (ArtworkType.POSTER == artworkType) {
                artworkNames.add(directoryName);
                artworkNames.add(directoryName + ".poster");
                artworkNames.add(directoryName + "-poster");
            } else if (ArtworkType.FANART == artworkType) {
                artworkNames.add(directoryName + ".fanart");
                artworkNames.add(directoryName + "-fanart");
            } else if (ArtworkType.BANNER == artworkType) {
                artworkNames.add(directoryName + ".banner");
                artworkNames.add(directoryName + "-banner");
            }
        }
        return artworkNames;
    }

    private Set<String> buildSpecialMap(ArtworkType artworkType, List<StageFile> videoFiles) {
        Set<String> artworkNames = new HashSet<String>();
        for (StageFile videoFile : videoFiles) {
            if (ArtworkType.POSTER == artworkType) {
                artworkNames.add(videoFile.getBaseName().toLowerCase());
                artworkNames.add(videoFile.getBaseName().toLowerCase() + ".poster");
                artworkNames.add(videoFile.getBaseName().toLowerCase() + "-poster");
            } else if (ArtworkType.FANART == artworkType) {
                artworkNames.add(videoFile.getBaseName().toLowerCase() + ".fanart");
                artworkNames.add(videoFile.getBaseName().toLowerCase() + "-fanart");
            } else if (ArtworkType.BANNER == artworkType) {
                artworkNames.add(videoFile.getBaseName().toLowerCase() + ".banner");
                artworkNames.add(videoFile.getBaseName().toLowerCase() + "-banner");
            }
        }
        return artworkNames;
    }

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingArtwork(ArtworkType artworkType, VideoData videoData) {
        List<StageFile> videoFiles = findVideoFiles(videoData);
        if (CollectionUtils.isEmpty(videoFiles)) {
            return null;
        }

        // search in same directory than video files
        Set<StageDirectory> directories = new HashSet<StageDirectory>();
        Set<String> artworkNames = this.buildSearchMap(artworkType, videoFiles, directories);
        List<StageFile> artworks = findArtworkStageFiles(directories, artworkNames);

        String artworkFolderName = PropertyTools.getProperty("yamj3.folder.name.artwork");
        if (StringUtils.isNotBlank(artworkFolderName)) {
            
            Library library = null;
            if (this.configService.getBooleanProperty("yamj3.librarycheck.folder.artwork", Boolean.TRUE)) {
                library = videoFiles.get(0).getStageDirectory().getLibrary();
            }
            
            artworkNames = this.buildSpecialMap(artworkType, videoFiles);
            List<StageFile> specials = this.stagingDao.findStageFilesInSpecialFolder(FileType.IMAGE, artworkFolderName, library, artworkNames);
            artworks.addAll(specials);
        }

        // search
        LOG.debug("Found {} local {}s for movie {}", artworks.size(), artworkType.toString().toLowerCase(), videoData.getIdentifier());
        return artworks;
    }

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingArtwork(ArtworkType artworkType, Season season) {
        List<StageFile> videoFiles = findVideoFiles(season);
        if (CollectionUtils.isEmpty(videoFiles)) {
            return null;
        }

        // search in same directory than video files
        Set<StageDirectory> directories = new HashSet<StageDirectory>();
        Set<String> artworkNames = this.buildSearchMap(artworkType, videoFiles, directories);
        List<StageFile> artworks = findArtworkStageFiles(directories, artworkNames);

        String artworkFolderName = PropertyTools.getProperty("yamj3.folder.name.artwork");
        if (StringUtils.isNotBlank(artworkFolderName)) {
            
            Library library = null;
            if (this.configService.getBooleanProperty("yamj3.librarycheck.folder.artwork", Boolean.TRUE)) {
                library = videoFiles.get(0).getStageDirectory().getLibrary();
            }

            artworkNames = this.buildSpecialMap(artworkType, videoFiles);
            List<StageFile> specials = this.stagingDao.findStageFilesInSpecialFolder(FileType.IMAGE, artworkFolderName, library, artworkNames);
            artworks.addAll(specials);
        }

        LOG.debug("Found {} local {}s for season {}", artworks.size(), artworkType.toString().toLowerCase(), season.getIdentifier());
        return artworks;
    }

    @SuppressWarnings("unchecked")
    private List<StageFile> findVideoFiles(VideoData videoData) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct f from StageFile f ");
        sb.append("join f.mediaFile m ");
        sb.append("join m.videoDatas v ");
        sb.append("where v.id=:videoDataId ");
        sb.append("and m.extra=:extra ");
        sb.append("and f.status != :duplicate ");
        sb.append("and f.status != :deleted ");

        final Map<String,Object> params = new HashMap<String,Object>();
        params.put("videoDataId", videoData.getId());
        params.put("duplicate", StatusType.DUPLICATE);
        params.put("deleted", StatusType.DELETED);
        params.put("extra", Boolean.FALSE);

        return stagingDao.findByNamedParameters(sb, params);
    }

    @SuppressWarnings("unchecked")
    private List<StageFile> findVideoFiles(Season season) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct f from StageFile f ");
        sb.append("join f.mediaFile m ");
        sb.append("join m.videoDatas v ");
        sb.append("join v.season sea ");
        sb.append("where sea.id=:seasonId ");
        sb.append("and m.extra=:extra ");
        sb.append("and f.status != :duplicate ");
        sb.append("and f.status != :deleted ");
        
        final Map<String,Object> params = new HashMap<String,Object>();
        params.put("seasonId", season.getId());
        params.put("duplicate", StatusType.DUPLICATE);
        params.put("deleted", StatusType.DELETED);
        params.put("extra", Boolean.FALSE);
        
        return stagingDao.findByNamedParameters(sb, params);
    }

    @SuppressWarnings("unchecked")
    private List<StageFile> findArtworkStageFiles(Set<StageDirectory> directories, Set<String> artworkNames) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct f from StageFile f ");
        sb.append("where f.stageDirectory in (:directories) ");
        sb.append("and f.fileType = :fileType ");
        sb.append("and lower(f.baseName) in (:artworkNames) ");
        sb.append("and f.status != :deleted ");

        final Map<String,Object> params = new HashMap<String,Object>();
        params.put("directories", directories);
        params.put("fileType", FileType.IMAGE);
        params.put("artworkNames", artworkNames);
        params.put("deleted", StatusType.DELETED);

        return stagingDao.findByNamedParameters(sb, params);
    }

    public List<StageFile> getPhotos(Person person) {
        List<StageFile> artworks;
        
        String photoFolderName = PropertyTools.getProperty("yamj3.folder.name.photo");
        if (StringUtils.isNotBlank(photoFolderName)) {
            Set<String> artworkNames = new HashSet<String>();
            artworkNames.add(person.getName().toLowerCase() + ".photo");
            artworkNames.add(person.getName().toLowerCase() + "-photo");
            artworks = this.stagingDao.findStageFilesInSpecialFolder(FileType.IMAGE, photoFolderName, null, artworkNames);
        } else {
            artworks = Collections.emptyList();
        }

        LOG.debug("Found {} local photos for person '{}'", artworks.size(), person.getName());
        return artworks;
    }
}
