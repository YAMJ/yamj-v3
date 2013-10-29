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
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.api.model.dto.ApiGenreDTO;
import org.yamj.core.api.options.OptionsId;
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

    //<editor-fold defaultstate="collapsed" desc="Watched Methods">
    @RequestMapping(value = "/watched", method = RequestMethod.GET)
    @ResponseBody
    public ApiStatus markWatched(
            @RequestParam(required = true, defaultValue = "") String filename,
            @RequestParam(required = false, defaultValue = "-1") Integer amount) {

        int percentage;
        if (amount < 0) {
            percentage = 0;
        } else if (amount > 100) {
            percentage = 100;
        } else {
            percentage = amount;
        }

        LOG.info("Received watched command for '{}' to value '{}'", filename, percentage);
        // TODO: Add write to database command
        return new ApiStatus(200, "Watch command successful");
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre Methods">
    @RequestMapping(value = "/genre")
    @ResponseBody
    public ApiWrapperList<Genre> getGenreFilename(@RequestParam(required = true, defaultValue = "") String filename) {
        LOG.info("Getting genres for filename '{}'", filename);
        ApiWrapperList<Genre> wrapper = new ApiWrapperList<Genre>();
        List<Genre> genres = jsonApiStorageService.getGenreFilename(wrapper, filename);
        wrapper.setResults(genres);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/genre/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<Genre> getGenre(@PathVariable String name) {
        Genre genre;
        ApiWrapperSingle<Genre> wrapper = new ApiWrapperSingle<Genre>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting genre with ID '{}'", name);
            genre = jsonApiStorageService.getGenre(Long.parseLong(name));
        } else {
            LOG.info("Getting genre with name '{}'", name);
            genre = jsonApiStorageService.getGenre(name);
        }
        wrapper.setResult(genre);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/genres", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<ApiGenreDTO> getGenres(@ModelAttribute("options") OptionsId options) {
        LOG.info("Getting genre list with {}", options.toString());

        ApiWrapperList<ApiGenreDTO> wrapper = new ApiWrapperList<ApiGenreDTO>();
        wrapper.setOptions(options);
        List<ApiGenreDTO> results = jsonApiStorageService.getGenres(wrapper);
        wrapper.setResults(results);
        wrapper.setStatusCheck();
        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Certification Methods">
    @RequestMapping(value = "/certification/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<Certification> getCertification(@PathVariable String name) {
        Certification certification;
        ApiWrapperSingle<Certification> wrapper = new ApiWrapperSingle<Certification>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting genre with ID '{}'", name);
            certification = jsonApiStorageService.getCertification(Long.parseLong(name));
        } else {
            LOG.info("Getting certification '{}'", name);
            certification = jsonApiStorageService.getCertification(name);
        }
        wrapper.setResult(certification);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/certifications", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<Certification> getCertifications(@ModelAttribute("options") OptionsId options) {
        LOG.info("Getting certification list with {}", options.toString());

        ApiWrapperList<Certification> wrapper = new ApiWrapperList<Certification>();
        wrapper.setOptions(options);
        List<Certification> results = jsonApiStorageService.getCertifications(wrapper);
        wrapper.setResults(results);
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Studio Methods">
    @RequestMapping(value = "/studio/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<Studio> getStudio(@PathVariable String name) {
        Studio studio;
        ApiWrapperSingle<Studio> wrapper = new ApiWrapperSingle<Studio>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting studio with ID '{}'", name);
            studio = jsonApiStorageService.getStudio(Long.parseLong(name));
        } else {
            LOG.info("Getting studio '{}'", name);
            studio = jsonApiStorageService.getStudio(name);
        }
        wrapper.setResult(studio);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/studios", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<Studio> getStudios(@ModelAttribute("options") OptionsId options) {
        LOG.info("Getting studio list with {}", options.toString());

        ApiWrapperList<Studio> wrapper = new ApiWrapperList<Studio>();
        wrapper.setOptions(options);
        List<Studio> results = jsonApiStorageService.getStudios(wrapper);
        wrapper.setResults(results);
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Boxed-Set Methods">
    @RequestMapping(value = "/boxedset/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperSingle<BoxedSet> getBoxSet(@PathVariable String name) {
        BoxedSet boxedSet;
        ApiWrapperSingle<BoxedSet> wrapper = new ApiWrapperSingle<BoxedSet>();
        if (StringUtils.isNumeric(name)) {
            LOG.info("Getting boxset with ID '{}'", name);
            boxedSet = jsonApiStorageService.getBoxedSet(Long.parseLong(name));
        } else {
            LOG.info("Getting boxset '{}'", name);
            boxedSet = jsonApiStorageService.getBoxedSet(name);
        }
        wrapper.setResult(boxedSet);
        wrapper.setStatusCheck();
        return wrapper;
    }

    @RequestMapping(value = "/boxedsets", method = RequestMethod.GET)
    @ResponseBody
    public ApiWrapperList<BoxedSet> getBoxSets(@ModelAttribute("options") OptionsId options) {
        LOG.info("Getting boxset list with {}", options.toString());

        ApiWrapperList<BoxedSet> wrapper = new ApiWrapperList<BoxedSet>();
        wrapper.setOptions(options);
        List<BoxedSet> results = jsonApiStorageService.getBoxedSets(wrapper);
        wrapper.setResults(results);
        wrapper.setStatus(new ApiStatus(200, "OK"));

        return wrapper;
    }
    //</editor-fold>
}
