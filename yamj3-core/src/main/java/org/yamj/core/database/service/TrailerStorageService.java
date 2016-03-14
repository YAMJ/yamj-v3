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
package org.yamj.core.database.service;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.dao.MetadataDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;

@Service("trailerStorageService")
public class TrailerStorageService {

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private MetadataDao metadataDao;
    
    @Transactional(readOnly = true)
    public List<QueueDTO> getTrailerQueueForScanning(final int maxResults) {
        return metadataDao.getMetadataQueue(Trailer.QUERY_SCANNING_QUEUE, maxResults);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getTrailerQueueForProcessing(final int maxResults) {
        return commonDao.getQueueIdOnly(Trailer.QUERY_PROCESSING_QUEUE, maxResults);
    }

    @Transactional(readOnly = true)
    public VideoData getRequiredVideoData(Long id) {
        List<VideoData> objects = this.commonDao.namedQueryById(VideoData.QUERY_REQUIRED_FOR_TRAILER, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(readOnly = true)
    public Series getRequiredSeries(Long id) {
        List<Series> objects = this.commonDao.namedQueryById(Series.QUERY_REQUIRED_FOR_TRAILER, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }
    
    @Transactional(readOnly = true)
    public Trailer getRequiredTrailer(Long id) {
        List<Trailer> objects = this.commonDao.namedQueryById(Trailer.QUERY_REQUIRED, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional
    public void errorTrailer(Long id) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("status", StatusType.ERROR);
        commonDao.executeUpdate(Trailer.UPDATE_STATUS, params);
    }

    @Transactional
    public void errorTrailerVideoData(Long id) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("status", StatusType.ERROR);
        commonDao.executeNamedQueryUpdate(VideoData.UPDATE_TRAILER_STATUS, params);
    }

    @Transactional
    public void errorTrailerSeries(Long id) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", id);
        params.put("status", StatusType.ERROR);
        commonDao.executeNamedQueryUpdate(Series.UPDATE_TRAILER_STATUS, params);
    }

    @Transactional
    public void updateTrailer(Trailer trailer) {
        this.commonDao.updateEntity(trailer);
    }

    @Transactional
    public void updateTrailer(VideoData videoData, List<Trailer> trailers) {
        if (CollectionUtils.isEmpty(videoData.getTrailers())) {
            // no trailers presents; just store all
            this.commonDao.storeAll(trailers);
        } else if (CollectionUtils.isNotEmpty(trailers)) {
            for (Trailer trailer : trailers) {
                final int index = videoData.getTrailers().indexOf(trailer);
                if (index < 0) {
                    // just store if not contained before
                    videoData.getTrailers().add(trailer);
                    commonDao.saveEntity(trailer);
                } else {
                    // reset deletion status
                    Trailer stored = videoData.getTrailers().get(index);
                    resetDeletionStatus(stored);
                }
            }
        }

        // update not found stage files to DONE
        for (Trailer trailer : trailers) {
            StageFile stageFile = trailer.getStageFile();
            if (stageFile != null && stageFile.isNotFound()) {
                stageFile.setStatus(StatusType.DONE);
                this.commonDao.updateEntity(stageFile);
            }
        }
        
        // set status of video data
        videoData.setTrailerLastScanned(new Date());
        if (CollectionUtils.isEmpty(trailers) && CollectionUtils.isEmpty(videoData.getTrailers())) {
            videoData.setTrailerStatus(StatusType.NOTFOUND);
        } else {
            videoData.setTrailerStatus(StatusType.DONE);
        }

        // update artwork in database
        this.commonDao.updateEntity(videoData);
    }

    @Transactional
    public void updateTrailer(Series series, List<Trailer> trailers) {
        if (CollectionUtils.isEmpty(series.getTrailers())) {
            // no trailers presents; just store all
            this.commonDao.storeAll(trailers);
        } else if (CollectionUtils.isNotEmpty(trailers)) {
            for (Trailer trailer : trailers) {
                final int index = series.getTrailers().indexOf(trailer);
                if (index < 0) {
                    // just store if not contained before
                    series.getTrailers().add(trailer);
                    commonDao.saveEntity(trailer);
                } else {
                    // reset deletion status
                    Trailer stored = series.getTrailers().get(index);
                    resetDeletionStatus(stored);
                }
            }
        }

        // update not found stage files to DONE
        for (Trailer trailer : trailers) {
            StageFile stageFile = trailer.getStageFile();
            if (stageFile != null && stageFile.isNotFound()) {
                stageFile.setStatus(StatusType.DONE);
                this.commonDao.updateEntity(stageFile);
            }
        }
        
        // set status of series
        series.setTrailerLastScanned(new Date());
        if (CollectionUtils.isEmpty(trailers) && CollectionUtils.isEmpty(series.getTrailers())) {
            series.setTrailerStatus(StatusType.NOTFOUND);
        } else {
            series.setTrailerStatus(StatusType.DONE);
        }

        // update artwork in database
        this.commonDao.updateEntity(series);
    }
    
    private void resetDeletionStatus(Trailer trailer) {
        if (trailer.isDeleted()) {
            trailer.setStatus(trailer.getPreviousStatus());
            this.commonDao.updateEntity(trailer);
        }
    }

}

