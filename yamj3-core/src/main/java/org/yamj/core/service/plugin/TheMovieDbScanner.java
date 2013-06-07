/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.plugin;

import org.yamj.core.tools.OverrideTools;

import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;
import com.omertron.themoviedbapi.model.PersonType;
import com.omertron.themoviedbapi.model.ProductionCountry;
import com.omertron.themoviedbapi.results.TmdbResultsList;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;

@Service("tmdbScanner")
public class TheMovieDbScanner implements IMovieScanner, IPersonScanner, InitializingBean {

    public static final String TMDB_SCANNER_ID = "tmdb";
    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbScanner.class);
    private static final String DEFAULT_LANGUAGE = PropertyTools.getProperty("themoviedb.language", "en");
    private static final boolean INCLUDE_ADULT = PropertyTools.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);
    private static final int SEARCH_MATCH = PropertyTools.getIntProperty("themoviedb.searchMatch", 3);
    @Autowired
    private PluginMetadataService pluginMetadataService;
    @Autowired
    private TheMovieDbApi tmdbApi;

    @Override
    public String getScannerName() {
        return TMDB_SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() {
        // register this scanner
        pluginMetadataService.registerMovieScanner(this);
        pluginMetadataService.registerPersonScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String tmdbID = videoData.getSourcedbId(TMDB_SCANNER_ID);
        String imdbID = videoData.getSourcedbId(ImdbScanner.IMDB_SCANNER_ID);
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
            videoData.setSourcedbId(TMDB_SCANNER_ID, tmdbID);
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
            List<MovieDb> movieList = tmdbApi.searchMovie(title, year, DEFAULT_LANGUAGE, INCLUDE_ADULT, 0).getResults();
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
//        String tmdbID = videoData.getSourcedbId(TMDB_SCANNER_ID);
        String tmdbID = getMovieId(videoData);

        if (StringUtils.isBlank(tmdbID)) {
            LOG.debug("Missing TMDB ID for {}", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        return updateVideoData(videoData);
    }

    private ScanResult updateVideoData(VideoData videoData) {
        String tmdbID = videoData.getSourcedbId(TMDB_SCANNER_ID);
        MovieDb moviedb;

        if (StringUtils.isBlank(tmdbID)) {
            LOG.error("Failed retrieving TheMovieDb information for {}, missing id.", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        try {
            moviedb = tmdbApi.getMovieInfo(Integer.parseInt(tmdbID), DEFAULT_LANGUAGE);
        } catch (MovieDbException ex) {
            LOG.error("Failed retrieving TheMovieDb information for {}, error: {}", videoData.getTitle(), ex.getMessage());
            return ScanResult.ERROR;
        }

        if (OverrideTools.checkOverwriteTitle(videoData, TMDB_SCANNER_ID)) {
            videoData.setTitle(moviedb.getTitle(), TMDB_SCANNER_ID);
        }
        
        if (OverrideTools.checkOverwritePlot(videoData, TMDB_SCANNER_ID)) {
            videoData.setPlot(moviedb.getOverview(), TMDB_SCANNER_ID);
        }
        
        if (OverrideTools.checkOverwriteOutline(videoData, TMDB_SCANNER_ID)) {
            videoData.setOutline(moviedb.getOverview(), TMDB_SCANNER_ID);
        }
        
        if (OverrideTools.checkOverwriteCountry(videoData, TMDB_SCANNER_ID)) {
            for (ProductionCountry country : moviedb.getProductionCountries()) {
                videoData.setCountry(country.getName(), TMDB_SCANNER_ID);
                break;
            }
        }
        
        if (OverrideTools.checkOverwriteYear(videoData, TMDB_SCANNER_ID)) {
            String year = moviedb.getReleaseDate();
            // Check if this is the default year and skip it
            if (!"1900-01-01".equals(year)) {
                year = (new DateTime(year)).toString("yyyy");
                videoData.setPublicationYear(Integer.parseInt(year), TMDB_SCANNER_ID);
            }
        }
        
        if (OverrideTools.checkOverwriteGenres(videoData, TMDB_SCANNER_ID)) {
            // GENRES
            Set<String> genreNames = new HashSet<String>();
            for (com.omertron.themoviedbapi.model.Genre genre : moviedb.getGenres()) {
                genreNames.add(genre.getName());
            }
            videoData.setGenreNames(genreNames, TMDB_SCANNER_ID);
        }
        
        // CAST & CREW
        try {
            CreditDTO credit;
            for (com.omertron.themoviedbapi.model.Person person : tmdbApi.getMovieCasts(Integer.parseInt(tmdbID)).getResults()) {
                credit = new CreditDTO();
                credit.setSourcedb(TMDB_SCANNER_ID);
                credit.setSourcedbId(String.valueOf(person.getId()));
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
                    } else if (person.getDepartment().equalsIgnoreCase("sound")) {
                        credit.setJobType(JobType.SOUND);
                    } else if (person.getDepartment().equalsIgnoreCase("camera")) {
                        credit.setJobType(JobType.CAMERA);
                    } else if (person.getDepartment().equalsIgnoreCase("art")) {
                        credit.setJobType(JobType.ART);
                    } else if (person.getDepartment().equalsIgnoreCase("editing")) {
                        credit.setJobType(JobType.EDITING);
                    } else if (person.getDepartment().equalsIgnoreCase("costume & make-up")) {
                        credit.setJobType(JobType.COSTUME_MAKEUP);
                    } else if (person.getDepartment().equalsIgnoreCase("crew")) {
                        credit.setJobType(JobType.CREW);
                    } else if (person.getDepartment().equalsIgnoreCase("visual effects")) {
                        credit.setJobType(JobType.EFFECTS);
                    } else if (person.getDepartment().equalsIgnoreCase("lighting")) {
                        credit.setJobType(JobType.LIGHTING);
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

    @Override
    public String getPersonId(Person person) {
        String id = person.getPersonId(TMDB_SCANNER_ID);
        if (StringUtils.isNotBlank(id)) {
            return id;
        } else if (StringUtils.isNotBlank(person.getName())) {
            return getPersonId(person.getName());
        } else {
            LOG.error("No ID or Name found for {}", person.toString());
            return "";
        }
    }

    @Override
    public String getPersonId(String name) {
        String id = "";
        com.omertron.themoviedbapi.model.Person closestPerson = null;
        int closestMatch = Integer.MAX_VALUE;
        boolean foundPerson = Boolean.FALSE;

        try {
            TmdbResultsList<com.omertron.themoviedbapi.model.Person> results = tmdbApi.searchPeople(name, INCLUDE_ADULT, 0);
            LOG.info("{}: Found {} results", name, results.getResults().size());
            for (com.omertron.themoviedbapi.model.Person person : results.getResults()) {
                if (name.equalsIgnoreCase(person.getName())) {
                    id = String.valueOf(person.getId());
                    foundPerson = Boolean.TRUE;
                    break;
                } else {
                    LOG.trace("{}: Checking against '{}'", name, person.getName());
                    int lhDistance = StringUtils.getLevenshteinDistance(name, person.getName());
                    LOG.trace("{}: Current closest match is {}, this match is {}", name, closestMatch, lhDistance);
                    if (lhDistance < closestMatch) {
                        LOG.trace("{}: TMDB ID {} is a better match ", name, person.getId());
                        closestMatch = lhDistance;
                        closestPerson = person;
                    }
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
            LOG.warn("Failed to get information on '{}' from {}, error: {}", name, TMDB_SCANNER_ID, ex.getMessage());
        }
        return id;
    }

    @Override
    public ScanResult scan(Person person) {
        String id = getPersonId(person);
        if (StringUtils.isBlank(id) || !StringUtils.isNumeric(id)) {
            return ScanResult.MISSING_ID;
        }

        try {
            LOG.debug("Getting information on {}-'{}' from {}", person.getId(), person.getName(), TMDB_SCANNER_ID);
            com.omertron.themoviedbapi.model.Person tmdbPerson = tmdbApi.getPersonInfo(Integer.parseInt(id));

            person.setBiography(tmdbPerson.getBiography());
            person.setBirthPlace(tmdbPerson.getBirthplace());
            person.setPersonId(ImdbScanner.IMDB_SCANNER_ID, tmdbPerson.getImdbId());

            Date parsedDate = parseDate(tmdbPerson.getBirthday());
            if (parsedDate != null) {
                person.setBirthDay(parsedDate);
            }

            parsedDate = parseDate(tmdbPerson.getDeathday());
            if (parsedDate != null) {
                person.setDeathDay(parsedDate);
            }
        } catch (MovieDbException ex) {
            LOG.warn("Failed to get information on {}-'{}', error: {}", id, person.getName(), ex.getMessage());
            return ScanResult.ERROR;
        }

        LOG.debug("Successfully processed person: {}-'{}'", id, person.getName());
        return ScanResult.OK;
    }

    private Date parseDate(String dateToConvert) {
        if (StringUtils.isNotBlank(dateToConvert)) {
            try {
                return DateUtils.parseDate(dateToConvert, "yyyy-MM-dd");
            } catch (ParseException ex) {
                LOG.warn("Failed to convert date '{}'", dateToConvert);
            }
        }
        return null;
    }
}
