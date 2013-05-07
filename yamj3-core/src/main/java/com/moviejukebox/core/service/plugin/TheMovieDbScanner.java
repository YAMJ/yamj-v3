package com.moviejukebox.core.service.plugin;

import com.moviejukebox.common.tools.PropertyTools;
import com.moviejukebox.core.database.model.Genre;
import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.database.model.dto.CreditDTO;
import com.moviejukebox.core.database.model.type.JobType;
import com.omertron.themoviedbapi.MovieDbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;
import com.omertron.themoviedbapi.model.PersonType;
import com.omertron.themoviedbapi.model.ProductionCountry;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

@Service("tmdbScanner")
public class TheMovieDbScanner implements IMovieScanner, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbScanner.class);
    private static final String TMDB_SCANNER_ID = "tmdb";
    private static final String IMDB_SCANNER_ID = ImdbScanner.getScannerId();
    private static final String DEFAULT_LANGUAGE = PropertyTools.getProperty("themoviedb.language", "en");
    private static final boolean INCLUDE_ADULT = PropertyTools.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);
    private static final int SEARCH_MATCH = PropertyTools.getIntProperty("themoviedb.searchMatch", 3);
    @Autowired
    private PluginDatabaseService pluginDatabaseService;
    private static final String API_KEY = PropertyTools.getProperty("APIKEY.themoviedb", "");
    private static TheMovieDbApi tmdbApi;

    @Override
    public String getScannerName() {
        return TMDB_SCANNER_ID;
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
        String tmdbID = videoData.getMoviedbId(TMDB_SCANNER_ID);
        String imdbID = videoData.getMoviedbId(IMDB_SCANNER_ID);
        MovieDb moviedb = null;

        // First look to see if we have a TMDb ID as this will make looking the film up easier
        if (StringUtils.isNotBlank(tmdbID)) {
            // Search based on TMdb ID
            LOG.debug("Using TMDb ID ({}) for {}", tmdbID, videoData.getTitle());
            try {
                moviedb = tmdbApi.getMovieInfo(Integer.parseInt(tmdbID), DEFAULT_LANGUAGE);
            } catch (MovieDbException ex) {
                LOG.debug("Failed to get movie info using TMDB ID: {}, Error: {}", tmdbID, ex.getMessage());
                moviedb = null;
            }
        }

        if (moviedb == null && StringUtils.isNotBlank(imdbID)) {
            // Search based on IMDb ID
            LOG.debug("Using IMDb ID ({}) for {}", imdbID, videoData.getTitle());
            try {
                moviedb = tmdbApi.getMovieInfoImdb(imdbID, DEFAULT_LANGUAGE);
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
            LOG.debug("No ID found for {}, trying title search with '{} ({})'", videoData.getTitle(), videoData.getTitle(), videoData.getPublicationYear());
            tmdbID = getMovieId(videoData.getTitle(), videoData.getPublicationYear());
        }

        if (StringUtils.isNotBlank(tmdbID)) {
            LOG.info("Found TMDB ID: {}", tmdbID);
            videoData.setMoviedbId(TMDB_SCANNER_ID, tmdbID);
        } else {
            LOG.info("No TMDB ID found for ", videoData.getTitle());
        }
        return tmdbID;
    }

    @Override
    public String getMovieId(String title, int year) {
        MovieDb moviedb = null;

        try {
            // Search using movie name
            List<MovieDb> movieList = tmdbApi.searchMovie(title, year, DEFAULT_LANGUAGE, INCLUDE_ADULT, 0);
            LOG.info("Found {} potential matches for {} ({})", movieList.size(), title, year);
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
            LOG.info("TMDB ID found {} for {}", moviedb.getId(), title);
            return String.valueOf(moviedb.getId());
        }
        return "";
    }

    @Override
    public ScanResult scan(VideoData videoData) {
//        String tmdbID = videoData.getMoviedbId(TMDB_SCANNER_ID);
        String tmdbID = getMovieId(videoData);

        if (StringUtils.isBlank(tmdbID)) {
            LOG.debug("Missing TMDB ID for {}", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        return updateVideoData(videoData);
    }

    private ScanResult updateVideoData(VideoData videoData) {
        String tmdbID = videoData.getMoviedbId(TMDB_SCANNER_ID);
        MovieDb moviedb;

        if (StringUtils.isBlank(tmdbID)) {
            LOG.error("Failed retrieving TheMovieDb information for {}, missing id.", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        try {
            moviedb = tmdbApi.getMovieInfo(Integer.parseInt(tmdbID), DEFAULT_LANGUAGE);
        } catch (MovieDbException ex) {
            LOG.error("Failed retrieving TheMovieDb information for {}", videoData.getTitle());
            return ScanResult.ERROR;
        }

        videoData.setTitle(moviedb.getTitle(), TMDB_SCANNER_ID);
        videoData.setPlot(moviedb.getOverview(), TMDB_SCANNER_ID);
        videoData.setOutline(moviedb.getOverview(), TMDB_SCANNER_ID);
        for (ProductionCountry country : moviedb.getProductionCountries()) {
            videoData.setCountry(country.getName(), TMDB_SCANNER_ID);
            break;
        }

        // YEAR
        String year = moviedb.getReleaseDate();
        // Check if this is the default year and skip it
        if (!"1900-01-01".equals(year)) {
            year = (new DateTime(year)).toString("yyyy");
            videoData.setPublicationYear(Integer.parseInt(year), TMDB_SCANNER_ID);
        }

        // GENRES
        Set<Genre> genres = new HashSet<Genre>();
        for (com.omertron.themoviedbapi.model.Genre genre : moviedb.getGenres()) {
            genres.add(new Genre(genre.getName()));
        }
        videoData.setGenres(genres);

        // CAST
        try {
            CreditDTO credit;
            for (com.omertron.themoviedbapi.model.Person person : tmdbApi.getMovieCasts(Integer.parseInt(tmdbID))) {
                credit = new CreditDTO();
                credit.setMoviedb(TMDB_SCANNER_ID);
                credit.setMoviedbId(String.valueOf(person.getId()));
                credit.setName(person.getName());
                credit.setRole(person.getCharacter());

                if (person.getAka() != null && !person.getAka().isEmpty()) {
                    credit.setAka(person.getAka().get(0));
                }
                credit.setRole(person.getCharacter());

                if (person.getPersonType() == PersonType.CAST) {
                    credit.setJobType(JobType.ACTOR);
                } else if (person.getPersonType() == PersonType.CREW) {
                    if (person.getDepartment().equalsIgnoreCase("writing")) {
                        credit.setJobType(JobType.WRITER);
                    } else if (person.getDepartment().equalsIgnoreCase("directing")) {
                        credit.setJobType(JobType.DIRECTOR);
                    } else if (person.getDepartment().equalsIgnoreCase("production")) {
                        credit.setJobType(JobType.PRODUCER);
                    } else {
                        LOG.debug("Adding unknown department '{}' for: '{}', person: '{}'", person.getDepartment(), videoData.getTitle(), person.getName());
                        LOG.trace("Person: {}", person.toString());
                        credit.setJobType(JobType.UNKNOWN);
                    }
                } else {
                    LOG.debug("Unknown job type: '{}', for: '{}', person: '{}'", person.getPersonType().toString(), videoData.getTitle(), person.getName());
                    LOG.trace("Person: {}", person.toString());
                }
                videoData.addCreditDTO(credit);
            }
        } catch (MovieDbException ex) {
            LOG.error("Error getting cast from TheMovieDB: {}", ex.getMessage());
        }

        return ScanResult.OK;
    }
}
