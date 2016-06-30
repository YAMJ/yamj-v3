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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.yamj.plugin.api.model.type.ArtworkType;

@JsonInclude(Include.NON_DEFAULT)
public abstract class AbstractMetaDataDTO extends AbstractApiIdentifiableDTO {

    private String title;
    private String originalTitle;
    private Integer year = Integer.valueOf(-1);
    private String plot;
    private String outline;
    private Boolean watched;

    public AbstractMetaDataDTO() {}
    
    public AbstractMetaDataDTO(Long id) {
        super(id);
    }
    
    private Map<ArtworkType, List<ApiArtworkDTO>> artwork = new EnumMap<>(ArtworkType.class);

    public final String getTitle() {
        return title;
    }

    public final void setTitle(String title) {
        this.title = title;
    }
    
    public final String getOriginalTitle() {
        return originalTitle;
    }

    public final void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public final Integer getYear() {
        return year;
    }

    public final void setYear(Integer year) {
        this.year = year;
    }

    public final String getPlot() {
        return plot;
    }

    public final void setPlot(String plot) {
        this.plot = plot;
    }

    public final String getOutline() {
        return outline;
    }

    public final void setOutline(String outline) {
        this.outline = outline;
    }

    public final Boolean getWatched() {
        return watched;
    }

    public final void setWatched(Boolean watched) {
        this.watched = watched;
    }


    public final Map<ArtworkType, List<ApiArtworkDTO>> getArtwork() {
        return artwork;
    }

    public final void setArtwork(Map<ArtworkType, List<ApiArtworkDTO>> artwork) {
        this.artwork = artwork;
    }

    public final void addArtwork(List<ApiArtworkDTO> artworkList) {
        if (CollectionUtils.isNotEmpty(artworkList)) {
            for (ApiArtworkDTO aadto : artworkList) {
                addArtwork(aadto);
            }
        }
    }

    public final void addArtwork(ApiArtworkDTO newArtwork) {
        // Add a blank list if it doesn't already exist
        if (!artwork.containsKey(newArtwork.getArtworkType())) {
            artwork.put(newArtwork.getArtworkType(), new ArrayList<ApiArtworkDTO>(1));
        }
        this.artwork.get(newArtwork.getArtworkType()).add(newArtwork);
    }

    public final int getArtworkCount() {
        int count = 0;
        for (Map.Entry<ArtworkType, List<ApiArtworkDTO>> entry : artwork.entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }
}
