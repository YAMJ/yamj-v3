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
package org.yamj.core.database.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.service.file.StorageType;

@Entity
@Table(name = "artwork",
       uniqueConstraints = @UniqueConstraint(name = "UIX_ARTWORK_NATURALID", columnNames = {"artwork_type", "videodata_id", "season_id", "series_id", "person_id", "boxedset_id"}),
       indexes = {@Index(name = "IX_ARTWORK_TYPE", columnList = "artwork_type"),
                  @Index(name = "IX_ARTWORK_STATUS", columnList = "status")}
)
@SuppressWarnings("unused")
public class Artwork extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;

    @NaturalId(mutable = true)    
    @Type(type = "artworkType")
    @Column(name = "artwork_type", nullable = false)
    private ArtworkType artworkType;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_VIDEODATA"))
    private VideoData videoData;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "season_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_SEASON"))
    private Season season;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_SERIES"))
    private Series series;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "person_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_PHOTO"))
    private Person person;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "boxedset_id", foreignKey = @ForeignKey(name = "FK_ARTWORK_BOXEDSET"))
    private BoxedSet boxedSet;

    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "artwork")
    private Set<ArtworkLocated> artworkLocated = new HashSet<>(0);

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
    
    @Transient
    public StorageType getStorageType() {
        if (this.artworkType == ArtworkType.PHOTO) {
            return StorageType.PHOTO;
        }
        return StorageType.ARTWORK;
    }
    
    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getArtworkType())
                .append(getVideoData())
                .append(getSeason())
                .append(getSeries())
                .append(getPerson())
                .append(getBoxedSet())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Artwork)) {
            return false;
        }
        final Artwork other = (Artwork) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getArtworkType(), other.getArtworkType())
                .append(getVideoData(), other.getVideoData())
                .append(getSeason(), other.getSeason())
                .append(getSeries(), other.getSeries())
                .append(getPerson(), other.getPerson())
                .append(getBoxedSet(), other.getBoxedSet())
                .isEquals();
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
                if (getVideoData().isMovie()) {
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
                sb.append(getPerson().getIdentifier());
                sb.append("'");
            } else {
                sb.append(", target=Person");
            }
        } else if (getBoxedSet() != null) {
            if (Hibernate.isInitialized(getBoxedSet())) {
                sb.append(", boxedset-id='");
                sb.append(getBoxedSet().getIdentifier());
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
