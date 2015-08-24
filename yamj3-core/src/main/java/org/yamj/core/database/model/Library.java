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
import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "library",
        uniqueConstraints = @UniqueConstraint(name = "UIX_LIBRARY_NATURALID", columnNames = {"client", "player_path"})
)
public class Library extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3086992329257871600L;

    @NaturalId
    @Column(name = "client", nullable = false, length = 100)
    private String client;

    @NaturalId
    @Column(name = "player_path", nullable = false, length = 1000)
    private String playerPath;

    @Column(name = "base_directory", nullable = false, length = 1000)
    private String baseDirectory;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_scanned", nullable = false)
    private Date lastScanned;

    // GETTER and SETTER
    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getPlayerPath() {
        return playerPath;
    }

    public void setPlayerPath(String playerPath) {
        this.playerPath = playerPath;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getClient())
                .append(getPlayerPath())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Library) {
            final Library other = (Library) obj;
            return new EqualsBuilder()
                    .append(getClient(), other.getClient())
                    .append(getPlayerPath(), other.getPlayerPath())
                    .isEquals();
        }
        return false;
    }
}
