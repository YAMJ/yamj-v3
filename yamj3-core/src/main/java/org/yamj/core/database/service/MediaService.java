/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.service;

import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.dao.MediaDao;
import org.yamj.core.database.dao.PersonDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;

@Service("mediaService")
public class MediaService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaService.class);

    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private CommonDao commonDao;

    public void save(Object entity) {
        this.commonDao.saveEntity(entity);
    }

    public void update(Object entity) {
        this.commonDao.updateEntity(entity);
    }

    public VideoData getRequiredVideoData(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from VideoData vd ");
        sb.append("left outer join fetch vd.genres ");
        sb.append("left outer join fetch vd.credits c ");
        sb.append("where vd.id = ?" );

        @SuppressWarnings("unchecked")
        List<VideoData> objects = this.commonDao.getObjectsById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    public Series getRequiredSeries(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from Series ser ");
        sb.append("join fetch ser.seasons sea ");
        sb.append("join fetch sea.videoDatas vd ");
        sb.append("left outer join fetch vd.credits c ");
        sb.append("where ser.id = ?" );

        @SuppressWarnings("unchecked")
        List<Series> objects = this.commonDao.getObjectsById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    public Person getRequiredPerson(Long id) {
        final StringBuilder sb = new StringBuilder();
        sb.append("from Person person where person.id = ?");

        // later on there it could be necessary to fetch associated entities

        @SuppressWarnings("unchecked")
        List<Person> objects = this.commonDao.getObjectsById(sb, id);
        return DataAccessUtils.requiredUniqueResult(objects);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void store(VideoData videoData) {
        // update entity
        mediaDao.updateEntity(videoData);

        // update genres
        updateGenres(videoData);

        // update cast and crew
        updateCastCrew(videoData);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void store(Series series) {
        // update entity
        mediaDao.updateEntity(series);

        // update underlying seasons and episodes
        for (Season season : series.getSeasons()) {
            if (StatusType.PROCESSED.equals(season.getStatus())) {
                season.setStatus(StatusType.DONE);
                mediaDao.updateEntity(season);
            }
            for (VideoData videoData : season.getVideoDatas()) {
                if (StatusType.PROCESSED.equals(videoData.getStatus())) {
                    videoData.setStatus(StatusType.DONE);
                    store(videoData);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void store(Person person) {
        // update entity
        personDao.updateEntity(person);
    }

    private void updateGenres(VideoData videoData) {
        HashSet<Genre> genres = new HashSet<Genre>(0);
        for (Genre genre : videoData.getGenres()) {
            Genre stored = commonDao.getGenre(genre.getName());
            if (stored == null) {
                commonDao.saveEntity(genre);
                genres.add(genre);
            } else {
                genres.add(stored);
            }
        }
        videoData.setGenres(genres);
    }

    private void updateCastCrew(VideoData videoData) {
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

            // find person if not found
            if (person == null) {
                LOG.info("Attempting to retrieve information on '{}' from database", dto.getName());
                person = personDao.getPerson(dto.getName());
            } else {
                LOG.debug("Found '{}' in cast table", person.getName());
            }

            if (person != null) {
                // update person id
                if (person.setPersonId(dto.getSourcedb(), dto.getSourcedbId())) {
                    personDao.updateEntity(person);
                }
            } else {
                // create new person
                person = new Person();
                person.setName(dto.getName());
                person.setPersonId(dto.getSourcedb(), dto.getSourcedbId());
                person.setStatus(StatusType.NEW);
                personDao.saveEntity(person);
            }

            if (castCrew == null) {
                // create new association between person and video
                castCrew = new CastCrew();
                castCrew.setPerson(person);
                castCrew.setJob(dto.getJobType(), dto.getRole());
                castCrew.setVideoData(videoData);
                videoData.addCredit(castCrew);
                personDao.saveEntity(castCrew);
            } else if (castCrew.setJob(castCrew.getJobType(), dto.getRole())) {
                // updated role
                personDao.updateEntity(castCrew);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void errorVideoData(Long id) {
        VideoData videoData = mediaDao.getVideoData(id);
        if (videoData != null) {
            videoData.setStatus(StatusType.ERROR);
            mediaDao.updateEntity(videoData);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void errorSeries(Long id) {
        Series series = mediaDao.getSeries(id);
        if (series != null) {
            series.setStatus(StatusType.ERROR);
            mediaDao.updateEntity(series);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void errorPerson(Long id) {
        Person person = personDao.getPerson(id);
        if (person != null) {
            person.setStatus(StatusType.ERROR);
            personDao.updateEntity(person);
        }
    }
}
