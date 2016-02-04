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

import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
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
import org.yamj.core.database.model.dto.TraktMovieDTO;
import org.yamj.core.database.service.TraktTvStorageService;
import org.yamj.core.service.metadata.online.*;

@Service("traktTvService")
public class TraktTvService {

    private static final Logger LOG = LoggerFactory.getLogger(TraktTvService.class);
    private static final String TRAKTTV_ACCESS_TOKEN = "trakttv.auth.access";
    private static final String TRAKTTV_REFRESH_TOKEN = "trakttv.auth.refresh";
    private static final String TRAKTTV_EXPIRATION = "trakttv.auth.expiration";
    private static final String TRAKTTV_LAST_PULL_MOVIES = "trakttv.last.pull.movies";
    private static final String TRAKTTV_LAST_PULL_EPISODES = "trakttv.last.pull.episodes";
    private static final String TRAKTTV_LAST_PUSH_MOVIES = "trakttv.last.push.movies";
    private static final String TRAKTTV_LAST_PUSH_EPISODES = "trakttv.last.push.episodes";
    private static final String TRAKTTV_ERROR = "Trakt.TV error";
    
    @Autowired
    private ConfigService configService;
    @Autowired
    private TraktTvApi traktTvApi;
    @Autowired
    private TraktTvStorageService traktTvStorageService;

    @Value("${trakttv.collection.enabled:false}")
    private boolean collectionEnabled;
    @Value("${trakttv.push.enabled:false}")
    private boolean pushEnabled;
    @Value("${trakttv.pull.enabled:false}")
    private boolean pullEnabled;
    
    // AUTHORIZATION
    
    @PostConstruct
    public void init() {
        if (isExpired()) {
            // TODO request new access token by refresh token (if present)
        } else {
            traktTvApi.setAccessToken(configService.getProperty(TRAKTTV_ACCESS_TOKEN));
        }
    }

    public boolean isSynchronizationEnabled() {
        return (collectionEnabled || pushEnabled || pullEnabled);
    }
    
    public boolean isExpired() {
        // check expiration date
        final Date expirationDate = configService.getDateProperty(TRAKTTV_EXPIRATION);
        if (expirationDate == null || expirationDate.getTime() < System.currentTimeMillis()) {
            LOG.warn("Trakt.TV synchronization not possible cause expired");
            return true;
        }
        return false;
    }

    public TraktTvInfo getTraktTvInfo() {
        TraktTvInfo traktTvInfo = new TraktTvInfo();
        traktTvInfo.setSynchronization(isSynchronizationEnabled());
        traktTvInfo.setPush(pushEnabled);
        traktTvInfo.setPull(pullEnabled);
        traktTvInfo.setAuthorized(configService.getProperty(TRAKTTV_ACCESS_TOKEN)!=null);
        traktTvInfo.setExpirationDate(configService.getDateProperty(TRAKTTV_EXPIRATION));
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
            configService.setProperty(TRAKTTV_ACCESS_TOKEN, response.getAccessToken());
            
            // no authorization error
            return null;
        } catch (TraktTvException e) {
            LOG.debug(TRAKTTV_ERROR, e);
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
            LOG.trace(TRAKTTV_ERROR, ex);
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
            LOG.trace(TRAKTTV_ERROR, ex);
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
            LOG.trace(TRAKTTV_ERROR, ex);
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
            LOG.trace(TRAKTTV_ERROR, ex);
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
            LOG.trace(TRAKTTV_ERROR, ex);
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
            LOG.trace(TRAKTTV_ERROR, ex);
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
            LOG.trace(TRAKTTV_ERROR, ex);
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
            LOG.trace(TRAKTTV_ERROR, ex);
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
            LOG.trace(TRAKTTV_ERROR, ex);
        }
        return null;
    }

    private static void checkTempError(boolean throwTempError, TraktTvException ex) {
        if (throwTempError && ResponseTools.isTemporaryError(ex.getResponseCode())) {
            throw new TemporaryUnavailableException("Trakt.TV service temporary not available: " + ex.getResponseCode(), ex);
        }
    }

    // SYNCHRONIZATION

    public void pullWatchedMovies() {
        List<TrackedMovie> trackedMovies;
        try {
            trackedMovies = traktTvApi.syncService().getWatchedMovies(Extended.MINIMAL);
        } catch (Exception e) {
            LOG.error("Failed to get tracked movies", e);
            return;
        }
        LOG.info("Found {} watched movies on Trakt.TV", trackedMovies.size());
        
        // store last pull date for later use
        final Date lastPull = new Date();
        
        // get the updated movie IDs for setting watched status
        Date checkDate = this.configService.getDateProperty(TRAKTTV_LAST_PULL_MOVIES);
        if (checkDate == null) {
            // build a date long, long ago ...
            checkDate = DateTime.now().minusYears(20).toDate();
        }
        Map<String,List<Long>> updatedMovies = this.traktTvStorageService.getUpdatedMovieIds(checkDate);
        
        // nothing to do if no new or updated movies found
        if (updatedMovies.isEmpty()) {
            this.configService.setProperty(TRAKTTV_LAST_PULL_MOVIES, lastPull);
            return;
        }
        
        boolean noError = true;
        for (TrackedMovie movie : trackedMovies) {
            final Set<Long> updateable = getUpdateableMovies(movie.getMovie().getIds(), updatedMovies);
            if (updateable.size() > 0) {
                try {
                    this.traktTvStorageService.updateWatched(movie, updateable);
                } catch (Exception ex) {
                    LOG.error("Failed to updated watched movie: {}", movie);
                    LOG.warn(TRAKTTV_ERROR, ex);
                    noError = false;
                }
            }
        }
        
        // just set last pull date if no error occurred
        if (noError) {
            this.configService.setProperty(TRAKTTV_LAST_PULL_MOVIES, lastPull);
        }
    }

    private static Set<Long> getUpdateableMovies(Ids movieIds, Map<String,List<Long>> updatedMovies) {
        Set<Long> updateable = new HashSet<>();
        if (movieIds.trakt() != null) {
            List<Long> i = updatedMovies.get(TraktTvScanner.SCANNER_ID+"#"+movieIds.trakt());
            if (i != null) updateable.addAll(i);
        }
        if (movieIds.imdb() != null) {
            List<Long> i = updatedMovies.get(ImdbScanner.SCANNER_ID+"#"+movieIds.imdb());
            if (i != null) updateable.addAll(i);
        }
        if (movieIds.tmdb() != null) {
            List<Long> i = updatedMovies.get(TheMovieDbScanner.SCANNER_ID+"#"+movieIds.tmdb());
            if (i != null) updateable.addAll(i);
        }
        return updateable;
    }

    public void pullWatchedEpisodes() {
        List<TrackedShow> trackedShows;
        try {
            trackedShows = traktTvApi.syncService().getWatchedShows(Extended.MINIMAL);
        } catch (Exception e) {
            LOG.error("Failed to get tracked shows", e);
            return;
        }
        LOG.info("Found {} watched shows on Trakt.TV", trackedShows.size());

        // store last pull date for later use
        final Date lastPull = new Date();
        
        // get the updated movie IDs for setting watched status
        Date checkDate = this.configService.getDateProperty(TRAKTTV_LAST_PULL_EPISODES);
        if (checkDate == null) {
            // build a date long, long ago ...
            checkDate = DateTime.now().minusYears(20).toDate();
        }
        Map<String,List<Long>> updatedEpisodes = this.traktTvStorageService.getUpdatedEpisodeIds(checkDate);
        
        // nothing to do if no new or updated movies found
        if (updatedEpisodes.isEmpty()) {
            this.configService.setProperty(TRAKTTV_LAST_PULL_EPISODES, lastPull);
            return;
        }
        
        boolean noError = true;
        for (TrackedShow show : trackedShows) {
            for (TrackedSeason season : show.getSeasons()) {
                for (TrackedEpisode episode : season.getEpisodes()) {
                    final Set<Long> updateable = getUpdateableEpisodes(show.getShow().getIds(), season.getNumber(), episode.getNumber(), updatedEpisodes);
                    if (updateable.size() > 0) {
                        try {
                            this.traktTvStorageService.updateWatched(episode, updateable);
                        } catch (Exception ex) {
                            LOG.error("Failed to updated watched movie: {}", episode);
                            LOG.warn(TRAKTTV_ERROR, ex);
                            noError = false;
                        }
                    }
                }
            }
        }
        
        // just set last pull date if no error occurred
        if (noError) {
            this.configService.setProperty(TRAKTTV_LAST_PULL_EPISODES, lastPull);
        }
    }

    private static Set<Long> getUpdateableEpisodes(Ids showIds, int season, int episode, Map<String,List<Long>> updatedEpisodes) {
        Set<Long> updateable = new HashSet<>();
        if (showIds.trakt() != null) {
            List<Long> i = updatedEpisodes.get(TraktTvScanner.SCANNER_ID+"#"+showIds.trakt()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        if (showIds.tvdb() != null) {
            List<Long> i = updatedEpisodes.get(TheTVDbScanner.SCANNER_ID+"#"+showIds.tvdb()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        if (showIds.tvRage() != null) {
            List<Long> i = updatedEpisodes.get(TVRageScanner.SCANNER_ID+"#"+showIds.tvRage()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        if (showIds.imdb() != null) {
            List<Long> i = updatedEpisodes.get(ImdbScanner.SCANNER_ID+"#"+showIds.imdb()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        return updateable;
    }

    public void pushWatchedMovies() {
        List<TrackedMovie> trackedMovies;
        try {
            trackedMovies = traktTvApi.syncService().getCollectionMovies(Extended.MINIMAL);
        } catch (Exception e) {
            LOG.error("Failed to get collected movies", e);
            return;
        }
        LOG.info("Found {} collected movies on Trakt.TV", trackedMovies.size());
        
        // store last push date for later use
        final Date lastPush = new Date();
        
        // get the updated movie IDs for setting watched status
        Date checkDate = this.configService.getDateProperty(TRAKTTV_LAST_PUSH_MOVIES);
        if (checkDate == null) {
            // build a date long, long ago ...
            checkDate = DateTime.now().minusYears(20).toDate();
        }
        Map<Long, TraktMovieDTO> watchedMovies = this.traktTvStorageService.getWatchedMovies(checkDate);
        
        // nothing to do if no new watched movies found
        if (watchedMovies.isEmpty()) {
            this.configService.setProperty(TRAKTTV_LAST_PUSH_MOVIES, lastPush);
            return;
        }
        
        List<SyncMovie> syncList = new ArrayList<>();
        for (TrackedMovie trackedMovie : trackedMovies) {
            TraktMovieDTO dto = findWatchedMovie(trackedMovie, watchedMovies.values());
            if (dto != null) {
                // found a matching watched movie
                SyncMovie syncMovie = new SyncMovie();
                syncMovie.ids(trackedMovie.getMovie().getIds());
                syncMovie.watchedAt(dto.getWatchedDate());
                syncList.add(syncMovie);
                LOG.debug("Trakt.TV watched movies sync: {}", dto.getIdentifier());
            }
        }
        
        boolean noError = true;
        if (syncList.size() > 0) {
            SyncItems items = new SyncItems();
            items.movies(syncList);
            try {
                this.traktTvApi.syncService().addItemsToWatchedHistory(items);
            } catch (Exception ex) {
                LOG.error("Failed to add items to watched history");
                LOG.warn(TRAKTTV_ERROR, ex);
                noError = false;
            }
        }

        if (noError) {
            this.configService.setProperty(TRAKTTV_LAST_PUSH_MOVIES, lastPush);
        }
    }
    
    public static TraktMovieDTO findWatchedMovie(TrackedMovie trackedMovie, Collection<TraktMovieDTO> watchedMovies) {
        final Ids ids = trackedMovie.getMovie().getIds();
        for (TraktMovieDTO watched : watchedMovies) {
            if (!watched.isValid()) {
                continue;
            }
            
            if (matchId(watched.getTrakt(), ids.trakt())) {
                return watched;
            }
            if (matchId(watched.getImdb(), ids.imdb())) {
                return watched;
            }
            if (matchId(watched.getTmdb(), ids.tmdb())) {
                return watched;
            }
        }
        return null;
    }

    private static boolean matchId(Object id1, Object id2) {
        if (id1 == null || id2 == null) {
            return false;
        }
        return id1.equals(id2);
    }

    public void pushWatchedEpisodes() {
        // TODO
    }
}
