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

import static org.hibernate.CacheMode.NORMAL;
import static org.hibernate.ScrollMode.FORWARD_ONLY;
import static org.yamj.common.type.StatusType.DELETED;
import static org.yamj.common.type.StatusType.DONE;
import static org.yamj.core.database.Literals.*;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.plugin.api.model.type.ArtworkType;

@Repository("artworkDao")
public class ArtworkDao extends HibernateDao {
    
    public List<ArtworkProfile> getAllArtworkProfiles() {
        return currentSession().getNamedQuery(ArtworkProfile.QUERY_GET_ALL)
                .setReadOnly(true)
                .setCacheable(true)
                .list();
    }
    
    public ArtworkProfile getArtworkProfile(String profileName, MetaDataType metaDataType, ArtworkType artworkType) {
        return currentSession().byNaturalId(ArtworkProfile.class)
                .using("profileName", profileName)
                .using(LITERAL_METADATA_TYPE, metaDataType)
                .using(LITERAL_ARTWORK_TYPE, artworkType)
                .load();
    }

    public List<ArtworkProfile> getPreProcessArtworkProfiles(MetaDataType metaDataType, ArtworkType artworkType) {
        return currentSession().createCriteria(ArtworkProfile.class)
                .add(Restrictions.eq(LITERAL_METADATA_TYPE, metaDataType))
                .add(Restrictions.eq(LITERAL_ARTWORK_TYPE, artworkType))
                .add(Restrictions.eq("preProcess", Boolean.TRUE))
                .list();
    }

    public ArtworkLocated getStoredArtworkLocated(ArtworkLocated located) {
        Criteria criteria = currentSession().createCriteria(ArtworkLocated.class)
                .add(Restrictions.eq("artwork", located.getArtwork()))
                .setCacheable(true);
                
        if (located.getStageFile() != null) {
            criteria.add(Restrictions.eq("stageFile", located.getStageFile()));
        } else {
            criteria.add(Restrictions.eq(LITERAL_SOURCE, located.getSource()));
            criteria.add(Restrictions.eq("url", located.getUrl()));
        }
        
        return (ArtworkLocated) criteria.uniqueResult();
    }

    public ArtworkGenerated getStoredArtworkGenerated(ArtworkLocated located, ArtworkProfile profile) {
        return (ArtworkGenerated) currentSession().createCriteria(ArtworkGenerated.class)
                .add(Restrictions.eq("artworkLocated", located))
                .add(Restrictions.eq("artworkProfile", profile))
                .setCacheable(true)
                .uniqueResult();
    }
    
    public List<QueueDTO> getArtworkQueueForScanning(final int maxResults,boolean scanPhoto) {
        return currentSession().getNamedQuery(Artwork.QUERY_SCANNING_QUEUE)
                .setString("personStatus", scanPhoto?DONE.name():"NONE")
                .setReadOnly(true)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .setMaxResults(maxResults)
                .list();
    }

    public List<QueueDTO> getArtworkQueueForProcessing(final int maxResults) {
        final List<QueueDTO> queueElements = new ArrayList<>(maxResults);
        
        try (ScrollableResults scroll = currentSession().getNamedQuery(Artwork.QUERY_PROCESSING_QUEUE)
                .setReadOnly(true)
                .setMaxResults(maxResults)
                .scroll(FORWARD_ONLY)
            )
        {
            Object[] row;
            while (scroll.next()) {
                row = scroll.get();
                
                QueueDTO dto = new QueueDTO(convertRowElementToLong(row[0]));
                dto.setLocatedArtwork(convertRowElementToBoolean(row[1]));
                queueElements.add(dto);
            }
        }
        
        return queueElements;
    }

    public List<Artwork> getBoxedSetArtwork(String boxedSetName, ArtworkType artworkType) {
        return currentSession().createCriteria(Artwork.class)
                .add(Restrictions.eq(LITERAL_ARTWORK_TYPE, artworkType))
                .createAlias("boxedSet", "bs")
                .add(Restrictions.ilike("bs.name", boxedSetName))
                .list();
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
            if (stored.isDeleted()) {
                stored.setStatus(stored.getPreviousStatus());
                this.updateEntity(stored);
            }
        }
    }
    
    public List<ArtworkLocated> getArtworkLocatedWithCacheFilename(long lastId) {
        return currentSession().createCriteria(ArtworkLocated.class)
                .add(Restrictions.isNotNull(LITERAL_CACHE_FILENAME))
                .add(Restrictions.ne(LITERAL_SOURCE, DELETED))
                .add(Restrictions.gt(LITERAL_ID, lastId))
                .addOrder(Order.asc(LITERAL_ID))
                .setMaxResults(100)
                .list();
    }
    
    public ArtworkLocated getArtworkLocated(Artwork artwork, String source, String hashCode) {
        return currentSession().byNaturalId(ArtworkLocated.class)
                .using("artwork", artwork)
                .using(LITERAL_SOURCE, source)
                .using("hashCode", hashCode)
                .load();        
        
    }
    
    public ArtworkGenerated getArtworkGenerated(Long locatedId, String profileName) {
        return (ArtworkGenerated)currentSession().getNamedQuery(ArtworkGenerated.QUERY_GET)
                .setLong("locatedId", locatedId)
                .setString("profileName", profileName)
                .uniqueResult();
    }
}
