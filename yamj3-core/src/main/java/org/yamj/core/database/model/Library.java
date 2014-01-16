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

import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "library",
    uniqueConstraints= @UniqueConstraint(name="UIX_LIBRARY_NATURALID", columnNames={"client", "player_path"})
)
public class Library extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3086992329257871600L;
    
    @NaturalId
    @Column(name = "client", nullable = false, length = 100)
    private String client;
    
    @NaturalId
    @Column(name = "player_path", nullable = false, length = 255)
    private String playerPath;
    
    @Column(name = "base_directory", nullable = false, length = 255)
    private String baseDirectory;
    
    @Temporal(value = TemporalType.TIMESTAMP)
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
        final int prime = 7;
        int result = 1;
        result = prime * result + (this.client == null ? 0 : this.client.hashCode());
        result = prime * result + (this.playerPath == null ? 0 : this.playerPath.hashCode());
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
        if (!(other instanceof Library)) {
            return false;
        }
        Library castOther = (Library) other;

        if (!StringUtils.equals(this.client, castOther.client)) {
            return false;
        }
        return StringUtils.equals(this.playerPath, castOther.playerPath);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Library [ID=");
        sb.append(getId());
        sb.append(", client=");
        sb.append(getClient());
        sb.append(", playerPath=");
        sb.append(getPlayerPath());
        sb.append(", baseDirectory=");
        sb.append(getBaseDirectory());
        sb.append(", lastScanned=");
        sb.append(getLastScanned());
        sb.append("]");
        return sb.toString();
    }
}
