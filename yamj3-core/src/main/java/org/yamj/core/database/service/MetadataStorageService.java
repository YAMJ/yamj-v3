/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.VideoData;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.dao.MetadataDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.StepType;

@Service("metadataStorageService")
public class MetadataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataStorageService.class);
    @Autowired
    private CommonDao commonDao;
    @Autowired
    private MetadataDao metadataDao;

    @Transactional
    public void save(Object entity) {
        this.commonDao.saveEntity(entity);
    }

    @Transactional
    public void update(Object entity) {
        this.commonDao.updateEntity(entity);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getMediaQueueForScanning(final int maxResults, StepType step) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select vd.id,'");
        sql.append(MetaDataType.MOVIE);
        sql.append("' as mediatype,vd.create_timestamp,vd.update_timestamp ");
        sql.append("from videodata vd ");
        sql.append("where vd.status in ('NEW','UPDATED') ");
        sql.append("and vd.step='");
        sql.append(step.name());
        sql.append("' and vd.episode<0 ");
        sql.append("union ");
        sql.append("select ser.id,'");
        sql.append(MetaDataType.SERIES);
        sql.append("' as mediatype,ser.create_timestamp,ser.update_timestamp ");
        sql.append("from series ser, season sea, videodata vd ");
        sql.append("where ser.id=sea.series_id ");
        sql.append("and ser.step='");
        sql.append(step.name());
        sql.append("' and sea.id=vd.season_id ");
        sql.append("and (ser.status in ('NEW','UPDATED') ");
        sql.append(" or  (ser.status='DONE' and sea.status in ('NEW','UPDATED')) ");
        sql.append(" or  (ser.status='DONE' and vd.status in  ('NEW','UPDATED'))) ");

        return metadataDao.getMetadataQueue(sql, maxResults);
    }

    @Transactional(readOnly = true)
    public List<QueueDTO> getPersonQueueForScanning(final int maxResults) {
        final StringBuilder sql = new StringBuilder();
        sql.append("select id, '");
        sql.append(MetaDataType.PERSON);
        sql.append("' as mediatype, create_timestamp, update_timestamp ");
        sql.append("from person ");
        sql.append("where status in ('NEW','UPDATED') ");

        return metadataDao.getMetadataQueue(sql, maxResults);
    }

    @Transactional(readOnly = true)
    public VideoData getRequiredVideoData(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from VideoData vd ");
        sb.append("left outer join fetch vd.genres ");
        sb.append("left outer join fetch vd.credits c ");
        sb.append("where vd.id = :id");

        @SuppressWarnings("unchecked")
        List<VideoData> objects = this.commonDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(readOnly = true)
    public Series getRequiredSeries(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from Series ser ");
        sb.append("join fetch ser.seasons sea ");
        sb.append("join fetch sea.videoDatas vd ");
        sb.append("left outer join fetch vd.credits c ");
        sb.append("where ser.id = :id");

        @SuppressWarnings("unchecked")
        List<Series> objects = this.commonDao.findById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(readOnly = true)
    public Person getRequiredPerson(Long id) {
        // later on there it could be necessary to fetch associated entities
        return metadataDao.getById(Person.class, id);
    }

    /**
     * Store associated entities, like genres or cast.
     * 
     * @param videoData
     */
    public void storeAssociatedEntities(VideoData videoData) {
        
        // store new genres
        for (String genreName : videoData.getGenreNames()) {
            try {
                this.commonDao.storeNewGenre(genreName);
            } catch (Exception ex) {
                LOG.error("Failed to store genre '{}', error: {}", genreName, ex.getMessage());
                LOG.trace("Storage error", ex);
            }
        }

        // store persons
        for (CreditDTO creditDTO : videoData.getCreditDTOS()) {
            try {
                this.metadataDao.storePerson(creditDTO);
            } catch (Exception ex) {
                LOG.error("Failed to store person '{}', error: {}", creditDTO.getName(), ex.getMessage());
                LOG.trace("Storage error", ex);
            }
        }
    }

    /**
     * Store associated entities, like genres or cast.
     * 
     * @param series
     */
    public void storeAssociatedEntities(Series series) {

        // store new genres
        for (String genreName : series.getGenreNames()) {
            try {
                this.commonDao.storeNewGenre(genreName);
            } catch (Exception ex) {
                LOG.error("Failed to store genre '{}', error: {}", genreName, ex.getMessage());
                LOG.trace("Storage error", ex);
            }
        }

        for (Season season : series.getSeasons()) {
            for (VideoData videoData : season.getVideoDatas()) {
                this.storeAssociatedEntities(videoData);
            }
        }
    }

    @Transactional
    public void updatePerson(Person person) {
        // update entity
        metadataDao.updateEntity(person);
    }

    @Transactional
    public void updateVideoData(VideoData videoData) {
        StepType actualStep = videoData.getStep();
        this.updateVideoData(videoData, actualStep);
    }

    private void updateVideoData(VideoData videoData, StepType actualStep) {
        // set next step
        videoData.setNextStep(actualStep);
        
        // update entity
        metadataDao.updateEntity(videoData);

        // update genres
        updateGenres(videoData);

        // update cast and crew
        updateCastCrew(videoData);
    }

    /**
     * Set the next step in processing
     * 
     * @param videoData
     */
    @Transactional
    public void setNextStep(VideoData videoData) {
        StepType actualStep = videoData.getStep();
        videoData.setNextStep(actualStep);
        metadataDao.updateEntity(videoData);
    }

    @Transactional
    public void updateSeries(Series series) {
        // get actual step
        StepType actualStep = series.getStep(); 
        // set next step
        series.setNextStep(actualStep);
        
        // update entity
        metadataDao.updateEntity(series);

        // update genres
        updateGenres(series);

        // update underlying seasons and episodes
        for (Season season : series.getSeasons()) {
            season.setNextStep(actualStep);
            metadataDao.updateEntity(season);

            for (VideoData videoData : season.getVideoDatas()) {
                updateVideoData(videoData, actualStep);
            }
        }
    }

    /**
     * Set the next step in processing
     * 
     * @param series
     */
    @Transactional
    public void setNextStep(Series series) {
        StepType actualStep = series.getStep();
        series.setNextStep(actualStep);
        metadataDao.updateEntity(series);

        // update underlying seasons and episodes
        for (Season season : series.getSeasons()) {
            season.setNextStep(actualStep);
            metadataDao.updateEntity(season);

            for (VideoData videoData : season.getVideoDatas()) {
                videoData.setNextStep(actualStep);
                metadataDao.updateEntity(videoData);
            }
        }
    }

    /**
     * Update genres for VideoData from the database
     *
     * @param videoData
     */
    private void updateGenres(VideoData videoData) {
        if (CollectionUtils.isEmpty(videoData.getGenreNames())) {
            return;
        }

        Set<Genre> genres = new LinkedHashSet<Genre>();
        for (String genreName : videoData.getGenreNames()) {
            Genre genre = commonDao.getByName(Genre.class, genreName);
            if (genre != null) {
                genres.add(genre);
            }
        }
        videoData.setGenres(genres);
    }

    /**
     * Update genres for Series from the database
     *
     * @param series
     */
    private void updateGenres(Series series) {
        if (CollectionUtils.isEmpty(series.getGenreNames())) {
            return;
        }

        Set<Genre> genres = new LinkedHashSet<Genre>();
        for (String genreName : series.getGenreNames()) {
            Genre genre = commonDao.getByName(Genre.class, genreName);
            if (genre != null) {
                genres.add(genre);
            }
        }
        series.setGenres(genres);
    }

    /**
     * Update cast and crew to the database
     *
     * @param videoData
     */
    private void updateCastCrew(VideoData videoData) {
        if (CollectionUtils.isEmpty(videoData.getCreditDTOS())) {
            return;
        }
        
        for (CreditDTO dto : videoData.getCreditDTOS()) {
            Person person = null;
            CastCrew castCrew = null;

            for (CastCrew credit : videoData.getCredits()) {
                if ((credit.getJobType() == dto.getJobType()) && StringUtils.equalsIgnoreCase(dto.getName(), credit.getPerson().getName())) {
                    castCrew = credit;
                    person = credit.getPerson();
                    break;
                }
            }

            // find person if not found in cast 
            if (person == null) {
                LOG.info("Attempting to retrieve information on '{}' from database", dto.getName());
                person = metadataDao.getByName(Person.class, dto.getName());
            } else {
                LOG.debug("Found '{}' in cast table", person.getName());
            }

            if (person == null) {
                // NOTE: person should have been stored before; just be sure
                //       to avoid null constraint violation
                LOG.warn("Person '{}' not found, skipping", dto.getName());
            } else {
                
                try {
                    if (castCrew == null) {
                        // create new association between person and video
                        castCrew = new CastCrew();
                        castCrew.setPerson(person);
                        castCrew.setJob(dto.getJobType(), dto.getRole());
                        castCrew.setVideoData(videoData);
                        videoData.addCredit(castCrew);
                        metadataDao.saveEntity(castCrew);
                    } else if (castCrew.setJob(castCrew.getJobType(), dto.getRole())) {
                        // updated role
                        metadataDao.updateEntity(castCrew);
                    }
                } catch (ConstraintViolationException ex) {
                    LOG.warn("Failed to save/update record for person {}-{}, job '{}', error: {}", person.getId(), person.getName(), dto.getJobType(), ex.getMessage());
                }
            }
        }
    }

    @Transactional
    public void errorVideoData(Long id) {
        VideoData videoData = metadataDao.getById(VideoData.class, id);
        if (videoData != null) {
            videoData.setStatus(StatusType.ERROR);
            metadataDao.updateEntity(videoData);
        }
    }

    @Transactional
    public void errorSeries(Long id) {
        Series series = metadataDao.getById(Series.class, id);
        if (series != null) {
            series.setStatus(StatusType.ERROR);
            metadataDao.updateEntity(series);
        }
    }

    @Transactional
    public void errorPerson(Long id) {
        Person person = metadataDao.getById(Person.class, id);
        if (person != null) {
            person.setStatus(StatusType.ERROR);
            metadataDao.updateEntity(person);
        }
    }

    @Transactional
    public void saveArtwork(Artwork artwork) {
        if (artwork != null) {
            metadataDao.saveEntity(artwork);
        }
    }

    @Transactional
    public void updateArtwork(Artwork artwork) {
        if (artwork != null) {
            metadataDao.updateEntity(artwork);
        }
    }
}
