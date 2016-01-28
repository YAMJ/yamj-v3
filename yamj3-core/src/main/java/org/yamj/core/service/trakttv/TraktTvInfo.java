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
package org.yamj.core.service.trakttv;

import java.util.Date;

public class TraktTvInfo {

    private boolean scrobble;
    private boolean push;
    private boolean pull;
    private boolean authorized;
    private Date expirationDate;
    private String message;
    
    public boolean isScrobble() {
        return scrobble;
    }

    public TraktTvInfo setScrobble(boolean scrobble) {
        this.scrobble = scrobble;
        return this;
    }
    
    public boolean isPush() {
        return push;
    }

    public TraktTvInfo setPush(boolean push) {
        this.push = push;
        return this;
    }

    public boolean isPull() {
        return pull;
    }

    public TraktTvInfo setPull(boolean pull) {
        this.pull = pull;
        return this;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public TraktTvInfo setAuthorized(boolean authorized) {
        this.authorized = authorized;
        return this;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public TraktTvInfo setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public TraktTvInfo setMessage(String message) {
        this.message = message;
        return this;
    }
}
