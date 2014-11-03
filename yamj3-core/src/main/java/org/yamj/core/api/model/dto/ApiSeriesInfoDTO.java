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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.yamj.core.database.model.type.ArtworkType;

@JsonInclude(Include.NON_DEFAULT) 
public class ApiSeriesInfoDTO extends AbstractApiDTO {

    private Long seriesId;
    private String title;
    private Integer year;
    private Boolean watched;
    private List<ApiSeasonInfoDTO> seasonList;
    private Map<ArtworkType, List<ApiArtworkDTO>> artwork = new EnumMap<ArtworkType, List<ApiArtworkDTO>>(ArtworkType.class);

    public Long getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(Long seriesId) {
        this.seriesId = seriesId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ApiSeasonInfoDTO> getSeasonList() {
        return seasonList;
    }

    public void setSeasonList(List<ApiSeasonInfoDTO> seasonList) {
        this.seasonList = seasonList;
    }

    public Integer getYear() {
        return year;
    }

    public Boolean getWatched() {
        return watched;
    }
    
    public Map<ArtworkType, List<ApiArtworkDTO>> getArtwork() {
        return artwork;
    }

    public void setYear(Integer seriesYear) {
        this.year = seriesYear;
    }

    public void setSeriesYear(Integer seriesYear) {
        this.year = seriesYear;
    }

    public void setWatched(Boolean watched) {
        this.watched = watched;
    }

    public void setArtwork(Map<ArtworkType, List<ApiArtworkDTO>> artwork) {
        this.artwork = artwork;
    }

    public void addArtwork(ApiArtworkDTO newArtwork) {
        // Add a blank list if it doesn't already exist
        if (!artwork.containsKey(newArtwork.getArtworkType())) {
            artwork.put(newArtwork.getArtworkType(), new ArrayList<ApiArtworkDTO>(1));
        }
        this.artwork.get(newArtwork.getArtworkType()).add(newArtwork);
    }

    public int getArtworkCount() {
        int count = 0;
        for (Map.Entry<ArtworkType, List<ApiArtworkDTO>> entry : artwork.entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }
}
