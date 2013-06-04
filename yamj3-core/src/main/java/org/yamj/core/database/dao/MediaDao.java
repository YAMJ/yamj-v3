package org.yamj.core.database.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.MediaFile;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.dto.QueueDTOComparator;
import org.yamj.core.database.model.type.MetaDataType;
import org.yamj.core.hibernate.ExtendedHibernateDaoSupport;

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

    public List<QueueDTO> getMediaQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select vd.id,'");
        sql.append(MetaDataType.VIDEODATA);
        sql.append("' as mediatype,vd.create_timestamp,vd.update_timestamp ");
        sql.append("from videodata vd ");
        sql.append("where vd.status in ('NEW','UPDATED') ");
        sql.append("and vd.episode<0 ");
        sql.append("union ");
        sql.append("select ser.id,'");
        sql.append(MetaDataType.SERIES);
        sql.append("' as mediatype,ser.create_timestamp,ser.update_timestamp ");
        sql.append("from series ser, season sea, videodata vd ");
        sql.append("where ser.id=sea.series_id ");
        sql.append("and sea.id=vd.season_id ");
        sql.append("and (ser.status in ('NEW','UPDATED') ");
        sql.append(" or  sea.status in ('NEW','UPDATED') ");
        sql.append(" or  vd.status in  ('NEW','UPDATED')) ");
        
        return this.getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<QueueDTO>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<QueueDTO> doInHibernate(Session session) throws HibernateException, SQLException {
                SQLQuery query = session.createSQLQuery(sql.toString());
                query.setReadOnly(true);
                query.setCacheable(true);
                if (maxResults > 0) {
                    query.setMaxResults(maxResults);
                }
                
                List<QueueDTO> queueElements = new ArrayList<QueueDTO>();
                List<Object[]> objects = query.list();
                for (Object[] object : objects) {
                    QueueDTO queueElement = new QueueDTO();
                    queueElement.setId(convertRowElementToLong(object[0]));
                    queueElement.setMetadataType(convertRowElementToString(object[1]));
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
