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

import javax.persistence.UniqueConstraint;

import org.yamj.common.type.StatusType;
import javax.persistence.Column;
import org.hibernate.annotations.Type;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.hibernate.usertypes.EnumStringUserType;
import java.io.Serializable;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Parameter;

@TypeDefs({
    @TypeDef(name = "artworkType", typeClass = EnumStringUserType.class,
            parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.ArtworkType")}),
    @TypeDef(name = "statusType", typeClass = EnumStringUserType.class,
            parameters = { @Parameter(name = "enumClassName", value = "org.yamj.common.type.StatusType")})
})

@Entity
@Table(name = "artwork",
    uniqueConstraints= @UniqueConstraint(name="UIX_ARTWORK_NATURALID", columnNames={"artwork_type", "videodata_id", "season_id", "series_id"})
)
public class Artwork extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;

    @NaturalId
    @Type(type = "artworkType")
    @Column(name = "artwork_type", nullable = false)
    private ArtworkType artworkType;

    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @Column(name = "url")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_STAGEFILE")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "stagefile_id")
    private StageFile stageFile;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_VIDEODATA")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id")
    private VideoData videoData;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_SEASON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "season_id")
    private Season season;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_SERIES")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id")
    private Series series;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public StageFile getStageFile() {
        return stageFile;
    }

    public void setStageFile(StageFile stageFile) {
        this.stageFile = stageFile;
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
