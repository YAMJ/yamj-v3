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
package org.yamj.core.service.metadata.online;

import com.moviejukebox.allocine.AllocineApi;
import com.moviejukebox.allocine.AllocineException;
import com.moviejukebox.allocine.model.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.yamj.core.web.ResponseTools;

@Service
public class AllocineApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(AllocineApiWrapper.class);

    private final Lock searchMoviesLock = new ReentrantLock(true);
    private final Lock searchSeriesLock = new ReentrantLock(true);
    private final Lock searchPersonLock = new ReentrantLock(true);
    
    @Autowired
    private Cache allocineSearchCache;
    @Autowired
    private Cache allocineInfoCache;
    @Autowired
    private AllocineApi allocineApi;

    public String getAllocineMovieId(String title, int year, boolean throwTempError) {
        final String cacheKey = "movie###" + title;
        Search search = allocineSearchCache.get(cacheKey, Search.class);
        
        if (search == null) {
            searchMoviesLock.lock();
            try {
                search = allocineApi.searchMovies(title);
                allocineSearchCache.put(cacheKey, search);
            } catch (AllocineException ex) {
                if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                    throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
                }
                LOG.error("Failed retrieving Allocine id for movie '{}': {}", title, ex.getMessage());
                LOG.trace("Allocine error" , ex);
                return null;
            } finally {
                searchMoviesLock.unlock();
            }
        }
        
        if (!search.isValid()) {
            return null;
        }
        
        // if we have a valid year try to find the first movie that match
        if (search.getTotalResults() > 1 && year > 0) {
            for (Movie movie : search.getMovies()) {
                if (movie != null) {
                    int movieProductionYear = movie.getProductionYear();
                    if (movieProductionYear <= 0) {
                        continue;
                    }
                    if (movieProductionYear == year) {
                        return String.valueOf(movie.getCode());
                    }
                }
            }
        }
        
        // we don't find a movie or there only one result, return the first
        if (!search.getMovies().isEmpty()) {
            Movie movie = search.getMovies().get(0);
            if (movie != null) {
                return String.valueOf(movie.getCode());
            }
        }
        
        // no id found
        return null;
    }

    public String getAllocineSeriesId(String title, int year, boolean throwTempError) {
        final String cacheKey = "series###" + title;
        Search search = allocineSearchCache.get(cacheKey, Search.class);
        
        if (search == null) {
            searchSeriesLock.lock();
            try {
                search = allocineApi.searchTvSeries(title);
                allocineSearchCache.put(cacheKey, search);
            } catch (AllocineException ex) {
                if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                    throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
                }
                LOG.error("Failed retrieving Allocine id for series '{}': {}", title, ex.getMessage());
                LOG.trace("Allocine error" , ex);
                return null;
            } finally {
              searchSeriesLock.unlock();
            }
        }      

        if (!search.isValid()) {
            return null;
        }

        // if we have a valid year try to find the first series that match
        if (search.getTotalResults() > 1 && year > 0) {
            for (TvSeries serie : search.getTvSeries()) {
                if (serie != null) {
                    int serieStart = serie.getYearStart();
                    if (serieStart <= 0) {
                        continue;
                    }
                    int serieEnd = serie.getYearEnd();
                    if (serieEnd <= 0) {
                        serieEnd = serieStart;
                    }
                    if (year >= serieStart && year <= serieEnd) {
                        return String.valueOf(serie.getCode());
                    }
                }
            }
        }
        
        // we don't find a series or there only one result, return the first
        if (!search.getTvSeries().isEmpty()) {
            TvSeries serie = search.getTvSeries().get(0);
            if (serie != null) {
                return String.valueOf(serie.getCode());
            }
        }
        
        // no id found
        return null;
    }

    public String getAllocinePersonId(String name, boolean throwTempError) {
        final String cacheKey = "person###" + name;
        Search search = allocineSearchCache.get(cacheKey, Search.class);
        
        if (search == null) {
            searchPersonLock.lock();
            try {
                search = allocineApi.searchPersons(name);
                allocineSearchCache.put(cacheKey, search);
            } catch (AllocineException ex) {
                if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                    throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
                }
                LOG.error("Failed retrieving Allocine id for person '{}': {}", name, ex.getMessage());
                LOG.trace("Allocine error" , ex);
                return null;
            } finally {
                searchPersonLock.unlock();
            }
        }          
        
        if (!search.isValid()) {
            return null;
        }
        
        // find for matching person
        if (search.getTotalResults() > 1) {
            for (ShortPerson person : search.getPersons()) {
                if (person != null) {
                    // find exact name (ignoring case)
                    if (StringUtils.equalsIgnoreCase(name, person.getName())) {
                        return String.valueOf(person.getCode());
                    }
                }
            }
        }
        
        // we don't find a person or there only one result, return the first
        if (!search.getPersons().isEmpty()) {
            ShortPerson person = search.getPersons().get(0);
            if (person != null) {
                return String.valueOf(person.getCode());
            }
        }
        
        // no id found
        return null;
    }

    public MovieInfos getMovieInfos(String allocineId, boolean throwTempError) {
        MovieInfos movieInfos = allocineInfoCache.get(allocineId, MovieInfos.class);
        if (movieInfos == null) {
            try {
                movieInfos = allocineApi.getMovieInfos(allocineId);
                if (movieInfos != null && movieInfos.isValid()) {
                    // add to the cache
                    allocineInfoCache.put(allocineId, movieInfos);
                }
            } catch (AllocineException ex) {
                if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                    throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
                }
                LOG.error("Failed retrieving Allocine infos for movie id {}: {}", allocineId, ex.getMessage());
                LOG.trace("Allocine error" , ex);
            }
        }
        
        return movieInfos;
    }

    public TvSeriesInfos getTvSeriesInfos(String allocineId, boolean throwTempError) {
        TvSeriesInfos tvSeriesInfos = allocineInfoCache.get(allocineId, TvSeriesInfos.class);
        if (tvSeriesInfos == null) {
            try {
                tvSeriesInfos = allocineApi.getTvSeriesInfos(allocineId);
                if (tvSeriesInfos != null && tvSeriesInfos.isValid()) {
                    // add to the cache
                    allocineInfoCache.put(allocineId, tvSeriesInfos);
                }
            } catch (AllocineException ex) {
                if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                    throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
                }
                LOG.error("Failed retrieving Allocine infos for series id {}: {}", allocineId, ex.getMessage());
                LOG.trace("Allocine error" , ex);
            }
        }
        return tvSeriesInfos;
    }
     
    public TvSeasonInfos getTvSeasonInfos(TvSeriesInfos tvSeriesInfos, int season, boolean throwTempError) {
        if (season  > tvSeriesInfos.getSeasonCount()) {
            // invalid season
            return null;
        }
        
        int seasonCode = tvSeriesInfos.getSeasonCode(season);
        if (seasonCode <= 0) return null;
        return getTvSeasonInfos(String.valueOf(seasonCode), throwTempError); 
    }

    public TvSeasonInfos getTvSeasonInfos(String allocineId, boolean throwTempError) {
        TvSeasonInfos tvSeasonInfos = null;
        try {
            tvSeasonInfos = allocineApi.getTvSeasonInfos(allocineId);
        } catch (AllocineException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving Allocine infos for season id {}: {}", allocineId, ex.getMessage());
            LOG.trace("Allocine error" , ex);
        }
        return tvSeasonInfos;
    }

    public PersonInfos getPersonInfos(String allocineId, boolean throwTempError) {
        PersonInfos personInfos = null;
        try {
            personInfos = allocineApi.getPersonInfos(allocineId);
        } catch (AllocineException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving Allocine infos for person id {}: {}", allocineId, ex.getMessage());
            LOG.trace("Allocine error" , ex);
        }
        return personInfos;
    }

    public FilmographyInfos getFilmographyInfos(String allocineId, boolean throwTempError) {
        FilmographyInfos filmographyInfos = null;
        try {
            filmographyInfos = allocineApi.getPersonFilmography(allocineId);
        } catch (AllocineException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving Allocine filmography for person id {}: {}", allocineId, ex.getMessage());
            LOG.trace("Allocine error" , ex);
        }
        return filmographyInfos;
    }

    public EpisodeInfos getEpisodeInfos(String allocineId, boolean throwTempError) {
        EpisodeInfos episodeInfos = null;
        try {
            episodeInfos = allocineApi.getEpisodeInfos(allocineId);
        } catch (AllocineException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
              throw new TemporaryUnavailableException("Allocine service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving Allocine infos for episode id {}: {}", allocineId, ex.getMessage());
            LOG.trace("Allocine error" , ex);
        }
        return episodeInfos;
    }
}   
