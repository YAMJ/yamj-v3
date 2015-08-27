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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.core.CachingNames;
import org.yamj.core.service.metadata.online.TemporaryUnavailableException;
import org.yamj.core.web.ResponseTools;

@Service
public class AllocineApiSearch {

    private static final Logger LOG = LoggerFactory.getLogger(AllocineApiSearch.class);

    private final Lock searchMoviesLock = new ReentrantLock(true);
    private final Lock searchSeriesLock = new ReentrantLock(true);
    private final Lock searchPersonLock = new ReentrantLock(true);
    
    @Autowired
    private AllocineApi allocineApi;

    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #name}")
    public Search searchMovies(String name, boolean throwTempError) {
        Search search = null;
        searchMoviesLock.lock();
        try {
            search = allocineApi.searchMovies(name);
        } catch (AllocineException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving Allocine id for movie '{}': {}", name, ex.getMessage());
            LOG.trace("Allocine error" , ex);
        } finally {
            searchMoviesLock.unlock();
        }
        return (search == null ? new Search() : search);
    }
    
    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #name}")
    public Search searchTvSeries(String name, boolean throwTempError) {
        Search search = null;
        searchSeriesLock.lock();
        try {
            search = allocineApi.searchTvSeries(name);
        } catch (AllocineException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving Allocine id for series '{}': {}", name, ex.getMessage());
            LOG.trace("Allocine error" , ex);
        } finally {
            searchSeriesLock.unlock();
        }
        return (search == null ? new Search() : search);
    }

    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #name}")
    public Search searchPersons(String name, boolean throwTempError) {
        Search search = null;
        searchPersonLock.lock();
        try {
            search = allocineApi.searchPersons(name);
        } catch (AllocineException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving Allocine id for person '{}': {}", name, ex.getMessage());
            LOG.trace("Allocine error" , ex);
        } finally {
            searchPersonLock.unlock();
        }
        return (search == null ? new Search() : search);
    }
}   
