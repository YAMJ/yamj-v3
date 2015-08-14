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

import java.util.Date;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.TrailerDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;

@Service("trailerStorageService")
public class TrailerStorageService {

    @Autowired
    private TrailerDao trailerDao;
    
    @Transactional(readOnly = true)
    public List<QueueDTO> getTrailerQueueForScanning(final int maxResults) {
        return trailerDao.getTrailerQueueForScanning(maxResults);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getTrailerQueueForProcessing(final int maxResults) {
        return trailerDao.getTrailerQueueForProcessing(maxResults);
    }

    @Transactional(readOnly = true)
    public VideoData getRequiredVideoData(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("FROM VideoData vd ");
        sb.append("LEFT OUTER JOIN FETCH vd.trailers t ");
        sb.append("LEFT OUTER JOIN FETCH t.stageFile s ");
        sb.append("WHERE vd.id = :id ");

        List<VideoData> objects = this.trailerDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(readOnly = true)
    public Series getRequiredSeries(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("FROM Series ser ");
        sb.append("LEFT OUTER JOIN FETCH ser.trailers t ");
        sb.append("LEFT OUTER JOIN FETCH t.stageFile s ");
        sb.append("WHERE ser.id = :id ");

        List<Series> objects = this.trailerDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }
    
    @Transactional(readOnly = true)
    public Trailer getRequiredTrailer(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("FROM Trailer t ");
        sb.append("LEFT OUTER JOIN FETCH t.videoData ");
        sb.append("LEFT OUTER JOIN FETCH t.series ");
        sb.append("LEFT OUTER JOIN FETCH t.stageFile ");
        sb.append("WHERE t.id = :id");

        List<Trailer> objects = this.trailerDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional
    public boolean errorTrailer(Long id) {
        Trailer trailer = trailerDao.getById(Trailer.class, id);
        if (trailer != null) {
            trailer.setStatus(StatusType.ERROR);
            trailerDao.updateEntity(trailer);
            return true;
        }
        return false;
    }

    @Transactional
    public void errorTrailerVideoData(Long id) {
        VideoData videoData = trailerDao.getById(VideoData.class, id);
        if (videoData != null) {
            videoData.setTrailerStatus(StatusType.ERROR);
            trailerDao.updateEntity(videoData);
        }
    }

    @Transactional
    public void errorTrailerSeries(Long id) {
        Series series = trailerDao.getById(Series.class, id);
        if (series != null) {
            series.setTrailerStatus(StatusType.ERROR);
            trailerDao.updateEntity(series);
        }
    }

    @Transactional
    public void updateTrailer(Trailer trailer) {
        this.trailerDao.updateEntity(trailer);
    }

    @Transactional
    public void updateTrailer(VideoData videoData, List<Trailer> trailers) {
        if (CollectionUtils.isEmpty(videoData.getTrailers())) {
            // no trailers presents; just store all
            this.trailerDao.storeAll(trailers);
        } else if (CollectionUtils.isNotEmpty(trailers)) {
            for (Trailer trailer : trailers) {
                if (!videoData.getTrailers().contains(trailer)) {
                    // just store if not contained before
                    videoData.getTrailers().add(trailer);
                    trailerDao.saveEntity(trailer);
                } else {
                    // find matching stored trailer and reset deleted status
                    for (Trailer stored : videoData.getTrailers()) {
                        if (stored.equals(trailer)) {
                            trailerDao.resetDeletionStatus(stored);
                            break;
                        }
                    }
                }
            }
        }

        // update not found stage files to DONE
        for (Trailer trailer : trailers) {
            StageFile stageFile = trailer.getStageFile();
            if (stageFile != null && StatusType.NOTFOUND.equals(stageFile.getStatus())) {
                stageFile.setStatus(StatusType.DONE);
                this.trailerDao.updateEntity(stageFile);
            }
        }
        
        // set status of video data
        videoData.setTrailerLastScanned(new Date(System.currentTimeMillis()));
        if (CollectionUtils.isEmpty(trailers) && CollectionUtils.isEmpty(videoData.getTrailers())) {
            videoData.setTrailerStatus(StatusType.NOTFOUND);
        } else {
            videoData.setTrailerStatus(StatusType.DONE);
        }

        // update artwork in database
        this.trailerDao.updateEntity(videoData);
    }

    @Transactional
    public void updateTrailer(Series series, List<Trailer> trailers) {
        if (CollectionUtils.isEmpty(series.getTrailers())) {
            // no trailers presents; just store all
            this.trailerDao.storeAll(trailers);
        } else if (CollectionUtils.isNotEmpty(trailers)) {
            for (Trailer trailer : trailers) {
                if (!series.getTrailers().contains(trailer)) {
                    // just store if not contained before
                    series.getTrailers().add(trailer);
                    trailerDao.saveEntity(trailer);
                } else {
                    // find matching stored trailer and reset deleted status
                    loop: for (Trailer stored : series.getTrailers()) {
                        if (stored.equals(trailer)) {
                            trailerDao.resetDeletionStatus(stored);
                            break loop;
                        }
                    }
                }
            }
        }

        // update not found stage files to DONE
        for (Trailer trailer : trailers) {
            StageFile stageFile = trailer.getStageFile();
            if (stageFile != null && StatusType.NOTFOUND.equals(stageFile.getStatus())) {
                stageFile.setStatus(StatusType.DONE);
                this.trailerDao.updateEntity(stageFile);
            }
        }
        
        // set status of series
        series.setTrailerLastScanned(new Date(System.currentTimeMillis()));
        if (CollectionUtils.isEmpty(trailers) && CollectionUtils.isEmpty(series.getTrailers())) {
            series.setTrailerStatus(StatusType.NOTFOUND);
        } else {
            series.setTrailerStatus(StatusType.DONE);
        }

        // update artwork in database
        this.trailerDao.updateEntity(series);
    }
}

