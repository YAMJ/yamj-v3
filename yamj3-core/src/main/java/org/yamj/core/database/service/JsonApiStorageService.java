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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.dto.*;
import org.yamj.core.api.options.OptionsPlayer;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.config.ConfigService;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.dao.*;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.service.metadata.online.OnlineScannerService;

@Service("jsonApiStorageService")
@Transactional(readOnly = true)
public class JsonApiStorageService {

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private MetadataDao metadataDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private ArtworkDao artworkDao;
    @Autowired
    private ApiDao apiDao;
    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private ConfigService configService;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private OnlineScannerService onlineScannerService;
    
    public List<Configuration> getConfiguration(String property) {
        return configService.getConfiguration(property);
    }

    //<editor-fold defaultstate="collapsed" desc="Index Methods">
    public void getVideoList(ApiWrapperList<ApiVideoDTO> wrapper) {
        apiDao.getVideoList(wrapper);
        
        // localization
        if (CollectionUtils.isNotEmpty(wrapper.getResults())) {
            for (ApiVideoDTO video : wrapper.getResults()) {
                localizeCertifications(video.getCertifications(), wrapper.getOptions().getLanguage());
                localizeCountries(video.getCountries(), wrapper.getOptions().getLanguage());
                localizeFiles(video.getFiles(), wrapper.getOptions().getLanguage());
            }
        }
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
        
        if (wrapper.getResult() != null && CollectionUtils.isNotEmpty(wrapper.getResult().getFilmography())) {
            for (ApiFilmographyDTO filmo : wrapper.getResult().getFilmography()) {
                String releaseCountry = localeService.getDisplayCountry(wrapper.getOptions().getLanguage(), filmo.getReleaseCountryCode());
                filmo.setReleaseCountry(releaseCountry);
            }
        }
    }

    public void getPersonListByVideoType(MetaDataType metaDataType, ApiWrapperList<ApiPersonDTO> wrapper) {
        apiDao.getPersonListByVideoType(metaDataType, wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre Methods">
    public Genre getGenre(Long id) {
        return commonDao.getGenre(id);
    }

    public Genre getGenre(String name) {
        return commonDao.getGenre(name);
    }

    public List<ApiGenreDTO> getGenres(ApiWrapperList<ApiGenreDTO> wrapper) {
        return commonDao.getGenres(wrapper);
    }

    public List<ApiGenreDTO> getGenreFilename(ApiWrapperList<ApiGenreDTO> wrapper, String filename) {
        return commonDao.getGenreFilename(wrapper, filename);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Studio Methods">
    public Studio getStudio(Long id) {
        return commonDao.getStudio(id);
    }

    public Studio getStudio(String name) {
        return commonDao.getStudio(name);
    }

    public List<Studio> getStudios(ApiWrapperList<Studio> wrapper) {
        return commonDao.getStudios(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Country Methods">
    public ApiCountryDTO getCountry(Long id, String language) {
        Country country = commonDao.getCountry(id);
        if (country == null) return null;

        ApiCountryDTO dto = new ApiCountryDTO();
        dto.setId(country.getId());
        dto.setCountryCode(country.getCountryCode());
        localize(dto, language);
        return dto;
    }

    public ApiCountryDTO getCountry(String countryCode, String language) {
        Country country =  commonDao.getCountry(countryCode);
        if (country == null) return null;
        
        ApiCountryDTO dto = new ApiCountryDTO();
        dto.setId(country.getId());
        dto.setCountryCode(country.getCountryCode());
        localize(dto, language);
        return dto;
    }

    public List<ApiCountryDTO> getCountries(ApiWrapperList<ApiCountryDTO> wrapper) {
        List<ApiCountryDTO> result = commonDao.getCountries(wrapper);
        localizeCountries(result, wrapper.getOptions().getLanguage());
        return result;
    }

    public List<ApiCountryDTO> getCountryFilename(ApiWrapperList<ApiCountryDTO> wrapper, String filename) {
        List<ApiCountryDTO> result = commonDao.getCountryFilename(wrapper, filename);
        localizeCountries(result, wrapper.getOptions().getLanguage());
        return result;
    }

    private void localizeCountries(List<ApiCountryDTO> countries, String inLanguage) {
        if (CollectionUtils.isEmpty(countries)) return;
        for (ApiCountryDTO dto : countries) {
            localize(dto, inLanguage);
        }
    }

    private void localize(ApiCountryDTO dto, String language) {
        String country = localeService.getDisplayCountry(language, dto.getCountryCode());
        dto.setCountry(country);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Award Methods">
    public List<ApiAwardDTO> getAwards(ApiWrapperList<ApiAwardDTO> wrapper) {
        return commonDao.getAwards(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Certification Methods">
    public List<ApiCertificationDTO> getCertifications(ApiWrapperList<ApiCertificationDTO> wrapper) {
        List<ApiCertificationDTO> result = commonDao.getCertifications(wrapper);
        localizeCertifications(result, wrapper.getOptions().getLanguage());
        return  result;
    }
    
    private void localizeCertifications(List<ApiCertificationDTO> certifications, String inLanguage) {
        if (CollectionUtils.isEmpty(certifications)) return;
        for (ApiCertificationDTO cert : certifications) {
            localize(cert, inLanguage);
        }
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
        return playerDao.getByNaturalIdCaseInsensitive(PlayerInfo.class, "name", playerName);
    }

    public PlayerInfo getPlayerInfo(Long playerId) {
        return playerDao.getById(PlayerInfo.class, playerId);
    }

    public List<PlayerInfo> getPlayerList() {
        return playerDao.getPlayerList();
    }

    public List<PlayerInfo> getPlayerList(OptionsPlayer options) {
        return playerDao.getPlayerList(options);
    }

    @Transactional
    public void deletePlayer(Long playerId) {
        PlayerInfo playerInfo = this.getPlayerInfo(playerId);
        if (playerInfo == null) return;
        playerDao.deleteEntity(playerInfo);
    }

    @Transactional
    public void storePlayer(PlayerInfo player) {
        PlayerInfo playerInfo = this.getPlayerInfo(player.getName());
        if (playerInfo == null) {
            playerDao.saveEntity(player);
        } else {
            playerInfo.setDeviceType(player.getDeviceType());
            playerInfo.setIpAddress(player.getIpAddress());
            playerDao.updateEntity(playerInfo);
        }
    }

    @Transactional
    public boolean storePlayerPath(Long playerId, PlayerPath playerPath) {
        PlayerInfo playerInfo = this.getPlayerInfo(playerId);
        if (playerInfo == null) return false;
        
        playerInfo.addPath(playerPath);
        playerDao.updateEntity(playerInfo);
        return true;
    }
    
    @Transactional
    public boolean deletePlayerPath(Long playerId, Long pathId) {
        PlayerInfo playerInfo = this.getPlayerInfo(playerId);
        if (playerInfo == null) return false;

        ListIterator<PlayerPath> iter = playerInfo.getPaths().listIterator();
        while (iter.hasNext()) {
            PlayerPath path = iter.next();
            if (path.getId() == pathId) {
                iter.remove();
                playerDao.updateEntity(playerInfo);
                return true;
            }
        }
        return false;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre methods">
    @Transactional
    public boolean addGenre(String name, String targetApi) {
        Genre genre = this.commonDao.getGenre(name);
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
        Genre genre = commonDao.getGenre(id);
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

    public void getEpisodeList(ApiWrapperList<ApiEpisodeDTO> wrapper) {
        apiDao.getEpisodeList(wrapper);
        
        // localization
        if (CollectionUtils.isNotEmpty(wrapper.getResults())) {
            for (ApiEpisodeDTO episode : wrapper.getResults()) {
                localizeCertifications(episode.getCertifications(), wrapper.getOptions().getLanguage());
                localizeCountries(episode.getCountries(), wrapper.getOptions().getLanguage());
                localizeFiles(episode.getFiles(), wrapper.getOptions().getLanguage());
            }
        }
    }

    public void getSingleVideo(ApiWrapperSingle<ApiVideoDTO> wrapper) {
        apiDao.getSingleVideo(wrapper);
        
        if (wrapper.getResult() != null) {
            // localization
            localizeCertifications(wrapper.getResult().getCertifications(), wrapper.getOptions().getLanguage());
            localizeCountries(wrapper.getResult().getCountries(), wrapper.getOptions().getLanguage());
            localizeFiles(wrapper.getResult().getFiles(), wrapper.getOptions().getLanguage());
        }
    }

    private void localizeFiles(List<ApiFileDTO> files, String inLanguage) {
        if (CollectionUtils.isEmpty(files)) return;
        for (ApiFileDTO file : files) {
            for (ApiAudioCodecDTO codec : file.getAudioCodecs()) {
                final String language = localeService.getDisplayLanguage(inLanguage, codec.getLanguageCode());
                codec.setLanguage(language);
            }
            for (ApiSubtitleDTO subtitle : file.getSubtitles()) {
                final String language = localeService.getDisplayLanguage(inLanguage, subtitle.getLanguageCode());
                subtitle.setLanguage(language);
            }
        }
    }
    
    public List<CountGeneric> getJobCount(List<String> requiredJobs) {
        return apiDao.getJobCount(requiredJobs);
    }

    public void getSeriesInfo(ApiWrapperList<ApiSeriesInfoDTO> wrapper) {
        apiDao.getSeriesInfo(wrapper);
        
        // localization
        if (CollectionUtils.isNotEmpty(wrapper.getResults())) {
            for (ApiSeriesInfoDTO series : wrapper.getResults()) {
                localizeCertifications(series.getCertifications(), wrapper.getOptions().getLanguage());
                localizeCountries(series.getCountries(), wrapper.getOptions().getLanguage());
            }
        }
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
            updatedWatched(type, id, watched);
            return new ApiStatus(200, "Set " + type + " " + id + " to " + watched);
        }
        
        return new ApiStatus(400, "No " + type + " ID provided");
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
        if (CollectionUtils.isEmpty(ids)) {
            return new ApiStatus(400, "No " + type + " IDs provided");
        }
        
        for (Long id : ids) {
            updatedWatched(type, id, watched);
        }
        
        return new ApiStatus(200, "Set " + type + " " + ids + " to " + watched);
    }
    
    private void updatedWatched(MetaDataType type, long id, boolean watched) {
        Collection<VideoData> videoDatas = null;
        if (MetaDataType.SERIES.equals(type)) {
            Series series = commonDao.getById(Series.class, id);
            if (series != null) {
                videoDatas = new HashSet<>();
                for (Season season : series.getSeasons()) {
                    videoDatas.addAll(season.getVideoDatas());
                }
            }
        } else if (MetaDataType.SEASON.equals(type)) {
            Season season = commonDao.getById(Season.class, id);
            if (season != null) {
                videoDatas = season.getVideoDatas();
            }
        } else {
            VideoData videoData = commonDao.getById(VideoData.class, id);
            if (videoData != null) {
                videoDatas = Collections.singleton(videoData);
            }
        }

        // nothing to do
        if (videoDatas == null) return;
        
        final Date watchedApiDate = new Date(System.currentTimeMillis());
        for (VideoData videoData : videoDatas) {
            // this will also set watched status to watched flag due the actual date
            videoData.setWatchedApi(watched, watchedApiDate);
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Rescan methods">
    /**
     * Set status of single metadata to UPDATED.
     *
     * @param type
     * @param id
     * @return
     */
    @Transactional
    public ApiStatus rescanSingle(MetaDataType type, Long id) {
        boolean rescan = false;
        
        if (id != null && id > 0L) {
            switch (type) {
                case PERSON:
                    Person person = metadataDao.getPerson(id);
                    if (person != null) {
                        commonDao.markAsUpdated(person.getPhoto());
                        commonDao.markAsUpdated(person);
                        rescan = true;
                    }
                    break;
                case FILMOGRAPHY:
                    person = metadataDao.getPerson(id);
                    if (person != null) {
                        commonDao.markAsUpdatedForFilmography(person);
                        rescan = true;
                    }
                    break;
                case SERIES:
                    Series series = commonDao.getById(Series.class, id);
                    if (series != null) {
                        commonDao.markAsUpdated(series.getArtworks());
                        commonDao.markAsUpdated(series);
                        rescan = true;
                        for (Season season : series.getSeasons()) {
                            commonDao.markAsUpdated(season.getArtworks());
                            commonDao.markAsUpdated(season);
                            for (VideoData episode : season.getVideoDatas()) {
                                commonDao.markAsUpdated(episode.getArtworks());
                                commonDao.markAsUpdated(episode);
                            }
                        }
                    }
                    break;
                case SEASON:
                    Season season = commonDao.getById(Season.class, id);
                    if (season != null) {
                        commonDao.markAsUpdated(season.getArtworks());
                        commonDao.markAsUpdated(season);
                        for (VideoData episode : season.getVideoDatas()) {
                            commonDao.markAsUpdated(episode.getArtworks());
                            commonDao.markAsUpdated(episode);
                        }
                        rescan = true;
                    }
                    break;
                case MOVIE:
                case EPISODE:
                    VideoData videoData = commonDao.getById(VideoData.class, id);
                    if (videoData != null) {
                        commonDao.markAsUpdated(videoData.getArtworks());
                        commonDao.markAsUpdated(videoData);
                        rescan = true;
                    }
                    break;
                default:
                    // nothing to rescan
                    break;
            }
            
            if (rescan) {
                StringBuilder sb = new StringBuilder("Rescan ");
                sb.append(type.name().toLowerCase());
                sb.append(" for ID: ");
                sb.append(id);
                return new ApiStatus(200, sb.toString());
            }
            
            StringBuilder sb = new StringBuilder("No ");
            sb.append(type.name().toLowerCase());
            sb.append(" found for rescanning ID: ");
            sb.append(id);
            return new ApiStatus(404, sb.toString());
        }

        return new ApiStatus(410, "No valid " + type.name().toLowerCase() + " ID provided");
    }

    /**
     * Set status of artwork to UPDATED.
     *
     * @param type
     * @param id
     * @return
     */
    @Transactional
    public ApiStatus rescanArtwork(MetaDataType type, Long id) {
        boolean rescan = false;
        
        if (id != null && id > 0L) {
            switch (type) {
                case PERSON:
                    Person person = metadataDao.getPerson(id);
                    if (person != null) {
                        this.commonDao.markAsUpdated(person.getPhoto());
                        rescan = true;
                    }
                    break;
                case SERIES:
                    Series series = commonDao.getById(Series.class, id);
                    if (series != null) {
                        this.commonDao.markAsUpdated(series.getArtworks());
                        rescan = true;
                    }
                    break;
                case SEASON:
                    Season season = commonDao.getById(Season.class, id);
                    if (season != null) {
                        this.commonDao.markAsUpdated(season.getArtworks());
                        rescan = true;
                    }
                    break;
                case BOXSET:
                    BoxedSet boxedSet = commonDao.getBoxedSet(id);
                    if (boxedSet != null) {
                        this.commonDao.markAsUpdated(boxedSet.getArtworks());
                        rescan = true;
                    }
                    break;
                case MOVIE:
                case EPISODE:
                    VideoData videoData = commonDao.getById(VideoData.class, id);
                    if (videoData != null) {
                        this.commonDao.markAsUpdated(videoData.getArtworks());
                        rescan = true;
                    }
                    break;
                default:
                    // nothing to rescan
                    break;
            }
            
            if (rescan) {
                StringBuilder sb = new StringBuilder("Rescan ");
                sb.append(type.name().toLowerCase());
                sb.append(" artwork for ID: ");
                sb.append(id);
                return new ApiStatus(200, sb.toString());
            }
            
            StringBuilder sb = new StringBuilder("No ");
            sb.append(type.name().toLowerCase());
            sb.append(" found for artwork rescanning ID: ");
            sb.append(id);
            return new ApiStatus(404, sb.toString());
        }

        return new ApiStatus(410, "No valid " + type.name().toLowerCase() + " ID provided");
    }
    
    /**
     * Set status of artwork to UPDATED.
     *
     * @param type
     * @param id
     * @return
     */
    @Transactional
    public ApiStatus rescanTrailer(MetaDataType type, Long id) {
        boolean rescan = false;
        
        if (id != null && id > 0L) {
            switch (type) {
                case SERIES:
                    Series series = commonDao.getById(Series.class, id);
                    if (series != null) {
                        commonDao.markAsUpdatedForTrailers(series);
                        rescan = true;
                    }
                    break;
                case MOVIE:
                    VideoData videoData = commonDao.getById(VideoData.class, id);
                    if (videoData != null) {
                        commonDao.markAsUpdatedForTrailers(videoData);
                        rescan = true;
                    }
                    break;
                default:
                    // nothing to rescan
                    break;
            }
            
            if (rescan) {
                StringBuilder sb = new StringBuilder("Rescan ");
                sb.append(type.name().toLowerCase());
                sb.append(" artwork for ID: ");
                sb.append(id);
                return new ApiStatus(200, sb.toString());
            }
            
            StringBuilder sb = new StringBuilder("No ");
            sb.append(type.name().toLowerCase());
            sb.append(" found for artwork rescanning ID: ");
            sb.append(id);
            return new ApiStatus(404, sb.toString());
        }

        return new ApiStatus(410, "No valid " + type.name().toLowerCase() + " ID provided");
    }
    
    @Transactional
    public ApiStatus rescanAll() {
        this.apiDao.rescanAll();
        return new ApiStatus(200, "Rescan forced for all meta data objects");
    }
    
    //</editor-fold>
    
    @Transactional
    public ApiStatus updateOnlineScan(MetaDataType type, Long id, String sourceDb, boolean disable) {
        // first check if source is known
        if (!onlineScannerService.isKnownScanner(type, sourceDb)) {
            StringBuilder sb = new StringBuilder();
            sb.append("The sourceDb ");
            sb.append(sourceDb);
            sb.append(" is not valid for " );
            sb.append(type.name().toLowerCase());
            return new ApiStatus(409, sb.toString());
        }

        if (MetaDataType.PERSON == type) {
            Person person = metadataDao.getPerson(id);
            if (person == null) {
                return new ApiStatus(404, "Person for ID " + id + " not found");
            }
            
            final boolean changed;
            if (disable) {
                changed = person.disableApiScan(sourceDb);
            } else {
                changed = person.enableApiScan(sourceDb);
            }

            // if skip value has changed then reset status
            if (changed) {
                person.setStatus(StatusType.UPDATED);
                commonDao.updateEntity(person);
            }
        } else if (MetaDataType.SERIES == type) {
            Series series = commonDao.getById(Series.class, id);
            if (series == null) {
                return new ApiStatus(404, "Series for ID " + id + " not found");
            }
        
            final boolean changed;
            if (disable) {
                changed = series.disableApiScan(sourceDb);
            } else {
                changed = series.enableApiScan(sourceDb);
            }
        
            // if something changed then reset status
            if (changed) {
                series.setStatus(StatusType.UPDATED);
                commonDao.updateEntity(series);
                for (Season season: series.getSeasons()) {
                    season.setStatus(StatusType.UPDATED);
                    commonDao.updateEntity(season);
                    for (VideoData videoDaa : season.getVideoDatas()) {
                        videoDaa.setStatus(StatusType.UPDATED);
                        commonDao.updateEntity(videoDaa);
                    }
                }
            }
        } else {
            VideoData videoData = commonDao.getById(VideoData.class, id);
            if (videoData == null) {
                return new ApiStatus(404, "VideoData for ID " + id + " not found");
            }

            final boolean changed;
            if (disable) {
                changed = videoData.disableApiScan(sourceDb);
            } else {
                changed = videoData.enableApiScan(sourceDb);
            }
            
            // if something changed then reset status
            if (changed) {
                videoData.setStatus(StatusType.UPDATED);
                commonDao.updateEntity(videoData);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(disable?"Disabled ":"Enabled ");
        sb.append(sourceDb);
        sb.append(" scan for "); 
        sb.append(type.name().toLowerCase());
        sb.append(" ID: ");
        sb.append(id);
        return new ApiStatus(200, sb.toString());
    }

    @Transactional
    public ApiStatus updateExternalId(MetaDataType type, Long id, String sourceDb, String sourceDbId) {
        // first check if source is known
        if (!onlineScannerService.isKnownScanner(type, sourceDb)) {
            StringBuilder sb = new StringBuilder();
            sb.append("The sourceDb ");
            sb.append(sourceDb);
            sb.append(" is not valid for " );
            sb.append(type.name().toLowerCase());
            return new ApiStatus(409, sb.toString());
        }

        if (MetaDataType.PERSON.equals(type)) {
            Person person = commonDao.getById(Person.class, id);
            if (person == null) {
                return new ApiStatus(404, "Person for ID " + id + " not found");
            }
    
            if (resetSourceDbId(person, sourceDb, sourceDbId)) {
                this.metadataStorageService.handleModifiedSources(person);
            }
        } else if (MetaDataType.SERIES.equals(type)) {
            Series series =  commonDao.getById(Series.class, id);
            if (series == null) {
                return new ApiStatus(404, "Series for ID " + id + " not found");
            }
            
            if (resetSourceDbId(series, sourceDb, sourceDbId)) {
                this.metadataStorageService.handleModifiedSources(series);
            }
        } else if (MetaDataType.SEASON.equals(type)) {
            Season season = commonDao.getById(Season.class, id);
            if (season == null) {
                return new ApiStatus(404, "Season for ID " + id + " not found");
            }

            if (resetSourceDbId(season, sourceDb, sourceDbId)) {
                this.metadataStorageService.handleModifiedSources(season);
            }
        } else {
            VideoData videoData = commonDao.getById(VideoData.class, id);
            if (videoData == null) {
                return new ApiStatus(404, "Video for ID " + id + " not found");
            } else if (MetaDataType.EPISODE.equals(type) && videoData.isMovie()) {
                return new ApiStatus(404, "Episode for ID " + id + " not found");
            } else if (MetaDataType.MOVIE.equals(type) && !videoData.isMovie()) {
                return new ApiStatus(404, "Movie for ID " + id + " not found");
            }
            
            if (resetSourceDbId(videoData, sourceDb, sourceDbId)) {
                this.metadataStorageService.handleModifiedSources(videoData);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.isBlank(sourceDbId)?"Removed":"Updated");
        sb.append(" external ID for " );
        sb.append(sourceDb);
        sb.append(" for ");
        sb.append(type.name().toLowerCase());
        sb.append(" ID: ");
        sb.append(id);
        return new ApiStatus(200, sb.toString());
    }
    
    private static boolean resetSourceDbId(IScannable scannable, String sourceDb, String sourceDbId) {
        final boolean changed;
        if (StringUtils.isBlank(sourceDbId)) {
            changed = scannable.removeSourceDbId(sourceDb);
        } else {
            changed = scannable.setSourceDbId(sourceDb, sourceDbId);
        }
        return changed;
    }
    
    
    //<editor-fold defaultstate="collapsed" desc="Year/Decade Methods">
    public List<ApiYearDecadeDTO> getYears(ApiWrapperList<ApiYearDecadeDTO> wrapper) {
        return apiDao.getYears(wrapper);
    }

    public List<ApiYearDecadeDTO> getDecades(ApiWrapperList<ApiYearDecadeDTO> wrapper) {
        return apiDao.getDecades(wrapper);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Trailer Methods">
    @Transactional
    public ApiStatus setTrailerStatus(Long id, StatusType status) {
        Trailer trailer = commonDao.getById(Trailer.class, id);
        if (trailer == null) {
            return new ApiStatus(404, "Trailer for ID " + id + " not found");
        }
        
        if (trailer.isCached() && (StatusType.NEW.equals(status) || StatusType.UPDATED.equals(status))) {
            // no download for already stored trailer
            trailer.setStatus(StatusType.DONE);
        } else {
            trailer.setStatus(status);
        }
        commonDao.updateEntity(trailer);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Set status ");
        sb.append(trailer.getStatus());
        sb.append(" for trailer ID: ");
        sb.append(trailer.getId());
        return new ApiStatus(200, sb.toString());
    }
    //</editor-fold>
}
