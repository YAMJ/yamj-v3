package com.moviejukebox.core.database.dao;

import com.moviejukebox.core.database.model.MediaFile;
import com.moviejukebox.core.database.model.ScanPath;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

@Service("mediaDao")
public class MediaDao extends ExtendedHibernateDaoSupport {

    public ScanPath getScanPath(final String playerPath) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<ScanPath>() {
            @Override
            public ScanPath doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(ScanPath.class);
                criteria.add(Restrictions.naturalId().set("playerPath", playerPath));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (ScanPath)criteria.uniqueResult();
            }
        });
    }
    
    public MediaFile getMediaFile(final String filePath) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<MediaFile>() {
            @Override
            public MediaFile doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(MediaFile.class);
                criteria.add(Restrictions.naturalId().set("filePath", filePath));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (MediaFile)criteria.uniqueResult();
            }
        });
    }
}
