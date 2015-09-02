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
package org.yamj.core.database.model.player;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.yamj.core.database.model.AbstractIdentifiable;

@Entity
@Table(name = "player_info",
    uniqueConstraints = @UniqueConstraint(name = "UIX_PLAYER_INFO_NATURALID", columnNames = {"name"})
)
public class PlayerInfo extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 2906323039735788880L;

    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "device_type", nullable = false, length = 200)
    private String deviceType;

    @Column(name = "ip_address", nullable = false, length = 15)
    private String ipAddress;

    @Fetch(FetchMode.JOIN)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "playerinfo_playerpath",
            joinColumns = @JoinColumn(name = "player_info_id"),
            foreignKey = @ForeignKey(name = "FK_PLAYERPATH_INFO"),
            inverseJoinColumns = @JoinColumn(name = "player_path_id"), 
            inverseForeignKey = @ForeignKey(name = "FK_PLAYERPATH_PATH"))
    private Set<PlayerPath> paths = new HashSet<>(0);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Set<PlayerPath> getPaths() {
        return paths;
    }

    public void setPaths(Set<PlayerPath> paths) {
        this.paths = paths;
    }

    public void addPath(PlayerPath path) {
        if (this.paths.size() > 0) {
            for (PlayerPath stored : this.paths) {
                if (stored.getSourcePath().equalsIgnoreCase(path.getSourcePath())) {
                    // just update target path
                    stored.setTargetPath(path.getTargetPath());
                    return;
                }
            }
        }
        
        // just add path
        this.paths.add(path);
    }
}
