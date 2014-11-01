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
package org.yamj.core.database.dao;

import java.util.*;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.hibernate.HibernateDao;

@Repository("stagingDao")
public class StagingDao extends HibernateDao {

    public Library getLibrary(String client, String playerPath) {
        return (Library)getSession().byNaturalId(Library.class)
                .using("client", client)
                .using("playerPath", playerPath)
                .load();
    }

    public StageDirectory getStageDirectory(String directoryPath, Library library) {
        return (StageDirectory)getSession().byNaturalId(StageDirectory.class)
                .using("directoryPath", directoryPath)
                .using("library", library)
                .load();
    }

    public StageFile getStageFile(long id) {
        return getById(StageFile.class, id);
    }

    public StageFile getStageFile(String baseName, String extension, StageDirectory stageDirectory) {
        return (StageFile)getSession().byNaturalId(StageFile.class)
                .using("baseName", baseName)
                .using("extension", extension)
                .using("stageDirectory", stageDirectory)
                .load();
    }

    public Long getNextStageFileId(FileType fileType, StatusType... statusTypes) {
        Criteria criteria = getSession().createCriteria(StageFile.class);
        criteria.add(Restrictions.eq("fileType", fileType));
        criteria.add(Restrictions.in("status", statusTypes));
        criteria.setProjection(Projections.min("id"));
        criteria.setCacheable(true);
        criteria.setCacheMode(CacheMode.NORMAL);
        return (Long) criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<StageDirectory> getRootDirectories() {
        Criteria criteria = getSession().createCriteria(StageDirectory.class);
        criteria.add(Restrictions.isNull("parentDirectory"));
        criteria.setCacheable(true);
        criteria.setCacheMode(CacheMode.NORMAL);
        return (List<StageDirectory>)criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<StageDirectory> getChildDirectories(StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return Collections.emptyList();
        }

        Criteria criteria = getSession().createCriteria(StageDirectory.class);
        criteria.add(Restrictions.eq("parentDirectory", stageDirectory));
        criteria.setCacheable(true);
        criteria.setCacheMode(CacheMode.NORMAL);
        return (List<StageDirectory>)criteria.list();
    }

    public List<VideoData> findVideoDatas(StageDirectory stageDirectory) {
        return this.findVideoDatas(null, stageDirectory);
    }

    @SuppressWarnings("unchecked")
    public List<VideoData> findVideoDatas(String baseName, StageDirectory stageDirectory) {
        if (stageDirectory == null) {
            return Collections.emptyList();
        }

        StringBuffer sb = new StringBuffer();
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
        
        Query query = getSession().createQuery(sb.toString());
        query.setParameter("fileType", FileType.VIDEO);
        query.setBoolean("extra", Boolean.FALSE);
        if (baseName != null) {
            query.setString("baseName", baseName.toLowerCase());
        }
        query.setParameter("stageDirectory", stageDirectory);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return (List<VideoData>)query.list();
    }

    @SuppressWarnings("unchecked")
    public List<VideoData> findVideoDatas(Collection<StageDirectory> stageDirectories) {
        if (CollectionUtils.isEmpty(stageDirectories)) {
            return Collections.emptyList();
        }
        
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT distinct vd ");
        sb.append("FROM VideoData vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND mf.extra=:extra ");
        sb.append("AND sf.stageDirectory in (:stageDirectories) ");
        
        Query query = getSession().createQuery(sb.toString());
        query.setParameter("fileType", FileType.VIDEO);
        query.setBoolean("extra", Boolean.FALSE);
        query.setParameterList("stageDirectories", stageDirectories);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return (List<VideoData>)query.list();
    }

    public StageFile findNfoFile(String searchName, StageDirectory stageDirectory) {
        if (stageDirectory == null) return null;
        
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND lower(sf.baseName)=:searchName ");
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        
        Query query = getSession().createQuery(sb.toString());
        query.setParameter("fileType", FileType.NFO);
        query.setString("searchName", searchName.toLowerCase());
        query.setParameter("stageDirectory", stageDirectory);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return (StageFile)query.uniqueResult();
    }
    
    public NfoRelation getNfoRelation(long stageFileId, long videoDataId) {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT * ");
        sb.append("FROM nfo_relation nfrel ");
        sb.append("WHERE nfrel.stagefile_id=:stageFileId ");
        sb.append("AND nfrel.videodata_id=:videoDataId ");
        
        SQLQuery query = getSession().createSQLQuery(sb.toString());
        query.setLong("stageFileId", stageFileId);
        query.setLong("videoDataId", videoDataId);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        query.addEntity(NfoRelation.class);
        return (NfoRelation)query.uniqueResult();
    }
    
    @SuppressWarnings("unchecked")
    public List<StageFile> getValidNFOFilesForVideo(long videoDataId) {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("JOIN sf.nfoRelations nfrel ");
        sb.append("JOIN nfrel.videoData vd ");
        sb.append("WHERE vd.id=:videoDataId ");
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status in (:statusSet) ");
        sb.append("ORDER by nfrel.priority DESC");
        
        Set<StatusType> statusSet = new HashSet<StatusType>();
        statusSet.add(StatusType.NEW);
        statusSet.add(StatusType.UPDATED);
        statusSet.add(StatusType.DONE);
        
        Query query = getSession().createQuery(sb.toString());
        query.setParameter("videoDataId", videoDataId);
        query.setParameter("fileType", FileType.NFO);
        query.setParameterList("statusSet", statusSet);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<StageFile> getValidNFOFilesForSeries(long seriesId) {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("JOIN sf.nfoRelations nfrel ");
        sb.append("JOIN nfrel.videoData vd ");
        sb.append("JOIN vd.season sea ");
        sb.append("JOIN sea.series ser ");
        sb.append("WHERE ser.id=:seriesId ");
        sb.append("AND sf.fileType=:fileType ");
        sb.append("AND sf.status in (:statusSet) ");
        sb.append("ORDER by nfrel.priority DESC");
        
        Set<StatusType> statusSet = new HashSet<StatusType>();
        statusSet.add(StatusType.NEW);
        statusSet.add(StatusType.UPDATED);
        statusSet.add(StatusType.DONE);
        
        Query query = getSession().createQuery(sb.toString());
        query.setParameter("seriesId", seriesId);
        query.setParameter("fileType", FileType.NFO);
        query.setParameterList("statusSet", statusSet);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }

    public List<Artwork> findMatchingArtworksForVideo(ArtworkType artworkType, StageDirectory stageDirectory)  {
        return this.findMatchingArtworksForVideo(artworkType, null, stageDirectory);
    }

    @SuppressWarnings("unchecked")
    public List<Artwork> findMatchingArtworksForVideo(ArtworkType artworkType, String baseName, StageDirectory stageDirectory)  {
        StringBuffer sb = new StringBuffer();
        // for movies
        sb.append("SELECT distinct a ");
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
        sb.append("UNION ");
        // for season
        sb.append("SELECT distinct a ");
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

        Set<StatusType> statusSet = new HashSet<StatusType>();
        statusSet.add(StatusType.NEW);
        statusSet.add(StatusType.UPDATED);
        statusSet.add(StatusType.DONE);

        Query query = getSession().createQuery(sb.toString());
        query.setParameter("artworkType", artworkType);
        query.setParameter("fileType", FileType.VIDEO);
        query.setBoolean("extra", Boolean.FALSE);
        if (baseName != null) {
            query.setString("baseName", baseName);
        }
        query.setParameterList("statusSet", statusSet);
        query.setParameter("stageDirectory", stageDirectory);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return query.list();
    }
}
