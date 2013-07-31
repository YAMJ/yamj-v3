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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.common.model.YamjInfo;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.Configuration;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@RequestMapping("/system/**")
@Service("systemInfoController")
public class SystemInfoController {

    private static final Logger LOG = LoggerFactory.getLogger(SystemInfoController.class);
    private static final YamjInfo YAMJ_INFO = new YamjInfo(SystemInfoController.class);
    @Autowired
    private JsonApiStorageService jsonApi;
    @Autowired
    private ConfigService configService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    public String getSystemUp() {
        StringBuilder sb = new StringBuilder("YAMJ v3 is running, uptime is ");
        sb.append(YAMJ_INFO.getUptime());
        return sb.toString();
    }

    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ResponseBody
    public YamjInfo getYamjInfo(@RequestParam(required = false, defaultValue = "false") String addcounts) {
        // Clear the list of counts (in case it is out of date)
        YAMJ_INFO.getCounts().clear();

        if (BooleanUtils.toBoolean(addcounts)) {
            for (MetaDataType singleType : MetaDataType.values()) {
                if (singleType == MetaDataType.UNKNOWN) {
                    continue;
                }

                CountTimestamp result = jsonApi.getCountTimestamp(singleType);
                if (result == null) {
                    LOG.warn("There was an error getting the count for {}", singleType.toString());
                } else {
                    YAMJ_INFO.addCount(result.getType(), result.getCount());
                }
            }
        }
        return YAMJ_INFO;
    }

    @RequestMapping(value = "/config/{property}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<Configuration> getConfiguration(@PathVariable String property) {
        ApiWrapperSingle<Configuration> wrapper = new ApiWrapperSingle<Configuration>();
        if (StringUtils.isBlank(property)) {
            wrapper.setResult(null);
        } else {
            wrapper.setResult(configService.getConfiguration(property));
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/config", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<Configuration> getConfiguration() {
        ApiWrapperList<Configuration> wrapper = new ApiWrapperList<Configuration>();
        wrapper.setResults(configService.getConfiguration());
        wrapper.setTotalCount(wrapper.getCount());
        wrapper.setStatusCheck();
        return wrapper;
    }
}
