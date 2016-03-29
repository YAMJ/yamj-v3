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
package org.yamj.core.config;

import static org.yamj.core.tools.Constants.DEFAULT_SPLITTER;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.JobType;

@Service("configServiceWrapper")
public class ConfigServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigServiceWrapper.class);

    @Autowired
    private ConfigService configService;

    public String getProperty(String key, String defaultValue) {
        return this.configService.getProperty(key, defaultValue);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return this.configService.getBooleanProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        return this.configService.getIntProperty(key, defaultValue);
    }

    public List<String> getPropertyAsList(String key, String defaultValue) {
        return this.configService.getPropertyAsList(key, defaultValue);
    }

    public boolean isLocalArtworkScanEnabled(Artwork artwork) {
        StringBuilder sb = new StringBuilder();
        sb.append("yamj3.artwork.scan.local.");
        addScanArtworkType(artwork, sb);

        return this.configService.getBooleanProperty(sb.toString(), true);
    }

    public boolean isAttachedArtworkScanEnabled(Artwork artwork) {
        StringBuilder sb = new StringBuilder();
        sb.append("yamj3.artwork.scan.attached.");
        addScanArtworkType(artwork, sb);

        return this.configService.getBooleanProperty(sb.toString(), false);
    }

    public boolean isOnlineArtworkScanEnabled(Artwork artwork, List<ArtworkLocated> locatedArtwork) { //NOSONAR
        StringBuilder sb = new StringBuilder();
        sb.append("yamj3.artwork.scan.online.");
        addScanArtworkType(artwork, sb);

        String value = this.configService.getProperty(sb.toString());
        if (StringUtils.isBlank(value)) {
            // defaults to TRUE if nothing present in properties
            return true;
        }
        
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }

        // any other case: check if valid artwork is present

        // check present artwork
        for (ArtworkLocated located : artwork.getArtworkLocated()) {
            if (located.isValid()) {
                return false;
            }
        }
        
        // check newly scanned artwork (from file: may be new or invalid)
        for (ArtworkLocated located : locatedArtwork) {
            if (located.isValid()) {
                return false;
            }
        }

        // do only scan if no valid files are found
        return true;
    }

    private static void addScanArtworkType(Artwork artwork, StringBuilder sb) {
        sb.append(artwork.getArtworkType().name().toLowerCase());

        switch(artwork.getArtworkType()) {
        case BANNER:
            sb.append(".");
            if (artwork.getSeason() != null) {
                sb.append("tvshow.season");
            } else {
                sb.append("tvshow.series");
            }
            break;
        case POSTER:
        case FANART:
            sb.append(".");
            if (artwork.getBoxedSet() != null) {
                sb.append("boxset");
            } else if (artwork.getVideoData() != null) {
                sb.append("movie");
            } else if (artwork.getSeason() != null) {
                sb.append("tvshow.season");
            } else {
                sb.append("tvshow.series");
            }
            break;
        default:
             break;
        }
    }

    public List<String> getArtworkTokens(ArtworkType artworkType) {
        final String configKey = "yamj3.artwork.token." + artworkType.name().toLowerCase();
        final String defaultValue;
        
        switch (artworkType) {
        case POSTER:
            defaultValue = "poster,cover,folder";
            break;
        case FANART:
            defaultValue = "fanart,backdrop,background";
            break;
        case BANNER:
            defaultValue = "banner";
            break;
        case VIDEOIMAGE:
            defaultValue = "videoimage";
            break;
        case PHOTO:
            defaultValue = "photo";
            break;
        default:
            defaultValue = "";
            break;
        }
        
        return Arrays.asList(this.configService.getProperty(configKey, defaultValue).toLowerCase().split(DEFAULT_SPLITTER));
    }
    
    public boolean isCastScanEnabled(final JobType jobType) {
        String key = "yamj3.scan.castcrew." + jobType.name().toLowerCase();
        boolean value = this.configService.getBooleanProperty(key, false);
        LOG.trace("CastCrew scanning for job '{}' is {}", jobType, value?"enabled":"disabled");
        return value;
    }

    public List<String> getSortStripPrefixes() {
        return this.configService.getPropertyAsList(
                        "yamj3.sort.strip.prefixes",
                        "A,An,The,Le,Les,De,Het,Een,El,Los,Las,Der,Die,Das,Ein,Eine");
    }
}
