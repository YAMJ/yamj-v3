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
package org.yamj.core.database.service;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.ArtworkDao;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkGenerated;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;

@Service("artworkStorageService")
public class ArtworkStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkStorageService.class);
    @Autowired
    private ArtworkDao artworkDao;
    
    @Transactional
    public void storeArtworkProfile(ArtworkProfile newProfile) {
        ArtworkProfile profile = artworkDao.getArtworkProfile(newProfile.getProfileName(), newProfile.getArtworkType());
        if (profile == null) {
            this.artworkDao.saveEntity(newProfile);
            LOG.info("Stored: {}", newProfile);
        } else {
            // TODO what to do if profile changed? set generated values to update?

            profile.setHeight(newProfile.getHeight());
            profile.setWidth(newProfile.getWidth());
            profile.setApplyToMovie(newProfile.isApplyToMovie());
            profile.setApplyToSeries(newProfile.isApplyToSeries());
            profile.setApplyToSeason(newProfile.isApplyToSeason());
            profile.setApplyToEpisode(newProfile.isApplyToEpisode());
            profile.setApplyToPerson(newProfile.isApplyToPerson());
            profile.setPreProcess(newProfile.isPreProcess());
            profile.setRoundedCorners(newProfile.isRoundedCorners());
            profile.setReflection(newProfile.isReflection());
            profile.setNormalize(newProfile.isNormalize());
            profile.setStretch(newProfile.isStretch());
            
            this.artworkDao.updateEntity(profile);
            LOG.info("Updated: {}", profile);
        }
    }

    @Transactional(readOnly = true)
    public List<ArtworkProfile> getPreProcessArtworkProfiles(ArtworkLocated located) {
        MetaDataType metaDataType = null;

        ArtworkType artworkType = located.getArtwork().getArtworkType();
        if (ArtworkType.PHOTO == artworkType) {
            metaDataType = MetaDataType.PERSON;
        } else if (ArtworkType.VIDEOIMAGE == artworkType) {
            metaDataType = MetaDataType.EPISODE;
        } else if (ArtworkType.BANNER == artworkType) {
            if (located.getArtwork().getSeries() != null) {
                metaDataType = MetaDataType.SERIES;
            } else {
                metaDataType = MetaDataType.SEASON;
            }
        } else if (ArtworkType.POSTER == artworkType || (ArtworkType.FANART == artworkType)) {
            if (located.getArtwork().getSeries() != null) {
                metaDataType = MetaDataType.SERIES;
            } else if (located.getArtwork().getSeason() != null) {
                metaDataType = MetaDataType.SEASON;
            } else {
                metaDataType = MetaDataType.MOVIE;
            }
        }

        return this.artworkDao.getPreProcessArtworkProfiles(artworkType, metaDataType);
    }

    @Transactional
    public void updateArtwork(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (CollectionUtils.isEmpty(artwork.getArtworkLocated())) {
            // no located artwork presents; just store all
            this.artworkDao.storeAll(locatedArtworks);
        } else if (CollectionUtils.isNotEmpty(locatedArtworks)) {
            for (ArtworkLocated located : locatedArtworks) {
                if (!artwork.getArtworkLocated().contains(located)) {
                    // just store if not contained before
                    artwork.getArtworkLocated().add(located);
                    this.artworkDao.saveEntity(located);
                }
            }
        }

        // set status of artwork
        if (CollectionUtils.isEmpty(locatedArtworks) && CollectionUtils.isEmpty(artwork.getArtworkLocated())) {
            artwork.setStatus(StatusType.NOTFOUND);
        } else {
            artwork.setStatus(StatusType.DONE);
        }

        // update artwork in database
        this.artworkDao.updateEntity(artwork);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getArtworkQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT art.id,art.artwork_type,art.create_timestamp,art.update_timestamp ");
        sql.append("FROM artwork art ");
        sql.append("LEFT OUTER JOIN videodata vd ON vd.id=art.videodata_id ");
        sql.append("LEFT OUTER JOIN season sea ON sea.id=art.season_id ");
        sql.append("LEFT OUTER JOIN series ser ON ser.id=art.series_id ");
        sql.append("LEFT OUTER JOIN person p ON p.id=art.person_id ");
        sql.append("WHERE art.status in ('NEW','UPDATED') ");
        sql.append("AND (vd.status is null OR vd.status='DONE') ");
        sql.append("AND (sea.status is null OR sea.status='DONE') ");
        sql.append("AND (ser.status is null OR ser.status='DONE') ");

        return artworkDao.getArtworkQueue(sql, maxResults);
    }

    @Transactional(readOnly = true)
    public Artwork getRequiredArtwork(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("FROM Artwork art ");
        sb.append("LEFT OUTER JOIN FETCH art.videoData ");
        sb.append("LEFT OUTER JOIN FETCH art.season ");
        sb.append("LEFT OUTER JOIN FETCH art.series ");
        sb.append("LEFT OUTER JOIN FETCH art.person ");
        sb.append("LEFT OUTER JOIN FETCH art.artworkLocated ");
        sb.append("WHERE art.id = :id");

        @SuppressWarnings("unchecked")
        List<Artwork> objects = this.artworkDao.findById(sb, id);
        Artwork artwork = DataAccessUtils.requiredUniqueResult(objects);

        if (artwork.getSeason() != null) {
            // also initialize series
            if (!Hibernate.isInitialized(artwork.getSeason().getSeries())) {
                Hibernate.initialize(artwork.getSeason().getSeries());
            }
        } else if (artwork.getVideoData() != null && artwork.getVideoData().getSeason() != null) {
            // also initialize season and series
            if (!Hibernate.isInitialized(artwork.getVideoData().getSeason())) {
                Hibernate.initialize(artwork.getVideoData().getSeason());
            }
            if (!Hibernate.isInitialized(artwork.getVideoData().getSeason().getSeries())) {
                Hibernate.initialize(artwork.getVideoData().getSeason().getSeries());
            }
        }
        return artwork;
    }

    @Transactional
    public void errorArtwork(Long id) {
        Artwork artwork = artworkDao.getArtwork(id);
        if (artwork != null) {
            artwork.setStatus(StatusType.ERROR);
            artworkDao.updateEntity(artwork);
        }
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getArtworLocatedQueue(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT loc.id, loc.create_timestamp, loc.update_timestamp ");
        sql.append("FROM artwork_located loc ");
        sql.append("WHERE loc.status in ('NEW','UPDATED')");

        return artworkDao.getArtworkLocatedQueue(sql, maxResults);
    }

    @Transactional(readOnly = true)
    public ArtworkLocated getRequiredArtworkLocated(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("FROM ArtworkLocated loc ");
        sb.append("JOIN FETCH loc.artwork art ");
        sb.append("LEFT OUTER JOIN FETCH art.videoData ");
        sb.append("LEFT OUTER JOIN FETCH art.season ");
        sb.append("LEFT OUTER JOIN FETCH art.series ");
        sb.append("LEFT OUTER JOIN FETCH art.person ");
        sb.append("LEFT OUTER JOIN FETCH loc.stageFile ");
        sb.append("WHERE loc.id = :id");

        @SuppressWarnings("unchecked")
        List<ArtworkLocated> objects = this.artworkDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional
    public boolean errorArtworkLocated(Long id) {
        ArtworkLocated located = artworkDao.getArtworkLocated(id);
        if (located != null) {
            located.setStatus(StatusType.ERROR);
            artworkDao.updateEntity(located);
            return true;
        }
        return false;
    }

    @Transactional
    public void updateArtworkLocated(ArtworkLocated located) {
        this.artworkDao.updateEntity(located);
    }

    @Transactional
    public void storeArtworkGenerated(ArtworkGenerated generated) {
        ArtworkGenerated stored = this.artworkDao.getStoredArtworkGenerated(generated);
        if (stored == null) {
            this.artworkDao.saveEntity(generated);
        } else {
            stored.setCacheDirectory(generated.getCacheDirectory());
            stored.setCacheFilename(generated.getCacheFilename());
            this.artworkDao.updateEntity(stored);
        }
    }
}
