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
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.hibernate.HibernateDao;

@Repository("artworkDao")
public class ArtworkDao extends HibernateDao {

    public List<ArtworkProfile> getAllArtworkProfiles() {
        return currentSession().getNamedQuery("artworkProfile.getAllArtworkProfiles")
                .setReadOnly(true)
                .setCacheable(true)
                .list();
    }
    
    public ArtworkProfile getArtworkProfile(String profileName, MetaDataType metaDataType, ArtworkType artworkType) {
        return currentSession().byNaturalId(ArtworkProfile.class)
                .using("profileName", profileName)
                .using("metaDataType", metaDataType)
                .using("artworkType", artworkType)
                .load();
    }

    public List<ArtworkProfile> getPreProcessArtworkProfiles(MetaDataType metaDataType, ArtworkType artworkType) {
        Criteria criteria = currentSession().createCriteria(ArtworkProfile.class);
        criteria.add(Restrictions.eq("metaDataType", metaDataType));
        criteria.add(Restrictions.eq("artworkType", artworkType));
        criteria.add(Restrictions.eq("preProcess", Boolean.TRUE));
        return criteria.list();
    }

    public List<QueueDTO> getArtworkQueue(final CharSequence sql, final int maxResults) {
        SQLQuery query = currentSession().createSQLQuery(sql.toString());
        query.setReadOnly(true);
        query.setCacheable(true);
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        List<Object[]> objects = query.list();

        List<QueueDTO> queueElements = new ArrayList<>(objects.size());
        for (Object[] object : objects) {
            QueueDTO queueElement = new QueueDTO(convertRowElementToLong(object[0]));
            queueElement.setArtworkType(convertRowElementToString(object[1]));
            queueElement.setDate(convertRowElementToDate(object[3]));
            if (queueElement.getDate() == null) {
                queueElement.setDate(convertRowElementToDate(object[2]));
            }
            queueElements.add(queueElement);
        }

        Collections.sort(queueElements);
        return queueElements;
    }

    public ArtworkLocated getStoredArtworkLocated(ArtworkLocated located) {
        Criteria criteria = currentSession().createCriteria(ArtworkLocated.class);
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

    public ArtworkGenerated getStoredArtworkGenerated(ArtworkLocated located, ArtworkProfile profile) {
        Criteria criteria = currentSession().createCriteria(ArtworkGenerated.class);
        criteria.add(Restrictions.eq("artworkLocated", located));
        criteria.add(Restrictions.eq("artworkProfile", profile));
        criteria.setCacheable(true);
        return (ArtworkGenerated) criteria.uniqueResult();
    }
    
    public List<QueueDTO> getArtworkLocatedQueue(final int maxResults) {
        return this.getQueue("artworkLocated.processQueue", maxResults);
    }

    public List<QueueDTO> getArtworkGeneratedQueue(final int maxResults) {
        return this.getQueue("artworkGenerated.processQueue", maxResults);
    }

    private List<QueueDTO> getQueue(final String queryName, final int maxResults) {
        Query query = currentSession().getNamedQuery(queryName);
        query.setReadOnly(true);
        query.setCacheable(true);
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        List<Object[]> objects = query.list();

        List<QueueDTO> queueElements = new ArrayList<>(objects.size());
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

    public List<Artwork> getBoxedSetArtwork(String boxedSetName, ArtworkType artworkType) {
        Criteria criteria = currentSession().createCriteria(Artwork.class);
        criteria.add(Restrictions.eq("artworkType", artworkType));
        criteria = criteria.createAlias("boxedSet", "bs");
        criteria.add(Restrictions.ilike("bs.name", boxedSetName.toLowerCase()));
        return criteria.list();
    }

    public void saveArtworkLocated(Artwork artwork, ArtworkLocated scannedLocated) {
        final int index = artwork.getArtworkLocated().indexOf(scannedLocated);
        if (index < 0) {
            // just store if not contained before
            artwork.getArtworkLocated().add(scannedLocated);
            this.saveEntity(scannedLocated);
        } else {
            // reset deletion status
            ArtworkLocated stored = artwork.getArtworkLocated().get(index);
            if (stored.getStatus() == StatusType.DELETED) {
                stored.setStatus(stored.getPreviousStatus());
                this.updateEntity(stored);
            }
        }
    }
    
    public List<ArtworkLocated> getArtworkLocatedWithCacheFilename(long lastId) {
        return currentSession().createCriteria(ArtworkLocated.class)
                .add(Restrictions.isNotNull("cacheFilename"))
                .add(Restrictions.ne("status", StatusType.DELETED))
                .add(Restrictions.gt("id", lastId))
                .addOrder(Order.asc("id"))
                .setMaxResults(100)
                .list();
    }
    
    public ArtworkLocated getArtworkLocated(Artwork artwork, String source, String hashCode) {
        return currentSession().byNaturalId(ArtworkLocated.class)
                .using("artwork", artwork)
                .using("source", source)
                .using("hashCode", hashCode)
                .load();        
        
    }
    
    public ArtworkGenerated getArtworkGenerated(Long locatedId, String profileName) {
        return (ArtworkGenerated)currentSession().getNamedQuery("artworkGenerated.getArtworkGenerated")
                .setLong("locatedId", locatedId)
                .setString("profileName", profileName)
                .uniqueResult();
    }
}
