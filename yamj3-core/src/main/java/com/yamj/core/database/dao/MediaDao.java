package com.yamj.core.database.dao;

import com.yamj.core.database.model.MediaFile;
import com.yamj.core.database.model.Season;
import com.yamj.core.database.model.Series;
import com.yamj.core.database.model.VideoData;
import com.yamj.core.database.model.dto.QueueDTO;
import com.yamj.core.database.model.dto.QueueDTOComparator;
import com.yamj.common.type.StatusType;
import com.yamj.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

@Service("mediaDao")
public class MediaDao extends ExtendedHibernateDaoSupport {

    public MediaFile getMediaFile(Long id) {
        return this.getHibernateTemplate().get(MediaFile.class, id);
    }

    public MediaFile getMediaFile(final String fileName) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<MediaFile>() {
            @Override
            public MediaFile doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(MediaFile.class);
                criteria.add(Restrictions.naturalId().set("fileName", fileName));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (MediaFile) criteria.uniqueResult();
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
                return (VideoData) criteria.uniqueResult();
            }
        });
    }

    public Season getSeason(Long id) {
        return this.getHibernateTemplate().get(Season.class, id);
    }

    public Season getSeason(final String identifier) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Season>() {
            @Override
            public Season doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Season.class);
                criteria.add(Restrictions.naturalId().set("identifier", identifier));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Season) criteria.uniqueResult();
            }
        });
    }

    public Series getSeries(Long id) {
        return this.getHibernateTemplate().get(Series.class, id);
    }

    public Series getSeries(final String identifier) {
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Series>() {
            @Override
            public Series doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(Series.class);
                criteria.add(Restrictions.naturalId().set("identifier", identifier));
                criteria.setCacheable(true);
                criteria.setCacheMode(CacheMode.NORMAL);
                return (Series) criteria.uniqueResult();
            }
        });
    }

    public List<Long> getVideoDataIds(final StatusType... statusTypes) {
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

    public List<QueueDTO> getMediaQueueForScanning() {
        final StringBuilder sql = new StringBuilder();
        sql.append("select vd.id,'videodata' as mediatype,vd.create_timestamp,vd.update_timestamp ");
        sql.append("from videodata vd ");
        sql.append("where vd.status in ('NEW','UPDATED') ");
        sql.append("and vd.episode<0 ");
        sql.append("union ");
        sql.append("select se.id,'series' as mediatype,se.create_timestamp,se.update_timestamp ");
        sql.append("from series se ");
        sql.append("where se.status in ('NEW','UPDATED') ");

        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<QueueDTO>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<QueueDTO> doInHibernate(Session session) throws HibernateException, SQLException {
                SQLQuery query = session.createSQLQuery(sql.toString());
                query.setReadOnly(true);
                query.setCacheable(true);

                List<QueueDTO> queueElements = new ArrayList<QueueDTO>();
                List<Object[]> objects = query.list();
                for (Object[] object : objects) {
                    QueueDTO queueElement = new QueueDTO();
                    queueElement.setId(convertRowElementToLong(object[0]));
                    queueElement.setType(convertRowElementToString(object[1]));
                    queueElement.setDate(convertRowElementToDate(object[3]));
                    if (queueElement.getDate() == null) {
                        queueElement.setDate(convertRowElementToDate(object[2]));
                    }
                    queueElements.add(queueElement);
                }

                Collections.sort(queueElements, new QueueDTOComparator());
                return queueElements;
            }
        });
    }
}
