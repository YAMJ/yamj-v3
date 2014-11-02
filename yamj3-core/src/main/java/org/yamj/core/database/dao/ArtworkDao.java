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
import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.dto.QueueDTOComparator;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.hibernate.HibernateDao;

@Repository("artworkDao")
public class ArtworkDao extends HibernateDao {

    public ArtworkProfile getArtworkProfile(String profileName, ArtworkType artworkType) {
        return (ArtworkProfile) getSession().byNaturalId(ArtworkProfile.class)
                .using("profileName", profileName)
                .using("artworkType", artworkType)
                .load();
    }

    @SuppressWarnings("unchecked")
    public List<ArtworkProfile> getPreProcessArtworkProfiles(ArtworkType artworkType, MetaDataType metaDataType) {
        Session session = getSession();
        Criteria criteria = session.createCriteria(ArtworkProfile.class);
        criteria.add(Restrictions.eq("artworkType", artworkType));
        criteria.add(Restrictions.eq("preProcess", Boolean.TRUE));
        if (MetaDataType.MOVIE == metaDataType) {
            criteria.add(Restrictions.eq("applyToMovie", Boolean.TRUE));
        } else if (MetaDataType.SERIES == metaDataType) {
            criteria.add(Restrictions.eq("applyToSeries", Boolean.TRUE));
        } else if (MetaDataType.SEASON == metaDataType) {
            criteria.add(Restrictions.eq("applyToSeason", Boolean.TRUE));
        } else if (MetaDataType.EPISODE == metaDataType) {
            criteria.add(Restrictions.eq("applyToEpisode", Boolean.TRUE));
        } else if (MetaDataType.PERSON == metaDataType) {
            criteria.add(Restrictions.eq("applyToPerson", Boolean.TRUE));
        }
        return criteria.list();
    }

    public Artwork getArtwork(Long id) {
        return getById(Artwork.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<QueueDTO> getArtworkQueue(final CharSequence sql, final int maxResults) {
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

    public ArtworkLocated getArtworkLocated(Long id) {
        return getById(ArtworkLocated.class, id);
    }

    public ArtworkLocated getStoredArtworkLocated(ArtworkLocated located) {
        Criteria criteria = getSession().createCriteria(ArtworkLocated.class);
        criteria.add(Restrictions.eq("artwork", located.getArtwork()));
        if (located.getStageFile() != null) {
            criteria.add(Restrictions.eq("stageFile", located.getStageFile()));
        } else {
            criteria.add(Restrictions.eq("source", located.getSource()));
            criteria.add(Restrictions.eq("url", located.getUrl()));
        }
        criteria.setCacheable(true);
        return (ArtworkLocated) criteria.uniqueResult();
    }

    public ArtworkGenerated getStoredArtworkGenerated(ArtworkGenerated generated) {
        Criteria criteria = getSession().createCriteria(ArtworkGenerated.class);
        criteria.add(Restrictions.eq("artworkLocated", generated.getArtworkLocated()));
        criteria.add(Restrictions.eq("artworkProfile", generated.getArtworkProfile()));
        criteria.setCacheable(true);
        return (ArtworkGenerated) criteria.uniqueResult();
    }
    
    @SuppressWarnings("unchecked")
    public List<QueueDTO> getArtworkLocatedQueue(final CharSequence sql, final int maxResults) {
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
            queueElement.setDate(convertRowElementToDate(object[2]));
            if (queueElement.getDate() == null) {
                queueElement.setDate(convertRowElementToDate(object[1]));
            }
            queueElements.add(queueElement);
        }

        Collections.sort(queueElements, new QueueDTOComparator());
        return queueElements;
    }

    public Artwork getArtwork(Person person, ArtworkType artworkType) {
        Criteria criteria = getSession().createCriteria(Artwork.class);
        criteria.add(Restrictions.eq("person", person));
        criteria.add(Restrictions.eq("artworkType", artworkType));
        criteria.setCacheable(true);
        return (Artwork) criteria.uniqueResult();
    }
}
