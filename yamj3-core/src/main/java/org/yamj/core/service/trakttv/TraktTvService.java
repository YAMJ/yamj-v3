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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.trakttv.TraktTvApi;
import org.yamj.api.trakttv.TraktTvException;
import org.yamj.api.trakttv.auth.TokenResponse;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.config.ConfigService;

@Service("traktTvService")
public class TraktTvService {

    private static final String TRAKTTV_AUTH_TOKEN = "trakttv.auth.access";
    private static final String TRAKTTV_REFRESH_TOKEN = "trakttv.auth.refresh";
    private static final String TRAKTTV_EXPIRATION = "trakttv.auth.expiration";
    
    private static final Logger LOG = LoggerFactory.getLogger(TraktTvService.class);
    
    @Autowired
    private ConfigService configService;
    @Autowired
    private TraktTvApi traktTvApi;
    
    public TraktTvInfo getTraktTvInfo() {
        TraktTvInfo traktTvInfo = new TraktTvInfo();
        // static values
        traktTvInfo.setPush(PropertyTools.getBooleanProperty("trakttv.scrobble.push", Boolean.FALSE));
        traktTvInfo.setPull(PropertyTools.getBooleanProperty("trakttv.scrobble.pull", Boolean.FALSE));
        traktTvInfo.setScrobble(traktTvInfo.isPush()|| traktTvInfo.isPull());
        // dynamic values
        traktTvInfo.setAuthorized(configService.getProperty(TRAKTTV_AUTH_TOKEN)!=null);
        long expiresAt = configService.getLongProperty(TRAKTTV_EXPIRATION, -1);
        if (expiresAt > 0) {
            traktTvInfo.setExpirationDate(new Date(expiresAt));
        }
        return traktTvInfo;
    }
    
    public String authorizeWithPin(String pin) {
        try {
            TokenResponse response = this.traktTvApi.requestAccessTokenByPin(pin);
            // set access token for API
            traktTvApi.setAccessToken(response.getAccessToken());

            // expiration date: creation date + expiration period * 1000 (cause given in seconds)
            long expireDate = (response.getCreatedAt() + response.getExpiresIn()) * 1000L;
            
            // store values in config for later use
            configService.setProperty(TRAKTTV_EXPIRATION, expireDate);
            configService.setProperty(TRAKTTV_REFRESH_TOKEN, response.getRefreshToken());
            configService.setProperty(TRAKTTV_AUTH_TOKEN, response.getAccessToken());
            // no authorization error
            return null;
        } catch (TraktTvException e) {
            LOG.debug("TraktTv error", e);
            return e.getResponse();
        } catch (Exception e) {
            LOG.debug("Unknown error", e);
            return "Unknow error occured";
        }
    }
}
