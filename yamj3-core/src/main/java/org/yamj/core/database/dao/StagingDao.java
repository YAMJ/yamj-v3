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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.hibernate.HibernateDao;

@Service("stagingDao")
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
    public List<VideoData> findVideoDatasForNFO(String baseName, StageDirectory stageDirectory) {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT distinct vd ");
        sb.append("FROM VideoData vd ");
        sb.append("JOIN vd.mediaFiles mf ");
        sb.append("JOIN mf.stageFiles sf ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND lower(sf.baseName)=:baseName ");
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        
        Query query = getSession().createQuery(sb.toString());
        query.setParameter("fileType", FileType.VIDEO);
        query.setParameter("baseName", baseName.toLowerCase());
        query.setParameter("stageDirectory", stageDirectory);
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        return (List<VideoData>)query.list();
    }

    public StageFile findStageFile(FileType fileType, String baseName, StageDirectory stageDirectory) {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT distinct sf ");
        sb.append("FROM StageFile sf ");
        sb.append("WHERE sf.fileType=:fileType ");
        sb.append("AND lower(sf.baseName)=:baseName ");
        sb.append("AND sf.stageDirectory=:stageDirectory ");
        
        Query query = getSession().createQuery(sb.toString());
        query.setParameter("fileType", fileType);
        query.setParameter("baseName", baseName.toLowerCase());
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
        query.setParameter("stageFileId", stageFileId);
        query.setParameter("videoDataId", videoDataId);
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
}
