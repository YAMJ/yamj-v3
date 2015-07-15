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
import org.yamj.core.database.model.type.ArtworkType;

/**
 * @author modmax
 */
@JsonInclude(Include.NON_DEFAULT)
public class ApiBoxedSetDTO extends AbstractApiIdentifiableDTO {

    private String name;
    private Integer memberCount;
    private Boolean watched;
    private List<ApiBoxedSetMemberDTO> members;
    private final Map<ArtworkType, List<ApiArtworkDTO>> artwork = new EnumMap<>(ArtworkType.class);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Boolean getWatched() {
        return watched;
    }

    public void setWatched(Boolean watched) {
        this.watched = watched;
    }

    public List<ApiBoxedSetMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<ApiBoxedSetMemberDTO> members) {
        this.members = members;
    }

    public Map<ArtworkType, List<ApiArtworkDTO>> getArtwork() {
        return artwork;
    }

    public void setArtwork(List<ApiArtworkDTO> artworkList) {
        for (ApiArtworkDTO aadto : artworkList) {
            addArtwork(aadto);
        }
    }
    
    public void addArtwork(ApiArtworkDTO newArtwork) {
        // Add a blank list if it doesn't already exist
        if (!artwork.containsKey(newArtwork.getArtworkType())) {
            artwork.put(newArtwork.getArtworkType(), new ArrayList<ApiArtworkDTO>(1));
        }
        this.artwork.get(newArtwork.getArtworkType()).add(newArtwork);
    }
}
