package com.moviejukebox.core.service.plugin;

import com.moviejukebox.common.tools.PropertyTools;
import com.moviejukebox.core.database.model.VideoData;
import com.omertron.themoviedbapi.MovieDbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;
import com.omertron.themoviedbapi.model.ProductionCountry;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

@Service("themoviedbScanner")
public class TheMovieDbScanner implements IMovieScanner, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbScanner.class);
    private static final String TMDB_PLUGIN_ID = "tmdb";
    private static final String IMDB_ID = "imdb";
    private static final String defaultLanguage = PropertyTools.getProperty("themoviedb.language", "en");
    private static final boolean INCLUDE_ADULT = PropertyTools.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);
    private static final int SEARCH_MATCH = PropertyTools.getIntProperty("themoviedb.searchMatch", 3);
    @Autowired
    private PluginDatabaseService pluginDatabaseService;
    private static final String API_KEY = PropertyTools.getProperty("APIKEY.themoviedb", "");
    private static TheMovieDbApi tmdbApi;

    @Override
    public String getScannerName() {
        return TMDB_PLUGIN_ID;
    }

    @Override
    public void afterPropertiesSet() {
        if (StringUtils.isNotBlank(API_KEY)) {
            try {
                tmdbApi = new TheMovieDbApi(API_KEY);
                // register this scanner
                pluginDatabaseService.registerMovieScanner(this);

            } catch (MovieDbException ex) {
                LOG.error("Unable to initialise TheMovieDbScanner, error: {}", ex.getMessage());
            }
        } else {
            LOG.error("Failed to initialise TheMovieDbScanner, no API KEY available");
        }
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String tmdbID = videoData.getMoviedbId(TMDB_PLUGIN_ID);
        String imdbID = videoData.getMoviedbId(IMDB_ID);
        MovieDb moviedb = null;

        // First look to see if we have a TMDb ID as this will make looking the film up easier
        if (StringUtils.isNotBlank(tmdbID)) {
            // Search based on TMdb ID
            LOG.debug("Using TMDb ID ({}) for {}", tmdbID, videoData.getTitle());
            try {
                moviedb = tmdbApi.getMovieInfo(Integer.parseInt(tmdbID), defaultLanguage);
            } catch (MovieDbException ex) {
                LOG.debug("Failed to get movie info using TMDB ID: {}, Error: {}", tmdbID, ex.getMessage());
                moviedb = null;
            }
        }

        if (moviedb == null && StringUtils.isNotBlank(imdbID)) {
            // Search based on IMDb ID
            LOG.debug("Using IMDb ID ({}) for {}", imdbID, videoData.getTitle());
            try {
                moviedb = tmdbApi.getMovieInfoImdb(imdbID, defaultLanguage);
                tmdbID = String.valueOf(moviedb.getId());
                if (StringUtils.isBlank(tmdbID)) {
                    LOG.debug("Failed to get movie info using IMDB ID {} for {}", imdbID, videoData.getTitle());
                }
            } catch (MovieDbException ex) {
                LOG.debug("Failed to get movie info using IMDB ID: {}, Error: {}", imdbID, ex.getMessage());
                moviedb = null;
            }
        }

        if (moviedb == null && StringUtils.isNotBlank(videoData.getTitle())) {
            tmdbID = getMovieId(videoData.getTitle(), videoData.getPublicationYear());
        }

        return tmdbID;
    }

    @Override
    public String getMovieId(String title, int year) {
        List<MovieDb> movieList;
        MovieDb moviedb = null;

        try {
            // Search using movie name
            movieList = tmdbApi.searchMovie(title, year, defaultLanguage, INCLUDE_ADULT, 0);
            // Iterate over the list until we find a match
            for (MovieDb m : movieList) {
                LOG.debug("Checking " + m.getTitle() + " (" + m.getReleaseDate().substring(0, 4) + ")");
                if (TheMovieDbApi.compareMovies(m, title, String.valueOf(year), SEARCH_MATCH)) {
                    moviedb = m;
                    break;
                }

                // See if the original title is different and then compare it too
//                if (!movie.getTitle().equals(movie.getOriginalTitle())
//                        && TheMovieDbApi.compareMovies(m, movie.getOriginalTitle(), Integer.toString(movieYear))) {
//                    moviedb = m;
//                    break;
//                }
            }
        } catch (MovieDbException ex) {
            LOG.debug("Failed to get movie info for {}, error: {}", title, ex.getMessage());
            moviedb = null;
        }

        if (moviedb != null) {
            return String.valueOf(moviedb.getId());
        }
        return "";
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        String tmdbID = videoData.getMoviedbId(TMDB_PLUGIN_ID);
        if (StringUtils.isBlank(tmdbID)) {
            LOG.debug("Missing TMDB ID for {}", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        return updateVideoData(videoData);
    }

    private ScanResult updateVideoData(VideoData videoData) {
        ScanResult scanResult = ScanResult.OK;
        String tmdbID = videoData.getMoviedbId(TMDB_PLUGIN_ID);
        MovieDb moviedb;

        try {
            moviedb = tmdbApi.getMovieInfo(Integer.parseInt(tmdbID), defaultLanguage);
        } catch (MovieDbException ex) {
            LOG.error("Failed retrieving TheMovieDb information for {}", videoData.getTitle());
            return ScanResult.ERROR;
        }


        videoData.setTitle(moviedb.getTitle(), TMDB_PLUGIN_ID);
        videoData.setPlot(moviedb.getOverview(), TMDB_PLUGIN_ID);
        videoData.setOutline(moviedb.getOverview(), TMDB_PLUGIN_ID);
        for (ProductionCountry country : moviedb.getProductionCountries()) {
            videoData.setCountry(country.getName(), TMDB_PLUGIN_ID);
            break;
        }

        {
            String year = moviedb.getReleaseDate();
            // Check if this is the default year and skip it
            if (!"1900-01-01".equals(year)) {
                year = (new DateTime(year)).toString("yyyy");
                videoData.setPublicationYear(Integer.parseInt(year), TMDB_PLUGIN_ID);
            }
        }

        return scanResult;
    }
}
