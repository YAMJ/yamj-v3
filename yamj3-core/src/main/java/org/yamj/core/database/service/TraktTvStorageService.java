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

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.api.trakttv.model.Ids;
import org.yamj.api.trakttv.model.TrackedMovie;
import org.yamj.core.database.dao.TraktTvDao;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.metadata.online.TraktTvScanner;

@Service("traktTvStorageService")
public class TraktTvStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(TraktTvStorageService.class);

    @Autowired
    private TraktTvDao traktTvDao;
    @Autowired
    private MetadataStorageService metadataStorageService;
    
    @Transactional(readOnly = true)
    public List<Long> getIdsForMovies(Ids ids) {
        return traktTvDao.findMovieByIDs(ids);
        
    }
    
    @Transactional
    public void updateWatched(TrackedMovie trackedMovie, List<Long> ids) {
        final String traktTvId = trackedMovie.getMovie().getIds().trakt().toString();
        final Date lastWatched = trackedMovie.getLastWatchedAt().withMillisOfSecond(0).toDate();
        
        for (Long id : ids) {
            boolean updated = false;
            VideoData videoData = metadataStorageService.getRequiredVideoData(id);
            if (!StringUtils.equals(videoData.getSourceDbId(TraktTvScanner.SCANNER_ID), traktTvId)) {
                updated = true;
                videoData.setSourceDbId(TraktTvScanner.SCANNER_ID, traktTvId);
            }
            
            if (videoData.getWatchedTraktTvLastDate() == null || videoData.getWatchedTraktTvLastDate().before(lastWatched)) {
                updated = true;
                videoData.setWatchedTraktTv(true, lastWatched);
            }
            
            if (updated) {
                LOG.debug("Trakt.TV watched movie: {}", videoData.getIdentifier());
                traktTvDao.updateEntity(videoData);
            }
        }
    }
    
}
