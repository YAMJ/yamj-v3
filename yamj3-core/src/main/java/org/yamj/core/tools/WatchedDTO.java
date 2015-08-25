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
    
    private boolean watchedVideo = false;
    private Date watchedVideoDate;
    private boolean watchedMediaFile = true;
    private Date watchedMediaFileDate;
    private boolean watchedMediaApi = true;
    private Date watchedMediaApiDate;
  
    public void watchedVideo(boolean watched, Date watchedDate) {
        if (watchedDate == null) return;

        if (this.watchedVideoDate == null) {
            this.watchedVideo = watched;
            this.watchedVideoDate = watchedDate;
        } else if (watchedDate.after(this.watchedVideoDate)) {
            this.watchedVideo = watched;
            this.watchedVideoDate = watchedDate;
        }
    }

    public void watchedMediaFile(boolean watched, Date watchedDate) {
        if (watchedDate == null) return;

        if (this.watchedMediaFileDate == null) {
            this.watchedMediaFile = watched;
            this.watchedMediaFileDate = watchedDate;
        } else {
            this.watchedMediaFile = this.watchedMediaFile && watched;
            if (watchedDate.after(this.watchedMediaFileDate)) {            
                this.watchedMediaFileDate = watchedDate;
            }
        }
    }

    public void watchedMediaApi(boolean watched, Date watchedDate) {
        if (watchedDate == null) return;

        if (this.watchedMediaApiDate == null) {
            this.watchedMediaApi = watched;
            this.watchedMediaApiDate = watchedDate;
        } else {
            this.watchedMediaApi = this.watchedMediaApi && watched;
            if (watchedDate.after(this.watchedMediaApiDate)) {            
                this.watchedMediaApiDate = watchedDate;
            }
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
        
        if (this.watchedVideoDate != null) {
            watched = this.watchedVideo;
            watchedDate =  this.watchedVideoDate;
        }
        
        if (this.watchedMediaFileDate != null) {
            if (watchedDate == null) {
                watched = this.watchedMediaFile;
                watchedDate = this.watchedMediaFileDate;
            } else if (watchedDate.before(this.watchedMediaFileDate)) {
                watched = this.watchedMediaFile;
                watchedDate = this.watchedMediaFileDate;
            }
        }

        if (this.watchedMediaApiDate != null) {
            if (watchedDate == null) {
                watched = this.watchedMediaApi;
                watchedDate = this.watchedMediaApiDate;
            } else if (watchedDate.before(this.watchedMediaApiDate)) {
                watched = this.watchedMediaApi;
                watchedDate = this.watchedMediaApiDate;
            }
        }

        this.watched = Boolean.valueOf(watched); 
        this.watchedDate = watchedDate;
    }
}
