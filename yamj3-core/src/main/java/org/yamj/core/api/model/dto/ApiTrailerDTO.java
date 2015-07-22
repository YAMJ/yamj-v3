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
    private String filename;
    
    @JsonIgnore
    private String hashCode;
    @JsonIgnore
    private String cacheDir;
    @JsonIgnore
    private String cacheFilename;
    
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

    public String getHashCode() {
        return hashCode;
    }

    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getCacheFilename() {
        return cacheFilename;
    }

    public void setCacheFilename(String cacheFilename) {
        this.cacheFilename = cacheFilename;
    }
    
    public String getFilename() {
        if (filename == null && StringUtils.isNotBlank(this.cacheFilename)) {
            filename = FilenameUtils.normalize(FilenameUtils.concat(this.cacheDir, this.cacheFilename), Boolean.TRUE);
        }
        return filename;
    }
}
