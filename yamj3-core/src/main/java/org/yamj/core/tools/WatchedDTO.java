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

    private Boolean watched = null;
    private Date watchedDate = null;
    
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
        } else if (watchedDate.after(this.watchedNfoDate)) {
            this.watchedNfoDate = watchedDate;
            this.watchedNfo = this.watchedNfo && watched;
        }
    }

    public void setWatchedFile(boolean watched, Date watchedDate) {
        if (watchedDate == null) return;

        if (this.watchedFileDate == null) {
            this.watchedFile = watched;
            this.watchedFileDate = watchedDate;
        } else if (watchedDate.after(this.watchedFileDate)) {
            this.watchedFile = this.watchedFile && watched;
            this.watchedFileDate = watchedDate;
        }
    }

    public void setWatchedApi(boolean watched, Date watchedDate) {
        if (watchedDate == null) return;

        if (this.watchedApiDate == null) {
            this.watchedApi = watched;
            this.watchedApiDate = watchedDate;
        } else if (watchedDate.after(this.watchedApiDate)) {
            this.watchedApi = this.watchedApi && watched;
            this.watchedApiDate = watchedDate;
        }
    }

    public boolean isWatched() {
        this.evaluateWatched();
        return this.watched.booleanValue();
    }

    public Date getWatchedDate() {
        this.evaluateWatched();
        return this.watchedDate;
    }

    private void evaluateWatched() {
        if (this.watched != null) return;
        
        boolean watched = false;
        Date watchedDate = null;
        
        if (this.watchedNfoDate != null) {
            watched = this.watchedNfo;
            watchedDate =  this.watchedNfoDate;
        }
        
        if (this.watchedFileDate != null) {
            if (watchedDate == null) {
                watched = this.watchedFile;
                watchedDate = this.watchedFileDate;
            } else if (watchedDate.before(this.watchedFileDate)) {
                watched = this.watchedFile;
                watchedDate = this.watchedFileDate;
            }
        }

        if (this.watchedApiDate != null) {
            if (watchedDate == null) {
                watched = this.watchedApi;
                watchedDate = this.watchedApiDate;
            } else if (watchedDate.before(this.watchedApiDate)) {
                watched = this.watchedApi;
                watchedDate = this.watchedApiDate;
            }
        }

        this.watched = Boolean.valueOf(watched); 
        this.watchedDate = watchedDate;
    }
}
