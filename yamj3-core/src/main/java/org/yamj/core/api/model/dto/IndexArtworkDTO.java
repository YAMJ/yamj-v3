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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.type.ArtworkType;

/**
 *
 * @author Stuart
 */
public class IndexArtworkDTO {

    private String key = null;
    private MetaDataType source;
    private Long videoId = 0L;
    private Long artworkId = 0L;
    private Long locatedId = 0L;
    private Long generatedId = 0L;
    private ArtworkType artworkType;
    private String cacheDir = "";
    private String cacheFilename = "";

    @JsonIgnore
    public String Key() {
        if (StringUtils.isBlank(key)) {
            this.key = makeKey(source, videoId);
        }
        return key;
    }

    @JsonIgnore
    public MetaDataType getSource() {
        return source;
    }

    public void setSource(MetaDataType source) {
        this.source = source;
    }

    public void setSourceString(String source) {
        this.source = MetaDataType.fromString(source);
    }

    @JsonIgnore
    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        if (videoId == null) {
            this.videoId = 0L;
        } else {
            this.videoId = videoId;
        }
    }

    public long getArtworkId() {
        return artworkId;
    }

    public void setArtworkId(Long artworkId) {
        if (artworkId == null) {
            this.artworkId = 0L;
        } else {
            this.artworkId = artworkId;
        }
    }

    public long getLocatedId() {
        return locatedId;
    }

    public void setLocatedId(Long locatedId) {
        if (locatedId == null) {
            this.locatedId = 0L;
        } else {
            this.locatedId = locatedId;
        }
    }

    public long getGeneratedId() {
        return generatedId;
    }

    public void setGeneratedId(Long generatedId) {
        if (generatedId == null) {
            this.generatedId = 0L;
        } else {
            this.generatedId = generatedId;
        }
    }

    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public void setArtworkTypeString(String artworkType) {
        this.artworkType = ArtworkType.fromString(artworkType);
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getCacheFilename() {
        return cacheFilename;
    }

    public void setCacheFilename(String cacheFilename) {
        this.cacheFilename = cacheFilename;
    }

    @JsonIgnore
    public static String makeKey(IndexVideoDTO master) {
        return makeKey(master.getVideoType(), master.getId());
    }

    @JsonIgnore
    public static String makeKey(MetaDataType videoType, long id) {
        StringBuilder key = new StringBuilder();
        key.append(videoType.toString());
        key.append("-");
        key.append(id);
        return key.toString();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
