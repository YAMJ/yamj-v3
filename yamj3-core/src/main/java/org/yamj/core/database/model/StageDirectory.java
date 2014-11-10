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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "stage_directory",
    uniqueConstraints= @UniqueConstraint(name="UIX_STAGEDIRECTORY_NATURALID", columnNames={"directory_path", "library_id"})
)
public class StageDirectory extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 1706389732909764283L;
    
    @NaturalId
    @Column(name = "directory_path", nullable = false, length = 255)
    private String directoryPath;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "directory_date", nullable = false)
    private Date directoryDate;

    @Column(name = "directory_name", nullable = false, length = 255)
    private String directoryName;
    
    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_STAGEDIRECTORY_LIBRARY")
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_STAGEDIRECTORY_PARENT")
    @JoinColumn(name = "parent_id")
    private StageDirectory parentDirectory;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "stageDirectory")
    private Set<StageFile> stageFiles = new HashSet<StageFile>(0);

    // GETTER and SETTER
    
    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public Date getDirectoryDate() {
        return directoryDate;
    }

    public void setDirectoryDate(Date directoryDate) {
        this.directoryDate = directoryDate;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    public Library getLibrary() {
        return library;
    }

    public void setLibrary(Library library) {
        this.library = library;
    }

    public StageDirectory getParentDirectory() {
        return parentDirectory;
    }

    public void setParentDirectory(StageDirectory parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    public Set<StageFile> getStageFiles() {
        return stageFiles;
    }

    public void setStageFiles(Set<StageFile> stageFiles) {
        this.stageFiles = stageFiles;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (getDirectoryPath() == null ? 0 : getDirectoryPath().hashCode());
        result = prime * result + (getLibrary() == null ? 0 : getLibrary().hashCode());
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
        if (!(other instanceof StageDirectory)) {
            return false;
        }
        StageDirectory castOther = (StageDirectory) other;
        // first check the id
        if ((getId() > 0) && (castOther.getId() > 0)) {
            return getId() == castOther.getId();
        }
        // check the directory path
        if (!StringUtils.equals(getDirectoryPath(), castOther.getDirectoryPath())) {
            return false;
        }
        // check the library
        if (getLibrary() == null && castOther.getLibrary() == null) {
            return true;
        }
        if (getLibrary() == null || castOther.getLibrary() == null) {
            return false;
        }
        return getLibrary().equals(castOther.getLibrary());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StageDirectory [ID=");
        sb.append(getId());
        sb.append(", directorPath=");
        sb.append(getDirectoryPath());
        sb.append(", directoryDate=");
        sb.append(getDirectoryDate());
        if (getLibrary() != null) {
            sb.append(", library=");
            sb.append(getLibrary().getId());
        }
        sb.append("]");
        return sb.toString();
    }
}
