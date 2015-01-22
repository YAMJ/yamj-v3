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
package org.yamj.core.configuration;

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

        return this.configService.getBooleanProperty(sb.toString(), Boolean.TRUE);
    }

    public boolean isOnlineArtworkScanEnabled(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        StringBuilder sb = new StringBuilder();
        sb.append("yamj3.artwork.scan.online.");
        addScanArtworkType(artwork, sb);

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

    private static void addScanArtworkType(Artwork artwork, StringBuilder sb) {
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

    public boolean isCastScanEnabled(JobType jobType) {
        if (jobType == null) {
            return false;
        }

        String key = "yamj3.scan.castcrew." + jobType.name().toLowerCase();
        boolean value = this.configService.getBooleanProperty(key, Boolean.FALSE);

        if (LOG.isTraceEnabled()) {
            if (value) {
                LOG.trace("CastCrew scanning for job '{}' is enabled", jobType);
            } else {
                LOG.trace("CastCrew scanning for job '{}' is disabled", jobType);
            }
        }

        return value;
    }

    public List<String> getCertificationCountries() {
        return this.configService.getPropertyAsList("yamj3.certification.countries", "Germany,France,UK,USA");
    }

    public boolean isCertificationCountryAllowed(String country) {
        if (StringUtils.isBlank(country)) {
            return false;
        }
        for (String check : getCertificationCountries()) {
            if (StringUtils.equalsIgnoreCase(country, check)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getSortStripPrefixes() {
        return this.configService.getPropertyAsList(
                        "yamj3.sort.strip.prefixes",
                        "A,An,The,Le,Les,De,Het,Een,El,Los,Las,Der,Die,Das,Ein,Eine");
    }
}
