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
package org.yamj.core.api.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.type.ArtworkType;

/**
 * List of the options available for the indexes
 *
 * @author stuart.boston
 */
public class OptionsIndexVideo extends OptionsAbstractSortSearch {

    private String type = "";
    private String include = "";
    private String exclude = "";
    private String artwork = "";
    private Long id = -1L;
    @JsonIgnore
    List<String> artworkTypes = new ArrayList<String>();
    @JsonIgnore
    List<MetaDataType> videoTypes = new ArrayList<MetaDataType>();

    public void setInclude(String include) {
        this.include = include;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public String getInclude() {
        return include;
    }

    public String getExclude() {
        return exclude;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type.toUpperCase();
        this.videoTypes.clear();
    }

    public String getArtwork() {
        return artwork;
    }

    public void setArtwork(String artwork) {
        this.artwork = artwork.toUpperCase();
        this.artworkTypes.clear();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get a list of the artwork types to search for
     *
     * @return
     */
    public List<String> splitArtwork() {
        if (CollectionUtils.isEmpty(artworkTypes)) {
            if (StringUtils.containsIgnoreCase(artwork, "ALL")) {
                // Add all the types to the list
                for (ArtworkType at : ArtworkType.values()) {
                    artworkTypes.add(at.toString());
                }
                // Remove the unknown type
                artworkTypes.remove(ArtworkType.UNKNOWN.toString());
            } else {
                for (String param : StringUtils.split(artwork, ",")) {
                    // Validate that the string passed is a correct artwork type
                    ArtworkType at = ArtworkType.fromString(param);
                    if (at != ArtworkType.UNKNOWN) {
                        artworkTypes.add(at.toString());
                    }
                }
            }
        }
        return artworkTypes;
    }

    /**
     * Get a list of the video types to search for
     * @return
     */
    public List<MetaDataType> splitTypes() {
        if (CollectionUtils.isEmpty(videoTypes)) {
            if (StringUtils.containsIgnoreCase(type, "ALL") || StringUtils.isEmpty(type)) {
                videoTypes.add(MetaDataType.MOVIE);
                videoTypes.add(MetaDataType.SERIES);
                videoTypes.add(MetaDataType.SEASON);
            } else {
                for (String param : StringUtils.split(type, ",")) {
                    // Validate that the string passed is a correct artwork type
                    MetaDataType mdt = MetaDataType.fromString(param);
                    if (mdt != MetaDataType.UNKNOWN) {
                        videoTypes.add(mdt);
                    }
                }
            }
        }
        return videoTypes;
    }

    /**
     * Split the include list into a map of values
     */
    public Map<String, String> splitIncludes() {
        return splitDashList(include);
    }

    /**
     * Split the exclude list into a map of values
     */
    public Map<String, String> splitExcludes() {
        return splitDashList(exclude);
    }
}
