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
import org.springframework.stereotype.Component;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.service.ArtworkStorageService;

/**
 * Just used for initialization of artwork profiles at startup.
 */
@Component("artworkInitialization")
public class ArtworkInitialization {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkInitialization.class);
    
    @Autowired
    private ArtworkStorageService artworkStorageService;

    @PostConstruct
    public void init() {
        LOG.debug("Initialize artwork profiles");
        initArtworkProfile("default-fanart", ArtworkType.FANART, 1280, 720,
                        true, true, true, false, false, true, // apply
                        true, false, false, true, false); // processing 
        initArtworkProfile("default-poster", ArtworkType.POSTER, 224, 332,
                        true, true, true, false, false, true, // apply
                        true, false, false, true, false); // processing 
        initArtworkProfile("default-banner", ArtworkType.BANNER, 650, 120,
                        false, true, true, false, false, true, // apply
                        true, false, false, true, false); // processing 
        initArtworkProfile("default-videoimage", ArtworkType.VIDEOIMAGE, 400, 225,
                        false, false, false, true, false, false, // apply
                        true, false, false, true, false); // processing 
        initArtworkProfile("default-photo", ArtworkType.PHOTO, 200, 300,
                        false, false, false, false, true, false, // apply
                        true, false, false, true, false); // processing 
    }
    
    private void initArtworkProfile(String profileName, ArtworkType artworkType, int width, int height,
        boolean applyToMovie, boolean applyToSeries, boolean applyToSeason, boolean applyToEpisode, boolean applyToPerson, boolean applyToBoxedSet,
        boolean preProcess, boolean roundedCorners, boolean reflection, boolean normalize, boolean stretch)
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
                artworkProfile.setApplyToEpisode(applyToEpisode);
                artworkProfile.setApplyToPerson(applyToPerson);
                artworkProfile.setApplyToBoxedSet(applyToBoxedSet);
                artworkProfile.setPreProcess(preProcess);
                artworkProfile.setRoundedCorners(roundedCorners);
                artworkProfile.setReflection(reflection);
                artworkProfile.setNormalize(normalize);
                artworkProfile.setStretch(stretch);
                this.artworkStorageService.saveArtworkProfile(artworkProfile);
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize artwork profile: "+profileName+" ("+artworkType.name()+")", e);
        }
    }

}
