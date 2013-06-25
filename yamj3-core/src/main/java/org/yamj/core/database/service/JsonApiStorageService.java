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

import java.io.Serializable;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.api.model.Parameters;
import org.yamj.core.database.dao.ApiDao;
import org.yamj.core.database.dao.ArtworkDao;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.MetaDataType;

@Service("jsonApiStorageService")
public class JsonApiStorageService {

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private ArtworkDao artworkDao;
    @Autowired
    private ApiDao apiDao;

    @Transactional(readOnly = true)
    public List getTestData() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT vd.id, '");
        sql.append(MetaDataType.MOVIE);
        sql.append("' AS mediatype, vd.create_timestamp, vd.update_timestamp, vd.status ");
        sql.append("FROM videodata vd ");
        sql.append("WHERE vd.episode<0 ");
        sql.append("UNION ");
        sql.append("SELECT ser.id,'");
        sql.append(MetaDataType.SERIES);
        sql.append("' AS mediatype, ser.create_timestamp, ser.update_timestamp, ser.status ");
        sql.append("FROM series ser, season sea, videodata vd ");
        sql.append("WHERE ser.id=sea.series_id ");
        sql.append("AND   sea.id=vd.season_id");

        return apiDao.getVideoList(sql.toString(), 10);
    }

    @Transactional(readOnly = true)
    public <T> T getEntityById(Class<T> entityClass, Serializable id) {
        return commonDao.getById(entityClass, id);
    }

    //<editor-fold defaultstate="collapsed" desc="VideoData Methods">
    @Transactional(readOnly = true)
    public List<VideoData> getVideoList(Parameters params) {
        return commonDao.getVideoList(params);
    }

    @Transactional(readOnly = true)
    public List<Series> getSeriesList(Parameters params) {
        return commonDao.getSeriesList(params);
    }

    @Transactional(readOnly = true)
    public List<Season> getSeasonList(Parameters params) {
        return commonDao.getSeasonList(params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre Methods">
    @Transactional(readOnly = true)
    public Genre getGenre(Serializable id) {
        return commonDao.getById(Genre.class, id);
    }

    @Transactional(readOnly = true)
    public Genre getGenre(String name) {
        return commonDao.getByName(Genre.class, name);
    }

    @Transactional(readOnly = true)
    public List<Genre> getGenres(Parameters params) {
        return commonDao.getList(Genre.class, params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Certification Methods">
    @Transactional(readOnly = true)
    public Certification getCertification(Serializable id) {
        return commonDao.getById(Certification.class, id);
    }

    @Transactional(readOnly = true)
    public Certification getCertification(String name) {
        return commonDao.getByName(Certification.class, name);
    }

    @Transactional(readOnly = true)
    public List<Certification> getCertifications(Parameters params) {
        return commonDao.getList(Certification.class, params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Boxed Set Methods">
    @Transactional(readOnly = true)
    public BoxedSet getBoxedSet(Serializable id) {
        return commonDao.getById(BoxedSet.class, id);
    }

    @Transactional(readOnly = true)
    public BoxedSet getBoxedSet(String name) {
        return commonDao.getByName(BoxedSet.class, name);
    }

    @Transactional(readOnly = true)
    public List<BoxedSet> getBoxedSets(Parameters params) {
        return commonDao.getList(BoxedSet.class, params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Studio Methods">
    @Transactional(readOnly = true)
    public Studio getStudio(Serializable id) {
        return commonDao.getById(Studio.class, id);
    }

    @Transactional(readOnly = true)
    public Studio getStudio(String name) {
        return commonDao.getByName(Studio.class, name);
    }

    @Transactional(readOnly = true)
    public List<Studio> getStudios(Parameters params) {
        return commonDao.getList(Studio.class, params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Artwork Methods">
    @Transactional(readOnly = true)
    public List<Artwork> getArtworkList(Parameters params) {
        return artworkDao.getArtworkList(params);
    }
    //</editor-fold>
}
