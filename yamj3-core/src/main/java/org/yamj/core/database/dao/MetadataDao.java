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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.CachingNames;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.CastCrew;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.tools.OverrideTools;

@Transactional
@Repository("metadataDao")
public class MetadataDao extends HibernateDao {

    @Autowired
    private ArtworkDao artworkDao;
    
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

        Collections.sort(queueElements);
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

    @Cacheable(value=CachingNames.DB_PERSON, key="#id", unless="#result==null")
    public Person getPerson(Long id) {
        return getById(Person.class, id);
    }

    @CacheEvict(value=CachingNames.DB_PERSON, key="#doubletPerson.id")
    public void duplicate(Person person, Person doubletPerson) {
        // find movies which contains the doublet
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT distinct vd ");
        sb.append("FROM VideoData vd ");
        sb.append("JOIN vd.credits credit ");
        sb.append("WHERE credit.castCrewPK.person.id=:id");
        
        Query query = currentSession().createQuery(sb.toString());
        query.setParameter("id", doubletPerson.getId());
        query.setCacheable(true);
        query.setCacheMode(CacheMode.NORMAL);
        List<VideoData> videoDatas = query.list();
        
        for (VideoData videoData : videoDatas) {
            // find doublet entries (for different jobs)
            List<CastCrew> doubletCredits = new ArrayList<>();
            for (CastCrew credit : videoData.getCredits()) {
                if (credit.getCastCrewPK().getPerson().equals(doubletPerson)) {
                    doubletCredits.add(credit);
                }
            }
            
            for (CastCrew doubletCredit : doubletCredits) {
                CastCrew newCredit = new CastCrew(person, videoData, doubletCredit.getCastCrewPK().getJobType());
                if (videoData.getCredits().contains(newCredit)) {
                    // just remove doublet person
                    videoData.getCredits().remove(doubletCredit);
                } else {
                    newCredit.setOrdering(doubletCredit.getOrdering());
                    newCredit.setRole(doubletCredit.getRole());
                    newCredit.setVoiceRole(doubletCredit.isVoiceRole());
                    videoData.getCredits().remove(doubletCredit);
                    videoData.getCredits().add(newCredit);
                } 
            }
            
            // update video data
            this.updateEntity(videoData);
        }
        
        // update doublet person
        doubletPerson.setStatus(StatusType.DELETED);
        this.updateEntity(doubletPerson);
    }
    
    public void storeMovieCredit(CreditDTO dto) {
        Person person = getByNaturalIdCaseInsensitive(Person.class, IDENTIFIER, dto.getIdentifier());
        if (person == null && StringUtils.isNotBlank(dto.getSource()) && StringUtils.isNotBlank(dto.getSourceId())) {
            // try to fetch person by source ID; note that a list may be returned due duplicates
            // TODO
        }
        
        if (person == null) {
            // create new person
            person = new Person(dto.getIdentifier());
            person.setSourceDbId(dto.getSource(), dto.getSourceId());
            person.setName(dto.getName(), dto.getSource());
            person.setFirstName(dto.getFirstName(), dto.getSource());
            person.setLastName(dto.getLastName(), dto.getSource());
            person.setBirthName(dto.getRealName(), dto.getSource());
            person.setStatus(StatusType.NEW);
            person.setFilmographyStatus(StatusType.NEW);
            this.saveEntity(person);

            // store artwork
            Artwork photo = new Artwork();
            photo.setArtworkType(ArtworkType.PHOTO);
            photo.setPerson(person);
            photo.setStatus(StatusType.NEW);
            person.setPhoto(photo);
            this.saveEntity(photo);
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
            } else if (StatusType.DELETED.equals(person.getStatus())) {
                // if previously deleted then set as updated now
                person.setStatus(StatusType.UPDATED);
            }

            // update person in database
            this.updateEntity(person);
        }
        
        if (CollectionUtils.isNotEmpty(dto.getPhotoDTOS())) {
            this.updateLocatedArtwork(person.getPhoto(), dto.getPhotoDTOS());
        }

        // set person id for later use
        dto.setPersonId(person.getId());
    }

    public void updateLocatedArtwork(Artwork artwork, Collection<ArtworkDetailDTO> dtos) {
        for (ArtworkDetailDTO dto : dtos) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource(dto.getSource());
            located.setUrl(dto.getUrl());
            located.setHashCode(dto.getHashCode());
            located.setPriority(5);
            located.setImageType(dto.getImageType());
            located.setStatus(StatusType.NEW);
            
            artworkDao.saveArtworkLocated(artwork, located);
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
