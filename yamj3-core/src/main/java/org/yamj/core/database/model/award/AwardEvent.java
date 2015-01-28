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
package org.yamj.core.database.model.award;

import org.yamj.core.database.model.AbstractIdentifiable;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "award_event",
       uniqueConstraints = @UniqueConstraint(name = "UIX_AWARDEVENT_NATURALID", columnNames = {"name", "sourcedb"})
)
public class AwardEvent extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -1181486841324976037L;

    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NaturalId(mutable = true)
    @Column(name = "sourcedb", nullable = false, length = 40)
    private String sourceDb;
    
    public AwardEvent() {
    }

    public AwardEvent(String name, String sourceDb) {
        this.name = name;
        this.sourceDb = sourceDb;
    }

    // GETTER and SETTER
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceDb() {
        return sourceDb;
    }

    public void setSourceDb(String sourceDb) {
        this.sourceDb = sourceDb;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getName())
                .append(getSourceDb())
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
        if (!(obj instanceof AwardEvent)) {
            return false;
        }
        final AwardEvent other = (AwardEvent) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values        
        return new EqualsBuilder()
                .append(getName(), other.getName())
                .append(getSourceDb(), other.getSourceDb())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AwardEvent [ID=");
        sb.append(getId());
        sb.append(", name=");
        sb.append(getName());
        sb.append(", source=");
        sb.append(getSourceDb());
        sb.append("]");
        return sb.toString();
    }
}
