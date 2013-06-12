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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Parameter;
import org.yamj.common.type.StatusType;
import org.yamj.core.hibernate.usertypes.EnumStringUserType;

@TypeDefs({
    @TypeDef(name = "artworkType", typeClass = EnumStringUserType.class,
            parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.ArtworkType")}),
    @TypeDef(name = "statusType", typeClass = EnumStringUserType.class,
            parameters = { @Parameter(name = "enumClassName", value = "org.yamj.common.type.StatusType")})
})

@Entity
@Table(name = "artwork_located")
public class ArtworkLocated extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;

    @Index(name = "IX_ARTWORKLOCATED_STATUS")
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORKLOCATED_STAGEFILE")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "stagefile_id")
    private StageFile stageFile;

    @Column(name = "source", length=50)
    private String source;

    @Column(name = "url", length=255)
    private String url;

    @Column(name = "language", length=30)
    private String language;

    @Column(name = "rating", nullable=false)
    private int rating = -1;
    
    @Column(name = "width", nullable = false)
    private int width = -1;
    
    @Column(name = "height", nullable = false)
    private int height = -1;

    @Column(name = "cache_filename",length = 200)
    private String cacheFilename;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORKLOCATED_ARTWORK")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "artworkLocated")
    private List<ArtworkGenerated> generatedArtworks = new ArrayList<ArtworkGenerated>(0);

    // GETTER and SETTER

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
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

    public String getCacheFilename() {
        return cacheFilename;
    }

    public void setCacheFilename(String cacheFilename) {
        this.cacheFilename = cacheFilename;
    }

    public Artwork getArtwork() {
        return artwork;
    }

    public void setArtwork(Artwork artwork) {
        this.artwork = artwork;
    }

    public List<ArtworkGenerated> getGeneratedArtworks() {
        return generatedArtworks;
    }

    public void setGeneratedArtworks(List<ArtworkGenerated> generatedArtworks) {
        this.generatedArtworks = generatedArtworks;
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
