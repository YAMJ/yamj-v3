/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.SQLQuery;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.dto.QueueDTOComparator;
import org.yamj.core.hibernate.HibernateDao;

@Service("metadataDao")
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

    public VideoData getVideoData(Long id) {
        return getById(VideoData.class, id);
    }

    public VideoData getVideoData(String identifier) {
        return (VideoData)getSession().byNaturalId(VideoData.class).using("identifier", identifier).load();
    }

    public Season getSeason(Long id) {
        return getById(Season.class, id);
    }

    public Season getSeason(String identifier) {
        return (Season)getSession().byNaturalId(Season.class).using("identifier", identifier).load();
    }

    public Series getSeries(Long id) {
        return getById(Series.class, id);
    }

    public Series getSeries(String identifier) {
        return (Series)getSession().byNaturalId(Series.class).using("identifier", identifier).load();
    }

    public Person getPerson(String name) {
        return (Person)getSession().byNaturalId(Person.class).using("name", name).load();
    }

    public Person getPerson(final long id) {
        return getById(Person.class, id);
    }
}
