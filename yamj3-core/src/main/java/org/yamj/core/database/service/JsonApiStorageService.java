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

import java.io.Serializable;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.dto.ApiArtworkDTO;
import org.yamj.core.api.model.dto.ApiEpisodeDTO;
import org.yamj.core.api.model.dto.ApiGenreDTO;
import org.yamj.core.api.model.dto.ApiPersonDTO;
import org.yamj.core.api.model.dto.ApiSeriesInfoDTO;
import org.yamj.core.api.model.dto.ApiVideoDTO;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.dao.ApiDao;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.dao.PlayerDao;
import org.yamj.core.database.model.BoxedSet;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Configuration;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.PlayerPath;
import org.yamj.core.database.model.Studio;

@Service("jsonApiStorageService")
public class JsonApiStorageService {

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private ApiDao apiDao;
    @Autowired
    private ConfigService configService;
    @Autowired
    private PlayerDao playerDao;

    @Transactional(readOnly = true)
    public <T> T getEntityById(Class<T> entityClass, Serializable id) {
        return commonDao.getById(entityClass, id);
    }

    @Transactional(readOnly = true)
    public List<Configuration> getConfiguration(String property) {
        return configService.getConfiguration(property);
    }

    //<editor-fold defaultstate="collapsed" desc="Index Methods">
    @Transactional(readOnly = true)
    public void getVideoList(ApiWrapperList<ApiVideoDTO> wrapper) {
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
    public void getPersonList(ApiWrapperList<ApiPersonDTO> wrapper) {
        apiDao.getPersonList(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Person Methods">
    @Transactional(readOnly = true)
    public void getPerson(ApiWrapperSingle<ApiPersonDTO> wrapper) {
        apiDao.getPerson(wrapper);
    }

    @Transactional(readOnly = true)
    public void getPersonListByVideoType(MetaDataType metaDataType, ApiWrapperList<ApiPersonDTO> wrapper) {
        apiDao.getPersonListByVideoType(metaDataType, wrapper);
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
    public List<ApiGenreDTO> getGenres(ApiWrapperList<ApiGenreDTO> wrapper) {
        return commonDao.getGenres(wrapper);
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
    public List<Certification> getCertifications(ApiWrapperList<Certification> wrapper) {
        return commonDao.getCertifications(wrapper);
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
    public List<BoxedSet> getBoxedSets(ApiWrapperList<BoxedSet> wrapper) {
        return commonDao.getBoxedSets(wrapper);
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
    public List<Studio> getStudios(ApiWrapperList<Studio> wrapper) {
        return commonDao.getStudios(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Artwork Methods">
    @Transactional(readOnly = true)
    public ApiArtworkDTO getArtworkById(Long id) {
        return apiDao.getArtworkById(id);
    }

    @Transactional(readOnly = true)
    public List<ApiArtworkDTO> getArtworkList(ApiWrapperList<ApiArtworkDTO> wrapper) {
        return apiDao.getArtworkList(wrapper);
    }
    //</editor-fold>

    @Transactional(readOnly = true)
    public void getEpisodeList(ApiWrapperList<ApiEpisodeDTO> wrapper) {
        apiDao.getEpisodeList(wrapper);
    }

    @Transactional(readOnly = true)
    public void getSingleVideo(ApiWrapperSingle<ApiVideoDTO> wrapper) {
        apiDao.getSingleVideo(wrapper);
    }

    @Transactional(readOnly = true)
    public List<CountGeneric> getJobCount(List<String> requiredJobs) {
        return apiDao.getJobCount(requiredJobs);
    }

    @Transactional(readOnly = true)
    public void getSeriesInfo(ApiWrapperList<ApiSeriesInfoDTO> wrapper) {
        apiDao.getSeriesInfo(wrapper);
    }

    // Player methods
    @Transactional(readOnly = true)
    public List<PlayerPath> getPlayer(ApiWrapperList<PlayerPath> wrapper) {
        return getPlayer((OptionsPlayer) wrapper.getOptions());
    }

    @Transactional(readOnly = true)
    public List<PlayerPath> getPlayer(OptionsPlayer options) {
        return playerDao.getPlayerEntries(options);
    }

    @Transactional(readOnly = true)
    public List<PlayerPath> getPlayer(String playerName) {
        return playerDao.getPlayerEntries(playerName);
    }

    @Transactional(readOnly = false)
    public void setPlayer(PlayerPath player) {
        playerDao.storePlayer(player);
    }

    @Transactional(readOnly = false)
    public void deletePlayer(String playerName) {
        playerDao.deletePlayer(playerName);
    }

}
