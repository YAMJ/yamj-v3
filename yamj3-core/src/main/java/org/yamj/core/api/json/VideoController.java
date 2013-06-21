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

import java.util.Collections;
import java.util.List;
import org.hibernate.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.model.ApiWrapperList;
import org.yamj.core.api.model.ApiWrapperSingle;
import org.yamj.core.api.model.ParameterType;
import org.yamj.core.api.model.Parameters;
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
    private static final String DEFAULT_FIELD = "title";    // Default field to be used in the parameters

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<VideoData> getVideoById(@PathVariable String id) {
        LOG.info("Getting video with ID '{}'", id);
        VideoData videoData = jsonApiStorageService.getEntityById(VideoData.class, Long.parseLong(id));
        ApiWrapperSingle<VideoData> wrapper = new ApiWrapperSingle<VideoData>(videoData);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<VideoData> getVideoList(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = DEFAULT_FIELD) String searchField,
            @RequestParam(required = false, defaultValue = "") String match,
            @RequestParam(required = false, defaultValue = "asc") String sort,
            @RequestParam(required = false, defaultValue = DEFAULT_FIELD) String sortField,
            @RequestParam(required = false, defaultValue = "-1") Integer start,
            @RequestParam(required = false, defaultValue = "-1") Integer max) {

        Parameters p = new Parameters();
        p.add(ParameterType.SEARCH, search);
        p.add(ParameterType.SEARCH_FIELD, searchField);
        p.add(ParameterType.MATCHMODE, match);
        p.add(ParameterType.SORT, sort);
        p.add(ParameterType.SORT_FIELD, sortField);
        p.add(ParameterType.START, start);
        p.add(ParameterType.MAX, max);

        LOG.info("Getting video list with {}", p.toString());
        ApiWrapperList<VideoData> wrapper = new ApiWrapperList<VideoData>();
        try {
            List<VideoData> results = jsonApiStorageService.getVideoList(p);
            wrapper.setResults(results);
            wrapper.setStatusCheck();
        } catch (QueryException ex) {
            List<VideoData> results = Collections.emptyList(); 
            wrapper.setResults(results);
            wrapper.setStatus(new ApiStatus(400, "Error with query"));
        }
        wrapper.setParameters(p);
        return wrapper;
    }

    @RequestMapping(value = "/series/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<Series> getSeriesById(@PathVariable String id) {
        LOG.info("Getting series with ID '{}'", id);
        Series series = jsonApiStorageService.getEntityById(Series.class, Long.parseLong(id));
        ApiWrapperSingle<Series> wrapper = new ApiWrapperSingle<Series>(series);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/series/list", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<Series> getSeriesList(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = DEFAULT_FIELD) String searchField,
            @RequestParam(required = false, defaultValue = "") String match,
            @RequestParam(required = false, defaultValue = "asc") String sort,
            @RequestParam(required = false, defaultValue = DEFAULT_FIELD) String sortField,
            @RequestParam(required = false, defaultValue = "-1") Integer start,
            @RequestParam(required = false, defaultValue = "-1") Integer max) {

        Parameters p = new Parameters();
        p.add(ParameterType.SEARCH, search);
        p.add(ParameterType.SEARCH_FIELD, searchField);
        p.add(ParameterType.MATCHMODE, match);
        p.add(ParameterType.SORT, sort);
        p.add(ParameterType.SORT_FIELD, sortField);
        p.add(ParameterType.START, start);
        p.add(ParameterType.MAX, max);

        LOG.info("Getting series list with {}", p.toString());
        ApiWrapperList<Series> wrapper = new ApiWrapperList<Series>();
        try {
            List<Series> results = jsonApiStorageService.getSeriesList(p);
            wrapper.setResults(results);
            wrapper.setStatusCheck();
        } catch (QueryException ex) {
            List<Series> results = Collections.emptyList(); 
            wrapper.setResults(results);
            wrapper.setStatus(new ApiStatus(400, "Error with query"));
        }
        wrapper.setParameters(p);
        return wrapper;
    }

    @RequestMapping(value = "/season/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<Season> getSeasonById(@PathVariable String id) {
        LOG.info("Getting season with ID '{}'", id);
        Season season = jsonApiStorageService.getEntityById(Season.class, Long.parseLong(id));
        ApiWrapperSingle<Season> wrapper = new ApiWrapperSingle<Season>(season);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/season/list", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<Season> getSeasonList(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = DEFAULT_FIELD) String searchField,
            @RequestParam(required = false, defaultValue = "") String match,
            @RequestParam(required = false, defaultValue = "asc") String sort,
            @RequestParam(required = false, defaultValue = DEFAULT_FIELD) String sortField,
            @RequestParam(required = false, defaultValue = "-1") Integer start,
            @RequestParam(required = false, defaultValue = "-1") Integer max) {

        Parameters p = new Parameters();
        p.add(ParameterType.SEARCH, search);
        p.add(ParameterType.SEARCH_FIELD, searchField);
        p.add(ParameterType.MATCHMODE, match);
        p.add(ParameterType.SORT, sort);
        p.add(ParameterType.SORT_FIELD, sortField);
        p.add(ParameterType.START, start);
        p.add(ParameterType.MAX, max);

        LOG.info("Getting series list with {}", p.toString());
        ApiWrapperList<Season> wrapper = new ApiWrapperList<Season>();
        try {
            List<Season> results = jsonApiStorageService.getSeasonList(p);
            wrapper.setResults(results);
            wrapper.setStatusCheck();
        } catch (QueryException ex) {
            List<Season> results = Collections.emptyList(); 
            wrapper.setResults(results);
            wrapper.setStatus(new ApiStatus(400, "Error with query"));
        }
        wrapper.setParameters(p);
        return wrapper;
    }
}
