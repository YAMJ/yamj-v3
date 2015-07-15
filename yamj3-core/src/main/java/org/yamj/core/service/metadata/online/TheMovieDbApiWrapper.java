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

import com.omertron.themoviedbapi.Compare;
import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.enumeration.SearchType;
import com.omertron.themoviedbapi.model.media.MediaCreditList;
import com.omertron.themoviedbapi.model.movie.MovieInfo;
import com.omertron.themoviedbapi.model.person.PersonCreditList;
import com.omertron.themoviedbapi.model.person.PersonFind;
import com.omertron.themoviedbapi.model.person.PersonInfo;
import com.omertron.themoviedbapi.results.ResultList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigService;
import org.yamj.core.web.ResponseTools;

@Service("tmdbApiWrapper")
public class TheMovieDbApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbApiWrapper.class);

    @Autowired
    private ConfigService configService;
    @Autowired
    private TheMovieDbApi tmdbApi;

    public String getMovieDbId(String title, int year, boolean throwTempError) {
        String defaultLanguage = configService.getProperty("themoviedb.language", "en");
        boolean includeAdult = configService.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);
        int searchMatch = configService.getIntProperty("themoviedb.searchMatch", 3);
        MovieInfo movieDb = null;

        try {
            // Search using movie name
            ResultList<MovieInfo> movieList = tmdbApi.searchMovie(title, 0, defaultLanguage, includeAdult, year, 0, null);
            LOG.info("Found {} potential matches for {} ({})", movieList.getResults().size(), title, year);
            // Iterate over the list until we find a match
            for (MovieInfo m : movieList.getResults()) {
                String relDate;
                if (StringUtils.isNotBlank(m.getReleaseDate()) && m.getReleaseDate().length() > 4) {
                    relDate = m.getReleaseDate().substring(0, 4);
                } else {
                    relDate = "";
                }
                LOG.debug("Checking {} ({})", m.getTitle(), relDate);
                if (Compare.movies(m, title, String.valueOf(year), searchMatch)) {
                    movieDb = m;
                    break;
                }
            }
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving TMDb id for movie '{}': {}", title, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }

        if (movieDb != null && movieDb.getId() != 0) {
            LOG.info("TMDB ID found {} for '{}'", movieDb.getId(), title);
            return String.valueOf(movieDb.getId());
        }
        return null;
    }

    public String getPersonId(String name, boolean throwTempError) {
        String id = null;
        PersonFind closestPerson = null;
        int closestMatch = Integer.MAX_VALUE;
        boolean foundPerson = Boolean.FALSE;
        boolean includeAdult = configService.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);

        try {
            ResultList<PersonFind> results = tmdbApi.searchPeople(name, 0, includeAdult, SearchType.PHRASE);
            LOG.info("{}: Found {} results", name, results.getResults().size());
            for (PersonFind person : results.getResults()) {
                if (name.equalsIgnoreCase(person.getName())) {
                    id = String.valueOf(person.getId());
                    foundPerson = Boolean.TRUE;
                    break;
                }
                LOG.trace("{}: Checking against '{}'", name, person.getName());
                int lhDistance = StringUtils.getLevenshteinDistance(name, person.getName());
                LOG.trace("{}: Current closest match is {}, this match is {}", name, closestMatch, lhDistance);
                if (lhDistance < closestMatch) {
                    LOG.trace("{}: TMDB ID {} is a better match ", name, person.getId());
                    closestMatch = lhDistance;
                    closestPerson = person;
                }
            }

            if (foundPerson) {
                LOG.debug("{}: Matched against TMDB ID: {}", name, id);
            } else if (closestMatch < Integer.MAX_VALUE && closestPerson != null) {
                id = String.valueOf(closestPerson.getId());
                LOG.debug("{}: Closest match is '{}' differing by {} characters", name, closestPerson.getName(), closestMatch);
            } else {
                LOG.debug("{}: No match found", name);
            }
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving TMDb id for person '{}': {}", name, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return id;
    }

    public PersonInfo getPersonInfo(int tmdbId, boolean throwTempError) {
        PersonInfo person = null;
        try {
            person = tmdbApi.getPersonInfo(tmdbId);
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get person info using TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return person;
    }

    public MovieInfo getMovieInfoByTMDB(int tmdbId, boolean throwTempError) {
        MovieInfo movieDb = null;
        try {
            String defaultLanguage = configService.getProperty("themoviedb.language", "en");
            movieDb = tmdbApi.getMovieInfo(tmdbId, defaultLanguage);
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get movie info using TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return movieDb;
    }

    public MovieInfo getMovieInfoByIMDB(String imdbId, boolean throwTempError) {
        MovieInfo movieDb = null;
        try {
            String defaultLanguage = configService.getProperty("themoviedb.language", "en");
            movieDb = tmdbApi.getMovieInfoImdb(imdbId, defaultLanguage);
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get movie info using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return movieDb;
    }

    public MediaCreditList getMovieCredits(int tmdbId, boolean throwTempError) {
        try {
            return tmdbApi.getMovieCredits(tmdbId);
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get movie cast for TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return null;
    }

    public PersonCreditList getPersonCredits(int tmdbId, boolean throwTempError) {
        try {
            String defaultLanguage = configService.getProperty("themoviedb.language", "en");
            return tmdbApi.getPersonCombinedCredits(tmdbId, defaultLanguage);
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get filmography for TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return null;
    }
}
