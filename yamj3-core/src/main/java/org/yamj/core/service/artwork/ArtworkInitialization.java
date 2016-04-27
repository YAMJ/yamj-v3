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
package org.yamj.core.service.artwork;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.type.ScalingType;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.plugin.api.model.type.ArtworkType;

/**
 * Just used for initialization of artwork profiles at startup.
 */
@Component("artworkInitialization")
@DependsOn("upgradeDatabaseService")
public class ArtworkInitialization {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkInitialization.class);
    
    @Autowired
    private ArtworkStorageService artworkStorageService;

    @PostConstruct
    public void init() {
        LOG.debug("Initialize artwork profiles");
        initArtworkProfile("default", MetaDataType.MOVIE, ArtworkType.FANART, 1280, 720); 
        initArtworkProfile("default", MetaDataType.SERIES, ArtworkType.FANART, 1280, 720); 
        initArtworkProfile("default", MetaDataType.SEASON, ArtworkType.FANART, 1280, 720); 
        initArtworkProfile("default", MetaDataType.BOXSET, ArtworkType.FANART, 1280, 720);
        initArtworkProfile("default", MetaDataType.MOVIE, ArtworkType.POSTER, 224, 332); 
        initArtworkProfile("default", MetaDataType.SERIES, ArtworkType.POSTER, 224, 332); 
        initArtworkProfile("default", MetaDataType.SEASON, ArtworkType.POSTER, 224, 332); 
        initArtworkProfile("default", MetaDataType.BOXSET, ArtworkType.POSTER, 224, 332);
        initArtworkProfile("default", MetaDataType.SERIES, ArtworkType.BANNER, 650, 120); 
        initArtworkProfile("default", MetaDataType.SEASON, ArtworkType.BANNER, 650, 120); 
        initArtworkProfile("default", MetaDataType.BOXSET, ArtworkType.BANNER, 650, 120); 
        initArtworkProfile("default", MetaDataType.EPISODE, ArtworkType.VIDEOIMAGE, 400, 225);
        initArtworkProfile("default", MetaDataType.PERSON, ArtworkType.PHOTO, 200, 300);
    }
    
    private void initArtworkProfile(String profileName, MetaDataType metaDataType, ArtworkType artworkType, int width, int height) {
        try {
            ArtworkProfile artworkProfile = artworkStorageService.getArtworkProfile(profileName, metaDataType, artworkType);
            if (artworkProfile == null) {
                artworkProfile = new ArtworkProfile();
                artworkProfile.setProfileName(profileName);
                artworkProfile.setMetaDataType(metaDataType);
                artworkProfile.setArtworkType(artworkType);
                artworkProfile.setWidth(width);
                artworkProfile.setHeight(height);
                artworkProfile.setScalingType(ScalingType.NORMALIZE);
                artworkProfile.setReflection(false);
                artworkProfile.setRoundedCorners(false);
                artworkProfile.setPreProcess(true);
                artworkProfile.setQuality(75);
                this.artworkStorageService.saveArtworkProfile(artworkProfile);
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize artwork profile: "+profileName+" ("+metaDataType.name()+","+artworkType.name()+")", e);
        }
    }
}
