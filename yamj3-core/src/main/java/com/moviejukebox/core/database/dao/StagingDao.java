package com.moviejukebox.core.database.dao;

import com.moviejukebox.core.database.model.Library;
import com.moviejukebox.core.database.model.StageDirectory;
import com.moviejukebox.core.database.model.StageFile;
import com.moviejukebox.core.database.model.type.FileType;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import org.hibernate.*;
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
                return (Library)criteria.uniqueResult();
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
                return (StageDirectory)criteria.uniqueResult();
            }
        });
    }

    public StageFile getStageFile(final String fileName, final StageDirectory stageDirectory) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<StageFile>() {
            @Override
            public StageFile doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(StageFile.class);
                criteria.add(Restrictions.naturalId().set("fileName", fileName).set("stageDirectory", stageDirectory));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (StageFile)criteria.uniqueResult();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<StageFile> getStageFiles(final int maxResults, FileType fileType, StatusType... statusTypes) {
        
        StringBuilder query = new StringBuilder();
        query.append("from StageFile f join fetch f.stageDirectory d ");
        query.append("where f.fileType = :fileType ");
        query.append("and f.status in (:statusTypes) ");

        HashMap<String,Object> params = new HashMap<String,Object>();
        params.put("fileType", fileType);
        params.put("statusTypes", statusTypes);
        
        return getExtendedHibernateTemplate().findByNamedParam(query, params, maxResults);
    }

    public StageFile getNextStageFile(final FileType fileType, final StatusType... statusTypes) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<StageFile>() {
            @Override
            public StageFile doInHibernate(Session session) throws HibernateException, SQLException {
                StringBuilder sb = new StringBuilder();
                sb.append("from StageFile f join fetch f.stageDirectory d ");
                sb.append("where f.fileType = :fileType ");
                sb.append("and f.status in (:statusTypes) ");

                ScrollableResults results = session.createQuery(sb.toString())
                        .setParameter("fileType", fileType)
                        .setParameterList("statusTypes", statusTypes)
                        .setMaxResults(1)
                        .scroll(ScrollMode.FORWARD_ONLY);
                
                // get just the first result
                if (results.next()) {
                    return (StageFile)results.get()[0];
                }
                
                return null;
            }
        });
    }
}
