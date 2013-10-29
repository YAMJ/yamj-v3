/*
 *      Copyright (c) 2004-2013 YAMJ Members
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.dto.ApiVideoDTO;
import org.yamj.core.api.options.OptionsIndexVideo;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.dto.ApiPersonDTO;
import org.yamj.core.api.options.OptionsIndexPerson;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@RequestMapping("/api/index/**")
public class IndexController {

    private static final Logger LOG = LoggerFactory.getLogger(IndexController.class);
    @Autowired
    private JsonApiStorageService jsonApiStorageService;

    @RequestMapping(value = "/video", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<ApiVideoDTO> getVideoList(
            @ModelAttribute("options") OptionsIndexVideo options) {
        LOG.debug("INDEX: Video list - Options: {}", options.toString());

        ApiWrapperList<ApiVideoDTO> wrapper = new ApiWrapperList<ApiVideoDTO>();
        wrapper.setOptions(options);
        jsonApiStorageService.getVideoList(wrapper);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/person", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<ApiPersonDTO> getPersonList(
            @ModelAttribute("options") OptionsIndexPerson options) {
        LOG.debug("INDEX: Person list - Options: {}", options.toString());

        ApiWrapperList<ApiPersonDTO> wrapper = new ApiWrapperList<ApiPersonDTO>();
        wrapper.setOptions(options);
        jsonApiStorageService.getPersonList(wrapper);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ResponseBody
    public List<CountTimestamp> getCount(@RequestParam(required = false, defaultValue = "all") String type) {
        List<CountTimestamp> results = new ArrayList<CountTimestamp>();
        List<MetaDataType> requiredTypes = new ArrayList<MetaDataType>();

        if (type.toLowerCase().indexOf("all") < 0) {
            for (String stringType : StringUtils.split(type, ",")) {
                requiredTypes.add(MetaDataType.fromString(stringType));
            }
        } else {
            LOG.debug("Getting a count of all types");
            requiredTypes = Arrays.asList(MetaDataType.values());
        }

        for (MetaDataType singleType : requiredTypes) {
            LOG.debug("Getting a count of '{}'", singleType.toString());
            CountTimestamp result = jsonApiStorageService.getCountTimestamp(singleType);
            if (result != null) {
                results.add(result);
            }
        }

        return results;
    }

    @RequestMapping(value = "/jobs")
    @ResponseBody
    public List<CountGeneric> getJobs(@RequestParam(required = false, defaultValue = "all") String job) {
        List<CountGeneric> results;

        if (StringUtils.isNotBlank(job) && job.toLowerCase().indexOf("all") < 0) {
            List<String> requiredJobs = Arrays.asList(StringUtils.split(job, ","));
            results = jsonApiStorageService.getJobCount(requiredJobs);
        } else {
            results = jsonApiStorageService.getJobCount(null);
        }
        return results;
    }
}
