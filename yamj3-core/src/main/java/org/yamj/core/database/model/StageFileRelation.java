/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.model;

import javax.persistence.Column;
import org.hibernate.annotations.Index;
import org.yamj.common.type.StatusType;

import java.io.Serializable;
import javax.persistence.*;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.yamj.core.database.model.type.RelationType;

@Entity
@Table(name = "stage_file_relation")
public class StageFileRelation implements Serializable {

    private static final long serialVersionUID = -1400973942176400014L;

    @EmbeddedId
    private StageFileRelationId stageFileRelationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_STAGEFILERELATION_VIDEO")
    @JoinColumn(name = "video_file_id", nullable = false, insertable=false, updatable=false)
    private StageFile videoFile;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @ForeignKey(name = "FK_STAGEFILERELATION_RELATED")
    @JoinColumn(name = "related_file_id", nullable = false, insertable=false, updatable=false)
    private StageFile relatedFile;

    @Type(type = "relationType")
    @Column(name = "relation_type", nullable = false, length = 30)
    private RelationType relationType;

    @Column(name = "priority", nullable = false)
    private int priority = -1;

    @Index(name = "FK_STAGEFILERELATION_STATUS")
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    
    public StageFileRelation() {}

    public StageFileRelation(StageFileRelationId stageFileRelationId) {
        this.stageFileRelationId = stageFileRelationId;
    }

    public StageFileRelation(StageFile videoFile, StageFile relatedFile) {
        this.stageFileRelationId = new StageFileRelationId(videoFile, relatedFile);
    }

    // GETTER and SETTER

    public StageFileRelationId getStageFileRelationId() {
        return stageFileRelationId;
    }
    
    public StageFile getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(StageFile videoFile) {
        this.videoFile = videoFile;
    }

    public StageFile getRelatedFile() {
        return relatedFile;
    }

    public void setRelatedFile(StageFile relatedFile) {
        this.relatedFile = relatedFile;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }
    
    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * this.stageFileRelationId.hashCode();
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
        if (!(other instanceof StageFileRelation)) {
            return false;
        }
        StageFileRelation castOther = (StageFileRelation) other;
        return this.stageFileRelationId.equals(castOther.stageFileRelationId);
    }

    // COMPOSITE ID

    @Embeddable
    public static class StageFileRelationId implements Serializable {

        private static final long serialVersionUID = 8619980017247996703L;

        @ManyToOne
        @JoinColumn(name = "video_file_id")
        private StageFile videoFile;

        @ManyToOne
        @JoinColumn(name = "related_file_id")
        private StageFile relatedFile;

        // required no arg constructor
        public StageFileRelationId() {}

        public StageFileRelationId(StageFile videoFile, StageFile relatedFile) {
            this.videoFile = videoFile;
            this.relatedFile = relatedFile;
        }

        public StageFile getVideoFile() {
            return videoFile;
        }

        public void setVideoFile(StageFile videoFile) {
            this.videoFile = videoFile;
        }

        public StageFile getRelatedFile() {
            return relatedFile;
        }

        public void setRelatedFile(StageFile relatedFile) {
            this.relatedFile = relatedFile;
        }

        @Override
        public int hashCode() {
            final int prime = 7;
            int result = 1;
            result = prime * result + (this.videoFile == null ? 0 : this.videoFile.hashCode());
            result = prime * result + (this.relatedFile == null ? 0 : this.relatedFile.hashCode());
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
            if (!(other instanceof StageFileRelationId)) {
                return false;
            }
            StageFileRelationId castOther = (StageFileRelationId)other;
            if (!videoFile.equals(castOther.videoFile)) {
                return false;
            }
            if (!relatedFile.equals(castOther.relatedFile)) {
                return false;
            }
            return true;
        }
    }
}
