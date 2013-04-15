package com.moviejukebox.core.database.dao;

import com.moviejukebox.core.database.model.MediaFile;
import com.moviejukebox.core.database.model.Season;
import com.moviejukebox.core.database.model.Series;
import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.*;
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

    public List<Long> getWaitingVideoDataIds(final StatusType... statusTypes) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<Long>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(VideoData.class);
                criteria.add(Restrictions.in("status", statusTypes));
                criteria.setProjection(Projections.id());
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return criteria.list();
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
