package com.yamj.core.database.dao;

import com.yamj.core.database.model.Artwork;
import com.yamj.core.database.model.dto.QueueDTO;
import com.yamj.core.database.model.dto.QueueDTOComparator;
import com.yamj.core.hibernate.ExtendedHibernateDaoSupport;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;

@Service("artworkDao")
public class ArtworkDao extends ExtendedHibernateDaoSupport {

    public Artwork getArtwork(Long id) {
        return this.getHibernateTemplate().get(Artwork.class, id);
    }

    public List<QueueDTO> getArtworkQueueForScanning() {
        final StringBuilder sql = new StringBuilder();
        sql.append("select id,artwork_type,create_timestamp,update_timestamp ");
        sql.append("from artwork ");
        sql.append("where status in ('NEW') ");
        
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
                    queueElement.setArtworkType(convertRowElementToString(object[1]));
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
