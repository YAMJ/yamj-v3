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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.service.metadata.online.OnlineScannerService;
import org.yamj.plugin.api.model.type.ArtworkType;

public final class ArtworkStorageTools {

    protected static final String SOURCE_UPLOAD = "upload";
    private static final String TYPE_MOVIE_SCANNER = "movie_scanner";
    private static final String TYPE_SERIES_SCANNER = "series_scanner";
    private static final String TYPE_PERSON_SCANNER = "person_scanner";

    private ArtworkStorageTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static StorageType getStorageType(ArtworkLocated located) {
        return getStorageType(located.getArtwork());
    }

    public static StorageType getStorageType(Artwork artwork) {
        return getStorageType(artwork.getArtworkType());
    }

    public static StorageType getStorageType(ArtworkProfile profile) {
        return getStorageType(profile.getArtworkType());
    }

    public static StorageType getStorageType(ArtworkType artworkType) {
        return artworkType == ArtworkType.PHOTO ? StorageType.PHOTO : StorageType.ARTWORK;
    }
    
    public static String buildCacheFilename(ArtworkLocated located) {
        return buildCacheFilename(located, null);
    }

    public static String buildCacheFilename(ArtworkLocated located, ArtworkProfile artworkProfile) {
        StringBuilder sb = new StringBuilder();
        
        // 1. video name
        if (located.getArtwork().getVideoData() != null) {
            sb.append(located.getArtwork().getVideoData().getIdentifier());
            if (located.getArtwork().getVideoData().isMovie()) {
                sb.append(".movie.");
            } else {
                sb.append(".episode.");
            }
        } else if (located.getArtwork().getSeason() != null) {
            sb.append(located.getArtwork().getSeason().getIdentifier());
            sb.append(".season.");
        } else if (located.getArtwork().getSeries() != null) {
            sb.append(located.getArtwork().getSeries().getIdentifier());
            sb.append(".series.");
        } else if (located.getArtwork().getPerson() != null) {
            sb.append(located.getArtwork().getPerson().getIdentifier());
            sb.append(".person.");
        } else if (located.getArtwork().getBoxedSet() != null) {
            sb.append(located.getArtwork().getBoxedSet().getIdentifier());
            sb.append(".boxset.");
        } else {
            // should never happen
            sb.append("unknown_");
            sb.append(located.getArtwork().getId());
            sb.append(".");
        }
        
        // 2. artwork type
        sb.append(located.getArtwork().getArtworkType().name().toLowerCase());
        sb.append(".");
        
        // 3. hash code
        if (StringUtils.isBlank(located.getHashCode())) {
            sb.append(located.getId());
        } else {
            sb.append(located.getHashCode());
        }
        sb.append(".");

        // 4. profile and suffix
        if (artworkProfile == null) {
            // it's the original image
            sb.append("original.");
            sb.append(located.getImageType().name().toLowerCase());
        } else {
            // it's a generated image
            sb.append(artworkProfile.getProfileName().toLowerCase());
            sb.append(".");
            sb.append(artworkProfile.getImageType().name().toLowerCase());
        }
        
        return sb.toString();
    }

    public static MetaDataType getMetaDataType(ArtworkLocated located) {
        return getMetaDataType(located.getArtwork());
    }

    public static MetaDataType getMetaDataType(Artwork artwork) { //NOSONAR
        MetaDataType metaDataType = MetaDataType.UNKNOWN;

        final ArtworkType artworkType = artwork.getArtworkType(); 
        switch(artworkType) {
        case PHOTO:
            metaDataType = MetaDataType.PERSON;
            break;
        case VIDEOIMAGE:
            metaDataType = MetaDataType.EPISODE;
            break;
        case BANNER:
            if (artwork.getBoxedSet() != null) {
                metaDataType = MetaDataType.BOXSET;
            } else if (artwork.getSeries() != null) {
                metaDataType = MetaDataType.SERIES;
            } else {
                metaDataType = MetaDataType.SEASON;
            }
            break;
        case POSTER:
        case FANART:
            if (artwork.getBoxedSet() != null) {
                metaDataType = MetaDataType.BOXSET;
            } else if (artwork.getSeries() != null) {
                metaDataType = MetaDataType.SERIES;
            } else if (artwork.getSeason() != null) {
                metaDataType = MetaDataType.SEASON;
            } else {
                metaDataType = MetaDataType.MOVIE;
            }
            break;
        default:
            break;
        }
        
        return metaDataType;
    }

    public static Set<String> determinePriorities(final String configValue, Set<String> allowedForScan) {
        final Set<String> result;
        if (StringUtils.isBlank(configValue)) {
            result = Collections.emptySet();
        } else {
            result = new LinkedHashSet<>();
            for (String config : configValue.toLowerCase().split(",")) {
                final Set<String> checkPrios;
                if (config.equalsIgnoreCase(TYPE_MOVIE_SCANNER)) {
                    checkPrios = OnlineScannerService.MOVIE_SCANNER;
                } else if (config.equalsIgnoreCase(TYPE_SERIES_SCANNER)) {
                    checkPrios = OnlineScannerService.SERIES_SCANNER;
                } else if (config.equalsIgnoreCase(TYPE_PERSON_SCANNER)) {
                    checkPrios = OnlineScannerService.PERSON_SCANNER;
                } else {
                    checkPrios = Collections.singleton(config);
                }
                
                for (String check : checkPrios) {
                    if (allowedForScan.contains(check)) {
                        result.add(check);
                    }
                }
            }
        }
        return result;
    }
}
