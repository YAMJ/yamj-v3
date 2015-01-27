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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.dto.ApiPersonDTO;
import org.yamj.core.api.options.OptionsId;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@ResponseBody
@RequestMapping(value = "/api/person/**", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
public class PersonController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonController.class);
    @Autowired
    private JsonApiStorageService jsonApiStorageService;

    @RequestMapping("/{id}")
    public ApiWrapperSingle<ApiPersonDTO> getPersonById(@ModelAttribute("options") OptionsId options) {
        ApiWrapperSingle<ApiPersonDTO> wrapper = new ApiWrapperSingle<>();
        if (options.getId() > 0) {
            LOG.info("Getting person with ID '{}'", options.getId());
            wrapper.setOptions(options);
            jsonApiStorageService.getPerson(wrapper);
            wrapper.setStatusCheck();
        } else {
            wrapper.setResult(null);
            wrapper.setStatusInvalidId();
        }
        return wrapper;
    }

    @RequestMapping("/movie")
    public ApiWrapperList<ApiPersonDTO> getPersonListByMovie(@ModelAttribute("options") OptionsId options) {
        return getPersonListByVideo(MetaDataType.MOVIE, options);
    }

    @RequestMapping("/series")
    public ApiWrapperList<ApiPersonDTO> getPersonListBySeries(@ModelAttribute("options") OptionsId options) {
        return getPersonListByVideo(MetaDataType.SERIES, options);
    }

    @RequestMapping("/season")
    public ApiWrapperList<ApiPersonDTO> getPersonListBySeason(@ModelAttribute("options") OptionsId options) {
        return getPersonListByVideo(MetaDataType.SEASON, options);
    }

    @RequestMapping("/episode")
    public ApiWrapperList<ApiPersonDTO> getPersonListByEpisode(@ModelAttribute("options") OptionsId options) {
        return getPersonListByVideo(MetaDataType.EPISODE, options);
    }

    private ApiWrapperList<ApiPersonDTO> getPersonListByVideo(MetaDataType metaDataType, OptionsId options) {
        ApiWrapperList<ApiPersonDTO> wrapper = new ApiWrapperList<>();

        if (options.getId() > 0L) {
            LOG.info("Getting person list for {} with ID '{}'", metaDataType, options.getId());
            wrapper.setOptions(options);
            jsonApiStorageService.getPersonListByVideoType(metaDataType, wrapper);
            wrapper.setStatusCheck();
        } else {
            wrapper.setResults(null);
            wrapper.setStatusInvalidId();
        }
        return wrapper;
    }
}
