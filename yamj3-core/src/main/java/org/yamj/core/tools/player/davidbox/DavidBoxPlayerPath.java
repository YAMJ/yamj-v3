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
package org.yamj.core.tools.player.davidbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class DavidBoxPlayerPath {

    @JsonProperty("device_type")
    private String deviceType;
    private boolean folder;
    @JsonProperty("lastAccessDate")
    private String lastAccessDate;
    @JsonProperty("modifyDate")
    private String modifiedDate;
    @JsonProperty("name")
    private String name;
    @JsonProperty("path")
    private String path;
    @JsonProperty("size")
    private int size;

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    @JsonProperty("isFolder")
    public void setFolder(String folder) {
        this.folder = BooleanUtils.toBoolean(folder);
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getLastAccessDate() {
        return lastAccessDate;
    }

    public void setLastAccessDate(String lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
