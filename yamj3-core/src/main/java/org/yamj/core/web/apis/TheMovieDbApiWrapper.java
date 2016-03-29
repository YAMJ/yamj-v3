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

import com.omertron.themoviedbapi.Compare;
import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.enumeration.SearchType;
import com.omertron.themoviedbapi.model.credits.CreditBasic;
import com.omertron.themoviedbapi.model.movie.MovieInfo;
import com.omertron.themoviedbapi.model.person.PersonCreditList;
import com.omertron.themoviedbapi.model.person.PersonFind;
import com.omertron.themoviedbapi.model.person.PersonInfo;
import com.omertron.themoviedbapi.model.tv.*;
import com.omertron.themoviedbapi.results.ResultList;
import com.omertron.themoviedbapi.tools.MethodSub;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.config.ConfigService;
import org.yamj.core.web.TemporaryUnavailableException;

@Service("tmdbApiWrapper")
public class TheMovieDbApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbApiWrapper.class);
    private static final String API_ERROR = "TheMovieDb error";
                    
    @Autowired
    private ConfigService configService;
    @Autowired
    private TheMovieDbApi tmdbApi;
    
    public String getMovieId(String title, int year, Locale locale, boolean throwTempError) { //NOSONAR
        boolean includeAdult = configService.getBooleanProperty("themoviedb.include.adult", false);
        int searchMatch = configService.getIntProperty("themoviedb.searchMatch", 3);
        
        MovieInfo movie = null;
        try {
            // Search using movie name
            ResultList<MovieInfo> movieList = tmdbApi.searchMovie(title, 0, locale.getLanguage(), includeAdult, year, 0, null);
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
                    movie = m;
                    break;
                }
            }
        } catch (MovieDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving TMDb id for movie '{}': {}", title, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }

        if (movie != null && movie.getId() != 0) {
            LOG.info("TMDB ID found {} for '{}'", movie.getId(), title);
            return String.valueOf(movie.getId());
        }
        return null;
    }

    public String getSeriesId(String title, int year, Locale locale, boolean throwTempError) { //NOSONAR
        String id = null;
        TVBasic closestTV = null;
        int closestMatch = Integer.MAX_VALUE;
        boolean foundTV = false;

        try {
            // Search using movie name
            ResultList<TVBasic> seriesList = tmdbApi.searchTV(title, 0, locale.getLanguage(), year, null);
            LOG.info("Found {} potential matches for {} ({})", seriesList.getResults().size(), title, year);
            // Iterate over the list until we find a match
            for (TVBasic tv : seriesList.getResults()) {
                if (title.equalsIgnoreCase(tv.getName())) {
                    id = String.valueOf(tv.getId());
                    foundTV = true;
                    break;
                }
                
                LOG.trace("{}: Checking against '{}'", title, tv.getName());
                int lhDistance = StringUtils.getLevenshteinDistance(title, tv.getName());
                LOG.trace("{}: Current closest match is {}, this match is {}", title, closestMatch, lhDistance);
                if (lhDistance < closestMatch) {
                    LOG.trace("{}: TMDB ID {} is a better match ", title, tv.getId());
                    closestMatch = lhDistance;
                    closestTV = tv;
                }
            }

            if (foundTV) {
                LOG.debug("{}: Matched against TMDB ID: {}", title, id);
            } else if (closestMatch < Integer.MAX_VALUE && closestTV != null) {
                id = String.valueOf(closestTV.getId());
                LOG.debug("{}: Closest match is '{}' differing by {} characters", title, closestTV.getName(), closestMatch);
            } else {
                LOG.debug("{}: No match found", title);
            }
        } catch (MovieDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving TMDb id for series '{}': {}", title, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        
        return id;
    }
    
    public String getPersonId(String name, boolean throwTempError) { //NOSONAR
        boolean includeAdult = configService.getBooleanProperty("themoviedb.includeAdult", false);

        String id = null;
        PersonFind closestPerson = null;
        int closestMatch = Integer.MAX_VALUE;
        boolean foundPerson = false;

        try {
            ResultList<PersonFind> results = tmdbApi.searchPeople(name, 0, includeAdult, SearchType.PHRASE);
            LOG.info("{}: Found {} results", name, results.getResults().size());
            for (PersonFind person : results.getResults()) {
                if (name.equalsIgnoreCase(person.getName())) {
                    id = String.valueOf(person.getId());
                    foundPerson = true;
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
            checkTempError(throwTempError, ex);
            LOG.error("Failed retrieving TMDb id for person '{}': {}", name, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        
        return id;
    }

    public PersonInfo getPersonInfo(int tmdbId, boolean throwTempError) {
        PersonInfo personInfo = null;
        try {
            personInfo = tmdbApi.getPersonInfo(tmdbId, MethodSub.COMBINED_CREDITS.getValue());
        } catch (MovieDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get person info using TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return personInfo;
    }

    public MovieInfo getMovieInfoByTMDB(int tmdbId, Locale locale, boolean throwTempError) {
        MovieInfo movieInfo = null;
        try {
            movieInfo = tmdbApi.getMovieInfo(tmdbId, locale.getLanguage(), MethodSub.RELEASES.getValue(), MethodSub.CREDITS.getValue());
        } catch (MovieDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get movie info using TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return movieInfo;
    }

    public TVInfo getSeriesInfo(int tmdbId, Locale locale, boolean throwTempError) {
        TVInfo tvInfo = null;
        try {
            tvInfo = tmdbApi.getTVInfo(tmdbId, locale.getLanguage(), MethodSub.EXTERNAL_IDS.getValue());
        } catch (MovieDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get series info using TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return tvInfo;
    }

    public TVSeasonInfo getSeasonInfo(String tmdbId, int season, Locale locale) {
        TVSeasonInfo tvSeasonInfo = null;
        if (StringUtils.isNumeric(tmdbId)) {
            try {
                tvSeasonInfo = tmdbApi.getSeasonInfo(Integer.parseInt(tmdbId), season, locale.getLanguage());
            } catch (MovieDbException ex) {
                LOG.error("Failed to get episodes using TMDb ID {} and season {}: {}", tmdbId, season, ex.getMessage());
                LOG.trace(API_ERROR, ex);
            }
        }
        return tvSeasonInfo;
    }

    public TVEpisodeInfo getEpisodeInfo(String tmdbId, int season, int episode, Locale locale) {
        TVEpisodeInfo tvEpisodeInfo = null;
        if (StringUtils.isNumeric(tmdbId)) {
            try {
                tvEpisodeInfo = tmdbApi.getEpisodeInfo(Integer.parseInt(tmdbId), season, episode, locale.getLanguage(), MethodSub.CREDITS.getValue(), MethodSub.EXTERNAL_IDS.getValue());
            } catch (MovieDbException ex) {
                LOG.error("Failed to get episodes using TMDb ID {} and season {}: {}", tmdbId, season, ex.getMessage());
                LOG.trace(API_ERROR, ex);
            }
        }
        return tvEpisodeInfo;
    }

    public MovieInfo getMovieInfoByIMDB(String imdbId, Locale locale, boolean throwTempError) {
        MovieInfo movieInfo = null;
        try {
            movieInfo = tmdbApi.getMovieInfoImdb(imdbId, locale.getLanguage(), MethodSub.RELEASES.getValue(), MethodSub.CREDITS.getValue());
        } catch (MovieDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get movie info using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return movieInfo;
    }

    public PersonCreditList<CreditBasic> getPersonCredits(int tmdbId, Locale locale, boolean throwTempError) {
        try {
            return tmdbApi.getPersonCombinedCredits(tmdbId, locale.getLanguage());
        } catch (MovieDbException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get filmography for TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }
    
    private static void checkTempError(boolean throwTempError, MovieDbException ex) {
        if (throwTempError && ResponseTools.isTemporaryError(ex)) {
            throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
        }
    }
}
