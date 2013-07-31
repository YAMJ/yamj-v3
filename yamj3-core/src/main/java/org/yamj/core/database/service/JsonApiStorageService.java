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
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.Parameters;
import org.yamj.core.api.model.dto.IndexArtworkDTO;
import org.yamj.core.api.model.dto.IndexPersonDTO;
import org.yamj.core.api.model.dto.IndexVideoDTO;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.dao.ApiDao;
import org.yamj.core.database.dao.ArtworkDao;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.*;

@Service("jsonApiStorageService")
public class JsonApiStorageService {

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private ArtworkDao artworkDao;
    @Autowired
    private ApiDao apiDao;
    @Autowired
    ConfigService configService;

    @Transactional(readOnly = true)
    public <T> T getEntityById(Class<T> entityClass, Serializable id) {
        return commonDao.getById(entityClass, id);
    }

    @Transactional(readOnly = true)
    public Configuration getConfiguration(String property) {
        return configService.getConfiguration(property);
    }

    @Transactional(readOnly = true)
    public List<Configuration> getConfiguration() {
        return configService.getConfiguration();
    }

    //<editor-fold defaultstate="collapsed" desc="Index Methods">
    @Transactional(readOnly = true)
    public void getVideoList(ApiWrapperList<IndexVideoDTO> wrapper) {
        apiDao.getVideoList(wrapper);
    }

    @Transactional(readOnly = true)
    public CountTimestamp getCountTimestamp(MetaDataType type) {
        CountTimestamp ct = null;
        if (type.equals(MetaDataType.MOVIE)) {
            ct = apiDao.getCountTimestamp(type, "videodata", "episode<0");
        } else if (type.equals(MetaDataType.SERIES)) {
            ct = apiDao.getCountTimestamp(type, "series", "");
        } else if (type.equals(MetaDataType.SEASON)) {
            ct = apiDao.getCountTimestamp(type, "season", "");
        } else if (type.equals(MetaDataType.EPISODE)) {
            ct = apiDao.getCountTimestamp(type, "videodata", "episode>=0");
        } else if (type.equals(MetaDataType.PERSON)) {
            ct = apiDao.getCountTimestamp(type, "person", "");
        }
        return ct;
    }

    @Transactional(readOnly = true)
    public void getPersonList(ApiWrapperList<IndexPersonDTO> wrapper) {
        apiDao.getPersonList(wrapper);

    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Video Methods">
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

    @Transactional(readOnly = true)
    public List<Genre> getGenreFilename(ApiWrapperList<Genre> wrapper, String filename) {
        return commonDao.getGenreFilename(wrapper, filename);
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
    public IndexArtworkDTO getArtworkById(Long id) {
        return apiDao.getArtworkById(id);
    }

    @Transactional(readOnly = true)
    public List<IndexArtworkDTO> getArtworkList(ApiWrapperList<IndexArtworkDTO> wrapper) {
        return apiDao.getArtworkList(wrapper);
    }

    //</editor-fold>
}
