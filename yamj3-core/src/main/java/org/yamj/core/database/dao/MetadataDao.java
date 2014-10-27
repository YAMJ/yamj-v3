/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.dto.QueueDTOComparator;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.hibernate.HibernateDao;

@Repository("metadataDao")
public class MetadataDao extends HibernateDao {

    @SuppressWarnings("unchecked")
    public List<QueueDTO> getMetadataQueue(final CharSequence sql, final int maxResults) {
        SQLQuery query = getSession().createSQLQuery(sql.toString());
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

    public VideoData getVideoData(String identifier) {
        return getByField(VideoData.class, "identifier", identifier);
    }

    public Season getSeason(String identifier) {
        return getByField(Season.class, "identifier", identifier);
    }

    public Series getSeries(String identifier) {
        return getByField(Series.class, "identifier", identifier);
    }
    
    public Person getPerson(String name) {
        return getByNameCaseInsensitive(Person.class, name);
    }

    @Transactional
    public synchronized void storePerson(CreditDTO dto) {
        Person person = this.getPerson(dto.getName());
        if (person == null) {
            // create new person
            person = new Person();
            person.setName(dto.getName());
            person.setBirthName(dto.getRealName(), dto.getSource());
            person.setSourceDbIds(dto.getPersonIdMap());
            person.setStatus(StatusType.NEW);
            person.setFilmographyStatus(StatusType.NEW);
            this.saveEntity(person);
        } else {
            // update person if ID's has has been changed
            if (person.setSourceDbIds(dto.getPersonIdMap())) {
                person.setStatus(StatusType.UPDATED);
                this.updateEntity(person);
            }
        }
    }
    
    public CastCrew getCastCrew(VideoData videoData, JobType jobType, String personName) {
        StringBuffer sb = new StringBuffer();
        sb.append("select distinct c ");
        sb.append("from CastCrew c ");
        sb.append("join c.person p ");
        sb.append("where c.videoData=:videoData " );
        sb.append("and c.jobType=:jobType ");
        sb.append("and lower(p.name)=:personName ");
        
        Query query = getSession().createQuery(sb.toString());
        query.setParameter("videoData", videoData);
        query.setParameter("jobType", jobType);
        query.setString("personName", personName.toLowerCase());
        return (CastCrew)query.uniqueResult();
    }
}
