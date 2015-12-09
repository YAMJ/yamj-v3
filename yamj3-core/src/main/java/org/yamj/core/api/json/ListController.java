/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.api.json;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.yamj.core.api.model.dto.ApiListDTO;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.database.service.JsonApiStorageService;

@RestController
@RequestMapping(value = "/api/list", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
public class ListController {

    @Autowired
    private JsonApiStorageService jsonApiStorageService;

    @RequestMapping("/movies")
    public ApiWrapperList<ApiListDTO> getMovieList() {
        ApiWrapperList<ApiListDTO> wrapper = new ApiWrapperList<>();
        wrapper.setResults(jsonApiStorageService.getMovieList(wrapper));
        return wrapper;
    }

    @RequestMapping("/series")
    public ApiWrapperList<ApiListDTO> getSeriesList() {
        ApiWrapperList<ApiListDTO> wrapper = new ApiWrapperList<>();
        wrapper.setResults(jsonApiStorageService.getSeriesList(wrapper));
        return wrapper;
    }
}

