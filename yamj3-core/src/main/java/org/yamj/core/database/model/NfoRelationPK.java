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
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;

@Embeddable
public class NfoRelationPK implements Serializable {

    private static final long serialVersionUID = -2719804527986484389L;

    @ManyToOne(fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_NFORELATION_STAGEFILE")
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "stagefile_id", nullable = false, insertable = false, updatable = false)
    private StageFile stageFile;

    @ManyToOne(fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_NFORELATION_VIDEODATA")
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "videodata_id", nullable = false, insertable = false, updatable = false)
    private VideoData videoData;

    public NfoRelationPK() {
    }

    public NfoRelationPK(StageFile stageFile, VideoData videoData) {
        this.stageFile = stageFile;
        this.videoData = videoData;
    }

    // GETTER AND SETTER
    public StageFile getStageFile() {
        return stageFile;
    }

    public void setStageFile(StageFile stageFile) {
        this.stageFile = stageFile;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getStageFile())
                .append(getVideoData())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NfoRelationPK) {
            final NfoRelationPK other = (NfoRelationPK) obj;
            return new EqualsBuilder()
                    .append(getStageFile().getId(), other.getStageFile().getId())
                    .append(getVideoData(), other.getVideoData())
                    .isEquals();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NfoRelationPK [stageFile='");
        sb.append(getStageFile().getFileName());
        sb.append("', videoData='");
        sb.append(getVideoData().getIdentifier());
        sb.append("]");
        return sb.toString();
    }
}
