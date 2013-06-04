package org.yamj.core.database.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.dto.QueueDTOComparator;
import org.yamj.core.hibernate.ExtendedHibernateDaoSupport;

@Service("artworkDao")
public class ArtworkDao extends ExtendedHibernateDaoSupport {

    public Artwork getArtwork(Long id) {
        return this.getHibernateTemplate().get(Artwork.class, id);
    }

    @SuppressWarnings("unchecked")
    public Artwork getRequiredArtwork(Long id) {
        final StringBuffer sb = new StringBuffer();
        sb.append("from Artwork art ");
        sb.append("left outer join fetch art.videoData ");
        sb.append("left outer join fetch art.season ");
        sb.append("left outer join fetch art.series ");
        sb.append("where art.id = ?");
        
        List<Artwork> artworks = getHibernateTemplate().find(sb.toString(), id);
        return DataAccessUtils.requiredUniqueResult(artworks);
    }

    public List<QueueDTO> getArtworkQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select art.id,art.artwork_type,art.create_timestamp,art.update_timestamp ");
        sql.append("from artwork art, videodata vd, series ser, season sea ");
        sql.append("where art.status = 'NEW' ");
        sql.append("and (art.videodata_id is null or (art.videodata_id=vd.id and vd.status='DONE')) ");
        sql.append("and (art.season_id is null or (art.season_id=sea.id and sea.status='DONE')) ");
        sql.append("and (art.series_id is null or (art.series_id=ser.id and ser.status='DONE')) ");
        
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
