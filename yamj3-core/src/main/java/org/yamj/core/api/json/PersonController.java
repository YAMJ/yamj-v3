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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.dto.ApiPersonDTO;
import org.yamj.core.api.options.OptionsId;
import org.yamj.core.api.options.UpdatePerson;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.service.JsonApiStorageService;
import org.yamj.core.scheduling.MetadataScanScheduler;

@RestController
@RequestMapping(value = "/api/person", produces = "application/json; charset=utf-8")
public class PersonController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonController.class);
    @Autowired
    private JsonApiStorageService jsonApiStorageService;
    @Autowired
    private MetadataScanScheduler metadataScanScheduler;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ApiWrapperSingle<ApiPersonDTO> getPerson(@ModelAttribute("options") OptionsId options) {
        ApiWrapperSingle<ApiPersonDTO> wrapper = new ApiWrapperSingle<>(options);
        if (options.getId() > 0L) {
            LOG.trace("Getting person with ID {}", options.getId());
            wrapper.setResult(jsonApiStorageService.getPerson(wrapper, options));
        } else {
            wrapper.setStatusInvalidId();
        }
        return wrapper;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ApiStatus updatePerson(@PathVariable("id") Long id, @RequestBody UpdatePerson update) {
        return jsonApiStorageService.updatePerson(id, update);
    }

    @RequestMapping(value = "/movie", method = RequestMethod.GET)
    public ApiWrapperList<ApiPersonDTO> getPersonListByMovie(@ModelAttribute("options") OptionsId options) {
        return getPersonListByVideo(MetaDataType.MOVIE, options);
    }

    @RequestMapping(value = "/series", method = RequestMethod.GET)
    public ApiWrapperList<ApiPersonDTO> getPersonListBySeries(@ModelAttribute("options") OptionsId options) {
        return getPersonListByVideo(MetaDataType.SERIES, options);
    }

    @RequestMapping(value = "/season", method = RequestMethod.GET)
    public ApiWrapperList<ApiPersonDTO> getPersonListBySeason(@ModelAttribute("options") OptionsId options) {
        return getPersonListByVideo(MetaDataType.SEASON, options);
    }

    @RequestMapping(value = "/episode", method = RequestMethod.GET)
    public ApiWrapperList<ApiPersonDTO> getPersonListByEpisode(@ModelAttribute("options") OptionsId options) {
        return getPersonListByVideo(MetaDataType.EPISODE, options);
    }

    private ApiWrapperList<ApiPersonDTO> getPersonListByVideo(MetaDataType metaDataType, OptionsId options) {
        ApiWrapperList<ApiPersonDTO> wrapper = new ApiWrapperList<>(options);
        if (options.getId() > 0L) {
            LOG.info("Getting person list for {} with ID {}", metaDataType, options.getId());
            wrapper.setResults(jsonApiStorageService.getPersonListByVideoType(metaDataType, wrapper));
        } else {
            wrapper.setStatusInvalidId();
        }
        return wrapper;
    }

    /**
     * Add or update an external id of a series.
     */
    @RequestMapping(value = "/updateexternalid", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus updateExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb,
            @RequestParam(required = true) String externalid
    ) {
        LOG.info("Set {} external id '{}' for person ID {}", sourcedb, externalid, id);
        ApiStatus apiStatus = this.jsonApiStorageService.updateExternalId(MetaDataType.PERSON, id, sourcedb, externalid);
        if (apiStatus.isSuccessful()) {
            metadataScanScheduler.triggerScanPeople();
        }
        return apiStatus;
        
    }

    /**
     * Add or update an external id of a series.
     */
    @RequestMapping(value = "/removeexternalid", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus removeExternalId(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb
    ) {
        LOG.info("Remove {} external id from person ID {}", sourcedb, id);
        ApiStatus apiStatus = this.jsonApiStorageService.updateExternalId(MetaDataType.PERSON, id, sourcedb, null);
        if (apiStatus.isSuccessful()) {
            metadataScanScheduler.triggerScanPeople();
        }
        return apiStatus;
    }

    /**
     * Enable online scan for one person.
     */
    @RequestMapping(value = "/enableonlinescan", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus enableOnlineScan(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb) 
    {
        if (id <= 0L) {
            return ApiStatus.INVALID_ID;
        }
        
        LOG.info("Enable {} online scan for person with ID {}", sourcedb, id);
        ApiStatus apiStatus = jsonApiStorageService.updateOnlineScan(MetaDataType.PERSON, id, sourcedb, false);
        if (apiStatus.isSuccessful()) {
            metadataScanScheduler.triggerScanPeople();
        }
        return apiStatus;
    }

    /**
     * Disable online scan for one person.
     */
    @RequestMapping(value = "/disableonlinescan", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus disableOnlineScan(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) String sourcedb) 
    {
        if (id <= 0L) {
            return ApiStatus.INVALID_ID;
        }
        
        LOG.info("Disable {} online scan for person with ID {}", sourcedb, id);
        ApiStatus apiStatus =  jsonApiStorageService.updateOnlineScan(MetaDataType.PERSON, id, sourcedb, true);
        if (apiStatus.isSuccessful()) {
            metadataScanScheduler.triggerScanPeople();
        }
        return apiStatus;
    }

    /**
     * Handle duplicate of a person.
     */
    @RequestMapping(value = "/duplicate", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus duplicate(
            @RequestParam(required = true) Long id,
            @RequestParam(required = true) Long doublet) 
    {
        if (id <= 0L || doublet <= 0L) {
            return ApiStatus.INVALID_ID;
        }
        
        LOG.info("Handle {} as doublet of {}", doublet, id);
        return jsonApiStorageService.duplicatePerson(id, doublet);
    }
}
