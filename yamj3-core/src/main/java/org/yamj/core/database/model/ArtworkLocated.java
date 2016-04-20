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

import org.yamj.plugin.api.type.ImageType;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;

@NamedQueries({    
    @NamedQuery(name = ArtworkLocated.QUERY_REQUIRED,
        query = "FROM ArtworkLocated loc JOIN FETCH loc.artwork art LEFT OUTER JOIN FETCH art.videoData LEFT OUTER JOIN FETCH art.season "+
                "LEFT OUTER JOIN FETCH art.series LEFT OUTER JOIN FETCH art.person LEFT OUTER JOIN FETCH art.boxedSet "+
                "LEFT OUTER JOIN FETCH loc.stageFile WHERE loc.id=:id"
    ),
    @NamedQuery(name = ArtworkLocated.QUERY_FOR_DELETION,
        query = "SELECT al.id FROM ArtworkLocated al WHERE al.status = 'DELETED'"
    ),
    @NamedQuery(name = ArtworkLocated.UPDATE_STATUS,
        query = "UPDATE ArtworkLocated SET status=:status WHERE id=:id"
    )
})

@Entity
@Table(name = "artwork_located",
       uniqueConstraints = @UniqueConstraint(name = "UIX_ARTWORKLOCATED_NATURALID", columnNames = {"artwork_id", "source", "hash_code"}),
       indexes = @Index(name = "IX_ARTWORKLOCATED_STATUS", columnList = "status")
)
public class ArtworkLocated extends AbstractStatefulPrev {

    private static final long serialVersionUID = -981494909436217076L;
    public static final String QUERY_REQUIRED = "artworkLocated.required";
    public static final String QUERY_FOR_DELETION = "artworkLocated.forDeletion";
    public static final String UPDATE_STATUS = "artworkLocated.updateStatus";

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "artwork_id", nullable = false, foreignKey = @ForeignKey(name = "FK_ARTWORKLOCATED_ARTWORK"))
    private Artwork artwork;

    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "stagefile_id", foreignKey = @ForeignKey(name = "FK_ARTWORKLOCATED_STAGEFILE"))
    private StageFile stageFile;

    @NaturalId(mutable = true)
    @Column(name = "source", nullable=false, length = 50)
    private String source;

    @NaturalId(mutable = true)
    @Column(name = "hash_code", nullable=false, length = 100)
    private String hashCode;

    @Column(name = "url", length = 1000)
    private String url;

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
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getHashCode() {
        return hashCode;
    }

    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }
    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
    
    public boolean isCached() {
        return !isNotCached();
    }

    public boolean isNotCached() {
        return StringUtils.isBlank(getCacheFilename()) || StringUtils.isBlank(getCacheDirectory());
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getArtwork())
                .append(getSource())
                .append(getHashCode())
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
        ArtworkLocated other = (ArtworkLocated) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getArtwork(), other.getArtwork())
                .append(getSource(), other.getSource())
                .append(getHashCode(), other.getHashCode())
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
        sb.append(", source=");
        sb.append(getSource());
        sb.append(", hashCode=");
        sb.append(getHashCode());
        if (StringUtils.isNotBlank(getUrl())) {
            sb.append(", url=");
            sb.append(getUrl());
        }
        if (getStageFile() != null && Hibernate.isInitialized(getStageFile())) {
            sb.append(", stageFile=");
            sb.append(getStageFile().getFileName());
        }
        sb.append("]");
        return sb.toString();
    }
}
