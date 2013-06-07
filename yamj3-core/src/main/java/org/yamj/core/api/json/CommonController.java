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
    public ListWrapper<Genre> getGenres(
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

        LOG.info("Getting genre list with {}", p.toString());

        ListWrapper<Genre> wrapper = new ListWrapper<Genre>();
        List<Genre> results = jsonApiStorageService.getGenres(p);
        wrapper.setResults(results);
        wrapper.setParameters(p);
        return wrapper;
    }

    @RequestMapping(value = "/certification/{name}", method = RequestMethod.GET)
    @ResponseBody
    public Certification getCertification(@PathVariable String name) {
        LOG.info("Getting certification '{}'", name);
        return jsonApiStorageService.getCertification(name);
    }

    @RequestMapping(value = "/certifications", method = RequestMethod.GET)
    @ResponseBody
    public ListWrapper<Certification> getCertifications(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "") String match,
            @RequestParam(required = false, defaultValue = "") String sort,
            @RequestParam(required = false, defaultValue = "certification_text") String field,
            @RequestParam(required = false, defaultValue = "-1") Integer start,
            @RequestParam(required = false, defaultValue = "-1") Integer max) {

        Parameters p = new Parameters();
        p.add(ParameterType.SEARCH, search);
        p.add(ParameterType.MATCHMODE, match);
        p.add(ParameterType.SORT, sort);
        p.add(ParameterType.SORT_FIELD, field);
        p.add(ParameterType.START, start);
        p.add(ParameterType.MAX, max);

        LOG.info("Getting certification list with {}", p.toString());

        ListWrapper<Certification> wrapper = new ListWrapper<Certification>();
        List<Certification> results = jsonApiStorageService.getCertifications(p);
        wrapper.setResults(results);
        wrapper.setParameters(p);
        return wrapper;
    }

    @RequestMapping(value = "/studio/{name}", method = RequestMethod.GET)
    @ResponseBody
    public Studio getStudio(@PathVariable String name) {
        LOG.info("Getting studio '{}'", name);
        return jsonApiStorageService.getStudio(name);
    }
}
