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
package org.yamj.core.database.dao;

import static org.hibernate.CacheMode.NORMAL;
import static org.yamj.core.CachingNames.DB_STAGEFILE;
import static org.yamj.core.database.Literals.*;

import java.util.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.core.service.file.FileTools;
import org.yamj.plugin.api.model.type.ArtworkType;

@Transactional
@Repository("stagingDao")
public class StagingDao extends HibernateDao {

    public Library getLibrary(String client, String playerPath) {
        return (Library) currentSession().createCriteria(Library.class)
                .add(Restrictions.eq("client", client))
                .add(Restrictions.eq("playerPath", playerPath))
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .uniqueResult();
    }

    public StageDirectory getStageDirectory(String directoryPath, Library library) {
        return currentSession().byNaturalId(StageDirectory.class)
                .using("directoryPath", directoryPath)
                .using("library", library)
                .load();
    }

    @Cacheable(value=DB_STAGEFILE, key="#id", unless="#result==null")
    public StageFile getStageFile(Long id) {
        return getById(StageFile.class, id);
    }

    public StageFile getStageFile(String baseName, String extension, StageDirectory stageDirectory) {
        return currentSession().byNaturalId(StageFile.class)
                .using(LITERAL_BASENAME, baseName)
                .using("extension", extension)
                .using(LITERAL_STAGE_DIRECTORY, stageDirectory)
                .load();
    }

    public Long getNextStageFileId(FileType fileType) {
        return (Long) currentSession().createCriteria(StageFile.class)
                .add(Restrictions.eq(LITERAL_FILE_TYPE, fileType))    
                .add(Restrictions.or(
                        Restrictions.eq(LITERAL_STATUS, StatusType.NEW),
                        Restrictions.eq(LITERAL_STATUS, StatusType.UPDATED)))
                .setProjection(Projections.min(LITERAL_ID))
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .uniqueResult();
    }

    public List<Long> getRootDirectories() {
        return currentSession().getNamedQuery(StageDirectory.QUERY_ROOT_DIRECTORIES).list();
    }
    
    public List<StageDirectory> getChildDirectories(StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return Collections.emptyList();
        }

        return currentSession().createCriteria(StageDirectory.class)
                .add(Restrictions.eq("parentDirectory", stageDirectory))
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }

    public List<VideoData> findVideoDatasForNFO(StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return Collections.emptyList();
        }
        
        return currentSession().getNamedQuery(VideoData.QUERY_FIND_VIDEOS_FOR_NFO_BY_DIRECTORY)
                .setBoolean(LITERAL_EXTRA, false)
                .setParameter(LITERAL_STAGE_DIRECTORY, stageDirectory)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }

    public List<VideoData> findVideoDatasForNFO(String baseName, StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return Collections.emptyList();
        }
        
        return currentSession().getNamedQuery(VideoData.QUERY_FIND_VIDEOS_FOR_NFO_BY_NAME_AND_DIRECTORY)
                .setBoolean(LITERAL_EXTRA, false)
                .setString(LITERAL_BASENAME, baseName.toLowerCase())
                .setParameter(LITERAL_STAGE_DIRECTORY, stageDirectory)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }

    public List<VideoData> findVideoDatasForNFO(String baseName, Library library) {
        if (library == null) {
            return Collections.emptyList();
        }

        return currentSession().getNamedQuery(VideoData.QUERY_FIND_VIDEOS_FOR_NFO_BY_NAME_AND_LIBRARY)
                .setBoolean(LITERAL_EXTRA, false)
                .setString(LITERAL_BASENAME, baseName.toLowerCase())
                .setParameter("library", library)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }

    public List<VideoData> findVideoDatasForNFO(Collection<StageDirectory> stageDirectories) {
        if (CollectionUtils.isEmpty(stageDirectories)) {
            return Collections.emptyList();
        }

        return currentSession().getNamedQuery(VideoData.QUERY_FIND_VIDEOS_FOR_NFO_BY_DIRECTORIES)
                .setBoolean(LITERAL_EXTRA, false)
                .setParameterList("stageDirectories", stageDirectories)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }

    public StageFile findNfoFile(String searchName, StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return null;
        }

        return (StageFile)currentSession().getNamedQuery(StageFile.QUERY_FIND_NFO)
                .setString("searchName", searchName.toLowerCase())
                .setParameter(LITERAL_STAGE_DIRECTORY, stageDirectory)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .uniqueResult();
    }

    public List<StageFile> getValidNFOFilesForVideo(long videoDataId) {
        return currentSession().getNamedQuery(StageFile.QUERY_VALID_NFOS_VIDEO)
                .setLong("videoDataId", videoDataId)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }

    public List<StageFile> getValidNFOFilesForSeries(long seriesId) {
        return currentSession().getNamedQuery(StageFile.QUERY_VALID_NFOS_SERIES)
                .setLong("seriesId", seriesId)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }

    public Set<Artwork> findMatchingArtworkForMovieOrSeason(ArtworkType artworkType, String baseName, StageDirectory stageDirectory) {
        // NOTE: union not supported in HQL, so each query has to be executed
        //       and mapped into a set to have uniqueness
        Set<Artwork> result = new HashSet<>();

        Map<String, Object> params = new HashMap<>();
        params.put(LITERAL_ARTWORK_TYPE, artworkType);
        params.put(LITERAL_EXTRA, Boolean.FALSE);
        if (baseName != null) {
            params.put(LITERAL_BASENAME, baseName);
        }
        params.put(LITERAL_STAGE_DIRECTORY, stageDirectory);

        // for movies
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT a FROM Artwork a ");
        sb.append("JOIN a.videoData vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf ");
        sb.append("WHERE a.artworkType=:artworkType AND sf.fileType='VIDEO' AND sf.status!='DELETED' ");
        sb.append("AND mf.extra=:extra AND vd.episode < 0 AND sf.stageDirectory=:stageDirectory ");
        if (baseName != null) {
            sb.append("AND lower(sf.baseName)=:baseName ");
        }
        result.addAll(this.findByNamedParameters(sb, params));

        // for season
        sb.setLength(0);
        sb.append("SELECT a FROM Artwork a ");
        sb.append("JOIN a.season sea JOIN sea.videoDatas vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf ");
        sb.append("WHERE a.artworkType=:artworkType AND sf.fileType='VIDEO' AND sf.status!='DELETED' ");
        sb.append("AND mf.extra=:extra AND sf.stageDirectory=:stageDirectory ");
        if (baseName != null) {
            sb.append("AND lower(sf.baseName)=:baseName ");
        }
        result.addAll(this.findByNamedParameters(sb, params));

        return result;
    }

    public List<Artwork> findMatchingArtworkForSeries(ArtworkType artworkType, StageDirectory stageDirectory, boolean videosOnlyInSubDirs) {
        Map<String, Object> params = new HashMap<>();
        params.put(LITERAL_ARTWORK_TYPE, artworkType);
        params.put(LITERAL_EXTRA, Boolean.FALSE);
        params.put(LITERAL_STAGE_DIRECTORY, stageDirectory);

        // for series
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT a FROM Artwork a ");
        sb.append("JOIN a.series ser JOIN ser.seasons sea JOIN sea.videoDatas vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf ");
        sb.append("WHERE a.artworkType=:artworkType AND sf.fileType='VIDEO' AND sf.status!='DELETED' ");
        sb.append("AND mf.extra=:extra AND vd.episode >= 0 ");
        if (videosOnlyInSubDirs) {
            sb.append("AND sf.stageDirectory.parentDirectory=:stageDirectory ");
        } else {
            sb.append("AND (sf.stageDirectory=:stageDirectory OR sf.stageDirectory.parentDirectory=:stageDirectory) ");
        }
        return this.findByNamedParameters(sb, params);
    }

    public Set<Artwork> findMatchingArtworkForMovieOrSeason(ArtworkType artworkType, String baseName, Library library) {
        // NOTE: union not supported in HQL, so each query has to be executed
        //       and mapped into a set to have uniqueness
        Set<Artwork> result = new HashSet<>();

        Map<String, Object> params = new HashMap<>();
        params.put(LITERAL_ARTWORK_TYPE, artworkType);
        params.put(LITERAL_EXTRA, Boolean.FALSE);
        params.put(LITERAL_BASENAME, baseName);
        if (library != null) {
            params.put("library", library);
        }

        // for movies
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT a FROM Artwork a ");
        sb.append("JOIN a.videoData vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf ");
        if (library != null) {
            sb.append("JOIN sf.stageDirectory sd ");
        }
        sb.append("WHERE a.artworkType=:artworkType AND sf.fileType='VIDEO' AND sf.status!='DELETED' ");
        sb.append("AND mf.extra=:extra AND vd.episode < 0 AND lower(sf.baseName)=:baseName ");
        if (library != null) {
            sb.append("AND sd.library=:library ");
        }
        result.addAll(this.findByNamedParameters(sb, params));

        // for season
        sb.setLength(0);
        sb.append("SELECT a FROM Artwork a ");
        sb.append("JOIN a.season sea JOIN sea.videoDatas vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf ");
        if (library != null) {
            sb.append("JOIN sf.stageDirectory sd ");
        }
        sb.append("WHERE a.artworkType=:artworkType AND sf.fileType='VIDEO' AND sf.status!='DELETED' ");
        sb.append("AND mf.extra=:extra AND lower(sf.baseName)=:baseName ");
        if (library != null) {
            sb.append("AND sd.library=:library ");
        }
        result.addAll(this.findByNamedParameters(sb, params));

        return result;
    }

    public List<Artwork> findMatchingVideoImages(String baseName, StageDirectory stageDirectory) {
        return currentSession().getNamedQuery(Artwork.QUERY_FIND_MATCHING_VIDEOIMAGES_BY_NAME_AND_DIRECTORY)
                .setString(LITERAL_BASENAME, baseName)
                .setParameter(LITERAL_STAGE_DIRECTORY, stageDirectory)
                .setBoolean(LITERAL_EXTRA, false)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }

    public List<StageFile> findStageFiles(FileType fileType, String searchName, String searchExtension, StageDirectory stageDirectory) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf FROM StageFile sf ");
        sb.append("WHERE sf.fileType=:fileType AND lower(sf.baseName)=:searchName ");
        if (searchExtension != null) {
            sb.append("AND lower(sf.extension)=:searchExtension ");
        }
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        sb.append("AND sf.status not in ('DUPLICATE','DELETED') ");

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter(LITERAL_FILE_TYPE, fileType);
        query.setString("searchName", searchName.toLowerCase());
        if (searchExtension != null) {
            query.setString("searchExtension", searchExtension.toLowerCase());
        }
        query.setParameter(LITERAL_STAGE_DIRECTORY, stageDirectory);
        query.setCacheable(true);
        query.setCacheMode(NORMAL);
        return query.list();
    }

    public List<StageFile> findStageFiles(FileType fileType, String searchName, String searchExtension, Library library) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf FROM StageFile sf ");
        if (library != null) {
            sb.append("JOIN sf.stageDirectory sd ");
        }
        sb.append("WHERE sf.fileType=:fileType AND lower(sf.baseName)=:searchName ");
        if (searchExtension != null) {
            sb.append("AND lower(sf.extension)=:searchExtension ");
        }
        if (library != null) {
            sb.append("AND sd.library=:library ");
        }
        sb.append("AND sf.status not in ('DUPLICATE','DELETED') ");

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter(LITERAL_FILE_TYPE, fileType);
        query.setString("searchName", searchName.toLowerCase());
        if (searchExtension != null) {
            query.setString("searchExtension", searchExtension.toLowerCase());
        }
        if (library != null) {
            query.setParameter("library", library);
        }
        query.setCacheable(true);
        query.setCacheMode(NORMAL);
        return query.list();
    }

    public List<StageFile> findStageFilesInSpecialFolder(FileType fileType, String folderName, Library library, Collection<String> searchNames) {
        if (StringUtils.isBlank(folderName) || CollectionUtils.isEmpty(searchNames)) {
            return Collections.emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        params.put(LITERAL_FILE_TYPE, fileType);
        if (library != null) {
            params.put("library", library);
        }
        params.put("folderName", folderName.toLowerCase());
        params.put("searchNames", searchNames);

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf FROM StageFile sf JOIN sf.stageDirectory sd ");
        sb.append("WHERE sf.fileType=:fileType ");
        if (library != null) {
            sb.append("AND sd.library=:library ");
        }
        sb.append("AND sf.status not in ('DUPLICATE','DELETED') ");
        sb.append("AND lower(sf.baseName) in (:searchNames) ");

        String dirFragment = StringEscapeUtils.escapeSql(FileTools.getPathFragment(folderName).toLowerCase());
        sb.append("AND (lower(sd.directoryName)=:folderName or lower(sd.directoryPath) like '%").append(dirFragment).append("%') ");

        return this.findByNamedParameters(sb, params);
    }

    public Date maxWatchedFileDate(StageFile videoFile, String folderName, boolean checkLibrary) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT max(sf.file_date) FROM stage_file sf ");
        sb.append("JOIN stage_directory sd ON sf.directory_id=sd.id AND ");
        if (StringUtils.isBlank(folderName)) {
            sb.append(" sd.id=:dirId ");
        } else {
            String dirFragment = StringEscapeUtils.escapeSql(FileTools.getPathFragment(folderName).toLowerCase());
            sb.append(" (sd.id=:dirId or lower(sd.directory_name)=:dirName or lower(sd.directory_path) like '%").append(dirFragment).append("%') ");
        }
        if (checkLibrary) {
            sb.append(" and sd.library_id=:libraryId ");
        }
        sb.append("WHERE sf.file_type='WATCHED' AND sf.status!='DELETED' ");
        sb.append("AND (lower(sf.base_name)=:check1 or lower(sf.base_name)=:check2) ");

        Query query = currentSession().createSQLQuery(sb.toString());
        query.setLong("dirId", videoFile.getStageDirectory().getId());
        if (checkLibrary) {
            query.setLong("libraryId", videoFile.getStageDirectory().getLibrary().getId());
        }
        query.setString("check1", StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase()));
        query.setString("check2", StringEscapeUtils.escapeSql(videoFile.getFileName().toLowerCase()));
        if (StringUtils.isNotBlank(folderName)) {
            query.setString("dirName", StringEscapeUtils.escapeSql(folderName.toLowerCase()));
        }

        return (Date) query.uniqueResult();
    }

    public List<StageFile> findVideoStageFiles(Artwork artwork) {
        final long id;
        final String namedQuery;
        if (artwork.getSeries() != null) {
            id = artwork.getSeries().getId();
            namedQuery = StageFile.QUERY_VIDEOFILES_FOR_SERIES;
        } else if (artwork.getSeason() != null) {
            id = artwork.getSeason().getId();
            namedQuery = StageFile.QUERY_VIDEOFILES_FOR_SEASON;
        } else {
            id = artwork.getVideoData().getId();
            namedQuery = StageFile.QUERY_VIDEOFILES_FOR_VIDEODATA;
        }
        
        return currentSession().getNamedQuery(namedQuery)
                .setLong(LITERAL_ID, id)
                .setBoolean(LITERAL_EXTRA, false)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }
}
