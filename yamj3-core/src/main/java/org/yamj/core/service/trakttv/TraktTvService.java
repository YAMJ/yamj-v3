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

import static org.yamj.plugin.api.common.Constants.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
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
import org.yamj.core.database.model.dto.TraktEpisodeDTO;
import org.yamj.core.database.model.dto.TraktMovieDTO;
import org.yamj.core.database.service.TraktTvStorageService;
import org.yamj.core.service.metadata.online.TraktTvScanner;
import org.yamj.plugin.api.web.TemporaryUnavailableException;

@Service("traktTvService")
public class TraktTvService {

    private static final Logger LOG = LoggerFactory.getLogger(TraktTvService.class);
    private static final String TRAKTTV_ACCESS_TOKEN = "trakttv.auth.access";
    private static final String TRAKTTV_REFRESH_TOKEN = "trakttv.auth.refresh";
    private static final String TRAKTTV_EXPIRATION = "trakttv.auth.expiration";
    private static final String TRAKTTV_LAST_COLLECT_MOVIES = "trakttv.last.collect.movies";
    private static final String TRAKTTV_LAST_COLLECT_EPISODES = "trakttv.last.collect.episodes";
    private static final String TRAKTTV_LAST_PULL_MOVIES = "trakttv.last.pull.movies";
    private static final String TRAKTTV_LAST_PULL_EPISODES = "trakttv.last.pull.episodes";
    private static final String TRAKTTV_LAST_PUSH_MOVIES = "trakttv.last.push.movies";
    private static final String TRAKTTV_LAST_PUSH_EPISODES = "trakttv.last.push.episodes";
    private static final String TRAKTTV_ERROR = "Trakt.TV error";
    private static final int SYNC_MAX_RESULTS = 100;
    
    private static final ReentrantLock REFRESH_LOCK = new ReentrantLock();
    private static boolean REFRESH_FAILED = false;
    
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
        if (!isSynchronizationEnabled()) {
            // nothing to do cause synchronization is not enabled
            return;
        }

        if (isExpired()) {
            // refresh access token
            refreshWhenExpired();
        } else {
            // set access token in API
            traktTvApi.setAccessToken(configService.getProperty(TRAKTTV_ACCESS_TOKEN));
        }
    }

    public boolean isSynchronizationEnabled() {
        return (collectionEnabled || pushEnabled || pullEnabled);
    }
    
    public boolean isExpired() {
        // check expiration date
        final Date expirationDate = configService.getDateProperty(TRAKTTV_EXPIRATION);
        return (expirationDate == null || expirationDate.getTime() < System.currentTimeMillis());
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

            // store values in configuration settings
            configService.setProperty(TRAKTTV_EXPIRATION, buildExpirationDate(response));
            configService.setProperty(TRAKTTV_REFRESH_TOKEN, response.getRefreshToken());
            configService.setProperty(TRAKTTV_ACCESS_TOKEN, response.getAccessToken());
            
            // no authorization error
            REFRESH_FAILED = false;
            return null;
        } catch (TraktTvException e) {
            LOG.debug(TRAKTTV_ERROR, e);
            return e.getResponse();
        } catch (Exception e) {
            LOG.debug("Unknown error", e);
            return "Unknow error occured";
        }
    }

    public boolean refreshWhenExpired() {
        REFRESH_LOCK.lock();
        try {
            if (REFRESH_FAILED) {
                // previous refresh failed
                return false;
            }
            
            LOG.info("Authorization expired; requesting new access token");
    
            String refreshToken = configService.getProperty(TRAKTTV_REFRESH_TOKEN);
            if (StringUtils.isBlank(refreshToken)) {
                LOG.warn("Refresh token not present; please authorize again");
                REFRESH_FAILED = true;
            } else {       
                try {
                    // retrieve access token via access token
                    TokenResponse response = traktTvApi.requestAccessTokenByRefresh(refreshToken);
                    LOG.info("Sucessfully refreshed access token");
                    
                    // store values in configuration settings
                    configService.setProperty(TRAKTTV_EXPIRATION, buildExpirationDate(response));
                    configService.setProperty(TRAKTTV_REFRESH_TOKEN, response.getRefreshToken());
                    configService.setProperty(TRAKTTV_ACCESS_TOKEN, response.getAccessToken());
                    
                    // set access token in API
                    traktTvApi.setAccessToken(response.getAccessToken());

                    REFRESH_FAILED = false;
                } catch (Exception ex) {
                    LOG.error("Failed to refresh access token", ex);
                    REFRESH_FAILED = true;
                }
            }
            return !REFRESH_FAILED;
        } finally {
            REFRESH_LOCK.unlock();
        }
    }
    
    private static long buildExpirationDate(TokenResponse response) {
        // expiration date: creation date + expiration period * 1000 (cause given in seconds)
        return (response.getCreatedAt() + response.getExpiresIn()) * 1000L;
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

    // COLLECTION
    
    private DateTime getCheckDate(String property) {
        Date checkDate = this.configService.getDateProperty(property);
        if (checkDate == null) {
            // build a date long, long ago ...
            return DateTime.now().minusYears(100);
        }
        return new DateTime(checkDate.getTime());
    }
    
    public void collectMovies() {
        // store last collection date for later use
        final Date lastCollection = new Date();

        // get the collected movies
        final Date checkDate = getCheckDate(TRAKTTV_LAST_COLLECT_MOVIES).toDate();
        Collection<TraktMovieDTO> collectedMovies = this.traktTvStorageService.getCollectedMovies(checkDate);
        LOG.info("Found {} collected movies", collectedMovies.size());

        // nothing to do if empty
        if (collectedMovies.isEmpty()) {
            this.configService.setProperty(TRAKTTV_LAST_COLLECT_MOVIES, lastCollection);
            return;
        }

        // find collected movies on Trakt.TV
        List<TrackedMovie> trackedMovies;
        try {
            trackedMovies = traktTvApi.syncService().getCollectionMovies(Extended.MINIMAL);
        } catch (Exception e) {
            LOG.error("Failed to get collected movies", e);
            return;
        }
        LOG.debug("Found {} collected movies on Trakt.TV", trackedMovies.size());
        
        // synchronize movies
        List<SyncMovie> syncMovies = new ArrayList<>();
        boolean noError = true;
        int counter = 0;
        for (TraktMovieDTO dto : collectedMovies) {
            final TrackedMovie movie = findMovie(dto, trackedMovies);
            if (movie != null) {
                LOG.trace("Movie {} already collected", dto.getIdentifier());
                if (dto.getTrakt() == null) {
                    // TODO store Trakt.TV id of movie
                }
            } else {
                // build movie and set collection date
                addSyncMovie(dto, syncMovies).collectedAt(dto.getCollectDate());
                LOG.debug("Trakt.TV collected movie: {}", dto.getIdentifier());
                counter++;
                
                if (counter == SYNC_MAX_RESULTS) {
                    // sync every 100 movies
                    noError = noError && syncCollectedMovies(syncMovies);
                    counter = 0;
                }
            }
        }
        
        // sync outstanding movies
        noError = noError && syncCollectedMovies(syncMovies);
        
        // if no error then set last collection date for next run
        if (noError) {
            this.configService.setProperty(TRAKTTV_LAST_COLLECT_MOVIES, lastCollection);
        }
    }

    private static TrackedMovie findMovie(TraktMovieDTO dto, List<TrackedMovie> movies) {
        for (TrackedMovie movie : movies) {
            if (match(movie, dto)) {
                return movie;
            }
        }
        return null;
    }

    private boolean syncCollectedMovies(List<SyncMovie> syncMovies) {
        boolean noError = true;
        if (syncMovies.size() > 0) {
            try {
                this.traktTvApi.syncService().addItemsToCollection(new SyncItems().movies(syncMovies));
            } catch (Exception ex) {
                LOG.error("Failed to add {} movies to collection", syncMovies.size());
                LOG.warn(TRAKTTV_ERROR, ex);
                noError = false;
            }
            // clear synchronized movies
            syncMovies.clear();
        }
        return noError;
    }

    public void collectEpisodes() {
        // store last collection date for later use
        final Date lastCollection = new Date();

        // get the collected episodes
        final Date checkDate = getCheckDate(TRAKTTV_LAST_COLLECT_EPISODES).toDate();
        Collection<TraktEpisodeDTO> collectedEpisodes = this.traktTvStorageService.getCollectedEpisodes(checkDate);
        LOG.info("Found {} collected episodes", collectedEpisodes.size());

        // nothing to do if empty
        if (collectedEpisodes.isEmpty()) {
            this.configService.setProperty(TRAKTTV_LAST_COLLECT_EPISODES, lastCollection);
            return;
        }

        // find collected shows on Trakt.TV
        List<TrackedShow> trackedShows;
        try {
            trackedShows = traktTvApi.syncService().getCollectionShows(Extended.MINIMAL);
        } catch (Exception e) {
            LOG.error("Failed to get collected shows", e);
            return;
        }
        LOG.debug("Found {} collected shows on Trakt.TV", trackedShows.size());

        // synchronize episodes
        List<SyncShow> syncShows = new ArrayList<>();
        boolean noError = true;
        int counter = 0;
        for (TraktEpisodeDTO dto : collectedEpisodes) {
            final TrackedEpisode episode = findEpisode(dto, trackedShows);
            if (episode != null) {
                LOG.trace("Episode {} already collected", dto.getIdentifier());
            } else {
                // build episode and set collection date
                addSyncEpisode(dto, syncShows).collectedAt(dto.getCollectDate());
                LOG.debug("Trakt.TV collected episode: {}", dto.getIdentifier());
                counter++;
                
                if (counter == SYNC_MAX_RESULTS) {
                    // sync every 100 episodes
                    noError = noError && syncCollectedShows(syncShows);
                    counter = 0;
                }
            }
        }

        // sync outstanding episodes
        noError = noError && syncCollectedShows(syncShows);
        
        // if no error then set last collection date for next run
        if (noError) {
            this.configService.setProperty(TRAKTTV_LAST_COLLECT_EPISODES, lastCollection);
        }
    }

    private static TrackedEpisode findEpisode(TraktEpisodeDTO dto, List<TrackedShow> shows) {
        List<TrackedSeason> seasons = null;
        for (TrackedShow show : shows) {
            if (match (show, dto)) {
                seasons = show.getSeasons();
                break;
            }
        }
        
        if (seasons == null || seasons.isEmpty()) {
            return null;
        }
        
        TrackedSeason matchingSeason = null;
        for (TrackedSeason season : seasons) {
            if (match(season.getNumber(), dto.getSeason())) {
                matchingSeason = season;
                break;
            }
        }
        if (matchingSeason == null) {
            return null;
        }
        
        for (TrackedEpisode episode : matchingSeason.getEpisodes()) {
            if (match(episode.getNumber(), dto.getEpisode())) {
                return episode;
            }
        }
        return null;
    }

    private boolean syncCollectedShows(List<SyncShow> syncShows) {
        boolean noError = true;
        if (syncShows.size() > 0) {
            try {
                this.traktTvApi.syncService().addItemsToCollection(new SyncItems().shows(syncShows));
            } catch (Exception ex) {
                LOG.error("Failed to add episodes to collection");
                LOG.warn(TRAKTTV_ERROR, ex);
                noError = false;
            }
            // clear synchronized shows
            syncShows.clear();
        }
        return noError;
    }

    // PULL SYNCHRONIZATION

    public boolean pullWatchedMovies() {
        // store last pull date for later use (without milliseconds)
        final Date lastPull = DateTime.now().withMillisOfSecond(0).toDate();
        
        // get watched movies from Trakt.TV
        List<TrackedMovie> watchedMovies;
        try {
            watchedMovies = traktTvApi.syncService().getWatchedMovies(Extended.MINIMAL);
        } catch (Exception e) {
            LOG.error("Failed to get watched movies", e);
            return false;
        }
        if (watchedMovies.isEmpty()) {
            // nothing to do, cause nothing has been watched
            LOG.trace("No watched movies found on Trakt.TV");
            return true;
        }

        // filter out movies which has been watched before check date
        final DateTime checkDate = getCheckDate(TRAKTTV_LAST_PULL_MOVIES);
        List<TrackedMovie> filteredMovies = new ArrayList<>();
        for (TrackedMovie movie : watchedMovies) {
            if (movie.getLastWatchedAt().isAfter(checkDate)) {
                filteredMovies.add(movie);
            }
        }
        LOG.info("Found {} new watched movies on Trakt.TV", filteredMovies.size());
        if (filteredMovies.isEmpty()) {
            // nothing to do, cause nothing has been watched after last pull date
            this.configService.setProperty(TRAKTTV_LAST_PULL_MOVIES, lastPull);
            return true;
        }
        
        // get all movie IDs from database
        Map<String,List<Long>> allMovieIds = this.traktTvStorageService.getAllMovieIds();
        if (allMovieIds.isEmpty()) {
            // nothing to do if no movies found
            this.configService.setProperty(TRAKTTV_LAST_PULL_MOVIES, lastPull);
            return true;
        }
        
        // update watched status for filtered movies
        boolean noError = true;
        for (TrackedMovie movie : filteredMovies) {
            final Set<Long> updateable = getUpdateableMovies(movie.getMovie().getIds(), allMovieIds);
            if (updateable.size() > 0) {
                try {
                    final String traktTvId = movie.getMovie().getIds().trakt().toString();
                    final Date lastWatched = movie.getLastWatchedAt().withMillisOfSecond(0).toDate();
                    this.traktTvStorageService.updateWatched(traktTvId, lastWatched, updateable);
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
        return noError;
    }

    private static Set<Long> getUpdateableMovies(Ids movieIds, Map<String,List<Long>> updatedMovies) {
        Set<Long> updateable = new HashSet<>();
        if (movieIds.trakt() != null) {
            List<Long> i = updatedMovies.get(TraktTvScanner.SCANNER_ID+"#"+movieIds.trakt());
            if (i != null) updateable.addAll(i);
        }
        if (movieIds.imdb() != null) {
            List<Long> i = updatedMovies.get(SOURCE_IMDB+"#"+movieIds.imdb());
            if (i != null) updateable.addAll(i);
        }
        if (movieIds.tmdb() != null) {
            List<Long> i = updatedMovies.get(SOURCE_TMDB+"#"+movieIds.tmdb());
            if (i != null) updateable.addAll(i);
        }
        return updateable;
    }

    public boolean pullWatchedEpisodes() {
        // store last pull date for later use (without milliseconds)
        final Date lastPull = DateTime.now().withMillisOfSecond(0).toDate();

        // get watched shows from Trakt.TV
        List<TrackedShow> watchedShows;
        try {
            watchedShows = traktTvApi.syncService().getWatchedShows(Extended.MINIMAL);
        } catch (Exception e) {
            LOG.error("Failed to get watched shows", e);
            return false;
        }
        if (watchedShows.isEmpty()) {
            // nothing to do, cause nothing has been watched
            LOG.trace("No watched shows found on Trakt.TV");
            return true;
        }

        // filter out episodes which has been watched before check date
        final DateTime checkDate = getCheckDate(TRAKTTV_LAST_PULL_EPISODES);
        List<WatchedEpisode> watchedEpisodes = new ArrayList<>();
        for (TrackedShow show : watchedShows) {
            for (TrackedSeason season : show.getSeasons()) {
                for (TrackedEpisode episode : season.getEpisodes()) {
                    if (episode.getLastWatchedAt() != null && episode.getLastWatchedAt().isAfter(checkDate)) {
                        final Date lastWatched = episode.getLastWatchedAt().withMillisOfSecond(0).toDate();
                        watchedEpisodes.add(new WatchedEpisode(show.getShow().getIds(), season.getNumber(), episode.getNumber(), lastWatched));
                    }
                }
            }
        }
        LOG.info("Found {} new watched episodes on Trakt.TV", watchedEpisodes.size());
        if (watchedEpisodes.isEmpty()) {
            // nothing to do, cause nothing has been watched
            this.configService.setProperty(TRAKTTV_LAST_PULL_EPISODES, lastPull);
            return true;
        }
        
        // get all episode IDs from database
        Map<String,List<Long>> allEpisodeIds = this.traktTvStorageService.getAllEpisodeIds();
        if (allEpisodeIds.isEmpty()) {
            // nothing to do if no episodes found
            this.configService.setProperty(TRAKTTV_LAST_PULL_EPISODES, lastPull);
            return true;
        }
        
        // update watched status for filtered episodes
        boolean noError = true;
        for (WatchedEpisode episode : watchedEpisodes) {
            final Set<Long> updateable = getUpdateableEpisodes(episode.getIds(), episode.getSeason(), episode.getEpisode(), allEpisodeIds);
            if (updateable.size() > 0) {
                try {
                    this.traktTvStorageService.updateWatched(episode.getLastWatched(), updateable);
                } catch (Exception ex) {
                    LOG.error("Failed to updated watched episode: {}", episode);
                    LOG.warn(TRAKTTV_ERROR, ex);
                    noError = false;
                }
            }
        }
        
        // just set last pull date if no error occurred
        if (noError) {
            this.configService.setProperty(TRAKTTV_LAST_PULL_EPISODES, lastPull);
        }
        return noError;
    }

    private static Set<Long> getUpdateableEpisodes(Ids showIds, int season, int episode, Map<String,List<Long>> updatedEpisodes) {
        Set<Long> updateable = new HashSet<>();
        if (showIds.trakt() != null) {
            List<Long> i = updatedEpisodes.get(TraktTvScanner.SCANNER_ID+"#"+showIds.trakt()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        if (showIds.tvdb() != null) {
            List<Long> i = updatedEpisodes.get(SOURCE_TVDB+"#"+showIds.tvdb()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        if (showIds.tvRage() != null) {
            List<Long> i = updatedEpisodes.get(SOURCE_TVRAGE+"#"+showIds.tvRage()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        if (showIds.imdb() != null) {
            List<Long> i = updatedEpisodes.get(SOURCE_IMDB+"#"+showIds.imdb()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        if (showIds.tmdb() != null) {
            List<Long> i = updatedEpisodes.get(SOURCE_TMDB+"#"+showIds.tmdb()+"#"+season+"#"+episode);
            if (i != null) updateable.addAll(i);
        }
        return updateable;
    }

    // PULL SYNCHRONIZATION

    public void pushWatchedMovies() {
        // store last push date for later use
        final Date lastPush = new Date();
        
        // get the updated movie IDs for setting watched status
        final Date checkDate = getCheckDate(TRAKTTV_LAST_PUSH_MOVIES).toDate();
        Collection<TraktMovieDTO> watchedMovies = this.traktTvStorageService.getWatchedMovies(checkDate);
        
        // synchronize movies
        List<SyncMovie> syncMovies = new ArrayList<>();
        boolean noError = true;
        int counter = 0;
        for (TraktMovieDTO dto : watchedMovies) {
            if (!dto.isValid()) {
                continue;
            }

            // build episode and set watched date
            addSyncMovie(dto, syncMovies).watchedAt(dto.getWatchedDate());
            LOG.debug("Trakt.TV watched movie sync: {}", dto.getIdentifier());
            counter++;
            
            if (counter == SYNC_MAX_RESULTS) {
                // sync every 100 movies
                noError = noError && syncWatchedMovies(syncMovies);
                counter = 0;
            }
        }
        
        // sync outstanding movies
        noError = noError && syncWatchedMovies(syncMovies);
        
        // if no error then set last push date for next run
        if (noError) {
            this.configService.setProperty(TRAKTTV_LAST_PUSH_MOVIES, lastPush);
        }
    }

    private boolean syncWatchedMovies(List<SyncMovie> syncMovies) {
        boolean noError = true;
        if (syncMovies.size() > 0) {
            try {
                this.traktTvApi.syncService().addItemsToWatchedHistory(new SyncItems().movies(syncMovies));
            } catch (Exception ex) {
                LOG.error("Failed to add {} movies to watched history", syncMovies.size());
                LOG.warn(TRAKTTV_ERROR, ex);
                noError = false;
            }
            // clear synchronized movies
            syncMovies.clear();
        }
        return noError;
    }

    public void pushWatchedEpisodes() {
        // store last push date for later use
        final Date lastPush = new Date();
        
        // get the updated movie IDs for setting watched status
        final Date checkDate = getCheckDate(TRAKTTV_LAST_PUSH_EPISODES).toDate();
        Collection<TraktEpisodeDTO> watchedEpisodes = this.traktTvStorageService.getWatchedEpisodes(checkDate);
        
        List<SyncShow> syncShows = new ArrayList<>();
        boolean noError = true;
        int counter = 0;
        for (TraktEpisodeDTO dto : watchedEpisodes) {
            if (!dto.isValid()) {
                continue;
            }

            // build episode and set watched date
            addSyncEpisode(dto, syncShows).watchedAt(dto.getWatchedDate());
            LOG.debug("Trakt.TV watched episode sync: {}", dto.getIdentifier());
            counter++;
            
            if (counter == SYNC_MAX_RESULTS) {
                // sync every 100 episodes
                noError = noError && syncWatchedShows(syncShows);
                counter = 0;
            }
        }
        
        // sync outstanding episodes
        noError = noError && syncWatchedShows(syncShows);
        
        // if no error then set last push date for next run
        if (noError) {
            this.configService.setProperty(TRAKTTV_LAST_PUSH_EPISODES, lastPush);
        }
    }

    private boolean syncWatchedShows(List<SyncShow> syncShows) {
        boolean noError = true;
        if (syncShows.size() > 0) {
            try {
                this.traktTvApi.syncService().addItemsToWatchedHistory(new SyncItems().shows(syncShows));
            } catch (Exception ex) {
                LOG.error("Failed to add episodes to watched history");
                LOG.warn(TRAKTTV_ERROR, ex);
                noError = false;
            }
            // clear synchronized shows
            syncShows.clear();
        }
        return noError;
    }

    // COMMON METHODS
    
    private static SyncMovie addSyncMovie(final TraktMovieDTO dto, final List<SyncMovie> syncMovies) {
        SyncMovie syncMovie = new SyncMovie().ids(new Ids().trakt(dto.getTrakt()).imdb(dto.getImdb()).tmdb(dto.getTmdb()));
        syncMovies.add(syncMovie.title(dto.getTitle()).year(dto.getYear()));
        return syncMovie;
    }
    
    private static SyncEpisode addSyncEpisode(final TraktEpisodeDTO dto, final List<SyncShow> syncShows) {
        // find matching show in already processed shows
        SyncShow syncShow = null;
        for (SyncShow show : syncShows) {
            if (match(show, dto)) {
                syncShow = show;
                break;
            }
        }

        // build a new show entry if not found
        if (syncShow == null) {
            syncShow = new SyncShow().ids(new Ids().trakt(dto.getTrakt()).imdb(dto.getImdb()).tmdb(dto.getTmdb()).tvdb(dto.getTvdb()).tvRage(dto.getTvRage()));
            syncShows.add(syncShow.title(dto.getTitle()).year(dto.getYear()));
        }

        // find matching season in already processed seasons of teh same show
        SyncSeason syncSeason = null;
        for (SyncSeason season : syncShow.seasons()) {
            if (season.number().intValue() == dto.getSeason()) {
                syncSeason = season;
                break;
            }
        }

        // build a new season entry if not found
        if (syncSeason == null) {
            syncSeason = new SyncSeason().number(dto.getSeason());
            syncShow.season(syncSeason);
        }
        
        // create a new episode, add it to season and return it
        SyncEpisode episode = new SyncEpisode().number(dto.getEpisode()).season(dto.getSeason());
        syncSeason.episode(episode);
        return episode;
    }

    private static boolean match(TrackedMovie movie, TraktMovieDTO dto) {
        if (match(movie.getMovie().getIds().trakt(), dto.getTrakt())) {
            return true;
        }
        if (match(movie.getMovie().getIds().imdb(), dto.getImdb())) {
            return true;
        }
        if (match(movie.getMovie().getIds().tmdb(), dto.getTmdb())) {
            return true;
        }
        if (match(movie.getMovie().getYear(), dto.getYear()) && StringUtils.equalsIgnoreCase(movie.getMovie().getTitle(), dto.getTitle())) {
            return true;
        }
        return false;
    }
    
    private static boolean match(TrackedShow show, TraktEpisodeDTO dto) {
        if (match(show.getShow().getIds().trakt(), dto.getTrakt())) {
            return true;
        }
        if (match(show.getShow().getIds().imdb(), dto.getImdb())) {
            return true;
        }
        if (match(show.getShow().getIds().tmdb(), dto.getTmdb())) {
            return true;
        }
        if (match(show.getShow().getIds().tvdb(), dto.getTvdb())) {
            return true;
        }
        if (match(show.getShow().getIds().tvRage(), dto.getTvRage())) {
            return true;
        }
        if (match(show.getShow().getYear(), dto.getYear()) && StringUtils.equalsIgnoreCase(show.getShow().getTitle(), dto.getTitle())) {
            return true;
        }
        return false;
    }

    private static boolean match(SyncShow show, TraktEpisodeDTO dto) {
        if (match(show.ids().trakt(), dto.getTrakt())) {
            return true;
        }
        if (match(show.ids().imdb(), dto.getImdb())) {
            return true;
        }
        if (match(show.ids().tmdb(), dto.getTmdb())) {
            return true;
        }
        if (match(show.ids().tvdb(), dto.getTvdb())) {
            return true;
        }
        if (match(show.ids().tvRage(), dto.getTvRage())) {
            return true;
        }
        return false;
    }

    private static boolean match(Object id1, Object id2) {
        if (id1 == null || id2 == null) {
            return false;
        }
        return id1.equals(id2);
    }

    static class WatchedEpisode {
        
        private final Ids ids;
        private final int season;
        private final int episode;
        private final Date lastWatched;
        
        public WatchedEpisode(Ids ids, int season, int episode, Date lastWatched) {
            this.ids = ids;
            this.season = season;
            this.episode = episode;
            this.lastWatched = lastWatched;
        }

        public Ids getIds() {
            return ids;
        }

        public int getSeason() {
            return season;
        }

        public int getEpisode() {
            return episode;
        }

        public Date getLastWatched() {
            return lastWatched;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }
}
