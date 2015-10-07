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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.yamj.common.type.StatusType;
import org.yamj.core.CachingNames;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.Library;
import org.yamj.core.database.model.StageDirectory;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.core.service.file.FileTools;

@Transactional
@Repository("stagingDao")
public class StagingDao extends HibernateDao {

    public Library getLibrary(String client, String playerPath) {
        Criteria criteria = currentSession().createCriteria(Library.class);
        criteria.add(Restrictions.eq("client", client));
        criteria.add(Restrictions.eq("playerPath", playerPath));
        criteria.setCacheable(true);
        criteria.setCacheMode(CacheMode.NORMAL);
        return (Library)criteria.uniqueResult();
    }

    public StageDirectory getStageDirectory(String directoryPath, Library library) {
        return currentSession().byNaturalId(StageDirectory.class)
                .using("directoryPath", directoryPath)
                .using("library", library)
                .load();
    }

    @Cacheable(value=CachingNames.DB_STAGEFILE, key="#id", unless="#result==null")
    public StageFile getStageFile(long id) {
        return getById(StageFile.class, id);
    }

    public StageFile getStageFile(String baseName, String extension, StageDirectory stageDirectory) {
        return currentSession().byNaturalId(StageFile.class)
                .using("baseName", baseName)
                .using("extension", extension)
                .using("stageDirectory", stageDirectory)
                .load();
    }

    public Long getNextStageFileId(FileType fileType, StatusType... statusTypes) {
        Criteria criteria = currentSession().createCriteria(StageFile.class);
        criteria.add(Restrictions.eq("fileType", fileType));
        criteria.add(Restrictions.in("status", statusTypes));
        criteria.setProjection(Projections.min("id"));
        criteria.setCacheable(true);
        criteria.setCacheMode(CacheMode.NORMAL);
        return (Long) criteria.uniqueResult();
    }

    public List<StageDirectory> getRootDirectories() {
        Criteria criteria = currentSession().createCriteria(StageDirectory.class);
        criteria.add(Restrictions.isNull("parentDirectory"));
        criteria.setCacheable(true);
        criteria.setCacheMode(CacheMode.NORMAL);
        return criteria.list();
    }

    public List<StageDirectory> getChildDirectories(StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return Collections.emptyList();
        }

        Criteria criteria = currentSession().createCriteria(StageDirectory.class);
        criteria.add(Restrictions.eq("parentDirectory", stageDirectory));
        criteria.setCacheable(true);
        criteria.setCacheMode(CacheMode.NORMAL);
        return criteria.list();
    }

    public List<VideoData> findVideoDatas(StageDirectory stageDirectory) {
        return this.findVideoDatas(null, stageDirectory);
    }

    public List<VideoData> findVideoDatas(String baseName, StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct vd ");
        sb.append("FROM VideoData vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND mf.extra=:extra ");
        if (baseName != null) {
            sb.append("AND lower(sf.baseName)=:baseName ");
        }
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        sb.append("AND sf.status != :deleted ");

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("fileType", FileType.VIDEO);
        query.setBoolean("extra", Boolean.FALSE);
        if (baseName != null) {
            query.setString("baseName", baseName.toLowerCase());
        }
        query.setParameter("stageDirectory", stageDirectory);
        query.setParameter("deleted", StatusType.DELETED);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    public List<VideoData> findVideoDatas(String baseName, Library library) {
        if (library == null) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct vd ");
        sb.append("FROM VideoData vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        sb.append("JOIN sf.stageDirectory sd ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND mf.extra=:extra ");
        sb.append("AND lower(sf.baseName)=:baseName ");
        sb.append("AND sd.library=:library ");
        sb.append("AND sf.status != :deleted ");

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("fileType", FileType.VIDEO);
        query.setBoolean("extra", Boolean.FALSE);
        query.setString("baseName", baseName.toLowerCase());
        query.setParameter("library", library);
        query.setParameter("deleted", StatusType.DELETED);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    public List<VideoData> findVideoDatas(Collection<StageDirectory> stageDirectories) {
        if (CollectionUtils.isEmpty(stageDirectories)) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct vd ");
        sb.append("FROM VideoData vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND mf.extra=:extra ");
        sb.append("AND sf.stageDirectory in (:stageDirectories) ");
        sb.append("AND sf.status != :deleted ");

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("fileType", FileType.VIDEO);
        query.setBoolean("extra", Boolean.FALSE);
        query.setParameterList("stageDirectories", stageDirectories);
        query.setParameter("deleted", StatusType.DELETED);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    public StageFile findNfoFile(String searchName, StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND lower(sf.baseName)=:searchName ");
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        sb.append("AND sf.status != :deleted ");

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("fileType", FileType.NFO);
        query.setString("searchName", searchName.toLowerCase());
        query.setParameter("stageDirectory", stageDirectory);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        query.setParameter("deleted", StatusType.DELETED);
        return (StageFile) query.uniqueResult();
    }

    public List<StageFile> getValidNFOFilesForVideo(long videoDataId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("JOIN FETCH sf.nfoRelations nfrel ");
        sb.append("JOIN nfrel.nfoRelationPK.videoData vd ");
        sb.append("WHERE vd.id=:videoDataId ");
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status in (:statusSet) ");
        sb.append("ORDER BY nfrel.priority DESC");

        Set<StatusType> statusSet = new HashSet<>();
        statusSet.add(StatusType.NEW);
        statusSet.add(StatusType.UPDATED);
        statusSet.add(StatusType.DONE);

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("videoDataId", videoDataId);
        query.setParameter("fileType", FileType.NFO);
        query.setParameterList("statusSet", statusSet);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    public List<StageFile> getValidNFOFilesForSeries(long seriesId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("JOIN FETCH sf.nfoRelations nfrel ");
        sb.append("JOIN nfrel.nfoRelationPK.videoData vd ");
        sb.append("JOIN vd.season sea ");
        sb.append("JOIN sea.series ser ");
        sb.append("WHERE ser.id=:seriesId ");
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status in (:statusSet) ");
        sb.append("ORDER BY nfrel.priority DESC");

        Set<StatusType> statusSet = new HashSet<>();
        statusSet.add(StatusType.NEW);
        statusSet.add(StatusType.UPDATED);
        statusSet.add(StatusType.DONE);

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("seriesId", seriesId);
        query.setParameter("fileType", FileType.NFO);
        query.setParameterList("statusSet", statusSet);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    public Set<Artwork> findMatchingArtworksForVideo(ArtworkType artworkType, StageDirectory stageDirectory) {
        return this.findMatchingArtworksForVideo(artworkType, null, stageDirectory);
    }

    public Set<Artwork> findMatchingArtworksForVideo(ArtworkType artworkType, String baseName, StageDirectory stageDirectory) {
        // NOTE: union not supported in HQL, so each query has to be executed
        //       and mapped into a set to have uniqueness
        Set<Artwork> result = new HashSet<>();

        Set<StatusType> statusSet = new HashSet<>();
        statusSet.add(StatusType.NEW);
        statusSet.add(StatusType.UPDATED);
        statusSet.add(StatusType.DONE);

        Map<String, Object> params = new HashMap<>();
        params.put("artworkType", artworkType);
        params.put("fileType", FileType.VIDEO);
        params.put("extra", Boolean.FALSE);
        if (baseName != null) {
            params.put("baseName", baseName);
        }
        params.put("statusSet", statusSet);
        params.put("stageDirectory", stageDirectory);

        // for movies
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT a ");
        sb.append("FROM Artwork a ");
        sb.append("JOIN a.videoData vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        sb.append("WHERE a.artworkType=:artworkType ");
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status in (:statusSet) ");
        sb.append("AND mf.extra=:extra ");
        sb.append("AND vd.episode < 0 ");
        if (baseName != null) {
            sb.append("AND lower(sf.baseName)=:baseName ");
        }
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        List<Artwork> artworks = this.findByNamedParameters(sb, params);
        result.addAll(artworks);

        // for season
        sb.setLength(0);
        sb.append("SELECT a ");
        sb.append("FROM Artwork a ");
        sb.append("JOIN a.season sea ");
        sb.append("JOIN sea.videoDatas vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        sb.append("WHERE a.artworkType=:artworkType ");
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status in (:statusSet) ");
        sb.append("AND mf.extra=:extra ");
        sb.append("AND vd.episode >= 0 ");
        if (baseName != null) {
            sb.append("AND lower(sf.baseName)=:baseName ");
        }
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        artworks = this.findByNamedParameters(sb, params);
        result.addAll(artworks);

        return result;
    }

    public Set<Artwork> findMatchingArtworksForVideo(ArtworkType artworkType, String baseName, Library library) {
        // NOTE: union not supported in HQL, so each query has to be executed
        //       and mapped into a set to have uniqueness
        Set<Artwork> result = new HashSet<>();

        Set<StatusType> statusSet = new HashSet<>();
        statusSet.add(StatusType.NEW);
        statusSet.add(StatusType.UPDATED);
        statusSet.add(StatusType.DONE);

        Map<String, Object> params = new HashMap<>();
        params.put("artworkType", artworkType);
        params.put("fileType", FileType.VIDEO);
        params.put("extra", Boolean.FALSE);
        params.put("baseName", baseName);
        params.put("statusSet", statusSet);
        if (library != null) {
            params.put("library", library);
        }

        // for movies
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT a ");
        sb.append("FROM Artwork a ");
        sb.append("JOIN a.videoData vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        if (library != null) {
            sb.append("JOIN sf.stageDirectory sd ");
        }
        sb.append("WHERE a.artworkType=:artworkType ");
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status in (:statusSet) ");
        sb.append("AND mf.extra=:extra ");
        sb.append("AND vd.episode < 0 ");
        sb.append("AND lower(sf.baseName)=:baseName ");
        if (library != null) {
            sb.append("AND sd.library=:library ");
        }
        List<Artwork> artworks = this.findByNamedParameters(sb, params);
        result.addAll(artworks);

        sb.setLength(0);
        sb.append("SELECT a ");
        sb.append("FROM Artwork a ");
        sb.append("JOIN a.season sea ");
        sb.append("JOIN sea.videoDatas vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        if (library != null) {
            sb.append("JOIN sf.stageDirectory sd ");
        }
        sb.append("WHERE a.artworkType=:artworkType ");
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status in (:statusSet) ");
        sb.append("AND mf.extra=:extra ");
        sb.append("AND vd.episode >= 0 ");
        sb.append("AND lower(sf.baseName)=:baseName ");
        if (library != null) {
            sb.append("AND sd.library=:library ");
        }
        artworks = this.findByNamedParameters(sb, params);
        result.addAll(artworks);

        return result;
    }

    public List<StageFile> findStageFiles(FileType fileType, String searchName, String searchExtension, StageDirectory stageDirectory) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND lower(sf.baseName)=:searchName ");
        if (searchExtension != null) {
            sb.append("AND lower(sf.extension)=:searchExtension ");
        }
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        sb.append("AND sf.status != :duplicate ");
        sb.append("AND sf.status != :deleted ");

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("fileType", fileType);
        query.setString("searchName", searchName.toLowerCase());
        if (searchExtension != null) {
            query.setString("searchExtension", searchExtension.toLowerCase());
        }
        query.setParameter("stageDirectory", stageDirectory);
        query.setParameter("duplicate", StatusType.DUPLICATE);
        query.setParameter("deleted", StatusType.DELETED);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    public List<StageFile> findStageFiles(FileType fileType, String searchName, String searchExtension, Library library) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        if (library != null) {
            sb.append("JOIN sf.stageDirectory sd ");
        }
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND lower(sf.baseName)=:searchName ");
        if (searchExtension != null) {
            sb.append("AND lower(sf.extension)=:searchExtension ");
        }
        if (library != null) {
            sb.append("AND sd.library=:library ");
        }
        sb.append("AND sf.status != :duplicate ");
        sb.append("AND sf.status != :deleted ");

        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("fileType", fileType);
        query.setString("searchName", searchName.toLowerCase());
        if (searchExtension != null) {
            query.setString("searchExtension", searchExtension.toLowerCase());
        }
        if (library != null) {
            query.setParameter("library", library);
        }
        query.setParameter("duplicate", StatusType.DUPLICATE);
        query.setParameter("deleted", StatusType.DELETED);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    public List<StageFile> findStageFilesInSpecialFolder(FileType fileType, String folderName, Library library, Collection<String> searchNames) {
        if (StringUtils.isBlank(folderName) || CollectionUtils.isEmpty(searchNames)) {
            return Collections.emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("fileType", fileType);
        if (library != null) {
            params.put("library", library);
        }
        params.put("folderName", folderName.toLowerCase());
        params.put("searchNames", searchNames);
        params.put("duplicate", StatusType.DUPLICATE);
        params.put("deleted", StatusType.DELETED);

        StringBuffer sb = new StringBuffer();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("JOIN sf.stageDirectory sd ");
        sb.append("WHERE sf.fileType=:fileType ");
        if (library != null) {
            sb.append("AND sd.library=:library ");
        }
        sb.append("AND sf.status != :duplicate ");
        sb.append("AND sf.status != :deleted ");
        sb.append("AND lower(sf.baseName) in (:searchNames) ");

        String dirFragment = StringEscapeUtils.escapeSql(FileTools.getPathFragment(folderName).toLowerCase());
        sb.append("AND (lower(sd.directoryName)=:folderName or lower(sd.directoryPath) like '%").append(dirFragment).append("%') ");

        return this.findByNamedParameters(sb, params);
    }

    public Date maxWatchedFileDate(StageFile videoFile, String folderName, boolean checkLibrary) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT max(sf.file_date) ");
        sb.append("FROM stage_file sf ");
        sb.append("JOIN stage_directory sd ON sf.directory_id=sd.id and ");
        if (StringUtils.isBlank(folderName)) {
            sb.append(" sd.id=:dirId ");
        } else {
            String dirFragment = StringEscapeUtils.escapeSql(FileTools.getPathFragment(folderName).toLowerCase());
            sb.append(" (sd.id=:dirId or lower(sd.directory_name)=:dirName or lower(sd.directory_path) like '%").append(dirFragment).append("%') ");
        }
        if (checkLibrary) {
            sb.append(" and sd.library_id=:libraryId ");
        }
        sb.append("WHERE sf.file_type=:watched ");
        sb.append("and (lower(sf.base_name)=:check1 or lower(sf.base_name)=:check2) ");
        sb.append("and sf.status != :deleted ");

        Query query = currentSession().createSQLQuery(sb.toString());
        query.setLong("dirId", videoFile.getStageDirectory().getId());
        if (checkLibrary) {
            query.setLong("libraryId", videoFile.getStageDirectory().getLibrary().getId());
        }
        query.setString("watched", FileType.WATCHED.toString());
        query.setString("check1", StringEscapeUtils.escapeSql(videoFile.getBaseName().toLowerCase()));
        query.setString("check2", StringEscapeUtils.escapeSql(videoFile.getFileName().toLowerCase()));
        query.setString("deleted", StatusType.DELETED.toString());
        if (StringUtils.isNotBlank(folderName)) {
            query.setString("dirName", StringEscapeUtils.escapeSql(folderName.toLowerCase()));
        }

        return (Date) query.uniqueResult();
    }

    public List<StageFile> findVideoStageFiles(Artwork artwork) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct sf ");
        
        long id;
        if (artwork.getSeries() != null) {
            id = artwork.getSeries().getId();
            sb.append("FROM Series ser ");
            sb.append("JOIN ser.seasons sea ");
            sb.append("JOIN sea.videoDatas vd ");
            sb.append("JOIN vd.mediaFiles mf ");
            sb.append("JOIN mf.stageFiles sf ");
            sb.append("WHERE sea.id=:id ");
        } else if (artwork.getSeason() != null) {
            id = artwork.getSeason().getId();
            sb.append("FROM Season sea ");
            sb.append("JOIN sea.videoDatas vd ");
            sb.append("JOIN vd.mediaFiles mf ");
            sb.append("JOIN mf.stageFiles sf ");
            sb.append("WHERE sea.id=:id ");
        } else {
            id = artwork.getVideoData().getId();
            sb.append("FROM VideoData vd ");
            sb.append("JOIN vd.mediaFiles mf ");
            sb.append("JOIN mf.stageFiles sf ");
            sb.append("WHERE vd.id=:id ");
        }
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status != :deleted ");
        
        Query query = currentSession().createQuery(sb.toString());
        query.setLong("id", id);
        query.setParameter("fileType", FileType.VIDEO);
        query.setBoolean("extra", Boolean.FALSE);
        query.setParameter("deleted", StatusType.DELETED);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }
}
