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

import java.io.Serializable;
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.NaturalId;
import org.yamj.core.api.model.dto.ApiAwardDTO;
import org.yamj.core.database.model.AbstractIdentifiable;

@NamedNativeQueries({
    @NamedNativeQuery(name = "metadata.award.series", resultSetMapping="metadata.award",
        query = "SELECT a.event, a.category, a.sourcedb, c.year, c.won, c.nominated "+
                "FROM series_awards c JOIN award a ON c.award_id=a.id WHERE c.series_id=:id ORDER BY year, event"
    ),
    @NamedNativeQuery(name = "metadata.award.season", resultSetMapping="metadata.award",
        query = "SELECT a.event, a.category, a.sourcedb, c.year, c.won, c.nominated "+
                "FROM series_awards c JOIN season sea ON c.series_id=sea.series_id JOIN award a ON c.award_id=a.id WHERE sea.id=:id ORDER BY year, event"
    ),
    @NamedNativeQuery(name = "metadata.award.movie", resultSetMapping="metadata.award",
        query = "SELECT a.event, a.category, a.sourcedb, c.year, c.won, c.nominated "+
                "FROM videodata_awards c JOIN award a ON c.award_id=a.id WHERE c.videodata_id=:id ORDER BY year, event"
    )    
})

@SqlResultSetMapping(name="metadata.award", classes={
    @ConstructorResult(
        targetClass=ApiAwardDTO.class,
        columns={
             @ColumnResult(name="event", type=String.class),
             @ColumnResult(name="category", type=String.class),
             @ColumnResult(name="sourcedb", type=String.class),
             @ColumnResult(name="year", type=Integer.class),
             @ColumnResult(name="won", type=Boolean.class),
             @ColumnResult(name="nominated", type=Boolean.class)
        }
    )}
)

@Entity
@Table(name = "award",
       uniqueConstraints = @UniqueConstraint(name = "UIX_AWARD_NATURALID", columnNames = {"event", "category", "sourcedb"})
)
public class Award extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -1181486841324976037L;

    @NaturalId
    @Column(name = "event", nullable = false, length = 255)
    private String event;

    @NaturalId
    @Column(name = "category", nullable = false, length = 255)
    private String category;

    @NaturalId
    @Column(name = "sourcedb", nullable = false, length = 40)
    private String sourceDb;
    
    public Award() {
        // empty constructor
    }

    public Award(String event, String category, String sourceDb) {
        this.event = event;
        this.category = category;
        this.sourceDb = sourceDb;
    }

    // GETTER and SETTER
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
                .append(getEvent())
                .append(getCategory())
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
        if (!(obj instanceof Award)) {
            return false;
        }
        Award other = (Award) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values        
        return new EqualsBuilder()
                .append(getEvent(), other.getEvent())
                .append(getCategory(), other.getCategory())
                .append(getSourceDb(), other.getSourceDb())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Award [ID=");
        sb.append(getId());
        sb.append(", event=");
        sb.append(getEvent());
        sb.append(", category=");
        sb.append(getCategory());
        sb.append(", source=");
        sb.append(getSourceDb());
        sb.append("]");
        return sb.toString();
    }
}
