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
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.model.dto.ApiEpisodeDTO;
import org.yamj.core.api.model.dto.ApiSeriesInfoDTO;
import org.yamj.core.api.model.dto.ApiVideoDTO;
import org.yamj.core.api.options.OptionsEpisode;
import org.yamj.core.api.options.OptionsIndexVideo;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@RequestMapping("/api/video/**")
public class VideoController {

    private static final Logger LOG = LoggerFactory.getLogger(VideoController.class);
    @Autowired
    private JsonApiStorageService jsonApiStorageService;

    /**
     * Get information on a movie
     *
     * TODO: Allow genres to be added to the returned data
     *
     * @param id
     * @param options
     * @return
     */
    @RequestMapping(value = "/movie/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<ApiVideoDTO> getVideoById(@PathVariable String id,
            @ModelAttribute("options") OptionsIndexVideo options) {
        ApiWrapperSingle<ApiVideoDTO> wrapper = new ApiWrapperSingle<ApiVideoDTO>();
        // Add the ID to the options
        options.setId(NumberUtils.toLong(id));
        // Set the type to movie
        options.setType("MOVIE");
        wrapper.setOptions(options);

        if (options.getId() > 0L) {
            LOG.info("Getting video with ID '{}'", options.getId());
            jsonApiStorageService.getSingleVideo(wrapper);
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    /**
     * Get information on a series
     *
     * TODO: Get associate seasons for the series
     *
     * @param id
     * @param options
     * @return
     */
    @RequestMapping(value = "/series/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<ApiVideoDTO> getSeriesById(@PathVariable String id,
            @ModelAttribute("options") OptionsIndexVideo options) {
        ApiWrapperSingle<ApiVideoDTO> wrapper = new ApiWrapperSingle<ApiVideoDTO>();
        // Add the ID to the options
        options.setId(NumberUtils.toLong(id));
        // Set the type to movie
        options.setType("SERIES");
        wrapper.setOptions(options);

        if (options.getId() > 0L) {
            LOG.info("Getting series with ID '{}'", options.getId());
            jsonApiStorageService.getSingleVideo(wrapper);
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    /**
     * Get information on a series
     *
     * TODO: Add episodes to the season
     *
     * @param id
     * @param options
     * @return
     */
    @RequestMapping(value = "/season/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<ApiVideoDTO> getSeasonById(@PathVariable String id,
            @ModelAttribute("options") OptionsIndexVideo options) {
        ApiWrapperSingle<ApiVideoDTO> wrapper = new ApiWrapperSingle<ApiVideoDTO>();
        // Add the ID to the options
        options.setId(NumberUtils.toLong(id));
        // Set the type to movie
        options.setType("SEASON");
        wrapper.setOptions(options);

        if (options.getId() > 0L) {
            LOG.info("Getting season with ID '{}'", options.getId());
            jsonApiStorageService.getSingleVideo(wrapper);
        }
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/seriesinfo/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<ApiSeriesInfoDTO> getSeriesInfo(@PathVariable Long id) {
        ApiWrapperList<ApiSeriesInfoDTO> wrapper = new ApiWrapperList<ApiSeriesInfoDTO>();

        LOG.info("Getting season list for ID '{}'", id);
        List<ApiSeriesInfoDTO> results = jsonApiStorageService.getSeasonList(id);
        wrapper.setResults(results);

        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/episodes")
    @ResponseBody
    public ApiWrapperList<ApiEpisodeDTO> getEpisodes(@ModelAttribute("options") OptionsEpisode options) {
        LOG.info("Getting episodes for seriesId '{}', seasonId '{}', season '{}'",
                options.getSeriesid() < 0L ? "All" : options.getSeriesid(),
                options.getSeasonid() < 0L ? "All" : options.getSeasonid(),
                options.getSeason() < 0L ? "All" : options.getSeason());

        ApiWrapperList<ApiEpisodeDTO> wrapper = new ApiWrapperList<ApiEpisodeDTO>();
        wrapper.setOptions(options);
        jsonApiStorageService.getEpisodeList(wrapper);
        return wrapper;
    }
}
