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
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.ImageType;

@Entity
@Table(name = "artwork_located",
       uniqueConstraints = @UniqueConstraint(name = "UIX_ARTWORKLOCATED_NATURALID", columnNames = {"artwork_id", "stagefile_id", "source", "url"}),
       indexes = {@Index(name = "IX_ARTWORKLOCATED_DOWNLOAD", columnList = "source,url"),
                  @Index(name = "IX_ARTWORKLOCATED_STATUS", columnList = "status")}
)
public class ArtworkLocated extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "artwork_id", nullable = false, foreignKey = @ForeignKey(name = "FK_ARTWORKLOCATED_ARTWORK"))
    private Artwork artwork;

    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "stagefile_id", foreignKey = @ForeignKey(name = "FK_ARTWORKLOCATED_STAGEFILE"))
    private StageFile stageFile;

    // only used for equality checks
    @Column(name = "stagefile_id", insertable = false, updatable = false)
    private Long stageFileId;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "url", length = 2000)
    private String url;

    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @Type(type = "statusType")
    @Column(name = "previous_status", length = 30)
    private StatusType previousStatus;

    @Column(name = "hash_code", length = 100)
    private String hashCode;

    @Column(name = "priority", nullable = false)
    private int priority = -1;

    @Column(name = "width", nullable = false)
    private int width = -1;

    @Column(name = "height", nullable = false)
    private int height = -1;

    @Column(name = "language_code", length = 4)
    private String languageCode;

    @Column(name = "rating", nullable = false)
    private int rating = -1;

    @Type(type = "imageType")
    @Column(name = "image_type", nullable = false, length = 4)
    private ImageType imageType;

    @Column(name = "cache_filename", length = 255)
    private String cacheFilename;

    @Column(name = "cache_dir", length = 50)
    private String cacheDirectory;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "artworkLocated")
    private Set<ArtworkGenerated> generatedArtworks = new HashSet<>(0);

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
        setStageFileId(stageFile == null ? null : stageFile.getId());
    }

    private Long getStageFileId() {
        return stageFileId;
    }

    private void setStageFileId(Long stageFileId) {
        this.stageFileId = stageFileId;
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
        if (StatusType.DELETED.equals(status)) {
            setPreviousStatus(this.status);
        } else {
            setPreviousStatus(null);
        }
        this.status = status;
    }

    public StatusType getPreviousStatus() {
        return previousStatus;
    }

    private void setPreviousStatus(StatusType previousStatus) {
        this.previousStatus = previousStatus;
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

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    public String getCacheFilename() {
        return cacheFilename;
    }

    public void setCacheFilename(String cacheFilename) {
        this.cacheFilename = cacheFilename;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
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
        return StatusType.DONE.equals(status) || StatusType.NEW.equals(status) || StatusType.UPDATED.equals(status);
    }

    public boolean isCached() {
        return StringUtils.isNotBlank(cacheFilename) && StringUtils.isNotBlank(cacheDirectory);
    }

    public boolean isNotCached() {
        return StringUtils.isBlank(cacheFilename) || StringUtils.isBlank(cacheDirectory);
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getArtwork())
                .append(getStageFileId())
                .append(getSource())
                .append(getUrl())
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
        if (!(obj instanceof ArtworkLocated)) {
            return false;
        }
        final ArtworkLocated other = (ArtworkLocated) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getSource(), other.getSource())
                .append(getUrl(), other.getUrl())
                .append(getArtwork(), other.getArtwork())
                .append(getStageFileId(), other.getStageFileId())
                .isEquals();
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
                sb.append(", stageFileId=");
                sb.append(getStageFileId());
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
