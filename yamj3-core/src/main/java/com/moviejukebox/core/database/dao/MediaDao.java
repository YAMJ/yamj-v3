package com.moviejukebox.core.database.dao;

import org.hibernate.Query;

import com.moviejukebox.core.database.model.MediaFile;
import com.moviejukebox.core.database.model.Season;
import com.moviejukebox.core.database.model.Series;
import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

@Service("mediaDao")
public class MediaDao extends ExtendedHibernateDaoSupport {

    public MediaFile getMediaFile(final String fileName) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<MediaFile>() {
            @Override
            public MediaFile doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(MediaFile.class);
                criteria.add(Restrictions.naturalId().set("fileName", fileName));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (MediaFile)criteria.uniqueResult();
            }
        });
    }

    public Long getNextVideoDataId(final StatusType... statusTypes) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Long>() {
            @Override
            public Long doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(VideoData.class);
                criteria.add(Restrictions.in("status", statusTypes));
                criteria.setProjection(Projections.min("id"));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                Long id = (Long)criteria.uniqueResult();
                
                if (id != null) {
                    Query query = session.createQuery("update VideoData set status=:status where id=:id");
                    query.setParameter("status", StatusType.PROCESS);
                    query.setLong("id", id);
                    query.executeUpdate();
                }
                
                return id;
            }
        });
    }

    public VideoData getVideoData(Long id) {
        return this.getHibernateTemplate().get(VideoData.class, id);
    }

    public VideoData getVideoData(final String identifier) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<VideoData>() {
            @Override
            public VideoData doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(VideoData.class);
                criteria.add(Restrictions.naturalId().set("identifier", identifier));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (VideoData)criteria.uniqueResult();
            }
        });
    }

    public Season getSeason(final String identifier) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Season>() {
            @Override
            public Season doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Season.class);
                criteria.add(Restrictions.naturalId().set("identifier", identifier));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Season)criteria.uniqueResult();
            }
        });
    }

    public Series getSeries(final String identifier) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Series>() {
            @Override
            public Series doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Series.class);
                criteria.add(Restrictions.naturalId().set("identifier", identifier));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Series)criteria.uniqueResult();
            }
        });
    }
}
