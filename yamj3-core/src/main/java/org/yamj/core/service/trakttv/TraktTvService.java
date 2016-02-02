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
package org.yamj.core.service.trakttv;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.api.trakttv.TraktTvApi;
import org.yamj.api.trakttv.TraktTvException;
import org.yamj.api.trakttv.auth.TokenResponse;
import org.yamj.api.trakttv.model.*;
import org.yamj.api.trakttv.model.enumeration.*;
import org.yamj.core.config.ConfigService;
import org.yamj.core.service.metadata.online.TemporaryUnavailableException;

@Service("traktTvService")
public class TraktTvService {

    private static final Logger LOG = LoggerFactory.getLogger(TraktTvService.class);
    private static final String TRAKTTV_AUTH_TOKEN = "trakttv.auth.access";
    private static final String TRAKTTV_REFRESH_TOKEN = "trakttv.auth.refresh";
    private static final String TRAKTTV_EXPIRATION = "trakttv.auth.expiration";
    private static final String API_ERROR = "Trakt.TV error";
    
    @Autowired
    private ConfigService configService;
    @Autowired
    private TraktTvApi traktTvApi;

    @Value("${trakttv.push.enabled:false}")
    private boolean pushEnabled;
    @Value("${trakttv.pull.enabled:false}")
    private boolean pullEnabled;
    
    // AUTHORIZATION
    
    public TraktTvInfo getTraktTvInfo() {
        TraktTvInfo traktTvInfo = new TraktTvInfo();
        // static values
        traktTvInfo.setPush(pushEnabled);
        traktTvInfo.setPull(pullEnabled);
        traktTvInfo.setSynchronization(pushEnabled || pullEnabled);
        // dynamic values
        traktTvInfo.setAuthorized(configService.getProperty(TRAKTTV_AUTH_TOKEN)!=null);
        long expiresAt = configService.getLongProperty(TRAKTTV_EXPIRATION, -1);
        if (expiresAt > 0) {
            traktTvInfo.setExpirationDate(new Date(expiresAt));
        }
        return traktTvInfo;
    }
    
    public String authorizeWithPin(String pin) {
        try {
            TokenResponse response = this.traktTvApi.requestAccessTokenByPin(pin);
            // set access token for API
            traktTvApi.setAccessToken(response.getAccessToken());

            // expiration date: creation date + expiration period * 1000 (cause given in seconds)
            long expireDate = (response.getCreatedAt() + response.getExpiresIn()) * 1000L;
            
            // store values in configuration settings
            configService.setProperty(TRAKTTV_EXPIRATION, expireDate);
            configService.setProperty(TRAKTTV_REFRESH_TOKEN, response.getRefreshToken());
            configService.setProperty(TRAKTTV_AUTH_TOKEN, response.getAccessToken());
            
            // no authorization error
            return null;
        } catch (TraktTvException e) {
            LOG.debug(API_ERROR, e);
            return e.getResponse();
        } catch (Exception e) {
            LOG.debug("Unknown error", e);
            return "Unknow error occured";
        }
    }

    // SEARCH BY ID
    
    public Integer searchMovieIdByIMDB(final String imdbId) {
        if (StringUtils.isBlank(imdbId)) {
            return null;
        }
        
        try {
            final List<SearchResult> searchResults = traktTvApi.searchService().idSearch(IdType.IMDB, imdbId);
            return getMovieId(searchResults);
        } catch (TraktTvException ex) {
            LOG.error("Failed to get movie ID for IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    public Integer searchMovieIdByTMDB(final String tmdbId) {
        if (StringUtils.isBlank(tmdbId)) {
            return null;
        }
        
        try {
            final List<SearchResult> searchResults = traktTvApi.searchService().idSearch(IdType.TMDB, tmdbId);
            return getMovieId(searchResults);
        } catch (TraktTvException ex) {
            LOG.error("Failed to get movie ID for TheMovieDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    public Integer searchMovieByTitleAndYear(final String title, final int year) {
        if (StringUtils.isBlank(title) || year <= 0) {
            return null;
        }
        
        try {
            final List<SearchResult> searchResults = traktTvApi.searchService().textSearch(title, SearchType.MOVIE, year, 1, 5);
            return getMovieId(searchResults);
        } catch (TraktTvException ex) {
            LOG.error("Failed to get movie ID for title '{}' and year '{}': {}", title, year, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }
    
    private static Integer getMovieId(List<SearchResult> searchResults) {
        if (CollectionUtils.isEmpty(searchResults)) {
            return null;
        }
        for (SearchResult searchResult : searchResults) {
            if (searchResult.getMovie() != null) {
                return searchResult.getMovie().getIds().trakt();
            }
        }
        return null;
    }

    public Integer searchShowIdByIMDB(String imdbId) {
        if (StringUtils.isBlank(imdbId)) {
            return null;
        }
        
        try {
            final List<SearchResult> searchResults = traktTvApi.searchService().idSearch(IdType.IMDB, imdbId);
            return getShowId(searchResults);
        } catch (TraktTvException ex) {
            LOG.error("Failed to get show ID for IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    public Integer searchShowIdByTMDB(String tmdbId) {
        if (StringUtils.isBlank(tmdbId)) {
            return null;
        }
        
        try {
            final List<SearchResult> searchResults = traktTvApi.searchService().idSearch(IdType.TMDB, tmdbId);
            return getShowId(searchResults);
        } catch (TraktTvException ex) {
            LOG.error("Failed to get show ID for TheMovieDb ID {}: {}", tmdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    public Integer searchShowIdByTVDB(String tvdbId) {
        if (StringUtils.isBlank(tvdbId)) {
            return null;
        }
        
        try {
            final List<SearchResult> searchResults = traktTvApi.searchService().idSearch(IdType.TVDB, tvdbId);
            return getShowId(searchResults);
        } catch (TraktTvException ex) {
            LOG.error("Failed to get show ID for TheTVDb ID {}: {}", tvdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    public Integer searchShowIdByTVRage(String tvRageId) {
        if (StringUtils.isBlank(tvRageId)) {
            return null;
        }
        
        try {
            final List<SearchResult> searchResults = traktTvApi.searchService().idSearch(IdType.TVRAGE, tvRageId);
            return getShowId(searchResults);
        } catch (TraktTvException ex) {
            LOG.error("Failed to get show ID for TVRage ID {}: {}", tvRageId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    public Integer searchShowByTitleAndYear(final String title, final int year) {
        if (StringUtils.isBlank(title) || year <= 0) {
            return null;
        }
        
        try {
            final List<SearchResult> searchResults = traktTvApi.searchService().textSearch(title, SearchType.SHOW, year, 1, 5);
            return getShowId(searchResults);
        } catch (TraktTvException ex) {
            LOG.error("Failed to get movie ID for title '{}' and year '{}': {}", title, year, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    private static Integer getShowId(List<SearchResult> searchResults) {
        if (CollectionUtils.isEmpty(searchResults)) {
            return null;
        }
        for (SearchResult searchResult : searchResults) {
            if (searchResult.getShow() != null) {
                return searchResult.getShow().getIds().trakt();
            }
        }
        return null;
    }

    // SCANNING

    public TraktTvMovie getMovie(String traktTvId, Locale locale, boolean throwTempError) {
        try {
            Movie movie = traktTvApi.movieService().getMovie(traktTvId, Extended.FULL);
            if (movie == null) {
                // no movie found
                return null;
            }

            TraktTvMovie traktTvMovie = new TraktTvMovie();
            traktTvMovie.setIds(movie.getIds());
            traktTvMovie.setTitle(movie.getTitle());
            traktTvMovie.setOverview(movie.getOverview());
            traktTvMovie.setTagline(movie.getTagline());
            traktTvMovie.setYear(movie.getYear());
            traktTvMovie.setRating(movie.getRating());
            traktTvMovie.setCertification(movie.getCertification());
            traktTvMovie.setReleaseDate(movie.getReleaseDate().toDate());
            traktTvMovie.setGenres(movie.getGenres());

            // get translation of movie
            if (movie.getAvailableTranslations().contains(locale.getLanguage())) {
                List<Translation> translations = traktTvApi.movieService().getTranslation(traktTvId, locale.getLanguage());
                if (CollectionUtils.isNotEmpty(translations)) {
                    final Translation translation = translations.get(0);
                    traktTvMovie.setTitle(translation.getTitle());
                    traktTvMovie.setOverview(translation.getOverview());
                    traktTvMovie.setTagline(translation.getTagline());
                }
            }

            // get releases
            List<Release> releases = traktTvApi.movieService().getReleases(traktTvId, locale.getCountry());
            for (Release release : releases) {
                if (release.getReleaseType() == ReleaseType.THEATRALIC) {
                    traktTvMovie.setReleaseCountry(StringUtils.upperCase(release.getCountry()));
                    if (release.getReleaseDate() != null) {
                        traktTvMovie.setReleaseDate(release.getReleaseDate().toDate());
                    }
                    traktTvMovie.setCertification(release.getCertification());
                }
            }
            
            // TODO get credits
            
            return traktTvMovie;
            
        } catch (TraktTvException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get movie for Trakt.TV ID {}: {}", traktTvId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    private static void checkTempError(boolean throwTempError, TraktTvException ex) {
        if (throwTempError && ResponseTools.isTemporaryError(ex.getResponseCode())) {
            throw new TemporaryUnavailableException("Trakt.TV service temporary not available: " + ex.getResponseCode(), ex);
        }
    }

   // SYNCHRONIZATION
}
