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
package org.yamj.core.database.service;

import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigService;
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

    private static Set<String> buildSearchMap(ArtworkType artworkType, List<StageFile> videoFiles, Set<StageDirectory> directories) {
        Set<String> artworkNames = new HashSet<>();

        // generic names (placed in folder)
        switch (artworkType) {
        case POSTER:
            artworkNames.add("poster");
            artworkNames.add("cover");
            artworkNames.add("folder");
            break;
        case FANART:
            artworkNames.add("fanart");
            artworkNames.add("backdrop");
            artworkNames.add("background");
            break;
        case BANNER:
            artworkNames.add("banner");
            break;
        default:
            // no artwork names for this type
            return artworkNames;
        }
        
        for (StageFile videoFile : videoFiles) {
            directories.add(videoFile.getStageDirectory());
            final String directoryName = StringEscapeUtils.escapeSql(videoFile.getStageDirectory().getDirectoryName().toLowerCase());
            
            switch (artworkType) {
            case POSTER:
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName()).toLowerCase());
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + ".poster"));
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + "-poster"));
                artworkNames.add(directoryName);
                artworkNames.add(directoryName + ".poster");
                artworkNames.add(directoryName + "-poster");
                break;
            case FANART:
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + ".fanart"));
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + "-fanart"));
                artworkNames.add(directoryName + ".fanart");
                artworkNames.add(directoryName + "-fanart");
                break;
            case BANNER:
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + ".banner"));
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + "-banner"));
                artworkNames.add(directoryName + ".banner");
                artworkNames.add(directoryName + "-banner");
                break;
            default:
                break;
            }
        }
        return artworkNames;
    }

    private static Set<String> buildSpecialMap(ArtworkType artworkType, List<StageFile> videoFiles) {
        Set<String> artworkNames = new HashSet<>();
        for (StageFile videoFile : videoFiles) {
            switch (artworkType) {
            case POSTER:
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase()));
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + ".poster"));
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + "-poster"));
                break;
            case FANART:
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + ".fanart"));
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + "-fanart"));
                break;
            case BANNER:
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + ".banner"));
                artworkNames.add(StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase() + "-banner"));
                break;
            default:
                break;
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
        Set<StageDirectory> directories = new HashSet<>();
        Set<String> artworkNames = buildSearchMap(artworkType, videoFiles, directories);
        List<StageFile> artworks = findArtworkStageFiles(directories, artworkNames);

        String artworkFolderName = PropertyTools.getProperty("yamj3.folder.name.artwork");
        if (StringUtils.isNotBlank(artworkFolderName)) {
            
            Library library = null;
            if (this.configService.getBooleanProperty("yamj3.librarycheck.folder.artwork", Boolean.TRUE)) {
                library = videoFiles.get(0).getStageDirectory().getLibrary();
            }
            
            artworkNames = buildSpecialMap(artworkType, videoFiles);
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
        Set<StageDirectory> directories = new HashSet<>();
        Set<String> artworkNames = buildSearchMap(artworkType, videoFiles, directories);
        List<StageFile> artworks = findArtworkStageFiles(directories, artworkNames);

        String artworkFolderName = PropertyTools.getProperty("yamj3.folder.name.artwork");
        if (StringUtils.isNotBlank(artworkFolderName)) {
            
            Library library = null;
            if (this.configService.getBooleanProperty("yamj3.librarycheck.folder.artwork", Boolean.TRUE)) {
                library = videoFiles.get(0).getStageDirectory().getLibrary();
            }

            artworkNames = buildSpecialMap(artworkType, videoFiles);
            List<StageFile> specials = this.stagingDao.findStageFilesInSpecialFolder(FileType.IMAGE, artworkFolderName, library, artworkNames);
            artworks.addAll(specials);
        }

        LOG.debug("Found {} local {}s for season {}", artworks.size(), artworkType.toString().toLowerCase(), season.getIdentifier());
        return artworks;
    }

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingArtwork(ArtworkType artworkType, BoxedSet boxedSet) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct f from StageFile f ");
        sb.append("where lower(f.baseName) like 'set_");
        sb.append(StringEscapeUtils.escapeSql(boxedSet.getName().toLowerCase()));
        sb.append("_%' ");
        if (ArtworkType.FANART == artworkType) {
            sb.append(" and lower(f.baseName) like '%fanart' ");
        } else if (ArtworkType.BANNER == artworkType) {
            sb.append(" and lower(f.baseName) like '%banner' ");
        } else {
            sb.append(" and lower(f.baseName) not like '%fanart' ");
            sb.append(" and lower(f.baseName) not like '%banner' ");
        }
        sb.append("and f.status != :deleted ");
        sb.append("and f.fileType = :fileType ");

        final Map<String,Object> params = new HashMap<>();
        params.put("deleted", StatusType.DELETED);
        params.put("fileType", FileType.IMAGE);
        
        return stagingDao.findByNamedParameters(sb, params);
    }
    
    private List<StageFile> findVideoFiles(VideoData videoData) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct f from StageFile f ");
        sb.append("join f.mediaFile m ");
        sb.append("join m.videoDatas v ");
        sb.append("where v.id=:videoDataId ");
        sb.append("and m.extra=:extra ");
        sb.append("and f.status != :duplicate ");
        sb.append("and f.status != :deleted ");

        final Map<String,Object> params = new HashMap<>();
        params.put("videoDataId", videoData.getId());
        params.put("duplicate", StatusType.DUPLICATE);
        params.put("deleted", StatusType.DELETED);
        params.put("extra", Boolean.FALSE);

        return stagingDao.findByNamedParameters(sb, params);
    }

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
        
        final Map<String,Object> params = new HashMap<>();
        params.put("seasonId", season.getId());
        params.put("duplicate", StatusType.DUPLICATE);
        params.put("deleted", StatusType.DELETED);
        params.put("extra", Boolean.FALSE);
        
        return stagingDao.findByNamedParameters(sb, params);
    }

    private List<StageFile> findArtworkStageFiles(Set<StageDirectory> directories, Set<String> artworkNames) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct f from StageFile f ");
        sb.append("where f.stageDirectory in (:directories) ");
        sb.append("and f.fileType = :fileType ");
        sb.append("and lower(f.baseName) in (:artworkNames) ");
        sb.append("and f.status != :deleted ");

        final Map<String,Object> params = new HashMap<>();
        params.put("directories", directories);
        params.put("fileType", FileType.IMAGE);
        params.put("artworkNames", artworkNames);
        params.put("deleted", StatusType.DELETED);

        return stagingDao.findByNamedParameters(sb, params);
    }

    public List<StageFile> getPhotos(Person person) {
        List<StageFile> artworks;

        Set<String> artworkNames = new HashSet<>();
        artworkNames.add(StringEscapeUtils.escapeSql(person.getName().toLowerCase()));
        artworkNames.add(StringEscapeUtils.escapeSql(person.getName().toLowerCase() + ".photo"));
        artworkNames.add(StringEscapeUtils.escapeSql(person.getName().toLowerCase() + "-photo"));
        artworkNames.add(person.getIdentifier().toLowerCase());
        artworkNames.add(person.getIdentifier().toLowerCase() + ".photo");
        artworkNames.add(person.getIdentifier().toLowerCase() + "-photo");

        
        String photoFolderName = PropertyTools.getProperty("yamj3.folder.name.photo");
        if (StringUtils.isNotBlank(photoFolderName)) {
            artworks = this.stagingDao.findStageFilesInSpecialFolder(FileType.IMAGE, photoFolderName, null, artworkNames);
        } else {
            // TODO search in complete library (needs refactoring of this method)
            artworks = Collections.emptyList();
        }

        LOG.debug("Found {} local photos for person '{}'", artworks.size(), person.getName());
        return artworks;
    }
}
