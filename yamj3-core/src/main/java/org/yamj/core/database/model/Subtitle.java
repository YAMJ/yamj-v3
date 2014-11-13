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
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.yamj.common.tools.EqualityTools;

@Entity
@Table(name = "subtitle",
    uniqueConstraints= @UniqueConstraint(name="UIX_SUBTITLE_NATURALID", columnNames={"mediafile_id", "stagefile_id", "counter"})
)
public class Subtitle extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -6279878819525772005L;

    @NaturalId
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mediafile_id", nullable = false)
    @ForeignKey(name = "FK_SUBTITLE_MEDIAFILE")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MediaFile mediaFile;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_SUBITLE_STAGEFILE")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "stagefile_id")
    private StageFile stageFile;

    @NaturalId
    @Column(name = "counter", nullable = false)
    private int counter = -1;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "language")
    private String language;

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public StageFile getStageFile() {
        return stageFile;
    }

    public void setStageFile(StageFile stageFile) {
        this.stageFile = stageFile;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }


    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }


    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (getMediaFile() == null ? 0 : getMediaFile().hashCode());
        result = prime * result + (getStageFile() == null ? 0 : getStageFile().hashCode());
        result = prime * result + getCounter();
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
        if (!(other instanceof Subtitle)) {
            return false;
        }
        Subtitle castOther = (Subtitle) other;
        // first check the id
        if ((getId() > 0) && (castOther.getId() > 0)) {
            return getId() == castOther.getId();
        }
        // check counter
        if (getCounter() != castOther.getCounter()) {
            return false;
        }
        // check media file
        if (EqualityTools.notEquals(getStageFile(), castOther.getStageFile())) {
            return false;
        }
        // check stage file
        return EqualityTools.equals(getStageFile(), castOther.getStageFile());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Subtitle [ID=");
        sb.append(getId());
        if (getMediaFile() != null && Hibernate.isInitialized(getMediaFile())) {
            sb.append(", mediaFile='");
            sb.append(getMediaFile().getFileName());
            sb.append("'");
        }
        if (getStageFile() != null && Hibernate.isInitialized(getStageFile())) {
            sb.append(", stageFile='");
            sb.append(getStageFile().getFullPath());
            sb.append("'");
        }
        sb.append("', counter=");
        sb.append(getCounter());
        sb.append(", format=");
        sb.append(getFormat());
        sb.append(", language=");
        sb.append(getLanguage());
        sb.append("]");
        return sb.toString();
    }
}
