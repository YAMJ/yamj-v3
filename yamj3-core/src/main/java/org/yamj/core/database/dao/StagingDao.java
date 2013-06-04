/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.dao;

import org.yamj.core.database.model.Library;
import org.yamj.core.database.model.StageDirectory;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.database.model.type.FileType;
import org.yamj.common.type.StatusType;
import org.yamj.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

@Service("stagingDao")
public class StagingDao extends ExtendedHibernateDaoSupport {

    public Library getLibrary(final String client, final String playerPath) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Library>() {
            @Override
            public Library doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Library.class);
                criteria.add(Restrictions.naturalId().set("client", client).set("playerPath", playerPath));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Library) criteria.uniqueResult();
            }
        });
    }

    public StageDirectory getStageDirectory(final String directoryPath, final Library library) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<StageDirectory>() {
            @Override
            public StageDirectory doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(StageDirectory.class);
                criteria.add(Restrictions.naturalId().set("directoryPath", directoryPath).set("library", library));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (StageDirectory) criteria.uniqueResult();
            }
        });
    }

    public StageFile getStageFile(long id) {
        return this.getHibernateTemplate().get(StageFile.class, id);
    }

    public StageFile getStageFile(final String fileName, final StageDirectory stageDirectory) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<StageFile>() {
            @Override
            public StageFile doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(StageFile.class);
                criteria.add(Restrictions.naturalId().set("fileName", fileName).set("stageDirectory", stageDirectory));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (StageFile) criteria.uniqueResult();
            }
        });
    }

    public Long getNextStageFileId(final FileType fileType, final StatusType... statusTypes) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Long>() {
            @Override
            public Long doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(StageFile.class);
                criteria.add(Restrictions.eq("fileType", fileType));
                criteria.add(Restrictions.in("status", statusTypes));
                criteria.setProjection(Projections.min("id"));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Long) criteria.uniqueResult();
            }
        });
    }
}
