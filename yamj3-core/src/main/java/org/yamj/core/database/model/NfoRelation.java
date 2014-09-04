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

import javax.persistence.Column;
import javax.persistence.Embeddable;

import java.io.Serializable;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;

@Entity
@Table(name = "nfo_relation")
public class NfoRelation implements Serializable {

    private static final long serialVersionUID = 1083402240122932701L;

    @EmbeddedId 
    private NfoRelationPK primary = new NfoRelationPK(); 

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_NFORELATION_STAGEFILE")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "stagefile_id", nullable = false, insertable = false, updatable = false)
    private StageFile stageFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_NFORELATION_VIDEODATA")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id", nullable = false, insertable = false, updatable = false)
    private VideoData videoData;

    @Column(name = "priority", nullable = false)
    private int priority = -1;

    // GETTER and SETTER

    public StageFile getStageFile() {
        return stageFile;
    }

    public void setStageFile(StageFile stageFile) {
        this.stageFile = stageFile;
        this.primary.stageFileId = stageFile.getId();
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
        this.primary.videoDataId = videoData.getId();
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (this.primary == null ? 0 : this.primary.hashCode());
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
        if (!(other instanceof NfoRelation)) {
            return false;
        }
        NfoRelation castOther = (NfoRelation) other;
        return this.primary.equals(castOther.primary);
    }

    @Embeddable
    public static class NfoRelationPK implements Serializable {

        private static final long serialVersionUID = 8030842012557188597L;
        
        @Column(name = "stagefile_id", insertable = false, updatable = false)
        protected long stageFileId = -1;

        @Column(name = "videodata_id", insertable = false, updatable = false)
        protected long videoDataId = -1;
       
        // EQUALITY CHECKS

        @Override
        public int hashCode() {
            final int prime = 7;
            int result = 1;
            result = prime * result + Long.valueOf(stageFileId).intValue();
            result = prime * result + Long.valueOf(videoDataId).intValue();
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
            if (!(other instanceof NfoRelationPK)) {
                return false;
            }
            NfoRelationPK castOther = (NfoRelationPK) other;
            
            if (this.stageFileId != castOther.stageFileId) {
                return false;
            }
            if (this.stageFileId != castOther.stageFileId) {
                return false;
            }
            return true;
        }
    }
}
