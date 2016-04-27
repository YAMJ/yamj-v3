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
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.CachingNames;
import org.yamj.core.database.dao.ArtworkDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.service.artwork.ArtworkStorageTools;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;
import org.yamj.plugin.api.model.type.ArtworkType;

@Service("artworkStorageService")
public class ArtworkStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkStorageService.class);
    
    @Autowired
    private ArtworkDao artworkDao;
    @Autowired
    private FileStorageService fileStorageService;

    @Transactional
    @CachePut(value=CachingNames.DB_ARTWORK_PROFILE, key="{#artworkProfile.profileName, #artworkProfile.metaDataType, #artworkProfile.artworkType}")
    public ArtworkProfile saveArtworkProfile(ArtworkProfile artworkProfile) {
        this.artworkDao.saveEntity(artworkProfile);
        return artworkProfile;
    }

    @Transactional
    @CachePut(value=CachingNames.DB_ARTWORK_PROFILE, key="{#artworkProfile.profileName, #artworkProfile.metaDataType, #artworkProfile.artworkType}")
    public ArtworkProfile updateArtworkProfile(ArtworkProfile artworkProfile) {
        this.artworkDao.updateEntity(artworkProfile);
        return artworkProfile;
    }

    @Transactional(readOnly = true)
    public ArtworkProfile getArtworkProfile(Long id) {
        return artworkDao.getById(ArtworkProfile.class, id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value=CachingNames.DB_ARTWORK_PROFILE, key="{#profileName, #metaDataType, #artworkType}", unless="#result==null")
    public ArtworkProfile getArtworkProfile(String profileName, MetaDataType metaDataType, ArtworkType artworkType) {
        return artworkDao.getArtworkProfile(profileName, metaDataType, artworkType);
    }

    @Transactional(readOnly = true)
    public List<ArtworkProfile> getAllArtworkProfiles() {
        return artworkDao.getAllArtworkProfiles();
    }

    @Transactional(readOnly = true)
    public List<ArtworkProfile> getPreProcessArtworkProfiles(ArtworkLocated located) {
        final MetaDataType metaDataType = ArtworkStorageTools.getMetaDataType(located);
        final ArtworkType artworkType = located.getArtwork().getArtworkType();
        return this.artworkDao.getPreProcessArtworkProfiles(metaDataType, artworkType);
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
            if (stageFile != null && stageFile.isNotFound()) {
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
    public List<QueueDTO> getArtworkQueueForScanning(final int maxResults, boolean scanPhoto) {
        return artworkDao.getArtworkQueueForScanning(maxResults, scanPhoto);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getArtworkQueueForProcessing(final int maxResults) {
        return artworkDao.getArtworkQueueForProcessing(maxResults);
    }

    @Transactional(readOnly = true)
    public Artwork getRequiredArtwork(long id) {
        List<Artwork> objects = this.artworkDao.namedQueryById(Artwork.QUERY_REQUIRED, id);
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
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("status", StatusType.ERROR);
        artworkDao.executeUpdate(Artwork.UPDATE_STATUS, params);
    }

    @Transactional(readOnly = true)
    public ArtworkLocated getRequiredArtworkLocated(long id) {
        List<ArtworkLocated> objects = this.artworkDao.namedQueryById(ArtworkLocated.QUERY_REQUIRED, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(readOnly = true)
    public ArtworkGenerated getRequiredArtworkGenerated(long id) {
        List<ArtworkGenerated> objects = this.artworkDao.namedQueryById(ArtworkGenerated.QUERY_REQUIRED, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional
    public void errorArtworkLocated(Long id) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("status", StatusType.ERROR);
        artworkDao.executeUpdate(ArtworkLocated.UPDATE_STATUS, params);
    }

    @Transactional
    public void errorArtworkGenerated(Long id) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("status", StatusType.ERROR);
        artworkDao.executeUpdate(ArtworkGenerated.UPDATE_STATUS, params);
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
    @CachePut(value=CachingNames.DB_ARTWORK_IMAGE, key="{#located.id, #profile.profileName}")
    public ArtworkGenerated storeArtworkGenerated(ArtworkLocated located, ArtworkProfile profile, String cacheDir, String cacheFileName) {
        ArtworkGenerated generated = this.artworkDao.getStoredArtworkGenerated(located, profile);
        if (generated == null) {
            generated = new ArtworkGenerated();
            generated.setArtworkLocated(located);
            generated.setArtworkProfile(profile);
            generated.setCacheDirectory(cacheDir);
            generated.setCacheFilename(cacheFileName);
            generated.setStatus(StatusType.DONE);
            this.artworkDao.saveEntity(generated);
        } else {
            generated.setCacheDirectory(cacheDir);
            generated.setCacheFilename(cacheFileName);
            generated.setStatus(StatusType.DONE);
            this.artworkDao.updateEntity(generated);
        }
        return generated;
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
    public Artwork getArtwork(ArtworkType artworkType, MetaDataType metaDataType, Long id) {
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
            final StorageType storageType = ArtworkStorageTools.getStorageType(located);
            
            if (!fileStorageService.existsFile(storageType, located.getCacheDirectory(), located.getCacheFilename())) {
                LOG.trace("Mark located artwork {} for UPDATE due missing original image", located.getId());
                
                // reset status and cache file name
                located.setStatus(StatusType.UPDATED);
                located.setCacheDirectory(null);
                located.setCacheFilename(null);
            } else {
                // check if one of the generated images is missing
                for (ArtworkGenerated generated : located.getGeneratedArtworks()) {
                    if (!fileStorageService.existsFile(storageType, generated.getCacheDirectory(), generated.getCacheFilename())) {
                        LOG.trace("Mark generated artwork {} for UPDATE due missing generated image", generated.getId());
                        // set status of generated to UPDATED
                        generated.setStatus(StatusType.UPDATED);
                    }
                }
            }
        }
        
        return newLastId;
    }

    @Transactional(readOnly=true)
    @Cacheable(value=CachingNames.DB_ARTWORK_IMAGE, key="{#locatedId, #profileName}", unless="#result==null")
    public ArtworkGenerated getArtworkGenerated(Long locatedId, String profileName) {
        return this.artworkDao.getArtworkGenerated(locatedId, profileName);
    }

    @Transactional
    public int generateImagesForProfile(Long id) {
        ArtworkProfile profile = artworkDao.getById(ArtworkProfile.class, id);
        if (profile == null) {
            // nothing to do if no profile found
            return 0;
        }
        
        Date profileDate = profile.getCreateTimestamp();
        if (profile.getUpdateTimestamp() != null) {
            profileDate = profile.getUpdateTimestamp();
        }

        Map<String,Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("profileDate", profileDate);
        return this.artworkDao.executeUpdate(ArtworkGenerated.UPDATE_STATUS_FOR_PROFILE, params);
    }
}
