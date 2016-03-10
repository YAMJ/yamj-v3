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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.service.file.FileTools;

@Service("artworkLocatorService")
public class ArtworkLocatorService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkLocatorService.class);

    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;

    @Value("${yamj3.folder.name.artwork:null}")
    private String artworkFolderName;
    @Value("${yamj3.folder.name.photo:null}")
    private String photoFolderName;
    
    @Transactional(readOnly = true)
    public List<StageFile> getMatchingArtwork(ArtworkType artworkType, VideoData videoData) {
        List<StageFile> videoFiles = findVideoFiles(videoData);
        if (videoFiles.isEmpty()) {
            return Collections.emptyList();
        }

        // get the tokens to use
        final List<String> tokens = this.configServiceWrapper.getArtworkTokens(artworkType);

        // build search map for artwork
        Set<StageDirectory> directories = new HashSet<>();
        Set<String> artworkNames = buildSearchMap(artworkType, videoFiles, directories, tokens);
        // add special movie artwork names
        artworkNames.add("movie");
        addNameWithTokens(artworkNames, "movie", tokens);
        // search in same directory than video files
        List<StageFile> artworks = findArtworkStageFiles(directories, artworkNames);

        // find artwork in possible given artwork folder
        artworks.addAll(searchInArtworkFolder(artworkType, videoFiles, tokens));

        LOG.debug("Found {} local {}s for movie {}", artworks.size(), artworkType.toString().toLowerCase(), videoData.getIdentifier());
        return artworks;
    }

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingArtwork(ArtworkType artworkType, Season season) {
        List<StageFile> videoFiles = findVideoFiles(season);
        if (videoFiles.isEmpty()) {
            return Collections.emptyList();
        }

        // get the tokens to use
        final List<String> tokens = this.configServiceWrapper.getArtworkTokens(artworkType);
        final String seasonNr = "season"+StringUtils.leftPad(Integer.toString(season.getSeason()), 2, '0');

        // build search map for artwork
        Set<StageDirectory> directories = new HashSet<>();
        Set<String> artworkNames = buildSearchMap(artworkType, videoFiles, directories, tokens);
        // add special season artwork names
        artworkNames.add("season");
        addNameWithTokens(artworkNames, "season", tokens);
        addNameWithTokens(artworkNames, seasonNr, tokens);
        // search in same directory than video files
        List<StageFile> artworks = findArtworkStageFiles(directories, artworkNames);

        // search in parent directories for season specific artwork names
        artworkNames.clear();
        addNameWithTokens(artworkNames, seasonNr, tokens);
        artworks.addAll(findArtworkStageFiles(FileTools.getParentDirectories(directories), artworkNames));

        // find artwork in possible given artwork folder
        artworks.addAll(searchInArtworkFolder(artworkType, videoFiles, tokens));
        
        LOG.debug("Found {} local {}s for season {}", artworks.size(), artworkType.toString().toLowerCase(), season.getIdentifier());
        return artworks;
    }

    @Transactional(readOnly = true)
    public List<StageFile> getMatchingArtwork(ArtworkType artworkType, Series series) {
        List<StageDirectory> directories  = findVideoDirectories(series);
        if (directories.isEmpty()) {
            return Collections.emptyList();
        }

        // get the tokens to use
        List<String> tokens = this.configServiceWrapper.getArtworkTokens(artworkType);

        // build map for artwork names
        Set<String> artworkNames = new HashSet<>();
        artworkNames.add("show");
        artworkNames.add("series");
        artworkNames.add("season-all");

        // search series specific names in same directory than video files
        List<StageFile> artworks = findArtworkStageFiles(directories, artworkNames);

        // extend artwork names for parent folder specific series names
        Set<StageDirectory> parentDirectories = FileTools.getParentDirectories(directories);
        artworkNames.addAll(tokens);
        for (StageDirectory parent : parentDirectories) {
            final String directoryName = StringEscapeUtils.escapeSql(parent.getDirectoryName().toLowerCase());
            switch (artworkType) {
            case POSTER:
                artworkNames.add(directoryName);
                //$FALL-THROUGH$
            default:
                addNameWithTokens(artworkNames, directoryName, tokens);
                break;
            }
        }
        // search series specific names in parent directory of video files
        artworks.addAll(findArtworkStageFiles(parentDirectories, artworkNames));
        
        LOG.debug("Found {} local {}s for series {}", artworks.size(), artworkType.toString().toLowerCase(), series.getIdentifier());
        return artworks;
    }
    
    private List<StageFile> searchInArtworkFolder(ArtworkType artworkType, List<StageFile> videoFiles, List<String> tokens) {
        if (StringUtils.isNotBlank(artworkFolderName)) {
            Library library = null;
            if (this.configServiceWrapper.getBooleanProperty("yamj3.librarycheck.folder.artwork", Boolean.TRUE)) {
                library = videoFiles.get(0).getStageDirectory().getLibrary();
            }

            Set<String> artworkNames = buildSpecialMap(artworkType, videoFiles, tokens);
            return this.stagingDao.findStageFilesInSpecialFolder(FileType.IMAGE, artworkFolderName, library, artworkNames);
        }
        return Collections.emptyList(); 
    }
    
    private static Set<String> buildSearchMap(ArtworkType artworkType, List<StageFile> videoFiles, Set<StageDirectory> directories, List<String> tokens) {
        final Set<String> artworkNames = new HashSet<>();
        
        // add all tokens
        artworkNames.addAll(tokens);
        
        for (StageFile videoFile : videoFiles) {
            directories.add(videoFile.getStageDirectory());
            final String directoryName = StringEscapeUtils.escapeSql(videoFile.getStageDirectory().getDirectoryName().toLowerCase());
            final String fileName = StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase());
            
            switch (artworkType) {
            case POSTER:
                artworkNames.add(fileName);
                artworkNames.add(directoryName);
                //$FALL-THROUGH$
            default:
                addNameWithTokens(artworkNames, fileName, tokens);
                addNameWithTokens(artworkNames, directoryName, tokens);
                break;
            }
        }
        return artworkNames;
    }

    private static Set<String> buildSpecialMap(ArtworkType artworkType, List<StageFile> videoFiles, List<String> tokens) {
        final Set<String> artworkNames = new HashSet<>();
        
        for (StageFile videoFile : videoFiles) {
            final String fileName = StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase());
            
            switch (artworkType) {
            case POSTER:
                artworkNames.add(fileName);
                //$FALL-THROUGH$
            default:
                addNameWithTokens(artworkNames, fileName, tokens);
            }
        }
        return artworkNames;
    }

    private static void addNameWithTokens(Set<String> artworkNames, String name, List<String> tokens) {
        for (String token : tokens) {
            artworkNames.add(name.concat(".").concat(token));
            artworkNames.add(name.concat("-").concat(token));
        }
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
        
        return stagingDao.findByNamedParameters(StageFile.class, sb, params);
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

        return stagingDao.findByNamedParameters(StageFile.class, sb, params);
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
        
        return stagingDao.findByNamedParameters(StageFile.class, sb, params);
    }

    private List<StageDirectory> findVideoDirectories(Series series) {
        final StringBuilder sb = new StringBuilder();
        sb.append("select distinct d from StageDirectory d ");
        sb.append("join d.stageFiles f ");
        sb.append("join f.mediaFile m ");
        sb.append("join m.videoDatas v ");
        sb.append("join v.season sea ");
        sb.append("join sea.series ser ");
        sb.append("where ser.id=:seriesId ");
        sb.append("and m.extra=:extra ");
        sb.append("and f.status != :duplicate ");
        sb.append("and f.status != :deleted ");
        
        final Map<String,Object> params = new HashMap<>();
        params.put("seriesId", series.getId());
        params.put("duplicate", StatusType.DUPLICATE);
        params.put("deleted", StatusType.DELETED);
        params.put("extra", Boolean.FALSE);
        
        return stagingDao.findByNamedParameters(StageDirectory.class, sb, params);
    }

    private List<StageFile> findArtworkStageFiles(Collection<StageDirectory> directories, Set<String> artworkNames) {
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

        return stagingDao.findByNamedParameters(StageFile.class, sb, params);
    }

    public List<StageFile> getPhotos(Person person) {
        Set<String> artworkNames = new HashSet<>();
        artworkNames.add(StringEscapeUtils.escapeSql(person.getName().toLowerCase()));
        artworkNames.add(StringEscapeUtils.escapeSql(person.getName().toLowerCase() + ".photo"));
        artworkNames.add(StringEscapeUtils.escapeSql(person.getName().toLowerCase() + "-photo"));
        artworkNames.add(person.getIdentifier().toLowerCase());
        artworkNames.add(person.getIdentifier().toLowerCase() + ".photo");
        artworkNames.add(person.getIdentifier().toLowerCase() + "-photo");
        
        List<StageFile> artworks;
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
