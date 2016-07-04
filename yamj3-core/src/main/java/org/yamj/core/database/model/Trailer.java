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

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.yamj.core.api.model.dto.ApiTrailerDTO;
import org.yamj.plugin.api.model.ITrailer;
import org.yamj.plugin.api.model.type.ContainerType;

@NamedQueries({    
    @NamedQuery(name = Trailer.QUERY_REQUIRED,
        query = "FROM Trailer t LEFT OUTER JOIN FETCH t.videoData LEFT OUTER JOIN FETCH t.series LEFT OUTER JOIN FETCH t.stageFile WHERE t.id = :id"
    ),
    @NamedQuery(name = Trailer.QUERY_FOR_DELETION,
        query = "SELECT t.id FROM Trailer t WHERE t.status = 'DELETED'"
    ),
    @NamedQuery(name = Trailer.UPDATE_STATUS,
        query = "UPDATE Trailer SET status=:status WHERE id=:id"
    )
})

@NamedNativeQueries({    
    @NamedNativeQuery(name = Trailer.QUERY_PROCESSING_QUEUE, resultSetMapping="id.queue",
        query = "SELECT t.id,(case when t.update_timestamp is null then t.create_timestamp else t.update_timestamp end) as maxdate "+
                "FROM trailer t WHERE t.status in ('NEW','UPDATED') ORDER BY maxdate ASC"
    ),
    @NamedNativeQuery(name = Trailer.QUERY_SCANNING_QUEUE, resultSetMapping = "metadata.queue",
        query = "SELECT vd.id,'MOVIE' as metatype,(case when vd.update_timestamp is null then vd.create_timestamp else vd.update_timestamp end) as maxdate "+
                "FROM videodata vd WHERE vd.trailer_status in ('NEW','UPDATED') and vd.status='DONE' and vd.episode<0 " +
                "UNION SELECT ser.id,'SERIES' as metatype,(case when ser.update_timestamp is null then ser.create_timestamp else ser.update_timestamp end) as maxdate "+
                "FROM series ser WHERE ser.trailer_status in ('NEW','UPDATED') and ser.status='DONE' ORDER BY maxdate ASC"
    ),
    @NamedNativeQuery(name = "metadata.trailer.series", resultSetMapping = "metadata.trailer",
        query = "SELECT t.id, t.title, t.url, t.source, t.hash_code, t.cache_dir, t.cache_filename FROM trailer t "+
                "WHERE t.series_id=:id and t.status not in ('DELETED','INVALID','DUPLICATE') ORDER BY t.id"
    ),
    @NamedNativeQuery(name = "metadata.trailer.movie", resultSetMapping = "metadata.trailer",
        query = "SELECT t.id, t.title, t.url, t.source, t.hash_code, t.cache_dir, t.cache_filename FROM trailer t "+
                "WHERE t.videodata_id=:id and t.status not in ('DELETED','INVALID','DUPLICATE') ORDER BY t.id"
    )
})

@SqlResultSetMapping(name = "metadata.trailer", classes={
    @ConstructorResult(
        targetClass=ApiTrailerDTO.class,
        columns={
             @ColumnResult(name="id", type=Long.class),
             @ColumnResult(name="title", type=String.class),
             @ColumnResult(name="url", type=String.class),
             @ColumnResult(name="source", type=String.class),
             @ColumnResult(name="hash_code", type=String.class),
             @ColumnResult(name="cache_dir", type=String.class),
             @ColumnResult(name="cache_filename", type=String.class)
        }
    )}
)

@Entity
@Table(name = "trailer",
       uniqueConstraints = @UniqueConstraint(name = "UIX_TRAILER_NATURALID", columnNames = {"videodata_id", "series_id", "source", "hash_code"}),
       indexes = {@Index(name = "IX_TRAILER_STATUS", columnList = "status")}
)
public class Trailer extends AbstractStatefulPrev implements ITrailer {

    private static final long serialVersionUID = -7853145730427742811L;
    public static final String QUERY_REQUIRED = "trailer.required";
    public static final String QUERY_FOR_DELETION = "trailer.forDeletion";
    public static final String UPDATE_STATUS = "trailer.updateStatus";
    public static final String QUERY_SCANNING_QUEUE = "trailer.scanningQueue";
    public static final String QUERY_PROCESSING_QUEUE = "trailer.processingQueue";
                    
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

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "stagefile_id", foreignKey = @ForeignKey(name = "FK_TRAILER_STAGEFILE"))
    private StageFile stageFile;

    @NaturalId(mutable = true)
    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @NaturalId(mutable = true)
    @Column(name = "hash_code", nullable = false, length = 100)
    private String hashCode;

    @Column(name = "url", length = 1000)
    private String url;

    @Type(type = "containerType")
    @Column(name = "container", nullable = false, length = 10)
    private ContainerType container;

    @Column(name = "cache_filename", length = 255)
    private String cacheFilename;

    @Column(name = "cache_dir", length = 50)
    private String cacheDirectory;

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String getHashCode() {
        return hashCode;
    }

    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public ContainerType getContainer() {
        return container;
    }

    public void setContainer(ContainerType container) {
        this.container = container;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
        Trailer other = (Trailer) obj;
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
