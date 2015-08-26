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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.database.model.type.ImageType;
import org.yamj.core.tools.CommonTools;

public class ArtworkDetailDTO {

    private final String source;
    private final String url;
    private final String hashCode;
    private final ImageType imageType;
    private String languageCode = null;
    private int rating = -1;

    public ArtworkDetailDTO(String source, String url) {
        this(source, url, null, ImageType.fromString(FilenameUtils.getExtension(url)));
    }

    public ArtworkDetailDTO(String source, String url, ImageType imageType) {
        this(source, url, null, imageType);
    }

    public ArtworkDetailDTO(String source, String url, String hashCode) {
        this(source, url, hashCode, ImageType.fromString(FilenameUtils.getExtension(url)));
    }

    public ArtworkDetailDTO(String source, String url, String hashCode, ImageType imageType) {
        this.source = source;
        this.url = url;
        if (StringUtils.isBlank(hashCode)) {
            this.hashCode = CommonTools.getSimpleHashCode(url);
        } else {
            this.hashCode = hashCode;
        }
        this.imageType = imageType;
    }
    
    public String getSource() {
        return source;
    }

    public String getUrl() {
        return url;
    }

    public String getHashCode() {
        return hashCode;
    }

    public ImageType getImageType() {
        return imageType;
    }
    
    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ArtworkDetailDTO [Source=");
        sb.append(getSource());
        sb.append(", url=");
        sb.append(getUrl());
        sb.append(", hashCode=");
        sb.append(getHashCode());
        sb.append(", imageType=");
        sb.append(getImageType());
        sb.append(", languageCode=");
        sb.append(getLanguageCode());
        sb.append(", rating=");
        sb.append(getRating());
        sb.append("]");
        return sb.toString();
    }
}
