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
import static org.yamj.common.type.StatusType.DELETED;
import static org.yamj.common.type.StatusType.NEW;
import static org.yamj.common.type.StatusType.UPDATED;
import static org.yamj.core.CachingNames.DB_PERSON;
import static org.yamj.core.database.Literals.LITERAL_ARTWORK_TYPE;
import static org.yamj.core.database.Literals.LITERAL_ID;
import static org.yamj.core.database.Literals.LITERAL_IDENTIFIER;
import static org.yamj.plugin.api.model.type.ArtworkType.PHOTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.artwork.ArtworkDTO;

@Transactional
@Repository("metadataDao")
public class MetadataDao extends HibernateDao {

    @Autowired
    private ArtworkDao artworkDao;

    public List<QueueDTO> getMetadataQueue(final String queryName, final int maxResults) {
        return currentSession().getNamedQuery(queryName)
                .setReadOnly(true)
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .setMaxResults(maxResults)
                .list();
    }

    public VideoData getVideoData(String identifier) {
        return getByNaturalIdCaseInsensitive(VideoData.class, LITERAL_IDENTIFIER, identifier);
    }

    public Season getSeason(String identifier) {
        return getByNaturalIdCaseInsensitive(Season.class, LITERAL_IDENTIFIER, identifier);
    }

    public Series getSeries(String identifier) {
        return getByNaturalIdCaseInsensitive(Series.class, LITERAL_IDENTIFIER, identifier);
    }

    public Person getPerson(String identifier) {
        return getByNaturalIdCaseInsensitive(Person.class, LITERAL_IDENTIFIER, identifier);
    }

    @Cacheable(value=DB_PERSON, key="#id", unless="#result==null")
    public Person getCacheablePerson(Long id) {
        return getById(Person.class, id);
    }

    @CacheEvict(value=DB_PERSON, key="#doubletPerson.id")
    public void duplicate(Person person, Person doubletPerson) {
        // find movies which contains the doublet
        List<VideoData> videoDatas = currentSession().getNamedQuery(VideoData.QUERY_FIND_VIDEOS_FOR_PERSON)
                .setLong(LITERAL_ID, doubletPerson.getId())
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
        
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
        doubletPerson.setStatus(DELETED);
        this.updateEntity(doubletPerson);
    }
    
    public void storeMovieCredit(CreditDTO dto) {
        Person person = getByNaturalIdCaseInsensitive(Person.class, LITERAL_IDENTIFIER, dto.getIdentifier());
        if (person == null) {
            // create new person
            person = new Person(dto.getIdentifier());
            person.setSourceDbId(dto.getSource(), dto.getSourceId());
            person.setName(dto.getName(), dto.getSource());
            person.setFirstName(dto.getFirstName(), dto.getSource());
            person.setLastName(dto.getLastName(), dto.getSource());
            person.setBirthName(dto.getRealName(), dto.getSource());
            person.setStatus(NEW);
            person.setFilmographyStatus(NEW);
            this.saveEntity(person);

            // store artwork
            Artwork photo = new Artwork();
            photo.setArtworkType(PHOTO);
            photo.setPerson(person);
            photo.setStatus(NEW);
            person.setPhoto(photo);
            this.saveEntity(photo);
        } else {
            // just update person in database
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
                person.setStatus(UPDATED);
            } else if (person.isDeleted()) {
                // if previously deleted then set as updated now
                person.setStatus(UPDATED);
            }
        }
        
        if (CollectionUtils.isNotEmpty(dto.getPhotoDTOS())) {
            this.updateLocatedArtwork(person.getPhoto(), dto.getPhotoDTOS());
        }

        // set person id for later use
        dto.setPersonId(person.getId());
    }

    public void updateLocatedArtwork(Artwork artwork, Collection<ArtworkDTO> dtos) {
        for (ArtworkDTO dto : dtos) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource(dto.getSource());
            located.setUrl(dto.getUrl());
            located.setHashCode(dto.getHashCode());
            located.setPriority(5);
            located.setImageType(dto.getImageType());
            located.setStatus(NEW);
            
            artworkDao.saveArtworkLocated(artwork, located);
        }
    }

    public List<Artwork> findPersonArtworks(String identifier) {
        return currentSession().getNamedQuery(Artwork.QUERY_FIND_PERSON_ARTWORKS)
                .setParameter(LITERAL_ARTWORK_TYPE, PHOTO)
                .setString(LITERAL_IDENTIFIER, identifier.toLowerCase())
                .setCacheable(true)
                .setCacheMode(NORMAL)
                .list();
    }
}
