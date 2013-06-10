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
package org.yamj.core.database.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Parameter;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.hibernate.usertypes.EnumStringUserType;

@TypeDefs({
    @TypeDef(name = "artworkType", typeClass = EnumStringUserType.class,
            parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.ArtworkType")}),
    @TypeDef(name = "statusType", typeClass = EnumStringUserType.class,
            parameters = { @Parameter(name = "enumClassName", value = "org.yamj.common.type.StatusType")})
})

@Entity
@Table(name = "artwork")
public class Artwork extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;

    @Index(name = "IX_ARTWORK_TYPE")
    @Type(type = "artworkType")
    @Column(name = "artwork_type", nullable = false)
    private ArtworkType artworkType;

    @Index(name = "IX_ARTWORK_STATUS")
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_VIDEODATA")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id")
    private VideoData videoData;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_SEASON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "season_id")
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_SERIES")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id")
    private Series series;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "artwork")
    private List<ArtworkLocated> reservoirs = new ArrayList<ArtworkLocated>(0);

    // GETTER and SETTER

    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    // TRANSIENT METHODS
    
    public IMetadata getMetadata() {
        if (getVideoData() != null) {
            return getVideoData();
        }
        if (getSeason() != null) {
            return getSeason() ;
        }
        if (getSeries() != null) {
            return getSeries();
        }
        
        return null;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Artwork [ID=");
        sb.append(getId());
        sb.append(", type=");
        sb.append(getArtworkType());
        sb.append(", destination=");
        if (getVideoData() != null) {
            sb.append("VideoData");
        } else if (getSeason() != null) {
            sb.append("Season");
        } else if (getSeries() != null) {
            sb.append("Series");
        } else {
            sb.append("Unknown");
        }
        sb.append("]");
        return sb.toString();
    }
}
