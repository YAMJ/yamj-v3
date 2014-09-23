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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.FileType;

@Service("artworkLocatorService")
public class ArtworkLocatorService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkLocatorService.class);

    @Autowired
    private StagingDao stagingDao;

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingPosters(VideoData videoData) {
        List<StageFile> videoFiles = findVideoFiles(videoData);
        if (CollectionUtils.isEmpty(videoFiles)) {
            return null;
        }

        // build search maps
        Set<StageDirectory> directories = new HashSet<StageDirectory>();
        Set<String> artworkNames = this.buildPosterSearchMap(videoFiles, directories);
        
        // get the posters
        List<StageFile> posters = findArtworkStageFiles(directories, artworkNames);
        LOG.debug("Found {} local posters for movie {}", posters.size(), videoData.getIdentifier());
        return posters;
    }

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingPosters(Season season) {
        List<StageFile> videoFiles = findVideoFiles(season);
        if (CollectionUtils.isEmpty(videoFiles)) {
            return null;
        }

        // build search maps
        Set<StageDirectory> directories = new HashSet<StageDirectory>();
        Set<String> artworkNames = this.buildPosterSearchMap(videoFiles, directories);
        
        // get the posters
        List<StageFile> posters = findArtworkStageFiles(directories, artworkNames);
        LOG.debug("Found {} local posters for season {}", posters.size(), season.getIdentifier());
        return posters;
    }

    private Set<String> buildPosterSearchMap(List<StageFile> videoFiles, Set<StageDirectory> directories) {
        Set<String> artworkNames = new HashSet<String>();
        artworkNames.add("poster");
        artworkNames.add("cover");
        artworkNames.add("folder");
        for (StageFile videoFile : videoFiles) {
            directories.add(videoFile.getStageDirectory());
            artworkNames.add(videoFile.getBaseName().toLowerCase());
            artworkNames.add(videoFile.getBaseName().toLowerCase() + ".poster");
            artworkNames.add(videoFile.getBaseName().toLowerCase() + "-poster");
            
            // same name as directory
            String directoryName = videoFile.getStageDirectory().getDirectoryName().toLowerCase();
            artworkNames.add(directoryName);
            artworkNames.add(directoryName + ".poster");
            artworkNames.add(directoryName + "-poster");
        }
        return artworkNames;
    }

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingFanarts(VideoData videoData) {
        List<StageFile> videoFiles = findVideoFiles(videoData);
        if (CollectionUtils.isEmpty(videoFiles)) {
            return null;
        }

        // build search maps
        Set<StageDirectory> directories = new HashSet<StageDirectory>();
        Set<String> artworkNames = this.buildFanartSearchMap(videoFiles, directories);

        // get the fanarts
        List<StageFile> fanarts = findArtworkStageFiles(directories, artworkNames);
        LOG.debug("Found {} local fanarts for movie {}", fanarts.size(), videoData.getIdentifier());
        return fanarts;
    }

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingFanarts(Season season) {
        List<StageFile> videoFiles = findVideoFiles(season);
        if (CollectionUtils.isEmpty(videoFiles)) {
            return null;
        }

        // build search maps
        Set<StageDirectory> directories = new HashSet<StageDirectory>();
        Set<String> artworkNames = this.buildFanartSearchMap(videoFiles, directories);
        
        // get the fanarts
        List<StageFile> fanarts = findArtworkStageFiles(directories, artworkNames);
        LOG.debug("Found {} local fanarts for season {}", fanarts.size(), season.getIdentifier());
        return fanarts;
    }

    private Set<String> buildFanartSearchMap(List<StageFile> videoFiles, Set<StageDirectory> directories) {
        Set<String> artworkNames = new HashSet<String>();
        artworkNames.add("fanart");
        artworkNames.add("backdrop");
        artworkNames.add("background");
        for (StageFile videoFile : videoFiles) {
            directories.add(videoFile.getStageDirectory());
            artworkNames.add(videoFile.getBaseName().toLowerCase() + ".fanart");
            artworkNames.add(videoFile.getBaseName().toLowerCase() + "-fanart");
            
            // same name as directory
            String directoryName = videoFile.getStageDirectory().getDirectoryName().toLowerCase();
            artworkNames.add(directoryName + ".fanart");
            artworkNames.add(directoryName + "-fanart");
        }
        return artworkNames;
    }
    
    @SuppressWarnings("unchecked")
    private List<StageFile> findVideoFiles(VideoData videoData) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct f from StageFile f ");
        sb.append("join f.mediaFile m ");
        sb.append("join m.videoDatas v ");
        sb.append("where v.id=:videoDataId ");
        sb.append("and f.status != :duplicate " );

        final Map<String,Object> params = new HashMap<String,Object>();
        params.put("videoDataId", videoData.getId());
        params.put("duplicate", StatusType.DUPLICATE);

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
        sb.append("and f.status != :duplicate " );

        final Map<String,Object> params = new HashMap<String,Object>();
        params.put("seasonId", season.getId());
        params.put("duplicate", StatusType.DUPLICATE);

        return stagingDao.findByNamedParameters(sb, params);
    }

    @SuppressWarnings("unchecked")
    private List<StageFile> findArtworkStageFiles(Set<StageDirectory> directories, Set<String> artworkNames) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct f from StageFile f ");
        sb.append("where f.stageDirectory in (:directories) ");
        sb.append("and f.fileType = :fileType ");
        sb.append("and lower(f.baseName) in (:artworkNames) ");

        final Map<String,Object> params = new HashMap<String,Object>();
        params.put("directories", directories);
        params.put("fileType", FileType.IMAGE);
        params.put("artworkNames", artworkNames);

        return stagingDao.findByNamedParameters(sb, params);
    }

    public List<StageFile> getPhotos(Person person) {
        // TODO: Scan for staged local files
        return Collections.emptyList();
    }
}
