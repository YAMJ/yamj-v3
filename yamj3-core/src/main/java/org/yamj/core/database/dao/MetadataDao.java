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

import java.util.*;
import org.hibernate.SQLQuery;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.dto.QueueDTOComparator;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;

@Transactional
@Repository("metadataDao")
public class MetadataDao extends HibernateDao {

    private static final String IDENTIFIER = "identifier";

    public List<QueueDTO> getMetadataQueue(final CharSequence sql, final int maxResults) {
        SQLQuery query = currentSession().createSQLQuery(sql.toString());
        query.setReadOnly(true);
        query.setCacheable(true);
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }

        List<QueueDTO> queueElements = new ArrayList<>();
        
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
        return getByNaturalIdCaseInsensitive(VideoData.class, IDENTIFIER, identifier);
    }

    public Season getSeason(String identifier) {
        return getByNaturalIdCaseInsensitive(Season.class, IDENTIFIER, identifier);
    }

    public Series getSeries(String identifier) {
        return getByNaturalIdCaseInsensitive(Series.class, IDENTIFIER, identifier);
    }

    @Cacheable("person")
    public Person getPerson(String identifier) {
        return getByNaturalIdCaseInsensitive(Person.class, IDENTIFIER, identifier);
    }

    public synchronized void storePerson(CreditDTO dto) {
        String identifier = MetadataTools.cleanIdentifier(dto.getName());

        Person person = this.getPerson(identifier);
        if (person == null) {
            // create new person
            person = new Person(identifier);
            person.setSourceDbId(dto.getSource(), dto.getSourceId());
            person.setName(dto.getName(), dto.getSource());
            person.setFirstName(dto.getFirstName(), dto.getSource());
            person.setLastName(dto.getLastName(), dto.getSource());
            person.setBirthName(dto.getRealName(), dto.getSource());
            person.setStatus(StatusType.NEW);
            person.setFilmographyStatus(StatusType.NEW);
            this.saveEntity(person);
        } else {
            // these values are not regarded for updating status
            if (OverrideTools.checkOverwriteFirstName(person, dto.getSource())) {
                person.setFirstName(dto.getFirstName(), dto.getSource());
            }
            if (OverrideTools.checkOverwriteLastName(person, dto.getSource())) {
                person.setLastName(dto.getLastName(), dto.getSource());
            }
            if (OverrideTools.checkOverwriteBirthName(person, dto.getSource())) {
                person.setBirthName(dto.getRealName(), dto.getSource());
            }

            if (person.setSourceDbId(dto.getSource(), dto.getSourceId())) {
                // if IDs have changed then person update is needed
                person.setStatus(StatusType.UPDATED);
            }
            if (StatusType.DELETED.equals(person.getStatus())) {
                // if previously deleted then set as updated now
                person.setStatus(StatusType.UPDATED);
            }

            // update person in database
            this.updateEntity(person);
        }
    }

    public List<Artwork> findPersonArtworks(String identifier) {
        StringBuilder sb = new StringBuilder();
        sb.append("select a ");
        sb.append("from Artwork a ");
        sb.append("join a.person p ");
        sb.append("WHERE a.artworkType=:artworkType ");
        sb.append("AND lower(p.identifier)=:identifier ");

        Map<String, Object> params = new HashMap<>();
        params.put("artworkType", ArtworkType.PHOTO);
        params.put(IDENTIFIER, identifier.toLowerCase());

        return this.findByNamedParameters(sb, params);
    }
}
