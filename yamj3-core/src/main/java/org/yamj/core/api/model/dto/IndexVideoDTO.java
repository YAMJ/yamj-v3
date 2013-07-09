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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.type.ArtworkType;

/**
 *
 * @author stuart.boston
 */
public class IndexVideoDTO extends AbstractIndexDTO {

    private MetaDataType videoType;
    private String title;
    private Integer videoYear;
    private Set<IndexGenreDTO> genres = new HashSet<IndexGenreDTO>();
    Map<ArtworkType, List<IndexArtworkDTO>> artwork = new EnumMap<ArtworkType, List<IndexArtworkDTO>>(ArtworkType.class);

    public IndexVideoDTO() {
        for(ArtworkType at: ArtworkType.values()) {
            artwork.put(at, new ArrayList<IndexArtworkDTO>(0));
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Getter Methods">
    public MetaDataType getVideoType() {
        return videoType;
    }

    public String getTitle() {
        return title;
    }

    public Integer getVideoYear() {
        return videoYear;
    }

    public Map<ArtworkType, List<IndexArtworkDTO>> getArtwork() {
        return artwork;
    }

    public int getArtworkCount() {
        return artwork.size();
    }

    public Set<IndexGenreDTO> getGenres() {
        return genres;
    }

    public int getGenreCount() {
        return genres.size();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Setter Methods">
    public void setVideoType(MetaDataType videoType) {
        this.videoType = videoType;
    }

    public void setVideoTypeString(String videoType) {
        this.videoType = MetaDataType.fromString(videoType);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVideoYear(Integer videoYear) {
        this.videoYear = videoYear;
    }

    public void setArtwork(Set<IndexArtworkDTO> artworkList) {
        for(IndexArtworkDTO ia:artworkList) {
            this.artwork.get(ia.getArtworkType()).add(ia);
        }
    }

    public void setGenres(Set<IndexGenreDTO> genres) {
        this.genres = genres;
    }
    //</editor-fold>

    public void addArtwork(IndexArtworkDTO newArtwork) {
        this.artwork.get(newArtwork.getArtworkType()).add(newArtwork);
    }
}
