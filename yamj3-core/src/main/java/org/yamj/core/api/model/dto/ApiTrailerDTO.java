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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@JsonInclude(Include.NON_DEFAULT)
public class ApiTrailerDTO extends AbstractApiIdentifiableDTO {

    private String url;
    private String source;
    private String title;
    private String cacheDir = "";
    private String cacheFilename = "";
    private String filename = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonIgnore
    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        if (StringUtils.isBlank(cacheDir)) {
            this.cacheDir = "";
        } else {
            this.cacheDir = cacheDir;
        }
    }

    @JsonIgnore
    public String getCacheFilename() {
        return cacheFilename;
    }

    public void setCacheFilename(String cacheFilename) {
        if (StringUtils.isBlank(cacheFilename)) {
            this.cacheFilename = "";
        } else {
            this.cacheFilename = cacheFilename;
        }
    }
    
    public String getFilename() {
        if (StringUtils.isBlank(this.filename)) {
            this.filename = FilenameUtils.normalize(FilenameUtils.concat(this.cacheDir, this.cacheFilename), Boolean.TRUE);
        }
        return filename;
    }
}
