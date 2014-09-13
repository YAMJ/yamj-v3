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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;

@Entity
@Table(name = "artwork_located",
    uniqueConstraints= @UniqueConstraint(name="UIX_ARTWORKLOCATED_NATURALID", columnNames={"artwork_id", "stagefile_id", "source", "url"})
)
public class ArtworkLocated extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORKLOCATED_ARTWORK")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORKLOCATED_STAGEFILE")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "stagefile_id")
    private StageFile stageFile;

    @Index(name = "IX_ARTWORKLOCATED_DOWNLOAD")
    @Column(name = "source", length = 50)
    private String source;

    @Index(name = "IX_ARTWORKLOCATED_DOWNLOAD")
    @Column(name = "url", length=255)
    private String url;

    @Index(name = "IX_ARTWORKLOCATED_STATUS")
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @Column(name = "hash_code", length = 100)
    private String hashCode;

    @Column(name = "priority", nullable = false)
    private int priority = -1;

    @Column(name = "width", nullable = false)
    private int width = -1;

    @Column(name = "height", nullable = false)
    private int height = -1;

    @Column(name = "language", length=30)
    private String language;

    @Column(name = "rating", nullable=false)
    private int rating = -1;

    @Column(name = "cache_filename",length = 255)
    private String cacheFilename;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "artworkLocated")
    private Set<ArtworkGenerated> generatedArtworks = new HashSet<ArtworkGenerated>(0);

    // GETTER and SETTER

    public Artwork getArtwork() {
        return artwork;
    }

    public void setArtwork(Artwork artwork) {
        this.artwork = artwork;
    }

    public StageFile getStageFile() {
        return stageFile;
    }

    public void setStageFile(StageFile stageFile) {
        this.stageFile = stageFile;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public String getHashCode() {
        return hashCode;
    }

    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getCacheFilename() {
        return cacheFilename;
    }

    public void setCacheFilename(String cacheFilename) {
        this.cacheFilename = cacheFilename;
    }

    public Set<ArtworkGenerated> getGeneratedArtworks() {
        return generatedArtworks;
    }

    public void setGeneratedArtworks(Set<ArtworkGenerated> generatedArtworks) {
        this.generatedArtworks = generatedArtworks;
    }

    // TRANSIENT METHODS
    
    public boolean isValidStatus() {
        if (status == null) {
            return false;
        }
        if (StatusType.DONE.equals(status)) {
            return true;
        }
        if (StatusType.NEW.equals(status)) {
            return true;
        }
        if (StatusType.UPDATED.equals(status)) {
            return true;
        }
        return false;
    }
    
    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (artwork == null ? 0 : artwork.hashCode());
        result = prime * result + (stageFile == null ? 0 : stageFile.hashCode());
        result = prime * result + (source == null ? 0 : source.hashCode());
        result = prime * result + (url == null ? 0 : url.hashCode());
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
        if (!(other instanceof ArtworkLocated)) {
            return false;
        }
        ArtworkLocated castOther = (ArtworkLocated) other;
        // first check the id
        if ((this.getId() > 0) && (castOther.getId() > 0)) {
            return this.getId() == castOther.getId();
        }
        // check source
        if (!StringUtils.equals(source, castOther.source)) {
            return false;
        }
        // check URL
        if (!StringUtils.equals(url, castOther.url)) {
            return false;
        }
        // check artwork
        if (this.artwork == null && castOther.artwork != null) {
            return false;
        }
        if (this.artwork != null && castOther.artwork == null) {
            return false;
        }
        if (this.artwork != null && castOther.artwork != null) {
            if (!this.artwork.equals(castOther.artwork)) {
                return false;
            }
        }
        // check stage file
        if (this.stageFile == null && castOther.stageFile != null) {
            return false;
        }
        if (this.stageFile != null && castOther.stageFile == null) {
            return false;
        }
        if (this.stageFile != null && castOther.stageFile != null) {
            if (!this.stageFile.equals(castOther.stageFile)) {
                return false;
            }
        }
        // all checks passed
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ArtworkLocated [ID=");
        sb.append(getId());
        if (Hibernate.isInitialized(getArtwork())) {
            sb.append(", type=");
            sb.append(getArtwork().getArtworkType());
        }
        if (StringUtils.isNotBlank(getUrl())) {
            sb.append(", source=");
            sb.append(getSource());
            sb.append(", url=");
            sb.append(getUrl());
        } else if (getStageFile() != null) {
            if (Hibernate.isInitialized(getStageFile())) {
                sb.append(", stageFile=");
                sb.append(getStageFile().getFileName());
            } else {
                sb.append(", stage file used");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
