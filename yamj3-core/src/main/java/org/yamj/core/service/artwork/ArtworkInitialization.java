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
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.ScalingType;
import org.yamj.core.database.service.ArtworkStorageService;

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
        initArtworkProfile("default-fanart", ArtworkType.FANART, 1280, 720, true, true, true, true, true); 
        initArtworkProfile("default-poster", ArtworkType.POSTER, 224, 332, true, true, true, true, true); 
        initArtworkProfile("default-banner", ArtworkType.BANNER, 650, 120, true, false, true, true, true);
        initArtworkProfile("default-videoimage", ArtworkType.VIDEOIMAGE, 400, 225, true, false, false, false, false);
        initArtworkProfile("default-photo", ArtworkType.PHOTO, 200, 300, true, false, false, false, false);
        initArtworkProfile("demand", ArtworkType.FANART, 1280, 720, false, true, true, true, true);
        initArtworkProfile("demand", ArtworkType.POSTER, 224, 332, false, true, true, true, true); 
        initArtworkProfile("demand", ArtworkType.BANNER, 650, 120, false, false, true, true, true);
        initArtworkProfile("demand", ArtworkType.VIDEOIMAGE, 400, 225, false, false, false, false, false);
        initArtworkProfile("demand", ArtworkType.PHOTO, 200, 300, false, false, false, false, false);
    }
    
    private void initArtworkProfile(String profileName, ArtworkType artworkType, int width, int height, boolean preProcess,
        boolean applyToMovie, boolean applyToSeries, boolean applyToSeason, boolean applyToBoxedSet)
    {
        try {
            ArtworkProfile artworkProfile = artworkStorageService.getArtworkProfile(profileName, artworkType);
            if (artworkProfile == null) {
                artworkProfile = new ArtworkProfile();
                artworkProfile.setProfileName(profileName);
                artworkProfile.setArtworkType(artworkType);
                artworkProfile.setWidth(width);
                artworkProfile.setHeight(height);
                artworkProfile.setApplyToMovie(applyToMovie);
                artworkProfile.setApplyToSeries(applyToSeries);
                artworkProfile.setApplyToSeason(applyToSeason);
                artworkProfile.setApplyToBoxedSet(applyToBoxedSet);
                artworkProfile.setScalingType(ScalingType.NORMALIZE);
                artworkProfile.setReflection(false);
                artworkProfile.setRoundedCorners(false);
                artworkProfile.setPreProcess(preProcess);
                artworkProfile.setQuality(75);
                this.artworkStorageService.saveArtworkProfile(artworkProfile);
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize artwork profile: "+profileName+" ("+artworkType.name()+")", e);
        }
    }
}
