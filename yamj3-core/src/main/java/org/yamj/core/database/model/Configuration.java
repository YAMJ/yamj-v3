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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.yamj.core.hibernate.Auditable;

@Entity
@Table(name = "configuration")
public class Configuration implements Auditable, Serializable {

    private static final long serialVersionUID = -3985190780763596771L;

    @Id
    @Column(name = "config_key", nullable = false, length = 255)
    private String key;

    @Column(name = "config_value", length = 255)
    private String value;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_timestamp", nullable = false, updatable = false)
    private Date createTimestamp;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_timestamp")
    private Date updateTimestamp;

    // GETTER and SETTER
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @JsonIgnore
    public Date getCreateTimestamp() {
        return this.createTimestamp;
    }

    public void setCreateTimestamp(final Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    @JsonIgnore
    public Date getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    public void setUpdateTimestamp(final Date updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getKey())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Configuration) {
            final Configuration other = (Configuration) obj;
            return new EqualsBuilder()
                    .append(getKey(), other.getKey())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration [Key='");
        sb.append(getKey());
        sb.append("', value='");
        sb.append(getValue());
        sb.append("']");
        return sb.toString();
    }
}
