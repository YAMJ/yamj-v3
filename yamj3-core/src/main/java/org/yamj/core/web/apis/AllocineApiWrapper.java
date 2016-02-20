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

import static org.yamj.core.web.apis.AllocineApiSearch.API_ERROR;
import static org.yamj.core.web.apis.AllocineApiSearch.checkTempError;

import com.moviejukebox.allocine.AllocineApi;
import com.moviejukebox.allocine.AllocineException;
import com.moviejukebox.allocine.model.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.core.CachingNames;
@Service
public class AllocineApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(AllocineApiWrapper.class);

    @Autowired
    private AllocineApi allocineApi;
    @Autowired
    private AllocineApiSearch allocineApiSearch;

    public String getAllocineMovieId(String title, int year, boolean throwTempError) {
        Search search = allocineApiSearch.searchMovies(title, throwTempError);
        if (search == null || !search.isValid()) {
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
        Search search = allocineApiSearch.searchTvSeries(title, throwTempError);
        if (search == null || !search.isValid()) {
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
        Search search = allocineApiSearch.searchPersons(name, throwTempError);
        if (search == null || !search.isValid()) {
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

    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #allocineId}", unless="#result==null")
    public MovieInfos getMovieInfos(String allocineId, boolean throwTempError) {
        MovieInfos movieInfos = null;
        try {
            movieInfos = allocineApi.getMovieInfos(allocineId);
        } catch (AllocineException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving Allocine infos for movie id {}: {}", allocineId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return movieInfos;
    }

    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #allocineId}", unless="#result==null")
    public TvSeriesInfos getTvSeriesInfos(String allocineId, boolean throwTempError) {
        TvSeriesInfos tvSeriesInfos = null;
        try {
            tvSeriesInfos = allocineApi.getTvSeriesInfos(allocineId);
        } catch (AllocineException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving Allocine infos for series id {}: {}", allocineId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return tvSeriesInfos;
    }

    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #allocineId}", unless="#result==null")
    public TvSeasonInfos getTvSeasonInfos(String allocineId) {
        TvSeasonInfos tvSeasonInfos = null;
        try {
            tvSeasonInfos = allocineApi.getTvSeasonInfos(allocineId);
        } catch (AllocineException ex) {
            LOG.error("Failed retrieving Allocine infos for season id {}: {}", allocineId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return tvSeasonInfos;
    }

    public EpisodeInfos getEpisodeInfos(String allocineId) {
        EpisodeInfos episodeInfos = null;
        if (StringUtils.isNotBlank(allocineId)) {
            try {
                episodeInfos = allocineApi.getEpisodeInfos(allocineId);
            } catch (AllocineException ex) {
                LOG.error("Failed retrieving Allocine infos for episode id {}: {}", allocineId, ex.getMessage());
                LOG.trace(API_ERROR, ex);
            }
        }
        return episodeInfos;
    }

    @Cacheable(value=CachingNames.API_ALLOCINE, key="{#root.methodName, #allocineId}", unless="#result==null")
    public PersonInfos getPersonInfos(String allocineId, boolean throwTempError) {
        PersonInfos personInfos = null;
        try {
            personInfos = allocineApi.getPersonInfos(allocineId);
        } catch (AllocineException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving Allocine infos for person id {}: {}", allocineId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return personInfos;
    }

    public FilmographyInfos getFilmographyInfos(String allocineId, boolean throwTempError) {
        FilmographyInfos filmographyInfos = null;
        try {
            filmographyInfos = allocineApi.getPersonFilmography(allocineId);
        } catch (AllocineException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving Allocine filmography for person id {}: {}", allocineId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return filmographyInfos;
    }
}   
