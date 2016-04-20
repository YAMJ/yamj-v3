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
package org.yamj.plugin.api.artwork.dto;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.yamj.plugin.api.artwork.tools.ArtworkTools;
import org.yamj.plugin.api.type.ImageType;

public class ArtworkDTO {

    private final String url;
    private final String hashCode;
    private final ImageType imageType;
    private String language = null;
    private int rating = -1;

    public ArtworkDTO(String url) {
        this(url, null, ImageType.fromString(FilenameUtils.getExtension(url)));
    }

    public ArtworkDTO(String url, ImageType imageType) {
        this(url, null, imageType);
    }

    public ArtworkDTO(String url, String hashCode) {
        this(url, hashCode, ImageType.fromString(FilenameUtils.getExtension(url)));
    }

    public ArtworkDTO(String url, String hashCode, ImageType imageType) {
        this.url = url;
        if (StringUtils.isBlank(hashCode)) {
            this.hashCode = ArtworkTools.getSimpleHashCode(url);
        } else {
            this.hashCode = hashCode;
        }
        this.imageType = imageType;
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
    
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
