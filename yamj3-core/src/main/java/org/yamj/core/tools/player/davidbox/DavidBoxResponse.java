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
package org.yamj.core.tools.player.davidbox;

import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class DavidBoxResponse {

    private int availableFile;
    private int availableFolder;
    private List<DavidBoxPlayerPath> fileList;
    private String name;
    private int totalFile;

    public DavidBoxResponse() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAvailableFile() {
        return availableFile;
    }

    public void setAvailableFile(int availableFile) {
        this.availableFile = availableFile;
    }

    public int getAvailableFolder() {
        return availableFolder;
    }

    public void setAvailableFolder(int availableFolder) {
        this.availableFolder = availableFolder;
    }

    public List<DavidBoxPlayerPath> getFileList() {
        return fileList;
    }

    public void setFileList(List<DavidBoxPlayerPath> fileList) {
        this.fileList = fileList;
    }

    public int getTotalFile() {
        return totalFile;
    }

    public void setTotalFile(int totalFile) {
        this.totalFile = totalFile;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
