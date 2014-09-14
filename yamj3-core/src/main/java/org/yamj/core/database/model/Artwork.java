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
package org.yamj.core.database.model;

import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.NaturalId;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.ArtworkType;

@Entity
@Table(name = "artwork",
    uniqueConstraints= @UniqueConstraint(name="UIX_ARTWORK_NATURALID", columnNames={"artwork_type","videodata_id","season_id","series_id","person_id"})
)
public class Artwork extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;

    @NaturalId(mutable = true)
    @Index(name = "IX_ARTWORK_TYPE")
    @Type(type = "artworkType")
    @Column(name = "artwork_type", nullable = false)
    private ArtworkType artworkType;
    
    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_VIDEODATA")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id")
    private VideoData videoData;
    
    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_SEASON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "season_id")
    private Season season;
    
    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_SERIES")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id")
    private Series series;
    
    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_PHOTO")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "person_id")
    private Person person;
    
    @Index(name = "IX_ARTWORK_STATUS")
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "artwork")
    private Set<ArtworkLocated> artworkLocated = new HashSet<ArtworkLocated>(0);

    // GETTER and SETTER
    
    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
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

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Set<ArtworkLocated> getArtworkLocated() {
        return artworkLocated;
    }

    public void addArtworkLocated(ArtworkLocated artworkLocated) {
        this.artworkLocated.add(artworkLocated);
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    // TRANSIENT METHODS
    
    public IMetadata getMetadata() {
        if (getVideoData() != null) {
            return getVideoData();
        }

        if (getSeason() != null) {
            return getSeason();
        }

        if (getSeries() != null) {
            return getSeries();
        }

        return null;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (artworkType == null ? 0 : artworkType.hashCode());
        result = prime * result + (videoData == null ? 0 : videoData.hashCode());
        result = prime * result + (season == null ? 0 : season.hashCode());
        result = prime * result + (season == null ? 0 : season.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Artwork)) {
            return false;
        }
        Artwork castOther = (Artwork) other;
        // first check the id
        if ((this.getId() > 0) && (castOther.getId() > 0)) {
            return this.getId() == castOther.getId();
        }
        // check artwork type
        if (this.artworkType != castOther.artworkType) {
            return false;
        }
        // check video data
        if (this.videoData == null && castOther.videoData != null) {
            return false;
        }
        if (this.videoData != null && castOther.videoData == null) {
            return false;
        }
        if (this.videoData != null && castOther.videoData != null) {
            if (!this.videoData.equals(castOther.videoData)) {
                return false;
            }
        }
        // check season
        if (this.season == null && castOther.season != null) {
            return false;
        }
        if (this.season != null && castOther.season == null) {
            return false;
        }
        if (this.season != null && castOther.season != null) {
            if (!this.season.equals(castOther.season)) {
                return false;
            }
        }
        // check series
        if (this.series == null && castOther.series != null) {
            return false;
        }
        if (this.series != null && castOther.series == null) {
            return false;
        }
        if (this.series != null && castOther.series != null) {
            if (!this.series.equals(castOther.series)) {
                return false;
            }
        }
        // check person photo
        if (this.person == null && castOther.person != null) {
            return false;
        }
        if (this.person != null && castOther.person == null) {
            return false;
        }
        if (this.person != null && castOther.person != null) {
            if (!this.person.equals(castOther.person)) {
                return false;
            }
        }
        // all checks passed
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Artwork [ID=");
        sb.append(getId());
        sb.append(", type=");
        sb.append(getArtworkType());
        if (getVideoData() != null) {
            if (Hibernate.isInitialized(getVideoData())) {
                if (getVideoData().getEpisode() < 0) {
                    sb.append(", movie-id='");
                    sb.append(getVideoData().getIdentifier());
                    sb.append("'");
                } else {
                    sb.append(", episode-id='");
                    sb.append(getVideoData().getIdentifier());
                    sb.append("', episode=");
                    sb.append(getVideoData().getEpisode());
                }
            } else {
                sb.append(", target=VideoData");
            }
        } else if (getSeason() != null) {
            if (Hibernate.isInitialized(getSeason())) {
                sb.append(", season-id='");
                sb.append(getSeason().getIdentifier());
                sb.append("', season=");
                sb.append(getSeason().getSeason());
            } else {
                sb.append(", target=Season");
            }
        } else if (getSeries() != null) {
            if (Hibernate.isInitialized(getSeries())) {
                sb.append(", series-id='");
                sb.append(getSeries().getIdentifier());
                sb.append("'");
            } else {
                sb.append(", target=Series");
            }
        } else if (getPerson() != null) {
            if (Hibernate.isInitialized(getPerson())) {
                sb.append(", person-id='");
                sb.append(getPerson().getId());
                sb.append("'");
            } else {
                sb.append(", target=Person");
            }
        } else {
            sb.append("Unknown");
        }
        sb.append("]");
        return sb.toString();
    }
}
