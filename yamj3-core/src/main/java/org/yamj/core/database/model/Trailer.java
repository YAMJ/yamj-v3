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
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;

@Entity
@Table(name = "trailer",
       uniqueConstraints = @UniqueConstraint(name = "UIX_TRAILER_NATURALID", columnNames = {"videodata_id", "series_id", "stagefile_id", "url"}),
       indexes = {@Index(name = "IX_TRAILER_STATUS", columnList = "status")}
)
public class Trailer extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -7853145730427742811L;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id", foreignKey = @ForeignKey(name = "FK_TRAILER_VIDEODATA"))
    private VideoData videoData;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id", foreignKey = @ForeignKey(name = "FK_TRAILER_SERIES"))
    private Series series;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "stagefile_id", foreignKey = @ForeignKey(name = "FK_TRAILER_STAGEFILE"))
    private StageFile stageFile;

    @NaturalId(mutable = true)
    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "hash_code", nullable = false, length = 100)
    private String hashCode;

    @Column(name = "cache_filename", length = 255)
    private String cacheFilename;

    @Column(name = "cache_dir", length = 50)
    private String cacheDirectory;

    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @Type(type = "statusType")
    @Column(name = "previous_status", length = 30)
    private StatusType previousStatus;

    @Column(name = "title", length = 255)
    private String title;

    // GETTER and SETTER
    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public StageFile getStageFile() {
        return stageFile;
    }

    public void setStageFile(StageFile stageFile) {
        this.stageFile = stageFile;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // TRANSIENT METHODS
    public boolean isCached() {
        return StringUtils.isNotBlank(getCacheFilename());
    }
    
    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getVideoData())
                .append(getSeries())
                .append(getStageFile())
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
        if (!(obj instanceof Trailer)) {
            return false;
        }
        final Trailer other = (Trailer) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getVideoData(), other.getVideoData())
                .append(getSeries(), other.getSeries())
                .append(getStageFile(), other.getStageFile())
                .append(getUrl(), other.getUrl())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Trailer [ID=");
        sb.append(getId());
        sb.append(", type=");
        if (videoData != null) {
            sb.append("movie");
        } else if (series != null) {
            sb.append("series");
        }
        sb.append(", locality=");
        if (StringUtils.isNotBlank(url)) {
            sb.append(url);
        } else {
            sb.append("local file");
        }
        sb.append("]");
        return sb.toString();
    }
}
