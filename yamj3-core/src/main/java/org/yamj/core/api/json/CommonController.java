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
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.Studio;
import org.yamj.core.database.service.JsonApiStorageService;

@Controller
@RequestMapping("/api/**")
public class CommonController {

    private static final Logger LOG = LoggerFactory.getLogger(CommonController.class);
    @Autowired
    private JsonApiStorageService jsonApiStorageService;

    @RequestMapping(value = "/genre/{name}", method = RequestMethod.GET)
    @ResponseBody
    public Genre getGenre(@PathVariable String name) {
        LOG.info("Getting genre '{}'", name);
        return jsonApiStorageService.getGenre(name);
    }

    @RequestMapping(value = "/genres", method = RequestMethod.GET)
    @ResponseBody
    public List<Genre> getGenres(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "") String place,
            @RequestParam(required = false, defaultValue = "") String sort,
            @RequestParam(required = false, defaultValue = "-1") Integer start,
            @RequestParam(required = false, defaultValue = "-1") Integer max) {

        LOG.info("Getting genre list Search: '{}', Place: '{}', Sort: '{}', Max: {}, Start: {}", search, place, sort, max, start);
        return jsonApiStorageService.getGenres(search, place, sort, start, max);
    }

    @RequestMapping(value = "/certification/{name}", method = RequestMethod.GET)
    @ResponseBody
    public Certification getCertification(@PathVariable String name) {
        LOG.info("Getting certification '{}'", name);
        return jsonApiStorageService.getCertification(name);
    }

    @RequestMapping(value = "/studio/{name}", method = RequestMethod.GET)
    @ResponseBody
    public Studio getStudio(@PathVariable String name) {
        LOG.info("Getting studio '{}'", name);
        return jsonApiStorageService.getStudio(name);
    }
}
