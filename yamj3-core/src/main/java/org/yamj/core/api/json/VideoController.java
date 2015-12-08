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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.builder.DataItem;
import org.yamj.core.api.model.dto.ApiEpisodeDTO;
import org.yamj.core.api.model.dto.ApiSeriesInfoDTO;
import org.yamj.core.api.model.dto.ApiVideoDTO;
import org.yamj.core.api.model.dto.ApiYearDecadeDTO;
import org.yamj.core.api.options.OptionsEpisode;
import org.yamj.core.api.options.OptionsIdArtwork;
import org.yamj.core.api.options.OptionsIndexVideo;
import org.yamj.core.api.options.OptionsMultiType;
import org.yamj.core.api.options.UpdateVideo;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.service.JsonApiStorageService;
import org.yamj.core.scheduling.ScanningScheduler;

@RestController
@RequestMapping(value = "/api/video/**", produces = "application/json; charset=utf-8")
public class VideoController {

    private static final Logger LOG = LoggerFactory.getLogger(VideoController.class);
    
    @Autowired
    private JsonApiStorageService jsonApiStorageService;
    @Autowired
    private ScanningScheduler scanningScheduler;

    /**
     * Get information on a video.
     *
     * @param type
     * @param options
     * @return
     */
    @RequestMapping(value = "/{type}/{id}", method = RequestMethod.GET)
    public ApiWrapperSingle<ApiVideoDTO> getVideo(@PathVariable("type") String type, @ModelAttribute("options") OptionsIndexVideo options) {
        ApiWrapperSingle<ApiVideoDTO> wrapper = new ApiWrapperSingle<>();
        wrapper.setOptions(options);

        final MetaDataType metaDataType = MetaDataType.fromString(type);
        if (MetaDataType.MOVIE == metaDataType || MetaDataType.SEASON == metaDataType || MetaDataType.SERIES == metaDataType) {
            // set the valid meta data type
            options.setType(metaDataType.name());

            if (options.getId() > 0L) {
                LOG.info("Getting {} with ID '{}'", options.getType(), options.getId());
                jsonApiStorageService.getSingleVideo(wrapper);
            }
            
            wrapper.setStatusCheck();
        } else {
            ApiStatus apiStatus = new ApiStatus(415, "Invalid meta data type '" + type + "' for single video");
            wrapper.setStatusCheck(apiStatus);
        }
        
        return wrapper;
    }

    /**
     * Update information on a video.
     *
     * @param type
     * @param id
     * @param update
     * @return
     */
    @RequestMapping(value = "/{type}/{id}", method = RequestMethod.PUT)
    public ApiStatus updateVideo(@PathVariable("type") String type, @PathVariable("id") Long id, @RequestBody UpdateVideo update) {
        final MetaDataType metaDataType = MetaDataType.fromString(type);
        
        if (MetaDataType.SERIES == metaDataType) {
            return jsonApiStorageService.updateSeries(id, update);
        }

        if (MetaDataType.SEASON == metaDataType) {
            return jsonApiStorageService.updateSeason(id, update);
        }

        if (MetaDataType.MOVIE == metaDataType || MetaDataType.EPISODE == metaDataType) {
            return jsonApiStorageService.updateVideoData(id, update);
        }

        return new ApiStatus(415, "Invalid meta data type '" + type + "' for video update");
    }

    /**
     * Enable online scan for one movie or series.
     */
    @RequestMapping(value = "/{type}/enableonlinescan", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus enableOnlineScan(
            @PathVariable("type") String type,        
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb) 
    {
        final MetaDataType metaDataType = MetaDataType.fromString(type);

        ApiStatus apiStatus;
        if (id <= 0L) {
            apiStatus =  new ApiStatus(410, "Not a valid ID");            
        } else if (MetaDataType.MOVIE == metaDataType || MetaDataType.SERIES == metaDataType) {
            LOG.info("Enable {} online scan for {} with ID '{}'", sourcedb, metaDataType.name().toLowerCase(), id);

            apiStatus = jsonApiStorageService.updateOnlineScan(metaDataType, id, sourcedb, false);
            if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        } else {
            apiStatus = new ApiStatus(415, "Invalid meta data type '" + type + "' for enabling online scan");
        }
        return apiStatus;
    }

    /**
     * Disable online scan for one movie or series.
     */
    @RequestMapping(value = "/{type}/disableonlinescan", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus disableOnlineScan(
            @PathVariable("type") String type,        
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb) 
    {
        final MetaDataType metaDataType = MetaDataType.fromString(type);

        ApiStatus apiStatus;
        if (id <= 0L) {
            apiStatus =  new ApiStatus(410, "Not a valid ID");            
        } else if (MetaDataType.MOVIE == metaDataType || MetaDataType.SERIES == metaDataType) {
            LOG.info("Disable {} online scan for {} with ID '{}'", sourcedb, metaDataType.name().toLowerCase(), id);

            apiStatus = jsonApiStorageService.updateOnlineScan(metaDataType, id, sourcedb, true);
            if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        } else {
            apiStatus = new ApiStatus(415, "Invalid meta data type '" + type + "' for disabling online scan");
        }
        return apiStatus;
    }

    /**
     * Add or update an external id for a meta data with videos.
     */
    @RequestMapping(value = "/{type}/updateexternalid", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus updateExternalId(
            @PathVariable("type") String type,        
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb,
            @RequestParam(required = true) String externalid
    ) {
        final MetaDataType metaDataType = MetaDataType.fromString(type);

        ApiStatus apiStatus;
        if (metaDataType.isWithVideos()) {
            LOG.info("Set {} external ID '{}' for {} ID {}", sourcedb, externalid, metaDataType.name().toLowerCase(), id);
            
            apiStatus = this.jsonApiStorageService.updateExternalId(metaDataType, id, sourcedb, externalid);
            if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        } else {
            apiStatus = new ApiStatus(415, "Invalid meta data type '" + type + "' for updating external id");
        }
        return apiStatus;
    }

    /**
     * Add or update an external id of a meta data with videos.
     */
    @RequestMapping(value = "/{type}/removeexternalid", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus removeExternalId(
            @PathVariable("type") String type,        
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb
    ) {
        final MetaDataType metaDataType = MetaDataType.fromString(type);

        ApiStatus apiStatus;
        if (metaDataType.isWithVideos()) {
            LOG.info("Remove {} external ID from {} ID {}", sourcedb,  metaDataType.name().toLowerCase(), id);
            
            apiStatus = this.jsonApiStorageService.updateExternalId(metaDataType, id, sourcedb, null);
            if (apiStatus.isSuccessful()) scanningScheduler.triggerScanMetaData();
        } else {
            apiStatus = new ApiStatus(415, "Invalid meta data type '" + type + "' for removing external id");
        }
        return apiStatus;
    }

    /**
     * Get information on a series
     *
     * @param options
     * @return
     */
    @RequestMapping(value = "/seriesinfo", method = RequestMethod.GET)
    public ApiWrapperList<ApiSeriesInfoDTO> getSeriesInfo(@ModelAttribute("options") OptionsIdArtwork options) {
        ApiWrapperList<ApiSeriesInfoDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);

        if (options.getId() > 0L) {
            LOG.info("Getting series info for SeriesID '{}'", options.getId());
            if (options.hasDataItem(DataItem.ARTWORK) && StringUtils.isBlank(options.getArtwork())) {
                options.setArtwork("all");
            }
            jsonApiStorageService.getSeriesInfo(wrapper);
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
    @RequestMapping(value = "/episodes", method = RequestMethod.GET)
    public ApiWrapperList<ApiEpisodeDTO> getEpisodes(@ModelAttribute("options") OptionsEpisode options) {
        LOG.info("Getting episodes for seriesId '{}', seasonId '{}', season '{}'",
                options.getSeriesid() < 0L ? "All" : options.getSeriesid(),
                options.getSeasonid() < 0L ? "All" : options.getSeasonid(),
                options.getSeason() < 0L ? "All" : options.getSeason());

        ApiWrapperList<ApiEpisodeDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        jsonApiStorageService.getEpisodeList(wrapper);
        wrapper.setStatusCheck();
        return wrapper;
    }
    
    /**
     * Get list with years.
     *
     * @param options
     * @return
     */
    @RequestMapping(value = "/years/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiYearDecadeDTO> getYears(@ModelAttribute("options") OptionsMultiType options) {
        LOG.info("Getting year list with {}", options.toString());

        ApiWrapperList<ApiYearDecadeDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getYears(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
        
    }

    /**
     * Get list with decades.
     *
     * @param options
     * @return
     */
    @RequestMapping(value = "/decades/list", method = RequestMethod.GET)
    public ApiWrapperList<ApiYearDecadeDTO> getDecades(@ModelAttribute("options") OptionsMultiType options) {
        LOG.info("Getting decade list with {}", options.toString());

        ApiWrapperList<ApiYearDecadeDTO> wrapper = new ApiWrapperList<>();
        wrapper.setOptions(options);
        wrapper.setResults(jsonApiStorageService.getDecades(wrapper));
        wrapper.setStatusCheck();
        return wrapper;
        
    }
}
