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

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.yamj.common.type.StatusType;

/**
 * Abstract implementation of a cacheable object with previous status
 */
@MappedSuperclass
public abstract class AbstractCacheable extends AbstractStateful {

    private static final long serialVersionUID = -1388803900038754325L;
    
    @Type(type = "statusType")
    @Column(name = "previous_status", length = 30)
    private StatusType previousStatus;
    
    @Column(name = "cache_filename", length = 255)
    private String cacheFilename;

    @Column(name = "cache_dir", length = 50)
    private String cacheDirectory;

    // CONSTRUCTORS
    
    public AbstractCacheable() {
        super();
    }
    
    @Override
    public void setStatus(StatusType status) {
        if (StatusType.DELETED.equals(status)) {
           setPreviousStatus(getStatus());
        } else {
            setPreviousStatus(null);
        }
        super.setStatus(status);
    }

    // GETTER and SETTER
    
    public StatusType getPreviousStatus() {
        return previousStatus;
    }

    private void setPreviousStatus(StatusType previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getCacheFilename() {
        return cacheFilename;
    }

    public void setCacheFilename(String cacheFilename) {
        this.cacheFilename = cacheFilename;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    // TRANSIENT METHODS
    
    public boolean isCached() {
        return !isNotCached();
    }

    public boolean isNotCached() {
        return StringUtils.isBlank(getCacheFilename()) || StringUtils.isBlank(getCacheDirectory());
    }

    
    public String getFullCacheFilename() {
        return FilenameUtils.concat(getCacheDirectory(), getCacheFilename());        
    }
}
