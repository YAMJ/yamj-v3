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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.builder.DataItem;
import org.yamj.core.api.model.dto.*;
import org.yamj.core.api.options.*;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.service.JsonApiStorageService;
import org.yamj.core.service.ScanningScheduler;

@Controller
@ResponseBody
@RequestMapping(value = "/api/video/**", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
public class VideoController {

    private static final Logger LOG = LoggerFactory.getLogger(VideoController.class);
    
    @Autowired
    private JsonApiStorageService jsonApi;
    @Autowired
    private ScanningScheduler scanningScheduler;

    /**
     * Get information on a movie
     *
     * @param options
     * @return
     */
    @RequestMapping("/movie/{id}")
    public ApiWrapperSingle<ApiVideoDTO> getVideoById(@ModelAttribute("options") OptionsIndexVideo options) {
        ApiWrapperSingle<ApiVideoDTO> wrapper = new ApiWrapperSingle<>();
        // Set the type to movie
        options.setType("MOVIE");
        wrapper.setOptions(options);

        if (options.getId() > 0L) {
            LOG.info("Getting video with ID '{}'", options.getId());
            jsonApi.getSingleVideo(wrapper);
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    /**
     * Enable online scan for one movie.
     */
    @RequestMapping("/movie/enableonlinescan")
    public ApiStatus enableMovieOnlineScan(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb) 
    {
        if (id <= 0L) {
            return new ApiStatus(410, "Not a valid ID");            
        }
        
        LOG.info("Enable {} online scan for movie with ID '{}'", sourcedb, id);
        ApiStatus apiStatus = jsonApi.updateOnlineScan(MetaDataType.MOVIE, id, sourcedb, false);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    /**
     * Disable online scan for one movie.
     */
    @RequestMapping("/movie/disableonlinescan")
    public ApiStatus disableMovieOnlineScan(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb) 
    {
        if (id <= 0L) {
            return new ApiStatus(410, "Not a valid ID");            
        }
        
        LOG.info("Disable {} online scan for movie with ID '{}'", sourcedb, id);
        ApiStatus apiStatus = jsonApi.updateOnlineScan(MetaDataType.MOVIE, id, sourcedb, true);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    /**
     * Add or update an external id of a movie.
     */
    @RequestMapping("/movie/updateexternalid")
    public ApiStatus updateMovieExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb,
            @RequestParam(required = true) String externalid
    ) {
        LOG.info("Set {} external ID '{}' for movie ID {}", sourcedb, externalid, id);
        ApiStatus apiStatus = this.jsonApi.updateExternalId(MetaDataType.MOVIE, id, sourcedb, externalid);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    /**
     * Add or update an external id of a movie.
     */
    @RequestMapping("/movie/removeexternalid")
    public ApiStatus removeMovieExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb
    ) {
        LOG.info("Remove {} external ID from movie ID {}", sourcedb, id);
        ApiStatus apiStatus = this.jsonApi.updateExternalId(MetaDataType.MOVIE, id, sourcedb, null);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    /**
     * Get information on a series
     *
     * TODO: Get associate seasons for the series
     *
     * @param options
     * @return
     */
    @RequestMapping("/series/{id}")
    public ApiWrapperSingle<ApiVideoDTO> getSeriesById(@ModelAttribute("options") OptionsIndexVideo options) {
        ApiWrapperSingle<ApiVideoDTO> wrapper = new ApiWrapperSingle<>();
        // Set the type to movie
        options.setType("SERIES");
        wrapper.setOptions(options);

        if (options.getId() > 0L) {
            LOG.info("Getting series with ID '{}'", options.getId());
            jsonApi.getSingleVideo(wrapper);
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    /**
     * Enable online scan for one series.
     */
    @RequestMapping("/series/enableonlinescan")
    public ApiStatus enableSeriesOnlineScan(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb) 
    {
        if (id <= 0L) {
            return new ApiStatus(410, "Not a valid ID");            
        }
        
        LOG.info("Enable {} online scan for series with ID '{}'", sourcedb, id);
        ApiStatus apiStatus = jsonApi.updateOnlineScan(MetaDataType.SERIES, id, sourcedb, false);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    /**
     * Disable online scan for one series.
     */
    @RequestMapping("/series/disableonlinescan")
    public ApiStatus disableSeriesOnlineScan(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb) 
    {
        if (id <= 0L) {
            return new ApiStatus(410, "Not a valid ID");            
        }
        
        LOG.info("Disable {} online scan for series with ID '{}'", sourcedb, id);
        ApiStatus apiStatus = jsonApi.updateOnlineScan(MetaDataType.SERIES, id, sourcedb, true);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    } 
    
    /**
     * Add or update an external id of a series.
     */
    @RequestMapping("/series/updateexternalid")
    public ApiStatus updateSeriesExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb,
            @RequestParam(required = true) String externalid
    ) {
        LOG.info("Set {} external ID '{}' for series ID {}", sourcedb, externalid, id);
        ApiStatus apiStatus = this.jsonApi.updateExternalId(MetaDataType.SERIES, id, sourcedb, externalid);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
        
    }

    /**
     * Add or update an external id of a series.
     */
    @RequestMapping("/series/removeexternalid")
    public ApiStatus removeSeriesExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb
    ) {
        LOG.info("Remove {} external ID from series ID {}", sourcedb, id);
        ApiStatus apiStatus = this.jsonApi.updateExternalId(MetaDataType.SERIES, id, sourcedb, null);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    /**
     * Add or update an external id of a series.
     */
    @RequestMapping("/season/updateexternalid")
    public ApiStatus updateSeasonExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb,
            @RequestParam(required = true) String externalid
    ) {
        LOG.info("Set {} external ID '{}' for season ID {}", sourcedb, externalid, id);
        ApiStatus apiStatus = this.jsonApi.updateExternalId(MetaDataType.SEASON, id, sourcedb, externalid);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
        
    }

    /**
     * Add or update an external id of a series.
     */
    @RequestMapping("/season/removeexternalid")
    public ApiStatus removeSeasonExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb
    ) {
        LOG.info("Remove {} external ID from season ID {}", sourcedb, id);
        ApiStatus apiStatus = this.jsonApi.updateExternalId(MetaDataType.SEASON, id, sourcedb, null);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    /**
     * Add or update an external id of a series.
     */
    @RequestMapping("/episode/updateexternalid")
    public ApiStatus updateEpisodeExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb,
            @RequestParam(required = true) String externalid
    ) {
        LOG.info("Set {} external ID '{}' for episode ID {}", sourcedb, externalid, id);
        ApiStatus apiStatus = this.jsonApi.updateExternalId(MetaDataType.EPISODE, id, sourcedb, externalid);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
        
    }

    /**
     * Add or update an external id of a series.
     */
    @RequestMapping("/episode/removeexternalid")
    public ApiStatus removeEpisodeExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb
    ) {
        LOG.info("Remove {} external ID from episode ID {}", sourcedb, id);
        ApiStatus apiStatus = this.jsonApi.updateExternalId(MetaDataType.EPISODE, id, sourcedb, null);
        if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        return apiStatus;
    }

    /**
     * Get information on a season
     *
     * TODO: Add episodes to the season
     *
     * @param options
     * @return
     */
    @RequestMapping("/season/{id}")
    public ApiWrapperSingle<ApiVideoDTO> getSeasonById(@ModelAttribute("options") OptionsIndexVideo options) {
        ApiWrapperSingle<ApiVideoDTO> wrapper = new ApiWrapperSingle<>();
        // Set the type to movie
        options.setType("SEASON");
        wrapper.setOptions(options);

        if (options.getId() > 0L) {
            LOG.info("Getting season with ID '{}'", options.getId());
            jsonApi.getSingleVideo(wrapper);
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    /**
     * Get information on a series
     *
     * @param options
     * @return
     */
    @RequestMapping("/seriesinfo")
    public ApiWrapperList<ApiSeriesInfoDTO> getSeriesInfo(@ModelAttribute("options") OptionsIdArtwork options) {
        ApiWrapperList<ApiSeriesInfoDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);

        if (options.getId() > 0L) {
            LOG.info("Getting series info for SeriesID '{}'", options.getId());
            if (options.hasDataItem(DataItem.ARTWORK) && StringUtils.isBlank(options.getArtwork())) {
                options.setArtwork("all");
            }
            jsonApi.getSeriesInfo(wrapper);
            wrapper.setStatusCheck();
        } else {
            wrapper.setStatusInvalidId();
        }
        return wrapper;
    }

    /**
     * Get information on episodes
     *
     * @param options
     * @return
     */
    @RequestMapping("/episodes")
    public ApiWrapperList<ApiEpisodeDTO> getEpisodes(@ModelAttribute("options") OptionsEpisode options) {
        LOG.info("Getting episodes for seriesId '{}', seasonId '{}', season '{}'",
                options.getSeriesid() < 0L ? "All" : options.getSeriesid(),
                options.getSeasonid() < 0L ? "All" : options.getSeasonid(),
                options.getSeason() < 0L ? "All" : options.getSeason());

        ApiWrapperList<ApiEpisodeDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        jsonApi.getEpisodeList(wrapper);
        wrapper.setStatusCheck();
        return wrapper;
    }
    
    /**
     * Get list with years.
     *
     * @param options
     * @return
     */
    @RequestMapping("/years/list")
    public ApiWrapperList<ApiYearDecadeDTO> getYears(@ModelAttribute("options") OptionsMultiType options) {
        LOG.info("Getting year list with {}", options.toString());

        ApiWrapperList<ApiYearDecadeDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApi.getYears(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
        
    }

    /**
     * Get list with decades.
     *
     * @param options
     * @return
     */
    @RequestMapping("/decades/list")
    public ApiWrapperList<ApiYearDecadeDTO> getDecades(@ModelAttribute("options") OptionsMultiType options) {
        LOG.info("Getting decade list with {}", options.toString());

        ApiWrapperList<ApiYearDecadeDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApi.getDecades(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
        
    }
}
