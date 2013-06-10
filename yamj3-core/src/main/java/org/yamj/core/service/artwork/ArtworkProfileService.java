/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.artwork;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.service.ArtworkStorageService;

@Service("artworkProfileService")
public class ArtworkProfileService implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkProfileService.class);

    @Autowired
    private ArtworkStorageService artworkStorageService;

    @Override
    public void afterPropertiesSet() throws Exception {
        String[] defaultProfiles = PropertyTools.getProperty("artwork.scanner.default.profiles", "").split(",");
        if (defaultProfiles.length > 0) {
            for (String defaultProfile : defaultProfiles) {
                String name = defaultProfile;
                boolean valid = true;

                String type = PropertyTools.getProperty("artwork.profile."+name+".type", ArtworkType.UNKNOWN.toString());
                ArtworkType artworkType = ArtworkType.fromString(type);
                if (ArtworkType.UNKNOWN == artworkType) {
                    LOG.warn("Property 'artwork.profile.{}.type' denotes invalid artwork type: {}", name, type);
                    valid = false;
                }

                String width = PropertyTools.getProperty("artwork.profile."+name+".width");
                if (!StringUtils.isNumeric(width)) {
                    LOG.warn("Property 'artwork.profile.{}.width' is not numeric: {}", name, width);
                    valid = false;
                }

                String height = PropertyTools.getProperty("artwork.profile."+name+".height");
                if (!StringUtils.isNumeric(width)) {
                    LOG.warn("Property 'artwork.profile.{}.height' is not numeric: {}", name, height);
                    valid = false;
                }

                boolean preProcess = PropertyTools.getBooleanProperty("artwork.profile."+name+".preProcess", Boolean.FALSE);
                
                if (!valid) {
                    LOG.warn("Profile {} has no valid setup, so skipping", name);
                    continue;
                }

                ArtworkProfile artworkProfile = new ArtworkProfile();
                artworkProfile.setProfileName(name);
                artworkProfile.setArtworkType(artworkType);
                artworkProfile.setWidth(Integer.parseInt(width));
                artworkProfile.setHeight(Integer.parseInt(height));
                artworkProfile.setPreProcess(preProcess);
                
                try {
                    this.artworkStorageService.storeArtworkProfile(artworkProfile);
                } catch (Exception error) {
                    LOG.error("Failed to store artwork profile {}", artworkProfile);
                    LOG.warn("Storage error", error);
                }
            }
        }
    }
    
    public List<ArtworkProfile> getPreProcessProfiles(ArtworkType artworkType) {
        // TODO cache profiles to avoid database access
        return this.artworkStorageService.getArtworkProfiles(artworkType, Boolean.TRUE);
    }
}
