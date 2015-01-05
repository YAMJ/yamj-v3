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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "stage_directory",
        uniqueConstraints = @UniqueConstraint(name = "UIX_STAGEDIRECTORY_NATURALID", columnNames = {"directory_path", "library_id"})
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
    private Set<StageFile> stageFiles = new HashSet<>(0);

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
        return new HashCodeBuilder()
                .append(getDirectoryPath())
                .append(getLibrary() == null ? 0 : Long.valueOf(getLibrary().getId()).hashCode())
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
        if (!(obj instanceof StageDirectory)) {
            return false;
        }
        final StageDirectory other = (StageDirectory) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getDirectoryPath(), other.getDirectoryPath())
                .append(getLibrary(), other.getLibrary())
                .isEquals();
    }
}
