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
package org.yamj.core.api.json;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.dto.ApiAwardDTO;
import org.yamj.core.api.model.dto.ApiBoxedSetDTO;
import org.yamj.core.api.model.dto.ApiCertificationDTO;
import org.yamj.core.api.model.dto.ApiCountryDTO;
import org.yamj.core.api.model.dto.ApiGenreDTO;
import org.yamj.core.api.model.dto.ApiNameDTO;
import org.yamj.core.api.model.dto.ApiRatingDTO;
import org.yamj.core.api.options.OptionsBoxedSet;
import org.yamj.core.api.options.OptionsMultiType;
import org.yamj.core.api.options.OptionsRating;
import org.yamj.core.api.options.OptionsSingleType;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;
import org.yamj.core.database.service.JsonApiStorageService;
import org.yamj.core.service.ScanningScheduler;
import org.yamj.core.service.TrailerProcessScheduler;

@RestController
@RequestMapping(value = "/api/**", produces = "application/json; charset=utf-8")
public class CommonController {

    private static final Logger LOG = LoggerFactory.getLogger(CommonController.class);
    @Autowired
    private JsonApiStorageService jsonApiStorageService;
    @Autowired 
    private ScanningScheduler scanningScheduler;
    @Autowired
    private TrailerProcessScheduler trailerProcessScheduler;

    //<editor-fold defaultstate="collapsed" desc="Alphabetical Methods">
    @RequestMapping(value = "/alphabetical/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiNameDTO> getAlphabeticals(@ModelAttribute("options") OptionsMultiType options) {
        LOG.info("Getting alphabetical list with {}", options.toString());

        ApiWrapperList<ApiNameDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getAlphabeticals(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Watched Methods">
    @RequestMapping(value = "/watched/{type}/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus markWatched(@PathVariable("type") String type, @PathVariable("id") Long id) {
        
        final MetaDataType metaDataType = MetaDataType.fromString(type);
        if (!metaDataType.isWithVideos()) {
            return new ApiStatus(415, "Invalid meta data type '" + type + "' for watching videos");
        }

        return jsonApiStorageService.updateWatchedSingle(metaDataType, id, true);
    }

    @RequestMapping(value = "/unwatched/{type}/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus markUnwatched(@PathVariable("type") String type, @PathVariable("id") Long id) {

        final MetaDataType metaDataType = MetaDataType.fromString(type);
        if (!metaDataType.isWithVideos()) {
            return new ApiStatus(415, "Invalid meta data type '" + type + "' for unwatching videos");
        }

        return jsonApiStorageService.updateWatchedSingle(metaDataType, id, false);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Rescan Methods">
    @RequestMapping(value = "/rescan/{type}/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus rescanMetaData(@PathVariable("type") String type, @PathVariable("id") Long id) {

        final MetaDataType metaDataType = MetaDataType.fromString(type);
        if (!metaDataType.isRescanMetaData()) {
            return new ApiStatus(415, "Invalid meta data type '" + type + "' for rescan");
        }

        ApiStatus apiStatus = jsonApiStorageService.rescanMetaData(metaDataType, id);
        if (apiStatus.isSuccessful()) this.scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    @RequestMapping(value = "/rescan/{type}/artwork/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus rescanArtwork(@PathVariable("type") String type, @PathVariable("id") Long id) {

        final MetaDataType metaDataType = MetaDataType.fromString(type);
        if (!metaDataType.isWithArtwork()) {
            return new ApiStatus(415, "Invalid meta data type '" + type + "' for artwork rescan");
        }

        ApiStatus apiStatus = jsonApiStorageService.rescanArtwork(metaDataType, id);
        if (apiStatus.isSuccessful()) this.scanningScheduler.triggerScanArtwork();
        return apiStatus;
    }

    @RequestMapping(value = "/rescan/{type}/trailer/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus rescanTrailer(@PathVariable("type") String type, @PathVariable("id") Long id) {

        final MetaDataType metaDataType = MetaDataType.fromString(type);
        if (!metaDataType.isWithTrailer()) {
            return new ApiStatus(415, "Invalid meta data type '" + type + "' for trailer rescan");
        }

        ApiStatus apiStatus = jsonApiStorageService.rescanTrailer(metaDataType, id);
        if (apiStatus.isSuccessful()) this.scanningScheduler.triggerScanTrailer();
        return apiStatus;
    }

    @RequestMapping(value = "/rescan/all", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus rescanAll() {
        ApiStatus apiStatus = jsonApiStorageService.rescanAll();
        if (apiStatus.isSuccessful()) this.scanningScheduler.triggerAllScans();
        return apiStatus;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Trailer Methods">
    @RequestMapping(value = "/trailer/delete/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus trailerDelete(@PathVariable("id") Long id) {
        return jsonApiStorageService.setTrailerStatus(id, StatusType.DELETED);
    }

    @RequestMapping(value = "/trailer/ignore/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus trailerIgnore( @PathVariable("id") Long id) {
        return jsonApiStorageService.setTrailerStatus(id, StatusType.IGNORE);
    }

    @RequestMapping(value = "/trailer/download/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus trailerDownload( @PathVariable("id") Long id) {
        ApiStatus apiStatus = jsonApiStorageService.setTrailerStatus(id, StatusType.UPDATED);
        if (apiStatus.isSuccessful()) trailerProcessScheduler.triggerProcess();
        return apiStatus;
    }

    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre Methods">
    @RequestMapping(value = "/genre", method = RequestMethod.GET)
    public ApiWrapperList<ApiGenreDTO> getGenreFilename(@RequestParam(required = true, defaultValue = "") String filename) {
        LOG.info("Getting genres for filename '{}'", filename);
        ApiWrapperList<ApiGenreDTO> wrapper = new ApiWrapperList<>();
        List<ApiGenreDTO> results = jsonApiStorageService.getGenreFilename(wrapper, filename);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/genre/{name}", method = RequestMethod.GET)
    public ApiWrapperSingle<ApiGenreDTO> getGenre(@PathVariable("name") String name) {
        Genre genre;
        ApiWrapperSingle<ApiGenreDTO> wrapper = new ApiWrapperSingle<>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting genre with ID '{}'", name);
            genre = jsonApiStorageService.getGenre(Long.parseLong(name));
        } else {
            LOG.info("Getting genre with name '{}'", name);
            genre = jsonApiStorageService.getGenre(name);
        }
        if (genre != null) {
            wrapper.setResult(new ApiGenreDTO(genre));
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/genres/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiGenreDTO> getGenres(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting genre list: used={}, full={}", options.getUsed(), options.getFull());

        ApiWrapperList<ApiGenreDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        List<ApiGenreDTO> results = jsonApiStorageService.getGenres(wrapper);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/genres/add", method = {RequestMethod.GET,RequestMethod.POST})
    public ApiStatus genreAdd(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = true, defaultValue = "") String target) {

        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(target)) {
            LOG.info("Adding genre '{}' with target '{}'", name, target);
            boolean result = this.jsonApiStorageService.addGenre(name, target);
            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully added genre '" + name + "' with target '" + target + "'");
            } else {
                status.setStatus(400);
                status.setMessage("Genre '" + name + "' already exists");
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid name/target specified, genre not added");
        }
        return status;
    }

    @RequestMapping(value = "/genres/update", method = {RequestMethod.GET,RequestMethod.PUT})
    public ApiStatus genreUpdate(
            @RequestParam(required = true, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "") String target) {

        ApiStatus status = new ApiStatus();
        if (StringUtils.isNotBlank(name)) {
            LOG.info("Updating genre '{}' with target '{}'", name, target);

            boolean result;
            if (StringUtils.isNumeric(name)) {
                result = this.jsonApiStorageService.updateGenre(Long.valueOf(name), target);
            } else {
                result = this.jsonApiStorageService.updateGenre(name, target);
            }

            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully updated genre '" + name + "' with target '" + target + "'");
            } else {
                status.setStatus(400);
                status.setMessage("Genre '" + name + "' does not exist");
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid name specified, genre not updated");
        }
        return status;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Studio Methods">
    @RequestMapping(value = "/studio/{name}", method = RequestMethod.GET)
    public ApiWrapperSingle<Studio> getStudio(@PathVariable("name") String name) {
        Studio studio;
        ApiWrapperSingle<Studio> wrapper = new ApiWrapperSingle<>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting studio with ID '{}'", name);
            studio = jsonApiStorageService.getStudio(Long.parseLong(name));
        } else {
            LOG.info("Getting studio '{}'", name);
            studio = jsonApiStorageService.getStudio(name);
        }
        wrapper.setResult(studio);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/studios/list", method = RequestMethod.GET)
    public ApiWrapperList<Studio> getStudios(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting studio list with {}", options.toString());

        ApiWrapperList<Studio> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        List<Studio> results = jsonApiStorageService.getStudios(wrapper);
        wrapper.setResults(results);
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Country Methods">
    @RequestMapping(value = "/country", method = RequestMethod.GET)
    public ApiWrapperList<ApiCountryDTO> getCountryFilename(@RequestParam(required = true, defaultValue = "") String filename) {
        LOG.info("Getting countries for filename '{}'", filename);
        ApiWrapperList<ApiCountryDTO> wrapper = new ApiWrapperList<>();
        List<ApiCountryDTO> results = jsonApiStorageService.getCountryFilename(wrapper, filename);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/country/{countryCode}", method = RequestMethod.GET)
    public ApiWrapperSingle<ApiCountryDTO> getCountry(@PathVariable String countryCode) {
        ApiCountryDTO country;
        ApiWrapperSingle<ApiCountryDTO> wrapper = new ApiWrapperSingle<>();
        if (StringUtils.isNumeric(countryCode)) {
            LOG.info("Getting country with ID '{}'", countryCode);
            country = jsonApiStorageService.getCountry(Long.parseLong(countryCode), wrapper.getOptions().getLanguage());
        } else {
            LOG.info("Getting country with country code {}", countryCode);
            country = jsonApiStorageService.getCountry(countryCode, wrapper.getOptions().getLanguage());
        }
        wrapper.setResult(country);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/countries/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiCountryDTO> getCountries(@ModelAttribute("options") OptionsSingleType options) {
        ApiWrapperList<ApiCountryDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        List<ApiCountryDTO> results = jsonApiStorageService.getCountries(wrapper);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Award Methods">
    @RequestMapping(value = "/awards/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiAwardDTO> getAwards(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting award list with {}", options.toString());

        ApiWrapperList<ApiAwardDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getAwards(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Certification Methods">
    @RequestMapping(value ="/certifications/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiCertificationDTO> getCertifications(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting certifications list with {}", options.toString());

        ApiWrapperList<ApiCertificationDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getCertifications(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="VideoSource Methods">
    @RequestMapping(value = "/videosources/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiNameDTO> getVideoSources(@ModelAttribute("options") OptionsSingleType options) {
        LOG.info("Getting video sources list with {}", options.toString());

        ApiWrapperList<ApiNameDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getVideoSources(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Rating Methods">
    @RequestMapping(value = "/ratings/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiRatingDTO> getRatings(@ModelAttribute("options") OptionsRating options) {
        LOG.info("Getting ratings list with {}", options.toString());

        ApiWrapperList<ApiRatingDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getRatings(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Boxed-Set Methods">
    @RequestMapping(value = "/boxset/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiBoxedSetDTO> getBoxSets(@ModelAttribute("options") OptionsBoxedSet options) {
        LOG.info("Getting boxset list with {}", options.toString());

        ApiWrapperList<ApiBoxedSetDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getBoxedSets(wrapper));
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }

    @RequestMapping(value = "/boxset/{id}", method = RequestMethod.GET)
    public ApiWrapperSingle<ApiBoxedSetDTO> getBoxSet(@ModelAttribute("options") OptionsBoxedSet options) {
        LOG.info("Getting boxset with {}", options.toString());

        ApiWrapperSingle<ApiBoxedSetDTO> wrapper = new ApiWrapperSingle<>();
        wrapper.setOptions(options);
        wrapper.setResult(jsonApiStorageService.getBoxedSet(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>
}
