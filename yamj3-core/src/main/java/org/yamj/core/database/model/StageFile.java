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

import org.yamj.common.tools.EqualityTools;

import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.FileType;

@Entity
@Table(name = "stage_file",
    uniqueConstraints= @UniqueConstraint(name="UIX_STAGEFILE_NATURALID", columnNames={"directory_id", "base_name", "extension"})
)
@SuppressWarnings("unused")
public class StageFile extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -6247352843375054146L;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_STAGEFILE_DIRECTORY")
    @JoinColumn(name = "directory_id", nullable = false)
    private StageDirectory stageDirectory;

    @Index(name = "IX_STAGEFILE_BASENAME")
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

    @Index(name = "IX_STAGEFILE_STATUS")
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "nfoRelationPK.stageFile")
    private List<NfoRelation> nfoRelations = new ArrayList<NfoRelation>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "stageFile")
    private Set<ArtworkLocated> artworkLocated = new HashSet<ArtworkLocated>(0);

    @Lob
    @Column(name = "content")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @ForeignKey(name = "FK_STAGEFILE_MEDIAFILE")
    @JoinColumn(name = "mediafile_id")
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

    public void addArtworkLocated(ArtworkLocated artworkLocated) {
        this.artworkLocated.add(artworkLocated);
    }

    // TRANSIENT METHODS
    
    public String getFileName() {
        return this.getBaseName() + "." + this.getExtension();
    }

    public String getArtworkHashCode() {
        int hash = this.getFullPath().hashCode();
        return String.valueOf((hash < 0 ? 0 - hash : hash));
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (getExtension() == null ? 0 : getExtension().hashCode());
        result = prime * result + (getBaseName() == null ? 0 : getBaseName().hashCode());
        result = prime * result + (getStageDirectory() == null ? 0 : Long.valueOf(getStageDirectory().getId()).hashCode());
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
        if (!(other instanceof StageFile)) {
            return false;
        }
        StageFile castOther = (StageFile) other;
        // first check the id
        if ((getId() > 0) && (castOther.getId() > 0)) {
            return getId() == castOther.getId();
        }
        // check extension
        if (!StringUtils.equals(getExtension(), castOther.getExtension())) {
            return false;
        }
        // check base name
        if (!StringUtils.equals(getBaseName(), castOther.getBaseName())) {
            return false;
        }
        // check stage directory
        return EqualityTools.equals(getStageDirectory(), castOther.getStageDirectory());
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
