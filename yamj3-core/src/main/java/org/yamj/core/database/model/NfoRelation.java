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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.yamj.common.tools.EqualityTools;

@Entity
@Table(name = "nfo_relation")
public class NfoRelation implements Serializable {

    private static final long serialVersionUID = 1083402240122932701L;

    @EmbeddedId 
    private NfoRelationPK nfoRelationPK; 

    @Column(name = "priority", nullable = false)
    private int priority = -1;

    // GETTER and SETTER

    public NfoRelation() {}

    public NfoRelation(StageFile stageFile, VideoData videoData) {
        setNfoRelationPK(new NfoRelationPK(stageFile, videoData));
    }

    public NfoRelationPK getNfoRelationPK() {
        return nfoRelationPK;
    }

    private void setNfoRelationPK(NfoRelationPK nfoRelationPK) {
        this.nfoRelationPK = nfoRelationPK;
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
        result = prime * result + (getNfoRelationPK() == null ? 0 : getNfoRelationPK().hashCode());
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
        return EqualityTools.equals(getNfoRelationPK(), castOther.getNfoRelationPK());
    }
}
