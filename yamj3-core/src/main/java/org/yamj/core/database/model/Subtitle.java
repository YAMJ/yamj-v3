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
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;

@Entity
@Table(name = "subtitle",
        uniqueConstraints = @UniqueConstraint(name = "UIX_SUBTITLE_NATURALID", columnNames = {"mediafile_id", "stagefile_id", "counter"})
)
public class Subtitle extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -6279878819525772005L;

    @NaturalId
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mediafile_id", nullable = false, foreignKey = @ForeignKey(name = "FK_SUBTITLE_MEDIAFILE"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MediaFile mediaFile;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "stagefile_id", foreignKey = @ForeignKey(name = "FK_SUBTITLE_STAGEFILE"))
    private StageFile stageFile;

    @NaturalId
    @Column(name = "counter", nullable = false)
    private int counter = -1;

    @Column(name = "format", nullable = false, length = 50)
    private String format;

    @Column(name = "language_code", nullable = false, length = 4)
    private String languageCode;

    @Column(name = "default_flag", nullable = false)
    private boolean defaultFlag = false;

    @Column(name = "forced_flag", nullable = false)
    private boolean forcedFlag = false;

    // GETTER and SETTER

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

    public boolean isDefaultFlag() {
        return defaultFlag;
    }

    public void setDefaultFlag(boolean defaultFlag) {
        this.defaultFlag = defaultFlag;
    }

    public boolean isForcedFlag() {
        return forcedFlag;
    }

    public void setForcedFlag(boolean forcedFlag) {
        this.forcedFlag = forcedFlag;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getMediaFile())
                .append(getStageFile())
                .append(getCounter())
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
        if (!(obj instanceof Subtitle)) {
            return false;
        }
        final Subtitle other = (Subtitle) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getCounter(), other.getCounter())
                .append(getMediaFile(), other.getMediaFile())
                .append(getStageFile(), other.getStageFile())
                .isEquals();
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
        sb.append(", languageCode=");
        sb.append(getLanguageCode());
        sb.append("]");
        return sb.toString();
    }
}
