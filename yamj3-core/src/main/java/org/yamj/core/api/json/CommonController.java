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
import org.apache.commons.lang3.StringUtils;
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
import org.yamj.core.database.model.BoxedSet;
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
    public ApiWrapperSingle<Genre> getGenre(@PathVariable String name) {
        Genre genre;
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting genre with ID '{}'", name);
            genre = jsonApiStorageService.getGenre(Long.parseLong(name));
        } else {
            LOG.info("Getting genre with name '{}'", name);
            genre = jsonApiStorageService.getGenre(name);
        }

        ApiWrapperSingle<Genre> wrapper = new ApiWrapperSingle<Genre>(genre);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/genres", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<Genre> getGenres(
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

        ApiWrapperList<Genre> wrapper = new ApiWrapperList<Genre>();
        List<Genre> results = jsonApiStorageService.getGenres(p);
        wrapper.setResults(results);
        wrapper.setParameters(p);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/certification/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<Certification> getCertification(@PathVariable String name) {
        Certification certification;
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting genre with ID '{}'", name);
            certification = jsonApiStorageService.getCertification(Long.parseLong(name));
        } else {
            LOG.info("Getting certification '{}'", name);
            certification = jsonApiStorageService.getCertification(name);
        }

        ApiWrapperSingle<Certification> wrapper = new ApiWrapperSingle<Certification>(certification);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/certifications", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<Certification> getCertifications(
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

        ApiWrapperList<Certification> wrapper = new ApiWrapperList<Certification>();
        List<Certification> results = jsonApiStorageService.getCertifications(p);
        wrapper.setResults(results);
        wrapper.setParameters(p);
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }

    @RequestMapping(value = "/studio/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<Studio> getStudio(@PathVariable String name) {
        Studio studio;
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting studio with ID '{}'", name);
            studio = jsonApiStorageService.getStudio(Long.parseLong(name));
        } else {
            LOG.info("Getting studio '{}'", name);
            studio = jsonApiStorageService.getStudio(name);
        }

        ApiWrapperSingle<Studio> wrapper = new ApiWrapperSingle<Studio>(studio);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/studios", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<Studio> getStudios(
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

        LOG.info("Getting studio list with {}", p.toString());

        ApiWrapperList<Studio> wrapper = new ApiWrapperList<Studio>();
        List<Studio> results = jsonApiStorageService.getStudios(p);
        wrapper.setResults(results);
        wrapper.setParameters(p);
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }

    @RequestMapping(value = "/boxedset/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<BoxedSet> getBoxSet(@PathVariable String name) {
        BoxedSet boxedSet;
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting boxset with ID '{}'", name);
            boxedSet = jsonApiStorageService.getBoxedSet(Long.parseLong(name));
        } else {
            LOG.info("Getting boxset '{}'", name);
            boxedSet = jsonApiStorageService.getBoxedSet(name);
        }

        ApiWrapperSingle<BoxedSet> wrapper = new ApiWrapperSingle<BoxedSet>(boxedSet);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/boxedsets", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<BoxedSet> getBoxSets(
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

        LOG.info("Getting boxset list with {}", p.toString());

        ApiWrapperList<BoxedSet> wrapper = new ApiWrapperList<BoxedSet>();
        List<BoxedSet> results = jsonApiStorageService.getBoxedSets(p);
        wrapper.setResults(results);
        wrapper.setParameters(p);
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }
}
