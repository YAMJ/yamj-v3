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
package org.yamj.core.database.service;

import java.util.*;
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
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;

@Service("artworkStorageService")
public class ArtworkStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkStorageService.class);
    
    @Autowired
    private ArtworkDao artworkDao;
    @Autowired
    private FileStorageService fileStorageService;

    @Transactional
    public void saveArtworkProfile(ArtworkProfile artworkProfile) {
        this.artworkDao.saveEntity(artworkProfile);
    }

    @Transactional
    public void updateArtworkProfile(ArtworkProfile artworkProfile) {
        this.artworkDao.updateEntity(artworkProfile);
    }

    @Transactional(readOnly = true)
    public ArtworkProfile getArtworkProfile(String profileName, ArtworkType artworkType) {
        return artworkDao.getArtworkProfile(profileName, artworkType);
    }

    @Transactional(readOnly = true)
    public List<ArtworkProfile> getAllArtworkProfiles() {
        return artworkDao.getAll(ArtworkProfile.class, "profileName");

    }

    @Transactional(readOnly = true)
    public List<ArtworkProfile> getPreProcessArtworkProfiles(ArtworkLocated located) {
        MetaDataType metaDataType = null;

        final ArtworkType artworkType = located.getArtwork().getArtworkType(); 
        switch(artworkType) {
        case PHOTO:
            metaDataType = MetaDataType.PERSON;
            break;
        case VIDEOIMAGE:
            metaDataType = MetaDataType.EPISODE;
            break;
        case BANNER:
            if (located.getArtwork().getBoxedSet() != null) {
                metaDataType = MetaDataType.BOXSET;
            } else if (located.getArtwork().getSeries() != null) {
                metaDataType = MetaDataType.SERIES;
            } else {
                metaDataType = MetaDataType.SEASON;
            }
            break;
        case POSTER:
        case FANART:
            if (located.getArtwork().getBoxedSet() != null) {
                metaDataType = MetaDataType.BOXSET;
            } else if (located.getArtwork().getSeries() != null) {
                metaDataType = MetaDataType.SERIES;
            } else if (located.getArtwork().getSeason() != null) {
                metaDataType = MetaDataType.SEASON;
            } else {
                metaDataType = MetaDataType.MOVIE;
            }
            break;
        default:
            break;
        }

        return this.artworkDao.getPreProcessArtworkProfiles(artworkType, metaDataType);
    }

    @Transactional
    public void updateArtwork(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (artwork.getArtworkLocated().isEmpty()) {
            // no located artwork presents; just store all
            this.artworkDao.storeAll(locatedArtworks);
        } else if (CollectionUtils.isNotEmpty(locatedArtworks)) {
            for (ArtworkLocated located : locatedArtworks) {
                this.artworkDao.saveArtworkLocated(artwork, located);
            }
        }

        // update not found stage files to DONE
        for (ArtworkLocated located : locatedArtworks) {
            StageFile stageFile = located.getStageFile();
            if (stageFile != null && StatusType.NOTFOUND.equals(stageFile.getStatus())) {
                stageFile.setStatus(StatusType.DONE);
                this.artworkDao.updateEntity(stageFile);
            }
        }
        
        // set status of artwork
        if (CollectionUtils.isEmpty(artwork.getArtworkLocated())) {
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
        sb.append("LEFT OUTER JOIN FETCH art.boxedSet ");
        sb.append("LEFT OUTER JOIN FETCH art.artworkLocated ");
        sb.append("WHERE art.id = :id");

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
        Artwork artwork = artworkDao.getById(Artwork.class, id);
        if (artwork != null) {
            artwork.setStatus(StatusType.ERROR);
            artworkDao.updateEntity(artwork);
        }
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getArtworLocatedQueue(final int maxResults) {
        return artworkDao.getArtworkLocatedQueue(maxResults);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getArtworGeneratedQueue(final int maxResults) {
        return artworkDao.getArtworkGeneratedQueue(maxResults);
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
        sb.append("LEFT OUTER JOIN FETCH art.boxedSet ");
        sb.append("LEFT OUTER JOIN FETCH loc.stageFile ");
        sb.append("WHERE loc.id = :id");

        List<ArtworkLocated> objects = this.artworkDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(readOnly = true)
    public ArtworkGenerated getRequiredArtworkGenerated(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("FROM ArtworkGenerated gen ");
        sb.append("JOIN FETCH gen.artworkLocated loc ");
        sb.append("JOIN FETCH gen.artworkProfile profile ");
        sb.append("JOIN FETCH loc.artwork art ");
        sb.append("WHERE gen.id = :id");

        List<ArtworkGenerated> objects = this.artworkDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional
    public boolean errorArtworkLocated(Long id) {
        ArtworkLocated located = artworkDao.getById(ArtworkLocated.class, id);
        if (located != null) {
            located.setStatus(StatusType.ERROR);
            artworkDao.updateEntity(located);
            return true;
        }
        return false;
    }

    @Transactional
    public void storeArtworkLocated(ArtworkLocated located) {
        this.artworkDao.storeEntity(located);
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
            stored.setStatus(generated.getStatus());
            this.artworkDao.updateEntity(stored);
        }
    }

    @Transactional
    public void updateArtworkGenerated(ArtworkGenerated generated) {
        this.artworkDao.updateEntity(generated);
    }

    @Transactional(readOnly=true)
    public ArtworkLocated getArtworkLocated(Artwork artwork, String source, String hashCode) {
        return this.artworkDao.getArtworkLocated(artwork, source, hashCode);
    }
    
    @Transactional(readOnly=true)
    public Artwork getArtwork(ArtworkType artworkType, MetaDataType metaDataType, long id) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT a FROM Artwork a ");
        switch (metaDataType) {
            case MOVIE: 
                sb.append("JOIN FETCH a.videoData vd WHERE vd.id=:id AND vd.episode<0 ");
                break;
            case EPISODE:
                sb.append("JOIN FETCH a.videoData vd WHERE vd.id=:id AND vd.episode>=0 ");
                break;
            case SERIES:
                sb.append("JOIN FETCH a.series ser WHERE ser.id=:id ");
                break;
            case SEASON:
                sb.append("JOIN FETCH a.season sea WHERE sea.id=:id ");
                break;
            case BOXSET:
                sb.append("JOIN FETCH a.boxedSet bs WHERE bs.id=:id ");
                break;
            default:
                sb.append("JOIN FETCH a.person p WHERE p.id=:id ");
                break;
        }
        sb.append("AND a.artworkType=:artworkType ");

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("artworkType", artworkType);
        return this.artworkDao.findUniqueByNamedParameters(Artwork.class, sb, params);
    }

    @Transactional
    public long checkArtworkSanity(long lastId) {
        long newLastId = -1;
        
        for (ArtworkLocated located : this.artworkDao.getArtworkLocatedWithCacheFilename(lastId)) {
            newLastId = Math.max(newLastId, located.getId());

            // if original file does not exists, then also all generated artwork can be deleted
            final StorageType storageType = located.getArtwork().getStorageType();
            
            if (!fileStorageService.existsFile(storageType, located.getCacheDirectory(), located.getCacheFilename())) {
                LOG.trace("Mark located artwork {} for UPDATE due missing original image", located.getId());
                
                // reset status and cache file name
                located.setStatus(StatusType.UPDATED);
                located.setCacheDirectory(null);
                located.setCacheFilename(null);
                this.artworkDao.updateEntity(located);
            } else {
                // check if one of the generated images is missing
                for (ArtworkGenerated generated : located.getGeneratedArtworks()) {
                    if (!fileStorageService.existsFile(storageType, generated.getCacheDirectory(), generated.getCacheFilename())) {
                        LOG.trace("Mark generated artwork {} for UPDATE due missing generated image", generated.getId());
                        // set status of generated to UPDATED
                        generated.setStatus(StatusType.UPDATED);
                        this.artworkDao.updateEntity(generated);
                    }
                }
            }
        }
        
        return newLastId;
    }
    
    @Transactional
    public int generateImagesForProfile(long id) {
        ArtworkProfile profile = artworkDao.getById(ArtworkProfile.class, id);
        if (profile == null) {
            // nothing to do if no profile found
            return 0;
        }
        
        Date profileDate = profile.getCreateTimestamp();
        if (profile.getUpdateTimestamp() != null) {
            profileDate = profile.getUpdateTimestamp();
        }
       
        final StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ArtworkGenerated gen ");
        sb.append("SET status='UPDATED' ");
        sb.append("WHERE gen.artworkProfile.id=:id ");
        sb.append("AND gen.status != 'UPDATED' ");
        sb.append("AND ((gen.updateTimestamp is null and gen.createTimestamp<=:profileDate) OR ");
        sb.append("     (gen.updateTimestamp is not null and gen.updateTimestamp<=:profileDate)) ");

        Map<String,Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("profileDate", profileDate);
        return this.artworkDao.executeUpdate(sb, params);
    }
}
