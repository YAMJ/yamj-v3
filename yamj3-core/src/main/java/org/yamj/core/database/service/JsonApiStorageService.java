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

import static org.yamj.core.CachingNames.API_EXTERNAL_IDS;
import static org.yamj.core.CachingNames.API_GENRES;
import static org.yamj.core.api.model.builder.DataItem.*;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.dto.*;
import org.yamj.core.api.options.*;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.dao.*;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.player.PlayerInfo;
import org.yamj.core.database.model.player.PlayerPath;
import org.yamj.core.service.metadata.online.OnlineScannerService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.model.type.JobType;

@Service("jsonApiStorageService")
@Transactional(readOnly = true)
public class JsonApiStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(ApiDao.class);
    private static final String API_SOURCE = "api";
        
    @Autowired
    private CommonDao commonDao;
    @Autowired
    private MetadataDao metadataDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private ApiDao apiDao;
    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private OnlineScannerService onlineScannerService;

    
    //<editor-fold defaultstate="collapsed" desc="Index Methods">
    public List<ApiVideoDTO> getVideoList(ApiWrapperList<ApiVideoDTO> wrapper, OptionsIndexVideo options) {
        List<ApiVideoDTO> results = apiDao.getVideoList(wrapper, options);
        
        for (ApiVideoDTO video : results) {

            if (MetaDataType.EPISODE != video.getVideoType()) {
                // not for episodes

                if (options.hasDataItem(GENRE)) {
                    video.setGenres(apiDao.getGenresForMetadata(video.getVideoType(), video.getId()));
                }

                if (options.hasDataItem(STUDIO)) {
                    video.setStudios(apiDao.getStudiosForMetadata(video.getVideoType(), video.getId()));
                }

                if (options.hasDataItem(COUNTRY)) {
                    video.setCountries(apiDao.getCountriesForMetadata(video.getVideoType(), video.getId()));
                    localizeCountries(video.getCountries(), options.getLanguage());
                }
            
                if (options.hasDataItem(CERTIFICATION)) {
                    video.setCertifications(apiDao.getCertificationsForMetadata(video.getVideoType(), video.getId()));
                    localizeCertifications(video.getCertifications(), options.getLanguage());
                }

                if (options.hasDataItem(AWARD)) {
                    video.setAwards(apiDao.getAwardsForMetadata(video.getVideoType(), video.getId()));
                }
            }
            
            if (options.hasDataItem(RATING)) {
                video.setRatings(apiDao.getRatingsForMetadata(video.getVideoType(), video.getId()));
            }

            
            if (options.hasDataItem(EXTERNALID)) {
                video.setExternalIds(apiDao.getExternalIdsForMetadata(video.getVideoType(), video.getId()));
            }
            
            if (options.hasDataItem(BOXSET)) {
                video.setBoxedSets(apiDao.getBoxedSetsForMetadata(video.getVideoType(), video.getId()));
            }
            
            if (options.hasDataItem(TRAILER) && (MetaDataType.SERIES == video.getVideoType() || MetaDataType.MOVIE.equals(video.getVideoType()))) {
                video.setTrailers(apiDao.getTrailersForMetadata(video.getVideoType(), video.getId()));
            }
            
            if (options.hasDataItem(VIDEOSOURCE)) {
                video.setVideoSource(apiDao.getVideoSourceForMetadata(video.getVideoType(), video.getId()));
            }
        }
        
        return results;
    }

    public CountTimestamp getCountTimestamp(MetaDataType type) {
        CountTimestamp ct;
        switch(type) {
            case MOVIE:
                ct = apiDao.getCountTimestamp(type, "videodata", "episode<0");
                break;
            case SERIES:
                ct = apiDao.getCountTimestamp(type, "series", "");
                break;
            case SEASON:
                ct = apiDao.getCountTimestamp(type, "season", "");
                break;
            case EPISODE:
                ct = apiDao.getCountTimestamp(type, "videodata", "episode>=0");
                break;
            case PERSON:
                ct = apiDao.getCountTimestamp(type, "person", "status != 'DELETED'");
                break;
            default:
                ct = null;
                break;
                    
        }
        return ct;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Person Methods">
    public List<ApiPersonDTO> getPersonList(ApiWrapperList<ApiPersonDTO> wrapper, OptionsId options) {
        return apiDao.getPersonList(wrapper, options);
    }

    public ApiPersonDTO getPerson(ApiWrapperSingle<ApiPersonDTO> wrapper, OptionsId options) {
        ApiPersonDTO person = apiDao.getPerson(wrapper, options);
        
        if (person != null) {
            for (ApiFilmographyDTO filmo : person.getFilmography()) {
                String releaseCountry = localeService.getDisplayCountry(options.getLanguage(), filmo.getReleaseCountryCode());
                filmo.setReleaseCountry(releaseCountry);
            }
        }
        
        return person;
    }

    public  List<ApiPersonDTO> getPersonListByVideoType(MetaDataType metaDataType, ApiWrapperList<ApiPersonDTO> wrapper) {
        return apiDao.getPersonListByVideoType(metaDataType, wrapper);
    }
    
    @Transactional
    public ApiStatus updatePerson(Long id, UpdatePerson update) {
        Person person = metadataDao.getById(Person.class, id);
        if (person == null) {
            return ApiStatus.notFound("ID " + id + " does not determine a valid person entry");
        }
        
        if (OverrideTools.checkOverwriteName(person, API_SOURCE)) {
            person.setName(update.getName(), API_SOURCE);
        }
        if (OverrideTools.checkOverwriteFirstName(person, API_SOURCE)) {
            person.setFirstName(update.getFirstName(), API_SOURCE);
        }
        if (OverrideTools.checkOverwriteLastName(person, API_SOURCE)) {
            person.setLastName(update.getLastName(), API_SOURCE);
        }
        if (OverrideTools.checkOverwriteBirthName(person, API_SOURCE)) {
            person.setBirthName(update.getBirthName(), API_SOURCE);
        }
        if (OverrideTools.checkOverwriteBirthDay(person, API_SOURCE)) {
            person.setBirthDay(update.getBirthDay(), API_SOURCE);
        }
        if (OverrideTools.checkOverwriteBirthPlace(person, API_SOURCE)) {
            person.setBirthPlace(update.getBirthPlace(), API_SOURCE);
        }
        if (OverrideTools.checkOverwriteDeathDay(person, API_SOURCE)) {
            person.setDeathDay(update.getDeathDay(), API_SOURCE);
        }
        if (OverrideTools.checkOverwriteDeathPlace(person, API_SOURCE)) {
            person.setDeathPlace(update.getDeathPlace(), API_SOURCE);
        }
        if (OverrideTools.checkOverwriteBiography(person, API_SOURCE)) {
            person.setBiography(update.getBiography(), API_SOURCE);
        }

        person.setLastScanned(new Date());
        return ApiStatus.ok("Updated person with ID "+id);
    }
    
    @Transactional
    public ApiStatus duplicatePerson(Long id, Long doubletId) {
        Person person = metadataDao.getById(Person.class, id);
        if (person == null || person.isDeleted()) {
            return ApiStatus.notFound("ID " + id + " does not determine a valid person entry");
        }
        Person doubletPerson = metadataDao.getById(Person.class, doubletId);
        if (doubletPerson == null || doubletPerson.isDeleted()) {
            return ApiStatus.notFound("ID " + doubletId + " does not determine a valid person entry");
        }
        
        this.metadataDao.duplicate(person, doubletPerson);
        return ApiStatus.ok("Marked "+doubletId+" as duplicate of "+id);
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

    public List<ApiGenreDTO> getGenreFilename(String filename) {
        return commonDao.getGenreFilename(filename);
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
        if (country == null) {
            return null;
        }

        ApiCountryDTO dto = new ApiCountryDTO(country.getId(), country.getCountryCode());
        localize(dto, language);
        return dto;
    }

    public ApiCountryDTO getCountry(String countryCode, String language) {
        Country country =  commonDao.getCountry(countryCode);
        if (country == null) {
            return null;
        }
        
        ApiCountryDTO dto = new ApiCountryDTO(country.getId(), country.getCountryCode());
        localize(dto, language);
        return dto;
    }

    public List<ApiCountryDTO> getCountries(ApiWrapperList<ApiCountryDTO> wrapper) {
        List<ApiCountryDTO> result = commonDao.getCountries(wrapper);
        localizeCountries(result, wrapper.getOptions().getLanguage());
        return result;
    }

    public List<ApiCountryDTO> getCountryFilename(String filename, String language) {
        List<ApiCountryDTO> result = commonDao.getCountryFilename(filename);
        localizeCountries(result, language);
        return result;
    }

    private void localizeCountries(List<ApiCountryDTO> countries, String inLanguage) {
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
    public List<ApiNameDTO> getAlphabeticals(OptionsMultiType options) {
        return apiDao.getAlphabeticals(options);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Artwork Methods">
    public ApiArtworkDTO getArtworkById(Long id) {
        return apiDao.getArtworkById(id);
    }

    public List<ApiArtworkDTO> getArtworkList(ApiWrapperList<ApiArtworkDTO> wrapper, OptionsIndexArtwork options) {
        return apiDao.getArtworkList(wrapper, options);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Player methods">
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
        if (playerInfo != null) {
            playerDao.deleteEntity(playerInfo);
        }
    }

    @Transactional
    public void storePlayer(PlayerInfo player) {
        PlayerInfo playerInfo = playerDao.getByNaturalIdCaseInsensitive(PlayerInfo.class, "name", player.getName());
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
        if (playerInfo == null) { 
            return false;
        }
        
        for (PlayerPath stored : playerInfo.getPaths()) {
            if (stored.getSourcePath().equals(playerPath.getSourcePath())) {
                stored.setTargetPath(playerPath.getTargetPath());
                playerDao.updateEntity(stored);
                return true;
            }
        }
        
        playerPath.setPlayerInfo(playerInfo);
        playerInfo.getPaths().add(playerPath);
        playerDao.updateEntity(playerInfo);
        return true;
    }

    @Transactional
    public boolean storePlayerPath(Long playerId, Long pathId, PlayerPath playerPath) {
        PlayerInfo playerInfo = this.getPlayerInfo(playerId);
        if (playerInfo != null) {
            for (PlayerPath stored : playerInfo.getPaths()) {
                if (stored.getId() == pathId) {
                    stored.setSourcePath(playerPath.getSourcePath());
                    stored.setTargetPath(playerPath.getTargetPath());
                    playerDao.updateEntity(stored);
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional
    public boolean deletePlayerPath(Long playerId, Long pathId) {
        PlayerInfo playerInfo = this.getPlayerInfo(playerId);
        if (playerInfo != null) {
            Iterator<PlayerPath> iter = playerInfo.getPaths().iterator();
            while (iter.hasNext()) {
                PlayerPath path = iter.next();
                if (path.getId() == pathId) {
                    iter.remove();
                    playerDao.updateEntity(playerInfo);
                    return true;
                }
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
    @CacheEvict(value=API_GENRES, allEntries=true)
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
    @CacheEvict(value=API_GENRES, allEntries=true)
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

    public  List<ApiEpisodeDTO> getEpisodeList(ApiWrapperList<ApiEpisodeDTO> wrapper, OptionsEpisode options) {
        List<ApiEpisodeDTO> results = apiDao.getEpisodeList(wrapper, options);
        
        for (ApiEpisodeDTO episode : results) {
            if (options.hasDataItem(GENRE)) {
                episode.setGenres(apiDao.getGenresForMetadata(MetaDataType.SERIES, episode.getSeriesId()));
            }

            if (options.hasDataItem(STUDIO)) {
                episode.setStudios(apiDao.getStudiosForMetadata(MetaDataType.SERIES, episode.getSeriesId()));
            }
            
            if (options.hasDataItem(COUNTRY)) {
                episode.setCountries(apiDao.getCountriesForMetadata(MetaDataType.SERIES, episode.getSeriesId()));
                localizeCountries(episode.getCountries(), options.getLanguage());
            }
            
            if (options.hasDataItem(CERTIFICATION)) {
                episode.setCertifications(apiDao.getCertificationsForMetadata(MetaDataType.SERIES, episode.getSeriesId()));
                localizeCertifications(episode.getCertifications(), options.getLanguage());
            }
            
            if (options.hasDataItem(RATING)) {
                episode.setRatings(apiDao.getRatingsForMetadata(MetaDataType.EPISODE, episode.getId()));
            }

            if (options.hasDataItem(AWARD)) {
                episode.setAwards(apiDao.getAwardsForMetadata(MetaDataType.SERIES, episode.getSeriesId()));
            }

            if (options.hasDataItem(FILES)) {
                episode.setFiles(apiDao.getFilesForMetadata(MetaDataType.EPISODE, episode.getId()));
                localizeFiles(episode.getFiles(), options.getLanguage());
            }
        }
        
        if (MapUtils.isNotEmpty(options.splitJobs())) {
            Set<String> jobs = options.getJobTypes();

            for (ApiEpisodeDTO episode : results) {
                List<ApiPersonDTO> cast = apiDao.getCastForMetadata(MetaDataType.EPISODE, episode.getId(), options.splitDataItems(), jobs);

                // just add given amount for jobs to cast
                Map<JobType, Integer> jobMap = new HashMap<>(options.splitJobs());
                for (ApiPersonDTO entry : cast) {
                    Integer amount = jobMap.get(entry.getJob());
                    if (amount == null) {
                        episode.addCast(entry);
                    } else if (amount > 0) {
                        episode.addCast(entry);
                        amount--;
                        jobMap.put(entry.getJob(), amount);
                    }
                }
            }
        } else if (options.isAllJobTypes()) {
            for (ApiEpisodeDTO episode : results) {
                episode.setCast(apiDao.getCastForMetadata(MetaDataType.EPISODE, episode.getId(), options.splitDataItems(), null));
            }
        }

        return results;
    }

    public ApiVideoDTO getSingleVideo(ApiWrapperSingle<ApiVideoDTO> wrapper, OptionsIndexVideo options) {
        ApiVideoDTO video = apiDao.getSingleVideo(wrapper, options);
        
        if (video != null) {
            
            if (options.hasDataItem(GENRE)) {
                video.setGenres(apiDao.getGenresForMetadata(video.getVideoType(), video.getId()));
            }

            if (options.hasDataItem(STUDIO)) {
                video.setStudios(apiDao.getStudiosForMetadata(video.getVideoType(), video.getId()));
            }

            if (options.hasDataItem(COUNTRY)) {
                video.setCountries(apiDao.getCountriesForMetadata(video.getVideoType(), video.getId()));
                localizeCountries(video.getCountries(), options.getLanguage());
            }

            if (options.hasDataItem(CERTIFICATION)) {
                video.setCertifications(apiDao.getCertificationsForMetadata(video.getVideoType(), video.getId()));
                localizeCertifications(video.getCertifications(), options.getLanguage());
            }

            if (options.hasDataItem(RATING)) {
                video.setRatings(apiDao.getRatingsForMetadata(video.getVideoType(), video.getId()));
            }

            if (options.hasDataItem(AWARD)) {
                video.setAwards(apiDao.getAwardsForMetadata(video.getVideoType(), video.getId()));
            }

            if (options.hasDataItem(EXTERNALID)) {
                video.setExternalIds(apiDao.getExternalIdsForMetadata(video.getVideoType(), video.getId()));
            }

            if (options.hasDataItem(BOXSET)) {
                video.setBoxedSets(apiDao.getBoxedSetsForMetadata(video.getVideoType(), video.getId()));
            }

            if (options.hasDataItem(TRAILER) && (MetaDataType.SERIES == video.getVideoType() || MetaDataType.MOVIE.equals(video.getVideoType()))) {
                video.setTrailers(apiDao.getTrailersForMetadata(video.getVideoType(), video.getId()));
            }

            if (options.hasDataItem(VIDEOSOURCE)) {
                video.setVideoSource(apiDao.getVideoSourceForMetadata(video.getVideoType(), video.getId()));
            }

            if (options.hasDataItem(FILES)) {
                video.setFiles(apiDao.getFilesForMetadata(video.getVideoType(), video.getId()));
                localizeFiles(video.getFiles(), options.getLanguage());
            }
            
            if (MapUtils.isNotEmpty(options.splitJobs())) {
                Set<String> jobs = options.getJobTypes();
                LOG.trace("Adding jobs for ID {}: {}", options.getId(), jobs);

                List<ApiPersonDTO> cast = apiDao.getCastForMetadata(video.getVideoType(), video.getId(), options.splitDataItems(), jobs);

                // just add given amount for jobs to cast
                Map<JobType, Integer> jobMap = new HashMap<>(options.splitJobs());
                for (ApiPersonDTO entry : cast) {
                    Integer amount = jobMap.get(entry.getJob());
                    if (amount == null) {
                        video.addCast(entry);
                    } else if (amount > 0) {
                        video.addCast(entry);
                        amount--;
                        jobMap.put(entry.getJob(), amount);
                    }
                }
            } else if (options.isAllJobTypes()) {
                LOG.trace("Adding all jobs for ID {}", options.getId());
                video.setCast(apiDao.getCastForMetadata(video.getVideoType(), video.getId(), options.splitDataItems(), null));
            }
        }

        return video;
    }

    @Transactional
    public ApiStatus updateVideoData(Long id, UpdateVideo update) {
        VideoData videoData = metadataDao.getById(VideoData.class, id);
        if (videoData == null) {
            return ApiStatus.notFound("ID " + id + " does not determine a valid video");
        }

        if (OverrideTools.checkOverwriteTitle(videoData, API_SOURCE)) {
            videoData.setTitle(update.getTitle(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(videoData, API_SOURCE)) {
            videoData.setTitleOriginal(update.getTitleOriginal(), API_SOURCE);
        }

        if (OverrideTools.checkOverwritePlot(videoData, API_SOURCE)) {
            videoData.setPlot(update.getPlot(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteOutline(videoData, API_SOURCE)) {
            videoData.setOutline(update.getOutline(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteQuote(videoData, API_SOURCE)) {
            videoData.setQuote(update.getQuote(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteTagline(videoData, API_SOURCE)) {
            videoData.setTagline(update.getTagline(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteYear(videoData, API_SOURCE)) {
            videoData.setPublicationYear(update.getPublicationYear(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteReleaseDate(videoData, API_SOURCE)) {
            videoData.setRelease(update.getReleaseDate(), API_SOURCE);
        }
        
        videoData.setTopRank(update.getTopRank());

        metadataDao.updateEntity(videoData);
        return ApiStatus.ok("Updated video with ID "+id);
    }

    @Transactional
    public ApiStatus updateSeries(Long id, UpdateVideo update) {
        Series series = metadataDao.getById(Series.class, id);
        if (series == null) {
            return ApiStatus.notFound("ID " + id + " does not determine a valid series");
        }

        if (OverrideTools.checkOverwriteTitle(series, API_SOURCE)) {
            series.setTitle(update.getTitle(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(series, API_SOURCE)) {
            series.setTitleOriginal(update.getTitleOriginal(), API_SOURCE);
        }

        if (OverrideTools.checkOverwritePlot(series, API_SOURCE)) {
            series.setPlot(update.getPlot(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteOutline(series, API_SOURCE)) {
            series.setOutline(update.getOutline(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteYear(series, API_SOURCE)) {
            series.setStartYear(update.getStartYear(), API_SOURCE);
            series.setEndYear(update.getEndYear(), API_SOURCE);
        }
        
        metadataDao.updateEntity(series);
        return ApiStatus.ok("Updated series with ID "+id);
    }
    
    @Transactional
    public ApiStatus updateSeason(Long id, UpdateVideo update) {
        Season season = metadataDao.getById(Season.class, id);
        if (season == null) {
            return ApiStatus.notFound("ID " + id + " does not determine a valid season");
        }
        
        if (OverrideTools.checkOverwriteTitle(season, API_SOURCE)) {
            season.setTitle(update.getTitle(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(season, API_SOURCE)) {
            season.setTitleOriginal(update.getTitleOriginal(), API_SOURCE);
        }

        if (OverrideTools.checkOverwritePlot(season, API_SOURCE)) {
            season.setPlot(update.getPlot(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteOutline(season, API_SOURCE)) {
            season.setOutline(update.getOutline(), API_SOURCE);
        }

        if (OverrideTools.checkOverwriteYear(season, API_SOURCE)) {
            season.setPublicationYear(update.getPublicationYear(), API_SOURCE);
        }
        
        metadataDao.updateEntity(season);
        return ApiStatus.ok("Updated season with ID "+id);
    }

    private void localizeFiles(List<ApiFileDTO> files, String inLanguage) {
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

    public List<ApiSeriesInfoDTO> getSeriesInfo(ApiWrapperList<ApiSeriesInfoDTO> wrapper, OptionsIdArtwork options) {
        List<ApiSeriesInfoDTO> results = apiDao.getSeriesInfo(wrapper, options);
        
        for (ApiSeriesInfoDTO series : results) {

            if (options.hasDataItem(GENRE)) {
                series.setGenres(apiDao.getGenresForMetadata(MetaDataType.SERIES, series.getId()));
            }

            if (options.hasDataItem(STUDIO)) {
                series.setStudios(apiDao.getStudiosForMetadata(MetaDataType.SERIES, series.getId()));
            }

            if (options.hasDataItem(COUNTRY)) {
                series.setCountries(apiDao.getCountriesForMetadata(MetaDataType.SERIES, series.getId()));
                localizeCountries(series.getCountries(), options.getLanguage());
            }

            if (options.hasDataItem(CERTIFICATION)) {
                series.setCertifications(apiDao.getCertificationsForMetadata(MetaDataType.SERIES, series.getId()));
                localizeCertifications(series.getCertifications(), options.getLanguage());
            }
            
            if (options.hasDataItem(RATING)) {
                series.setRatings(apiDao.getRatingsForMetadata(MetaDataType.SERIES, series.getId()));
            }

            if (options.hasDataItem(AWARD)) {
                series.setAwards(apiDao.getAwardsForMetadata(MetaDataType.SERIES, series.getId()));
            }
        }
        
        return results;
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
        updatedWatched(type, id, watched);
        return ApiStatus.ok("Set " + type + " " + id + " to " + watched);
    }

    /**
     * Update a list of videodata records
     *
     * @param type
     * @param ids
     * @param watched
     * @return
     */
    @Transactional
    public ApiStatus updateWatchedList(MetaDataType type, List<Long> ids, boolean watched) {
        if (CollectionUtils.isEmpty(ids)) {
            return ApiStatus.badRequest("No " + type + " IDs provided");
        }
        
        for (Long id : ids) {
            updatedWatched(type, id, watched);
        }
        
        return ApiStatus.ok("Set " + type + " " + ids + " to " + watched);
    }
    
    private void updatedWatched(MetaDataType type, Long id, boolean watched) {
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
        if (videoDatas == null) {
            return;
        }
        
        final Date watchedApiDate = new Date();
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
    public ApiStatus rescanMetaData(MetaDataType type, Long id) {
        boolean rescan = false;
        
        switch (type) {
            case PERSON:
                Person person = metadataDao.getById(Person.class, id);
                if (person != null) {
                    rescan = true;
                    markAsUpdated(person);
                    markAsUpdated(person.getPhoto());
                }
                break;
            case FILMOGRAPHY:
                person = metadataDao.getById(Person.class, id);
                if (person != null) {
                    rescan = true;
                    if (!StatusType.NEW.equals(person.getFilmographyStatus()) && !StatusType.UPDATED.equals(person.getFilmographyStatus())) {
                        person.setFilmographyStatus(StatusType.UPDATED);
                    }
                }
                break;
            case SERIES:
                Series series = commonDao.getById(Series.class, id);
                if (series != null) {
                    rescan = true;
                    markAsUpdated(series);
                    markAsUpdated(series.getArtworks());
                    
                    for (Season season : series.getSeasons()) {
                        markAsUpdated(season);
                        markAsUpdated(season.getArtworks());
                        
                        for (VideoData episode : season.getVideoDatas()) {
                            markAsUpdated(episode);
                            markAsUpdated(episode.getArtworks());
                        }
                    }
                }
                break;
            case SEASON:
                Season season = commonDao.getById(Season.class, id);
                if (season != null) {
                    rescan = true;
                    markAsUpdated(season);
                    markAsUpdated(season.getArtworks());
                    
                    for (VideoData episode : season.getVideoDatas()) {
                        markAsUpdated(episode);
                        markAsUpdated(episode.getArtworks());
                    }
                }
                break;
            case MOVIE:
            case EPISODE:
                VideoData videoData = commonDao.getById(VideoData.class, id);
                if (videoData != null) {
                    rescan = true;
                    markAsUpdated(videoData);
                    markAsUpdated(videoData.getArtworks());
                }
                break;
            default:
                // nothing to rescan
                break;
        }
        
        if (rescan) {
            return ApiStatus.ok("Rescan " + type.name().toLowerCase() + " for ID: " + id);
        }
        
        return ApiStatus.notFound("No " + type.name().toLowerCase() + " found for rescanning ID: " + id);
    }

    private static void markAsUpdated(Person person) {
        if (person.isNotUpdated()) {
            person.setStatus(StatusType.UPDATED);
            person.setFilmographyStatus(StatusType.UPDATED);
        }
    }

    private static void markAsUpdated(VideoData videoData) {
        if (videoData.isNotUpdated()) {
            videoData.setStatus(StatusType.UPDATED);
            if (videoData.isMovie()) {
                videoData.setTrailerStatus(StatusType.UPDATED);
            }
        }
    }
    
    private static void markAsUpdated(Season season) {
        if (season.isNotUpdated()) {
            season.setStatus(StatusType.UPDATED);
        }
    }

    private static void markAsUpdated(Series series) {
        if (series.isNotUpdated()) {
            series.setStatus(StatusType.UPDATED);
            series.setTrailerStatus(StatusType.UPDATED);
        }
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
        
        switch (type) {
            case PERSON:
                Person person = metadataDao.getById(Person.class, id);
                if (person != null) {
                    rescan = true;
                    markAsUpdated(person.getPhoto());
                }
                break;
            case SERIES:
                Series series = commonDao.getById(Series.class, id);
                if (series != null) {
                    rescan = true;
                    markAsUpdated(series.getArtworks());
                }
                break;
            case SEASON:
                Season season = commonDao.getById(Season.class, id);
                if (season != null) {
                    rescan = true;
                    markAsUpdated(season.getArtworks());
                }
                break;
            case BOXSET:
                BoxedSet boxedSet = commonDao.getBoxedSet(id);
                if (boxedSet != null) {
                    rescan = true;
                    markAsUpdated(boxedSet.getArtworks());
                }
                break;
            case MOVIE:
            case EPISODE:
                VideoData videoData = commonDao.getById(VideoData.class, id);
                if (videoData != null) {
                    rescan = true;
                    markAsUpdated(videoData.getArtworks());
                }
                break;
            default:
                // nothing to rescan
                break;
        }
        
        if (rescan) {
            return ApiStatus.ok("Rescan " + type.name().toLowerCase() + " artwork for ID: " + id);
        }
        return ApiStatus.notFound("No " + type.name().toLowerCase() + " found for artwork rescanning ID: " + id);
    }
    
    
    private static void markAsUpdated(List<Artwork> artworks) {
        for (Artwork artwork : artworks) {
            markAsUpdated(artwork);
        }
    }

    private static void markAsUpdated(Artwork artwork) {
        if (artwork.isNotUpdated()) {
            artwork.setStatus(StatusType.UPDATED);
        }
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
                        rescan = true;
                        if (!StatusType.NEW.equals(series.getTrailerStatus()) && !StatusType.UPDATED.equals(series.getTrailerStatus())) {
                            series.setTrailerStatus(StatusType.UPDATED);
                        }
                    }
                    break;
                case MOVIE:
                    VideoData videoData = commonDao.getById(VideoData.class, id);
                    if (videoData != null) {
                        rescan = true;
                        if (!StatusType.NEW.equals(videoData.getTrailerStatus()) && !StatusType.UPDATED.equals(videoData.getTrailerStatus())) {
                            videoData.setTrailerStatus(StatusType.UPDATED);
                        }
                    }
                    break;
                default:
                    // nothing to rescan
                    break;
            }
            
            if (rescan) {
                return ApiStatus.ok("Rescan " + type.name().toLowerCase() + " trailer for ID: " + id);
            }
            return ApiStatus.notFound("No " + type.name().toLowerCase() + " found for trailer rescanning ID: " + id);
        }

        return ApiStatus.badRequest("No valid " + type.name().toLowerCase() + " ID provided");
    }

    @Transactional
    public ApiStatus rescanAll() {
        this.apiDao.rescanAll();
        return ApiStatus.ok("Rescan forced for all meta data objects");
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
            return ApiStatus.conflict(sb.toString());
        }

        if (MetaDataType.PERSON == type) {
            Person person = metadataDao.getById(Person.class, id);
            if (person == null) {
                return ApiStatus.notFound("Person for ID " + id + " not found");
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
                return ApiStatus.notFound("Series for ID " + id + " not found");
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
                return ApiStatus.notFound("VideoData for ID " + id + " not found");
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
        return ApiStatus.ok(sb.toString());
    }

    @Transactional
    @CacheEvict(value=API_EXTERNAL_IDS, key="{#type, #id}")
    public ApiStatus updateExternalId(MetaDataType type, Long id, String sourceDb, String sourceDbId) {
        // first check if source is known
        if (!onlineScannerService.isKnownScanner(type, sourceDb)) {
            StringBuilder sb = new StringBuilder();
            sb.append("The sourceDb ");
            sb.append(sourceDb);
            sb.append(" is not valid for " );
            sb.append(type.name().toLowerCase());
            return ApiStatus.conflict(sb.toString());
        }

        if (MetaDataType.PERSON.equals(type)) {
            Person person = commonDao.getById(Person.class, id);
            if (person == null) {
                return ApiStatus.notFound("Person for ID " + id + " not found");
            }
    
            if (resetSourceDbId(person, sourceDb, sourceDbId)) {
                this.metadataStorageService.handleModifiedSources(person);
            }
        } else if (MetaDataType.SERIES.equals(type)) {
            Series series =  commonDao.getById(Series.class, id);
            if (series == null) {
                return ApiStatus.notFound("Series for ID " + id + " not found");
            }
            
            if (resetSourceDbId(series, sourceDb, sourceDbId)) {
                this.metadataStorageService.handleModifiedSources(series);
            }
        } else if (MetaDataType.SEASON.equals(type)) {
            Season season = commonDao.getById(Season.class, id);
            if (season == null) {
                return ApiStatus.notFound("Season for ID " + id + " not found");
            }

            if (resetSourceDbId(season, sourceDb, sourceDbId)) {
                this.metadataStorageService.handleModifiedSources(season);
            }
        } else {
            VideoData videoData = commonDao.getById(VideoData.class, id);
            if (videoData == null) {
                return ApiStatus.notFound("Video for ID " + id + " not found");
            } else if (MetaDataType.EPISODE.equals(type) && videoData.isMovie()) {
                return ApiStatus.notFound("Episode for ID " + id + " not found");
            } else if (MetaDataType.MOVIE.equals(type) && !videoData.isMovie()) {
                return ApiStatus.notFound("Movie for ID " + id + " not found");
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
        return ApiStatus.ok(sb.toString());
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
            return ApiStatus.notFound("Trailer for ID " + id + " not found");
        }
        
        if (trailer.isCached() && trailer.isUpdated()) {
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
        return ApiStatus.ok(sb.toString());
    }
    //</editor-fold>
}
