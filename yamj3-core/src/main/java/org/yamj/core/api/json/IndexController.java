/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.api.json;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.dto.IndexVideoDTO;
import org.yamj.core.api.options.OptionsIndexVideo;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.dto.IndexPersonDTO;
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
    public ApiWrapperList<IndexVideoDTO> getVideoList(
            @ModelAttribute("options") OptionsIndexVideo options) {
        LOG.debug("INDEX: Video list - Options: {}", options.toString());

        ApiWrapperList<IndexVideoDTO> wrapper = new ApiWrapperList<IndexVideoDTO>();
        wrapper.setParameters(options);
        jsonApiStorageService.getVideoList(wrapper);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/person", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<IndexPersonDTO> getPersonList(
            @ModelAttribute("options") OptionsIndexPerson options) {
        LOG.debug("INDEX: Person list - Options: {}", options.toString());

        ApiWrapperList<IndexPersonDTO> wrapper = new ApiWrapperList<IndexPersonDTO>();
        wrapper.setParameters(options);
        jsonApiStorageService.getPersonList(wrapper);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/count", method = RequestMethod.GET)
    @ResponseBody
    public List<CountTimestamp> getCount(@RequestParam(required = false, defaultValue = "all") String type) {
        List<CountTimestamp> results = new ArrayList<CountTimestamp>();
        if (type.toLowerCase().indexOf("all") < 0) {
            for (String stringType : StringUtils.tokenizeToStringArray(type, ",", true, true)) {
                MetaDataType singleType = MetaDataType.fromString(stringType);
                LOG.debug("Getting a count of '{}'", singleType.toString());
                results.add(jsonApiStorageService.getCountTimestamp(singleType));
            }
        } else {
            LOG.debug("Getting a count of all types");
            for (MetaDataType singleType : MetaDataType.values()) {
                LOG.debug("  Adding a count of '{}'", singleType.toString());
                results.add(jsonApiStorageService.getCountTimestamp(singleType));
            }
        }
        return results;
    }
}
