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
package org.yamj.core.tools;

import java.util.Date;

public final class WatchedDTO {

    private boolean watchedNfo = true;
    private Date watchedNfoDate;
    private boolean watchedFile = true;
    private Date watchedFileDate;
    private boolean watchedApi = true;
    private Date watchedApiDate;
  
    public void setWatchedNfo(boolean watched, Date watchedDate) {
        if (watchedDate == null) return;

        if (this.watchedNfoDate == null) {
            this.watchedNfo = watched;
            this.watchedNfoDate = watchedDate;
        } else {
            this.watchedNfo = this.watchedNfo && watched;
            if (watchedDate.after(this.watchedNfoDate)) {
                this.watchedNfoDate = watchedDate;
            }
        }
    }

    public void setWatchedFile(boolean watched, Date watchedDate) {
        if (watchedDate == null) return;

        if (this.watchedFileDate == null) {
            this.watchedFile = watched;
            this.watchedFileDate = watchedDate;
        } else {
            this.watchedFile = this.watchedFile && watched;
            if (watchedDate.after(this.watchedFileDate)) {
                this.watchedFileDate = watchedDate;
            }
        }
    }

    public void setWatchedApi(boolean watched, Date watchedDate) {
        if (watchedDate == null) return;

        if (this.watchedApiDate == null) {
            this.watchedApi = watched;
            this.watchedApiDate = watchedDate;
        } else {
            this.watchedApi = this.watchedApi && watched;
            if (watchedDate.after(this.watchedApiDate)) {
                this.watchedApiDate = watchedDate;
            }
        }
    }
    
    public boolean isWatched() {
        boolean result = false;
        Date watchedDate = null;
        if (this.watchedNfoDate != null) {
            watchedDate =  this.watchedNfoDate;
            result = this.watchedNfo;
        }
        
        if (this.watchedFileDate != null) {
            if (watchedDate == null) {
                watchedDate = this.watchedFileDate;
                result = this.watchedFile;
            } else if (watchedDate.before(watchedFileDate)) {
                watchedDate = this.watchedFileDate;
                result = this.watchedFile;
            }
        }

        if (this.watchedApiDate != null) {
            if (watchedDate == null) {
                watchedDate = this.watchedApiDate;
                result = this.watchedApi;
            } else if (watchedDate.before(watchedApiDate)) {
                watchedDate = this.watchedApiDate;
                result = this.watchedApi;
            }
        }
        
        return result;
    }

    public Date getWatchedDate() {
        Date watchedDate = this.watchedNfoDate;

        if (this.watchedFileDate != null) {
            if (watchedDate == null) {
                watchedDate = this.watchedFileDate;
            } else if (watchedDate.before(watchedFileDate)) {
                watchedDate = this.watchedFileDate;
            }
        }

        if (this.watchedApiDate != null) {
            if (watchedDate == null) {
                watchedDate = this.watchedApiDate;
            } else if (watchedDate.before(watchedApiDate)) {
                watchedDate = this.watchedApiDate;
            }
        }
        
        return watchedDate;
    }
}
