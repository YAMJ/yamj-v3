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
package org.yamj.core.configuration;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.type.ArtworkType;

@Service("configServiceWrapper")
public class ConfigServiceWrapper {

    @Autowired
    private ConfigService configService;

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
        StringBuffer sb = new StringBuffer();
        sb.append("yamj3.artwork.scan.local.");
        this.addScanArtworkType(artwork, sb);
        
        return this.configService.getBooleanProperty(sb.toString(), Boolean.TRUE);
    }

    public boolean isOnlineArtworkScanEnabled(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        StringBuffer sb = new StringBuffer();
        sb.append("yamj3.artwork.scan.online.");
        this.addScanArtworkType(artwork, sb);
        
        String value = this.configService.getProperty(sb.toString());
        if (StringUtils.isBlank(value)) {
            // default: true
            return true;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }
         
        // any other case: check if valid artwork is present

        // check present artworks
        for (ArtworkLocated located : artwork.getArtworkLocated()) {
            if (located.isValidStatus()) {
                return false;
            }
        }
        // check newly scanned artworks (from file: may be new or invalid)
        for (ArtworkLocated located : locatedArtworks) {
            if (located.isValidStatus()) {
                return false;
            }
        }
        
        // do only scan if no valid files are found
        return true;
    }

    private void addScanArtworkType(Artwork artwork, StringBuffer sb) {
        sb.append(artwork.getArtworkType().name().toLowerCase());
        
        if (ArtworkType.POSTER == artwork.getArtworkType() || ArtworkType.FANART == artwork.getArtworkType()) {
            sb.append(".");
            if (artwork.getVideoData() != null) {
                sb.append("movie");
            } else if (artwork.getSeason() != null) {
                sb.append("tvshow.season");
            } else {
                sb.append("tvshow.series");
            }
        } else if (ArtworkType.BANNER == artwork.getArtworkType()) {
            sb.append(".");
            if (artwork.getSeason() != null) {
                sb.append("tvshow.season");
            } else {
                sb.append("tvshow.series");
            }
        }
    }
}
