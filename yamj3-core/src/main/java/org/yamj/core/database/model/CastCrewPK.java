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
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.yamj.core.database.model.type.JobType;

@Embeddable
public class CastCrewPK implements Serializable {

    private static final long serialVersionUID = -1986488516405874557L;

    @ManyToOne(fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_CASTCREW_PERSON")
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "person_id", nullable = false, insertable = false, updatable = false)
    private Person person;

    @Index(name = "IX_CASTCREW_VIDEOJOB")
    @ManyToOne(fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_CASTCREW_VIDEODATA")
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "videodata_id", nullable = false, insertable = false, updatable = false)
    private VideoData videoData;

    @Index(name = "IX_CASTCREW_VIDEOJOB")
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
        final int prime = 7;
        int result = 1;
        result = prime * result + Long.valueOf(getPerson().getId()).hashCode();
        result = prime * result + Long.valueOf(getVideoData().getId()).hashCode();
        result = prime * result + getJobType().hashCode();
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
        if (!(other instanceof CastCrewPK)) {
            return false;
        }
        CastCrewPK castOther = (CastCrewPK) other;
        if (getJobType() != castOther.getJobType()) {
            return false;
        }
        if (getPerson().getId() != castOther.getPerson().getId()) {
            return false;
        }
        return getVideoData().getId() == castOther.getVideoData().getId();
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
