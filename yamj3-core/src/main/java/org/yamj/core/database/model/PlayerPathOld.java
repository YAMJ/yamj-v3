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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.NaturalId;

//@Entity
//@Table(name = "player_path_old")
@Deprecated
@SuppressWarnings("PersistenceUnitPresent")
public class PlayerPathOld extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 4L;

    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "ip_device", nullable = false, length = 50)
    private String ipDevice;
    @Column(name = "storage_path", nullable = false, length = 255)
    private String storagePath;

    // GETTERS and SETTERS
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpDevice() {
        return ipDevice;
    }

    public void setIpDevice(String ipDevice) {
        this.ipDevice = ipDevice;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.name)
                .append(this.ipDevice)
                .append(this.storagePath)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlayerPathOld) {
            final PlayerPathOld other = (PlayerPathOld) obj;
            return new EqualsBuilder()
                    .append(getName(), other.getName())
                    .append(getIpDevice(), other.getIpDevice())
                    .append(getStoragePath(), other.getStoragePath())
                    .isEquals();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
