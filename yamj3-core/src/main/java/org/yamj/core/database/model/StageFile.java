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
import java.util.*;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.FileType;

@Entity
@Table(name = "stage_file",
       uniqueConstraints = @UniqueConstraint(name = "UIX_STAGEFILE_NATURALID", columnNames = {"directory_id", "base_name", "extension"}),
       indexes = {@Index(name = "IX_STAGEFILE_BASENAME", columnList = "base_name"),
                  @Index(name = "IX_STAGEFILE_STATUS", columnList = "status")}
)
@SuppressWarnings("unused")
public class StageFile extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -6247352843375054146L;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "directory_id", nullable = false, foreignKey = @ForeignKey(name = "FK_STAGEFILE_DIRECTORY"))
    private StageDirectory stageDirectory;

    
    @NaturalId(mutable = true)
    @Column(name = "base_name", nullable = false, length = 255)
    private String baseName;

    @NaturalId(mutable = true)
    @Column(name = "extension", nullable = false, length = 30)
    private String extension;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "file_date", nullable = false)
    private Date fileDate;

    @Column(name = "file_size", nullable = false)
    private long fileSize = -1;

    @Type(type = "fileType")
    @Column(name = "file_type", nullable = false, length = 30)
    private FileType fileType;

    @Column(name = "full_path", nullable = false, length = 255)
    private String fullPath;

    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "nfoRelationPK.stageFile")
    private List<NfoRelation> nfoRelations = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "stageFile")
    private Set<ArtworkLocated> artworkLocated = new HashSet<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "stageFile")
    private Set<Subtitle> subtitles = new HashSet<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "stageFile")
    private List<Trailer> trailers = new ArrayList<>(0);

    @Lob
    @Column(name = "content")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "mediafile_id", foreignKey = @ForeignKey(name = "FK_STAGEFILE_MEDIAFILE"))
    private MediaFile mediaFile;

    // GETTER and SETTER
    public StageDirectory getStageDirectory() {
        return stageDirectory;
    }

    public void setStageDirectory(StageDirectory stageDirectory) {
        this.stageDirectory = stageDirectory;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Date getFileDate() {
        return fileDate;
    }

    public void setFileDate(Date fileDate) {
        this.fileDate = fileDate;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public List<NfoRelation> getNfoRelations() {
        return nfoRelations;
    }

    private void setNfoRelations(List<NfoRelation> nfoRelations) {
        this.nfoRelations = nfoRelations;
    }

    public void addNfoRelation(NfoRelation nfoRelation) {
        this.nfoRelations.add(nfoRelation);
    }

    private void setArtworkLocated(Set<ArtworkLocated> artworkLocated) {
        this.artworkLocated = artworkLocated;
    }

    public Set<ArtworkLocated> getArtworkLocated() {
        return artworkLocated;
    }

    public Set<Subtitle> getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(Set<Subtitle> subtitles) {
        this.subtitles = subtitles;
    }

    public List<Trailer> getTrailers() {
        return trailers;
    }

    public void setTrailers(List<Trailer> trailers) {
        this.trailers = trailers;
    }

    // TRANSIENT METHODS
    
    public String getFileName() {
        return this.getBaseName() + "." + this.getExtension();
    }

    public String getHashCode() {
        int hash = this.getFullPath().hashCode();
        return String.valueOf((hash < 0 ? 0 - hash : hash));
    }

    public String getHashCode(int increase) {
        int hash = this.getFullPath().hashCode();
        if (hash < 0 ) hash=(0-hash);
        hash = hash + increase;
        return String.valueOf(hash);
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getExtension())
                .append(getBaseName())
                .append(getStageDirectory())
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
        if (!(obj instanceof StageFile)) {
            return false;
        }
        final StageFile other = (StageFile) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getExtension(), other.getExtension())
                .append(getBaseName(), other.getBaseName())
                .append(getStageDirectory(), other.getStageDirectory())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StageFile [ID=");
        sb.append(getId());
        sb.append(", baseName=");
        sb.append(getBaseName());
        sb.append(", extension=");
        sb.append(getExtension());
        sb.append(", fileDate=");
        sb.append(getFileDate());
        sb.append(", fileSize=");
        sb.append(getFileSize());
        if (this.stageDirectory != null && Hibernate.isInitialized(this.stageDirectory)) {
            sb.append(", stageDirectory=");
            sb.append(getStageDirectory().getDirectoryPath());
        }
        sb.append("]");
        return sb.toString();
    }
}
