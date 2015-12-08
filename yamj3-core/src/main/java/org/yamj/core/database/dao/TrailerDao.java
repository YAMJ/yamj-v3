/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.SQLQuery;
import org.springframework.stereotype.Repository;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Trailer;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.hibernate.HibernateDao;

@Repository("trailerDao")
public class TrailerDao extends HibernateDao {

    public List<QueueDTO> getTrailerQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select vd.id,'");
        sql.append(MetaDataType.MOVIE);
        sql.append("' as metatype,vd.create_timestamp,vd.update_timestamp ");
        sql.append("from videodata vd ");
        sql.append("where vd.trailer_status in ('NEW','UPDATED') ");
        sql.append("and vd.status='DONE' ");
        sql.append("and vd.episode<0 ");
        sql.append("union ");
        sql.append("select ser.id,'");
        sql.append(MetaDataType.SERIES);
        sql.append("' as mediatype,ser.create_timestamp,ser.update_timestamp ");
        sql.append("from series ser ");
        sql.append("where ser.trailer_status in ('NEW','UPDATED') ");
        sql.append("and ser.status='DONE' ");
        
        SQLQuery query = currentSession().createSQLQuery(sql.toString());
        query.setReadOnly(true);
        query.setCacheable(true);
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }

        List<QueueDTO> queueElements = new ArrayList<>();
        
        List<Object[]> objects = query.list();
        for (Object[] object : objects) {
            QueueDTO queueElement = new QueueDTO(convertRowElementToLong(object[0]));
            queueElement.setMetadataType(convertRowElementToString(object[1]));
            queueElement.setDate(convertRowElementToDate(object[3]));
            if (queueElement.getDate() == null) {
                queueElement.setDate(convertRowElementToDate(object[2]));
            }
            queueElements.add(queueElement);
        }

        Collections.sort(queueElements);
        return queueElements;
    }

    public List<QueueDTO> getTrailerQueueForProcessing(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.id, t.create_timestamp, t.update_timestamp ");
        sql.append("FROM trailer t ");
        sql.append("WHERE t.status in ('NEW','UPDATED')");
        
        SQLQuery query = currentSession().createSQLQuery(sql.toString());
        query.setReadOnly(true);
        query.setCacheable(true);
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        List<QueueDTO> queueElements = new ArrayList<>();
        List<Object[]> objects = query.list();
        for (Object[] object : objects) {
            QueueDTO queueElement = new QueueDTO(convertRowElementToLong(object[0]));
            queueElement.setDate(convertRowElementToDate(object[2]));
            if (queueElement.getDate() == null) {
                queueElement.setDate(convertRowElementToDate(object[1]));
            }
            queueElements.add(queueElement);
        }

        Collections.sort(queueElements);
        return queueElements;
    }

    public void resetDeletionStatus(Trailer trailer) {
        if (StatusType.DELETED.equals(trailer.getStatus())) {
            trailer.setStatus(trailer.getPreviousStatus());
            this.updateEntity(trailer);
        }
    }
}
