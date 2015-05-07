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

import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.dto.ApiArtworkDTO;
import org.yamj.core.api.model.dto.ApiAwardDTO;
import org.yamj.core.api.model.dto.ApiBoxedSetDTO;
import org.yamj.core.api.model.dto.ApiEpisodeDTO;
import org.yamj.core.api.model.dto.ApiExternalIdDTO;
import org.yamj.core.api.model.dto.ApiNameDTO;
import org.yamj.core.api.model.dto.ApiPersonDTO;
import org.yamj.core.api.model.dto.ApiRatingDTO;
import org.yamj.core.api.model.dto.ApiSeriesInfoDTO;
import org.yamj.core.api.model.dto.ApiTargetDTO;
import org.yamj.core.api.model.dto.ApiVideoDTO;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.dao.ApiDao;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.dao.MediaDao;
import org.yamj.core.database.dao.PlayerDao;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Configuration;
import org.yamj.core.database.model.Country;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.player.PlayerInfo;

@Service("jsonApiStorageService")
@Transactional(readOnly = true)
public class JsonApiStorageService {

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private ApiDao apiDao;
    @Autowired
    private ConfigService configService;
    @Autowired
    private PlayerDao playerDao;

    public List<Configuration> getConfiguration(String property) {
        return configService.getConfiguration(property);
    }

    //<editor-fold defaultstate="collapsed" desc="Index Methods">
    public void getVideoList(ApiWrapperList<ApiVideoDTO> wrapper) {
        apiDao.getVideoList(wrapper);
    }

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
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Person Methods">
    public void getPersonList(ApiWrapperList<ApiPersonDTO> wrapper) {
        apiDao.getPersonList(wrapper);
    }

    public void getPerson(ApiWrapperSingle<ApiPersonDTO> wrapper) {
        apiDao.getPerson(wrapper);
    }

    public void getPersonListByVideoType(MetaDataType metaDataType, ApiWrapperList<ApiPersonDTO> wrapper) {
        apiDao.getPersonListByVideoType(metaDataType, wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre Methods">
    public Genre getGenre(Serializable id) {
        return commonDao.getById(Genre.class, id);
    }

    public Genre getGenre(String name) {
        return commonDao.getGenre(name);
    }

    public List<ApiTargetDTO> getGenres(ApiWrapperList<ApiTargetDTO> wrapper) {
        return commonDao.getGenres(wrapper);
    }

    public List<ApiTargetDTO> getGenreFilename(ApiWrapperList<ApiTargetDTO> wrapper, String filename) {
        return commonDao.getGenreFilename(wrapper, filename);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Studio Methods">
    public Studio getStudio(Serializable id) {
        return commonDao.getById(Studio.class, id);
    }

    public Studio getStudio(String name) {
        return commonDao.getStudio(name);
    }

    public List<Studio> getStudios(ApiWrapperList<Studio> wrapper) {
        return commonDao.getStudios(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Country Methods">
    public Country getCountry(Serializable id) {
        return commonDao.getById(Country.class, id);
    }

    public Country getCountry(String name) {
        return commonDao.getCountry(name);
    }

    public List<ApiTargetDTO> getCountries(ApiWrapperList<ApiTargetDTO> wrapper) {
        return commonDao.getCountries(wrapper);
    }

    public List<ApiTargetDTO> getCountryFilename(ApiWrapperList<ApiTargetDTO> wrapper, String filename) {
        return commonDao.getCountryFilename(wrapper, filename);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Award Methods">
    public List<ApiAwardDTO> getAwards(ApiWrapperList<ApiAwardDTO> wrapper) {
        return commonDao.getAwards(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Certification Methods">
    public List<Certification> getCertifications(ApiWrapperList<Certification> wrapper) {
        return commonDao.getCertifications(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="VideoSource Methods">
    public List<ApiNameDTO> getVideoSources(ApiWrapperList<ApiNameDTO> wrapper) {
        return mediaDao.getVideoSources(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Boxed Set Methods">
    public List<ApiBoxedSetDTO> getBoxedSets(ApiWrapperList<ApiBoxedSetDTO> wrapper) {
        return apiDao.getBoxedSets(wrapper);
    }

    public ApiBoxedSetDTO getBoxedSet(ApiWrapperSingle<ApiBoxedSetDTO> wrapper) {
        return apiDao.getBoxedSet(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Rating Methods">
    public List<ApiRatingDTO> getRatings(ApiWrapperList<ApiRatingDTO> wrapper) {
        return commonDao.getRatings(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Alphabetical Methods">
    public List<ApiNameDTO> getAlphabeticals(ApiWrapperList<ApiNameDTO> wrapper) {
        return apiDao.getAlphabeticals(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Artwork Methods">
    public ApiArtworkDTO getArtworkById(Long id) {
        return apiDao.getArtworkById(id);
    }

    public List<ApiArtworkDTO> getArtworkList(ApiWrapperList<ApiArtworkDTO> wrapper) {
        return apiDao.getArtworkList(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Player methods">
    public PlayerInfo getPlayerInfo(String playerName) {
        return playerDao.getPlayerInfo(playerName);
    }

    public PlayerInfo getPlayerInfo(Long playerId) {
        return playerDao.getPlayerInfo(playerId);
    }

    public List<PlayerInfo> getPlayerList() {
        return playerDao.getPlayerList();
    }

    public List<PlayerInfo> getPlayerList(OptionsPlayer options) {
        return playerDao.getPlayerList(options);
    }

    @Transactional
    public void deletePlayer(Long playerId) {
        playerDao.deletePlayer(playerId);
    }

    @Transactional
    public void deletePlayerPath(Long playerId, Long pathId) {
        playerDao.deletePlayerPath(playerId, pathId);
    }

    @Transactional
    public void storePlayer(PlayerInfo player) {
        playerDao.storePlayer(player);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre methods">
    @Transactional
    public boolean addGenre(String name, String targetApi) {
        Genre genre = commonDao.getGenre(name);
        if (genre != null) {
            return false;
        }
        genre = new Genre(name);
        genre.setTargetApi(targetApi);
        this.commonDao.saveEntity(genre);
        return true;
    }

    @Transactional
    public boolean updateGenre(long id, String targetApi) {
        Genre genre = commonDao.getById(Genre.class, id);
        if (genre == null) {
            return false;
        }
        genre.setTargetApi(StringUtils.trimToNull(targetApi));
        this.commonDao.updateEntity(genre);
        return true;
    }

    @Transactional
    public boolean updateGenre(String name, String targetApi) {
        Genre genre = commonDao.getGenre(name);
        if (genre == null) {
            return false;
        }
        genre.setTargetApi(StringUtils.trimToNull(targetApi));
        this.commonDao.updateEntity(genre);
        return true;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Country methods">
    @Transactional
    public boolean addCountry(String name, String targetApi) {
        Country country = commonDao.getCountry(name);
        if (country != null) {
            return false;
        }
        country = new Country(name);
        country.setTargetApi(targetApi);
        this.commonDao.saveEntity(country);
        return true;
    }

    @Transactional
    public boolean updateCountry(long id, String targetApi) {
        Country country = commonDao.getById(Country.class, id);
        if (country == null) {
            return false;
        }
        country.setTargetApi(StringUtils.trimToNull(targetApi));
        this.commonDao.updateEntity(country);
        return true;
    }

    @Transactional
    public boolean updateCountry(String name, String targetApi) {
        Country country = commonDao.getCountry(name);
        if (country == null) {
            return false;
        }
        country.setTargetApi(StringUtils.trimToNull(targetApi));
        this.commonDao.updateEntity(country);
        return true;
    }

    //</editor-fold>
    public void getEpisodeList(ApiWrapperList<ApiEpisodeDTO> wrapper) {
        apiDao.getEpisodeList(wrapper);
    }

    public void getSingleVideo(ApiWrapperSingle<ApiVideoDTO> wrapper) {
        apiDao.getSingleVideo(wrapper);
    }

    public List<CountGeneric> getJobCount(List<String> requiredJobs) {
        return apiDao.getJobCount(requiredJobs);
    }

    public void getSeriesInfo(ApiWrapperList<ApiSeriesInfoDTO> wrapper) {
        apiDao.getSeriesInfo(wrapper);
    }

    public VideoData getVideoData(Long id) {
        return commonDao.getVideoData(id);
    }

    public void updateVideoData(VideoData videoData) {
        commonDao.storeEntity(videoData);
    }

    public List<Long> getSeasonVideoIds(Long id) {
        return commonDao.getSeasonVideoIds(id);
    }

    public List<Long> getSeriesVideoIds(Long id) {
        return commonDao.getSeriesVideoIds(id);
    }

    //<editor-fold defaultstate="collapsed" desc="Watched methods">
    /**
     * Update a single videodata record
     *
     * @param type
     * @param id
     * @param watched
     * @return
     */
    @Transactional
    public ApiStatus updateWatchedSingle(MetaDataType type, Long id, boolean watched) {
        if (id != null && id > 0L) {
            VideoData video = commonDao.getVideoData(id);

            StringBuilder sb = new StringBuilder("Watched status for ");
            sb.append(type).append(" ID: ").append(video.getId());
            sb.append("-").append(video.getTitle());

            // Check to see if the status is the same
            if (video.isWatchedApi() == watched) {
                sb.append(" is already '").append(watched(watched)).append("' unchanged");
                return new ApiStatus(200, sb.toString());
            }

            // Set the watched status and update the video
            video.setWatchedApi(watched);
            commonDao.storeEntity(video);

            sb.append(" changed to ").append(watched(video.isWatchedApi()));
            return new ApiStatus(200, sb.toString());
        } else {
            return new ApiStatus(400, "No " + type + " ID provided");
        }
    }

    /**
     * Update a list of videodata records
     *
     * @param type
     * @param ids
     * @param watched
     * @param sourceId
     * @return
     */
    @Transactional
    public ApiStatus updateWatchedList(MetaDataType type, List<Long> ids, boolean watched, Long sourceId) {
        int watchedCount = 0;
        int unwatchedCount = 0;

        for (Long id : ids) {
            VideoData video = commonDao.getVideoData(id);
            if (video.isWatched()) {
                watchedCount++;
            } else {
                unwatchedCount++;
            }

            if (video.isWatchedApi() != watched) {
                // Set the watched status and update the video
                video.setWatchedApi(watched);
                commonDao.storeEntity(video);
            }
        }

        StringBuilder sb = new StringBuilder("Watched status for ");
        sb.append(type).append(" ID: ").append(sourceId);
        sb.append(" set to '").append(watched(watched)).append("'. ");
        sb.append(watchedCount).append(" were set to ").append(watched(watched)).append(", ");
        sb.append(unwatchedCount).append(" were already ").append(watched(watched));

        return new ApiStatus(200, sb.toString());
    }

    private String watched(boolean watched) {
        return watched ? "watched" : "unwatched";
    }
    //</editor-fold>

    public void getExternalIds(ApiWrapperList<ApiExternalIdDTO> wrapper) {
        apiDao.getExternalIds(wrapper);
    }

}
