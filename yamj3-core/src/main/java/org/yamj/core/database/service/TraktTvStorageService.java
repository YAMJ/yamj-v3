/*
 *      Copyright (c)
 2004-2015 YAMJ Members
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
package org.yamj.core.database.service;

import static org.yamj.plugin.api.common.Constants.SOURCE_TRAKTTV;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.database.dao.TraktTvDao;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.TraktEpisodeDTO;
import org.yamj.core.database.model.dto.TraktMovieDTO;

@Service("traktTvStorageService")
public class TraktTvStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(TraktTvStorageService.class);

    @Autowired
    private TraktTvDao traktTvDao;
    @Autowired
    private MetadataStorageService metadataStorageService;

    @Transactional(readOnly = true)
    public Map<String,List<Long>> getAllMovieIds() {
        return this.traktTvDao.getAllMovieIds();
    }

    @Transactional(readOnly = true)
    public Map<String,List<Long>> getAllEpisodeIds() {
        return this.traktTvDao.getAllEpisodeIds();
    }

    @Transactional(readOnly = true)
    public Collection<TraktMovieDTO> getWatchedMovies(Date checkDate) {
        return this.traktTvDao.getWatchedMovies(checkDate);
    }

    @Transactional(readOnly = true)
    public Collection<TraktEpisodeDTO> getWatchedEpisodes(Date checkDate) {
        return this.traktTvDao.getWatchedEpisodes(checkDate);
    }

    @Transactional(readOnly = true)
    public Collection<TraktMovieDTO> getCollectedMovies(Date checkDate) {
        return this.traktTvDao.getCollectedMovies(checkDate);
    }

    @Transactional(readOnly = true)
    public Collection<TraktEpisodeDTO> getCollectedEpisodes(Date checkDate) {
        return this.traktTvDao.getCollectedEpisodes(checkDate);
    }

    @Transactional
    public void updateWatched(String traktTvId, Date lastWatched, Collection<Long> ids) {
        boolean updated;
        for (Long id : ids) {
            updated = false;
            VideoData videoData = metadataStorageService.getRequiredVideoData(id);
            
            if (!StringUtils.equals(videoData.getSourceDbId(SOURCE_TRAKTTV), traktTvId)) {
                updated = true;
                videoData.setSourceDbId(SOURCE_TRAKTTV, traktTvId);
            }
            
            if (videoData.getWatchedTraktTvLastDate() == null || videoData.getWatchedTraktTvLastDate().before(lastWatched)) {
                updated = true;
                videoData.setWatchedTraktTv(true, lastWatched);
            }
            
            if (updated) {
                LOG.debug("Trakt.TV watched video: {}", videoData.getIdentifier());
                traktTvDao.updateEntity(videoData);
            }
        }
    }    

    @Transactional
    public void updateWatched(Date lastWatched, Collection<Long> ids) {
        for (Long id : ids) {
            VideoData videoData = metadataStorageService.getRequiredVideoData(id);
            if (videoData.getWatchedTraktTvLastDate() == null || videoData.getWatchedTraktTvLastDate().before(lastWatched)) {
                videoData.setWatchedTraktTv(true, lastWatched);
                LOG.debug("Trakt.TV watched video: {}", videoData.getIdentifier());
                traktTvDao.updateEntity(videoData);
            }
        }
    }    
}
