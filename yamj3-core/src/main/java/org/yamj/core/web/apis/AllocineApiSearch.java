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
package org.yamj.core.web.apis;

import com.moviejukebox.allocine.AllocineApi;
import com.moviejukebox.allocine.AllocineException;
import com.moviejukebox.allocine.model.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.CachingNames;
import org.yamj.core.service.metadata.online.TemporaryUnavailableException;

@Service
public class AllocineApiSearch {

    private static final Logger LOG = LoggerFactory.getLogger(AllocineApiSearch.class);
    protected static final String API_ERROR = "Allocine error";

    @Autowired
    private AllocineApi allocineApi;

    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #name}", unless="#result==null")
    public Search searchMovies(String name, boolean throwTempError) {
        Search search = null;
        try {
            search = allocineApi.searchMovies(name);
        } catch (AllocineException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving Allocine id for movie '{}': {}", name, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return search;
    }
    
    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #name}", unless="#result==null")
    public Search searchTvSeries(String name, boolean throwTempError) {
        Search search = null;
        try {
            search = allocineApi.searchTvSeries(name);
        } catch (AllocineException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving Allocine id for series '{}': {}", name, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return search;
    }

    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #name}", unless="#result==null")
    public Search searchPersons(String name, boolean throwTempError) {
        Search search = null;
        try {
            search = allocineApi.searchPersons(name);
        } catch (AllocineException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving Allocine id for person '{}': {}", name, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return search;
    }

    protected static void checkTempError(boolean throwTempError, AllocineException ex) {
        if (throwTempError && ResponseTools.isTemporaryError(ex)) {
            throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
        }
    }
}   

