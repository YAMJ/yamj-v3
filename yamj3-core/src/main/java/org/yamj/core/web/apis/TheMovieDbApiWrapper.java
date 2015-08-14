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
import com.omertron.themoviedbapi.model.media.MediaCreditList;
import com.omertron.themoviedbapi.model.movie.MovieInfo;
import com.omertron.themoviedbapi.model.person.PersonCreditList;
import com.omertron.themoviedbapi.model.person.PersonFind;
import com.omertron.themoviedbapi.model.person.PersonInfo;
import com.omertron.themoviedbapi.model.tv.TVBasic;
import com.omertron.themoviedbapi.model.tv.TVInfo;
import com.omertron.themoviedbapi.model.tv.TVSeasonInfo;
import com.omertron.themoviedbapi.results.ResultList;
import com.omertron.themoviedbapi.tools.MethodSub;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.core.CachingNames;
import org.yamj.core.config.ConfigService;
import org.yamj.core.service.metadata.online.TemporaryUnavailableException;
import org.yamj.core.web.ResponseTools;

@Service("tmdbApiWrapper")
public class TheMovieDbApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbApiWrapper.class);

    @Autowired
    private ConfigService configService;
    @Autowired
    private TheMovieDbApi tmdbApi;
    
    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #title, #year}")
    public String getMovieId(String title, int year, Locale locale, boolean throwTempError) {
        boolean includeAdult = configService.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);
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
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving TMDb id for movie '{}': {}", title, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }

        if (movie != null && movie.getId() != 0) {
            LOG.info("TMDB ID found {} for '{}'", movie.getId(), title);
            return String.valueOf(movie.getId());
        }
        return StringUtils.EMPTY;
    }

    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #title, #year}")
    public String getSeriesId(String title, int year, Locale locale, boolean throwTempError) {
        String id = null;
        TVBasic closestTV = null;
        int closestMatch = Integer.MAX_VALUE;
        boolean foundTV = Boolean.FALSE;

        try {
            // Search using movie name
            ResultList<TVBasic> seriesList = tmdbApi.searchTV(title, 0, locale.getLanguage(), year, null);
            LOG.info("Found {} potential matches for {} ({})", seriesList.getResults().size(), title, year);
            // Iterate over the list until we find a match
            for (TVBasic tv : seriesList.getResults()) {
                if (title.equalsIgnoreCase(tv.getName())) {
                    id = String.valueOf(tv.getId());
                    foundTV = Boolean.TRUE;
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
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed retrieving TMDb id for series '{}': {}", title, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        
        return (id == null ? StringUtils.EMPTY : id);
    }
    
    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #name}")
    public String getPersonId(String name, boolean throwTempError) {
        boolean includeAdult = configService.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);

        String id = null;
        PersonFind closestPerson = null;
        int closestMatch = Integer.MAX_VALUE;
        boolean foundPerson = Boolean.FALSE;

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
        
        return (id == null ? StringUtils.EMPTY : id);
    }

    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #tmdbId}", unless="#result==null")
    public PersonInfo getPersonInfo(int tmdbId, boolean throwTempError) {
        PersonInfo person = null;
        try {
            person = tmdbApi.getPersonInfo(tmdbId, "combined_credits");
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get person info using TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        
        return person;
    }

    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #tmdbId, #locale}", unless="#result==null")
    public MovieInfo getMovieInfoByTMDB(int tmdbId, Locale locale, boolean throwTempError) {
        MovieInfo movieDb = null;
        try {
            movieDb = tmdbApi.getMovieInfo(tmdbId, locale.getLanguage(), MethodSub.RELEASES.getValue(), MethodSub.CREDITS.getValue());
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get movie info using TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return movieDb;
    }

    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #tmdbId, #locale}", unless="#result==null")
    public TVInfo getSeriesInfo(int tmdbId, Locale locale, boolean throwTempError) {
        TVInfo tvInfo = null;
        try {
            tvInfo = tmdbApi.getTVInfo(tmdbId, locale.getLanguage());
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get series info using TMDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return tvInfo;
    }

    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #tmdbId, #season, #locale}", unless="#result==null")
    public TVSeasonInfo getSeasonInfo(int tmdbId, int season, Locale locale, boolean throwTempError) {
        TVSeasonInfo tvSeasonInfo = null;
        try {
            tvSeasonInfo = tmdbApi.getSeasonInfo(tmdbId, season, locale.getLanguage());
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get episodes using TMDb ID {} and season {}: {}", tmdbId, season, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return tvSeasonInfo;
    }

    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #tmdbId, #season}", unless="#result==null")
    public MediaCreditList getSeasonCredits(int tmdbId, int season, boolean throwTempError) {
        MediaCreditList mediaCreditList = null;
        try {
            mediaCreditList = tmdbApi.getSeasonCredits(tmdbId, season);
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get credits using TMDb ID {} and season {}: {}", tmdbId, season, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return mediaCreditList;
    }

    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #imdbId, #locale}", unless="#result==null")
    public MovieInfo getMovieInfoByIMDB(String imdbId, Locale locale, boolean throwTempError) {
        MovieInfo movieInfo = null;
        try {
            movieInfo = tmdbApi.getMovieInfoImdb(imdbId, locale.getLanguage(), MethodSub.RELEASES.getValue(), MethodSub.CREDITS.getValue());
        } catch (MovieDbException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("TheMovieDb service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get movie info using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace("TheMovieDb error", ex);
        }
        return movieInfo;
    }

    @Cacheable(value=CachingNames.API_TMDB, key="{#root.methodName, #tmdbId, #locale}", unless="#result==null")
    public PersonCreditList<CreditBasic> getPersonCredits(int tmdbId, Locale locale, boolean throwTempError) {
        try {
            return tmdbApi.getPersonCombinedCredits(tmdbId, locale.getLanguage());
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
