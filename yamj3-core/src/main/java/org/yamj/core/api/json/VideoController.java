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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.ListWrapper;
import org.yamj.core.api.ParameterType;
import org.yamj.core.api.Parameters;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@RequestMapping("/api/video/**")
public class VideoController {

    private static final Logger LOG = LoggerFactory.getLogger(VideoController.class);
    @Autowired
    private JsonApiStorageService jsonApiStorageService;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public VideoData getVideoById(@PathVariable String id) {
        LOG.info("Getting video with ID '{}'", id);
        return jsonApiStorageService.getEntityById(VideoData.class, Long.parseLong(id));
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public ListWrapper<VideoData> getVideoList(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "") String match,
            @RequestParam(required = false, defaultValue = "") String sort,
            @RequestParam(required = false, defaultValue = "name") String field,
            @RequestParam(required = false, defaultValue = "-1") Integer start,
            @RequestParam(required = false, defaultValue = "-1") Integer max) {

        Parameters p = new Parameters();
        p.add(ParameterType.SEARCH, search);
        p.add(ParameterType.MATCHMODE, match);
        p.add(ParameterType.SORT, sort);
        p.add(ParameterType.SORT_FIELD, field);
        p.add(ParameterType.START, start);
        p.add(ParameterType.MAX, max);

        LOG.info("Getting video list with {}", p.toString());
        ListWrapper<VideoData> wrapper = new ListWrapper<VideoData>();
        List<VideoData> results = jsonApiStorageService.getVideos(p);
        wrapper.setResults(results);
        wrapper.setParameters(p);
        return wrapper;
    }

    @RequestMapping(value = "/series/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Series getSeriesById(@PathVariable String id) {
        LOG.info("Getting series with ID '{}'", id);
        return jsonApiStorageService.getEntityById(Series.class, Long.parseLong(id));
    }

    @RequestMapping(value = "/season/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Season getSeasonById(@PathVariable String id) {
        LOG.info("Getting season with ID '{}'", id);
        return jsonApiStorageService.getEntityById(Season.class, Long.parseLong(id));
    }
}
