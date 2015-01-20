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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
import org.yamj.core.database.model.type.JobType;

@Embeddable
public class CastCrewPK implements Serializable {

    private static final long serialVersionUID = -1986488516405874557L;

    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "person_id", nullable = false, insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "FK_CASTCREW_PERSON"))
    private Person person;

    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "videodata_id", nullable = false, insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "FK_CASTCREW_VIDEODATA"))
    private VideoData videoData;

    @Type(type = "jobType")
    @Column(name = "job", nullable = false, length = 30, insertable = false, updatable = false)
    private JobType jobType;

    public CastCrewPK() {
    }

    public CastCrewPK(Person person, VideoData videoData, JobType jobType) {
        this.person = person;
        this.videoData = videoData;
        this.jobType = jobType;
    }

    // GETTER AND SETTER
    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getPerson().getId())
                .append(getVideoData().getId())
                .append(getJobType())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CastCrewPK) {
            final CastCrewPK other = (CastCrewPK) obj;
            return new EqualsBuilder()
                    .append(getJobType(), other.getJobType())
                    .append(getPerson().getId(), other.getPerson().getId())
                    .append(getVideoData().getId(), other.getVideoData().getId())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CastCrewPK [person='");
        sb.append(getPerson().getName());
        sb.append("', videoData='");
        sb.append(getVideoData().getIdentifier());
        sb.append("', job=");
        sb.append(getJobType());
        sb.append("]");
        return sb.toString();
    }
}
