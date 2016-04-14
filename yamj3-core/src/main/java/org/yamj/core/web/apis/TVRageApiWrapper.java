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
package org.yamj.core.web.apis;

import org.yamj.plugin.api.web.HTMLTools;
import org.yamj.plugin.api.web.TemporaryUnavailableException;

import com.omertron.tvrageapi.TVRageApi;
import com.omertron.tvrageapi.TVRageException;
import com.omertron.tvrageapi.model.EpisodeList;
import com.omertron.tvrageapi.model.ShowInfo;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yamj.api.common.tools.ResponseTools;

@Service
public class TVRageApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TVRageApiWrapper.class);
    
    @Autowired
    private TVRageApi tvRageApi;

    public ShowInfo getShowInfoByTitle(String title, boolean throwTempError) {
        try {
            List<ShowInfo> showList = tvRageApi.searchShow(HTMLTools.encodePlain(title));

            if (CollectionUtils.isEmpty(showList)) {
                // failed retrieving any results
                return null;
            }

            for (ShowInfo si : showList) {
                if (title.equalsIgnoreCase(si.getShowName())) {
                    return si;
                }
            }
        } catch (TVRageException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TVRage service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get TVRage ID by title '{}': {}", title, ex.getMessage());
            LOG.trace("TVRage error" , ex);
        }
        return null;
    }

    public ShowInfo getShowInfo(String tvRageId, boolean throwTempError) {
        ShowInfo showInfo = null;
        try {
            showInfo = tvRageApi.getShowInfo(NumberUtils.toInt(tvRageId));
        } catch (TVRageException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TVRage service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get show info using TVRage ID {}: {}", tvRageId, ex.getMessage());
            LOG.trace("TVRage error" , ex);
        }
        return showInfo;
    }

    public ShowInfo getShowInfoByVanityURL(String vanityUrl, boolean throwTempError) {
        ShowInfo showInfo = null;
        try {
            showInfo = tvRageApi.getShowInfo(vanityUrl);
        } catch (TVRageException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TVRage service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get show info using TVRage vanity url {}: {}", vanityUrl, ex.getMessage());
            LOG.trace("TVRage error" , ex);
        }
        return showInfo;
    }

    public EpisodeList getEpisodeList(String tvRageId, boolean throwTempError) {
        EpisodeList episodeList = null;
        try {
            episodeList = tvRageApi.getEpisodeList(tvRageId);
        } catch (TVRageException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TVRage service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get episodes using TVRage ID {}: {}", tvRageId, ex.getMessage());
            LOG.trace("TVRage error" , ex);
        }
        return episodeList;
    }
}
