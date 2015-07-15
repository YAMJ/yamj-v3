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
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "genre",
        uniqueConstraints = @UniqueConstraint(name = "UIX_GENRE_NATURALID", columnNames = {"name"})
)
public class Genre extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -5113519542293276527L;

    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "target_api", length = 100)
    private String targetApi;

    @Column(name = "target_xml", length = 100)
    private String targetXml;

    public Genre() {
    }

    public Genre(String name) {
        this.name = name;
    }

    // GETTER and SETTER
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetApi() {
        return targetApi;
    }

    public void setTargetApi(String targetApi) {
        this.targetApi = targetApi;
    }

    public String getTargetXml() {
        return targetXml;
    }

    public void setTargetXml(String targetXml) {
        this.targetXml = targetXml;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getName())
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
        if (!(obj instanceof Genre)) {
            return false;
        }
        final Genre other = (Genre) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values        
        return new EqualsBuilder()
                .append(getName(), other.getName())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Genre [ID=");
        sb.append(getId());
        sb.append(", name=");
        sb.append(getName());
        if (getTargetApi() != null) {
            sb.append(", targetApi=");
            sb.append(getTargetApi());
        }
        if (getTargetXml() != null) {
            sb.append(", targetXml=");
            sb.append(getTargetXml());
        }
        sb.append("]");
        return sb.toString();
    }
}
