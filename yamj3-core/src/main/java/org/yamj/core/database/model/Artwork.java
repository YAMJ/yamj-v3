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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.yamj.common.tools.EqualityTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.ArtworkType;

@Entity
@Table(name = "artwork",
    uniqueConstraints= @UniqueConstraint(name="UIX_ARTWORK_NATURALID", columnNames={"artwork_type","videodata_id","season_id","series_id","person_id","boxedset_id"})
)
@SuppressWarnings("unused")
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

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_BOXEDSET")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "boxedset_id")
    private BoxedSet boxedSet;
    
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

    public BoxedSet getBoxedSet() {
        return boxedSet;
    }

    public void setBoxedSet(BoxedSet boxedSet) {
        this.boxedSet = boxedSet;
    }

    private void setArtworkLocated(Set<ArtworkLocated> artworkLocated) {
        this.artworkLocated = artworkLocated;
    }

    public Set<ArtworkLocated> getArtworkLocated() {
        return artworkLocated;
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
        result = prime * result + (getArtworkType() == null ? 0 : getArtworkType().hashCode());
        result = prime * result + (getVideoData() == null ? 0 : getVideoData().hashCode());
        result = prime * result + (getSeason() == null ? 0 : getSeason().hashCode());
        result = prime * result + (getSeries() == null ? 0 : getSeries().hashCode());
        result = prime * result + (getPerson() == null ? 0 : getPerson().hashCode());
        result = prime * result + (getBoxedSet() == null ? 0 : getBoxedSet().hashCode());
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
        if ((getId() > 0) && (castOther.getId() > 0)) {
            return getId() == castOther.getId();
        }
        // check artwork type
        if (getArtworkType() != castOther.getArtworkType()) {
            return false;
        }
        // check video data
        if (EqualityTools.notEquals(getVideoData(), castOther.getVideoData())) {
            return false;
        }
        // check season
        if (EqualityTools.notEquals(getSeason(), castOther.getSeason())) {
            return false;
        }
        // check series
        if (EqualityTools.notEquals(getSeries(), castOther.getSeries())) {
            return false;
        }
        // check series
        if (EqualityTools.notEquals(getPerson(), castOther.getPerson())) {
            return false;
        }
        // check boxed set
        return EqualityTools.equals(getBoxedSet(), castOther.getBoxedSet());
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
        } else if (getBoxedSet() != null) {
            if (Hibernate.isInitialized(getBoxedSet())) {
                sb.append(", boxedset-id='");
                sb.append(getBoxedSet().getId());
                sb.append("'");
            } else {
                sb.append(", target=BoxedSet");
            }
        } else {
            sb.append("Unknown");
        }
        sb.append("]");
        return sb.toString();
    }
}
