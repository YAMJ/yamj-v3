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
package org.yamj.core.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.database.dao.MetadataDao;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;

@Controller
@RequestMapping("/api/video/**")
public class VideoController {

    private static final Logger LOG = LoggerFactory.getLogger(VideoController.class);
    @Autowired
    private MetadataDao metadataDao;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public VideoData getVideoById(@PathVariable String id) {
        LOG.info("Getting video with ID '{}'", id);
        return metadataDao.getVideoData(Long.parseLong(id));
    }

    @RequestMapping(value = "/series/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Series getSeriesById(@PathVariable String id) {
        LOG.info("Getting series with ID '{}'", id);
        return metadataDao.getSeries(Long.parseLong(id));
    }

    @RequestMapping(value = "/season/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Season getSeasonById(@PathVariable String id) {
        LOG.info("Getting season with ID '{}'", id);
        return metadataDao.getSeason(Long.parseLong(id));
    }
}
