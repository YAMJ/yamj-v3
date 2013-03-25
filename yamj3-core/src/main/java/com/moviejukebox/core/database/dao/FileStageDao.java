package com.moviejukebox.core.database.dao;

import com.moviejukebox.core.database.model.FileStage;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

@Service("fileStageDao")
public class FileStageDao extends ExtendedHibernateDaoSupport {

    public FileStage getFileStage(final long id) {
        return this.getHibernateTemplate().get(FileStage.class, id);
    }

    public void deleteFileStage(final long id) {
        this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Void>() {
            @Override
            public Void doInHibernate(final Session session) throws HibernateException {
                final Query query = session.createQuery("delete from FileStage where id = :id");
                query.setLong("id", id);
                query.executeUpdate();
                session.flush();
                return null;
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public List<FileStage> getFileStages(final int maxResults) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<FileStage>>() {
            @Override
            public List<FileStage> doInHibernate(final Session session) throws HibernateException {
                final Query query = session.createQuery("from FileStage order by id");
                query.setMaxResults(maxResults);
                return query.list();
            }
        });
    }
}
