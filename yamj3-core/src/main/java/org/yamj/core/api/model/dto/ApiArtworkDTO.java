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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.type.ArtworkType;

/**
 *
 * @author Stuart
 */
@JsonInclude(Include.NON_DEFAULT)
public class ApiArtworkDTO extends AbstractApiIdentifiableDTO {

    private String key = null;
    private MetaDataType source;
    private Long artworkId;
    private Long locatedId;
    private Long generatedId;
    private ArtworkType artworkType;
    private String cacheDir = "";
    private String cacheFilename = "";
    private String filename = "";

    public MetaDataType getSource() {
        return source;
    }

    public Long getArtworkId() {
        return artworkId;
    }

    public Long getLocatedId() {
        return locatedId;
    }

    public Long getGeneratedId() {
        return generatedId;
    }

    public ArtworkType getArtworkType() {
        return artworkType;
    }

    /**
     * This is the cache directory from the database.
     *
     * It will not be output directly, it will be part of the "filename"
     *
     * @return
     */
    @JsonIgnore
    public String getCacheDir() {
        return cacheDir;
    }

    /**
     * This is the cache filename from the database.
     *
     * It will not be output directly, it will be part of the "filename"
     *
     * @return
     */
    @JsonIgnore
    public String getCacheFilename() {
        return cacheFilename;
    }

    public String getFilename() {
        if (StringUtils.isBlank(this.filename)) {
            this.filename = FilenameUtils.normalize(FilenameUtils.concat(this.cacheDir, this.cacheFilename), Boolean.TRUE);
        }
        return filename;
    }

    public void setSource(String source) {
        this.source = MetaDataType.fromString(source);
    }

    public void setArtworkId(Long artworkId) {
        this.artworkId = artworkId;
    }

    public void setLocatedId(Long locatedId) {
        this.locatedId = locatedId;
    }

    public void setGeneratedId(Long generatedId) {
        this.generatedId = generatedId;
    }

    public void setArtworkType(String artworkType) {
        this.artworkType = ArtworkType.fromString(artworkType);
        if (this.artworkType == ArtworkType.VIDEOIMAGE) {
            this.source = MetaDataType.EPISODE;
        }
    }

    public void setCacheDir(String cacheDir) {
        if (StringUtils.isBlank(cacheDir)) {
            this.cacheDir = "";
        } else {
            this.cacheDir = cacheDir;
        }
    }

    public void setCacheFilename(String cacheFilename) {
        if (StringUtils.isBlank(cacheFilename)) {
            this.cacheFilename = "";
        } else {
            this.cacheFilename = cacheFilename;
        }
    }

    /**
     * Set the videodata ID.
     *
     * This will be populated to the sourceId with the source of "MOVIE"
     *
     * @param videodataId
     */
    public void setVideodataId(Long videodataId) {
        // Only set if the id is not null
        if (videodataId != null) {
            setId(videodataId);
            // Only overwrite the source if it is null
            if (this.source == null) {
                this.source = MetaDataType.MOVIE;
            }
        }
    }

    /**
     * Set the series ID.
     *
     * This will be populated to the sourceId with the source of "SERIES"
     *
     * @param seriesId
     */
    public void setSeriesId(Long seriesId) {
        // Only set if the id is not null
        if (seriesId != null) {
            setId(seriesId);
            this.source = MetaDataType.SERIES;
        }
    }

    /**
     * Set the season ID.
     *
     * This will be populated to the sourceId with the source of "SEASON"
     *
     * @param seasonId
     */
    public void setSeasonId(Long seasonId) {
        // Only set if the id is not null
        if (seasonId != null) {
            setId(seasonId);
            this.source = MetaDataType.SEASON;
        }
    }

    /**
     * Set the person ID
     *
     * This will be populated to the sourceId with the source of "PERSON"
     *
     * @param personId
     */
    public void setPersonId(Long personId) {
        // Only set if the id is not null
        if (personId != null) {
            setId(personId);
            this.source = MetaDataType.PERSON;
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Artwork key methods">
    @JsonIgnore
    public String key() {
        if (StringUtils.isBlank(key)) {
            this.key = makeKey(source, getId());
        }
        return key;
    }

    @JsonIgnore
    public static String makeKey(ApiVideoDTO master) {
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
    //</editor-fold>
}
