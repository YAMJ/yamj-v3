/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import java.util.HashSet;
import java.util.Set;
import org.yamj.core.database.model.FilmParticipation;
import org.yamj.core.database.model.type.ParticipationType;
import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.*;
import com.omertron.themoviedbapi.results.TmdbResultsList;
import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.configuration.ConfigServiceWrapper;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.service.metadata.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;

@Service("tmdbScanner")
public class TheMovieDbScanner implements IMovieScanner, IFilmographyScanner {

    public static final String SCANNER_ID = "tmdb";
    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbScanner.class);
    private static final String FROM_WIKIPEDIA = "From Wikipedia, the free encyclopedia";
    private static final String WIKIPEDIA_DESCRIPTION_ABOVE = "Description above from the Wikipedia";
    
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private TheMovieDbApi tmdbApi;

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() throws Exception {
        LOG.info("Initialize TheMovieDb scanner");
        
        // register this scanner
        onlineScannerService.registerMovieScanner(this);
        onlineScannerService.registerPersonScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String tmdbID = videoData.getSourceDbId(SCANNER_ID);
        String imdbID = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        String defaultLanguage = configServiceWrapper.getProperty("themoviedb.language", "en");
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
            LOG.debug("No ID found for {}, trying title search with '{} ({})'", videoData.getTitle(), videoData.getTitle(), videoData.getPublicationYear());
            tmdbID = getMovieId(videoData.getTitle(), videoData.getPublicationYear());
        }

        if (StringUtils.isNotBlank(tmdbID)) {
            LOG.info("Found TMDB ID: {}", tmdbID);
            videoData.setSourceDbId(SCANNER_ID, tmdbID);
        } else {
            LOG.info("No TMDB ID found for {}", videoData.getTitle());
        }
        return tmdbID;
    }

    @Override
    public String getMovieId(String title, int year) {
        MovieDb moviedb = null;
        String defaultLanguage = configServiceWrapper.getProperty("themoviedb.language", "en");
        boolean includeAdult = configServiceWrapper.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);
        int searchMatch = configServiceWrapper.getIntProperty("themoviedb.searchMatch", 3);

        try {
            // Search using movie name
            List<MovieDb> movieList = tmdbApi.searchMovie(title, year, defaultLanguage, includeAdult, 0).getResults();
            LOG.info("Found {} potential matches for {} ({})", movieList.size(), title, year);
            // Iterate over the list until we find a match
            for (MovieDb m : movieList) {
                String relDate;
                if (StringUtils.isNotBlank(m.getReleaseDate()) && m.getReleaseDate().length() > 4) {
                    relDate = m.getReleaseDate().substring(0, 4);
                } else {
                    relDate = "";
                }
                LOG.debug("Checking " + m.getTitle() + " (" + relDate + ")");
                if (TheMovieDbApi.compareMovies(m, title, String.valueOf(year), searchMatch)) {
                    moviedb = m;
                    break;
                }
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
        String tmdbID = getMovieId(videoData);

        if (StringUtils.isBlank(tmdbID)) {
            LOG.debug("Missing TMDB ID for {}", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        return updateVideoData(videoData);
    }

    private ScanResult updateVideoData(VideoData videoData) {
        String tmdbID = videoData.getSourceDbId(SCANNER_ID);
        String defaultLanguage = configServiceWrapper.getProperty("themoviedb.language", "en");
        MovieDb moviedb;

        if (StringUtils.isBlank(tmdbID)) {
            LOG.error("Failed retrieving TheMovieDb information for {}, missing id.", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        LOG.info("Getting information for TMDB ID:{}-{}", tmdbID, videoData.getTitle());

        try {
            moviedb = tmdbApi.getMovieInfo(Integer.parseInt(tmdbID), defaultLanguage);
        } catch (MovieDbException ex) {
            LOG.error("Failed retrieving TheMovieDb information for {}, error: {}", videoData.getTitle(), ex.getMessage());
            return ScanResult.ERROR;
        }

        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(StringUtils.trim(moviedb.getTitle()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
            videoData.setPlot(StringUtils.trim(moviedb.getOverview()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
            videoData.setOutline(StringUtils.trim(moviedb.getOverview()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteTagline(videoData, SCANNER_ID)) {
            videoData.setOutline(StringUtils.trim(moviedb.getTagline()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteCountry(videoData, SCANNER_ID)) {
            if (CollectionUtils.isNotEmpty(moviedb.getProductionCountries())) {
                for (ProductionCountry country : moviedb.getProductionCountries()) {
                    // TODO more countries
                    videoData.setCountry(StringUtils.trimToNull(country.getName()), SCANNER_ID);
                    break;
                }
            }
        }

        String releaseDateString = moviedb.getReleaseDate();
        if (StringUtils.isNotBlank(releaseDateString) && !"1900-01-01".equals(releaseDateString)) {
            Date releaseDate = MetadataTools.parseToDate(releaseDateString);
            if (releaseDate != null) {
                if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                    videoData.setReleaseDate(releaseDate, SCANNER_ID);
                }
                if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
                    videoData.setPublicationYear(MetadataTools.extractYearAsInt(releaseDate), SCANNER_ID);
                }
            }
        }

        if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            if (CollectionUtils.isNotEmpty(moviedb.getGenres())) {
                Set<String> genreNames = new HashSet<String>();
                for (com.omertron.themoviedbapi.model.Genre genre : moviedb.getGenres()) {
                    if (StringUtils.isBlank(genre.getName())) {
                        genreNames.add(StringUtils.trim(genre.getName()));
                    }
                }
                videoData.setGenreNames(genreNames, SCANNER_ID);
            }
        }

        if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
            if (CollectionUtils.isNotEmpty(moviedb.getProductionCompanies())) {
                Set<String> studioNames = new HashSet<String>();
                for (ProductionCompany company : moviedb.getProductionCompanies()) {
                    if (StringUtils.isBlank(company.getName())) {
                        studioNames.add(StringUtils.trim(company.getName()));
                    }
                }
                videoData.setStudioNames(studioNames, SCANNER_ID);
            }
        }

        // CAST & CREW
        try {
            CreditDTO credit;
            for (com.omertron.themoviedbapi.model.Person person : tmdbApi.getMovieCasts(Integer.parseInt(tmdbID)).getResults()) {
                JobType jobType = this.retrieveJobType(person.getPersonType(), person.getDepartment());
                if (!this.configServiceWrapper.isCastScanEnabled(jobType)) {
                    // scan not enabled for that job
                    continue;
                }
                
                credit = new CreditDTO(SCANNER_ID);
                credit.addPersonId(SCANNER_ID, String.valueOf(person.getId()));
                credit.setName(person.getName());
                credit.setJobType(jobType);
                credit.setRole(person.getCharacter());
                if (person.getAka() != null && !person.getAka().isEmpty()) {
                    credit.setRealName(person.getAka().get(0));
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
        String id = person.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNotBlank(id)) {
            return id;
        }
        
        if (StringUtils.isNotBlank(person.getName())) {
            id = getPersonId(person.getName());
            person.setSourceDbId(SCANNER_ID, id);
        } else {
            LOG.error("No ID or Name found for {}", person.toString());
            id = StringUtils.EMPTY;
        }
        return id;
    }

    @Override
    public String getPersonId(String name) {
        String id = "";
        com.omertron.themoviedbapi.model.Person closestPerson = null;
        int closestMatch = Integer.MAX_VALUE;
        boolean foundPerson = Boolean.FALSE;
        boolean includeAdult = configServiceWrapper.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);

        try {
            TmdbResultsList<com.omertron.themoviedbapi.model.Person> results = tmdbApi.searchPeople(name, includeAdult, 0);
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
            LOG.warn("Failed to get information on '{}' from {}, error: {}", name, SCANNER_ID, ex.getMessage());
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
            LOG.debug("Getting information on {}-'{}' from {}", person.getId(), person.getName(), SCANNER_ID);
            com.omertron.themoviedbapi.model.Person tmdbPerson = tmdbApi.getPersonInfo(Integer.parseInt(id));

            person.setSourceDbId(ImdbScanner.SCANNER_ID, StringUtils.trim(tmdbPerson.getImdbId()));

            if (OverrideTools.checkOverwriteBirthDay(person, SCANNER_ID)) {
                Date parsedDate = MetadataTools.parseToDate(tmdbPerson.getBirthday());
                person.setBirthDay(parsedDate, SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteBirthPlace(person, SCANNER_ID)) {
                person.setBirthPlace(StringUtils.trimToNull(tmdbPerson.getBirthplace()), SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteBirthName(person, SCANNER_ID)) {
                if (CollectionUtils.isNotEmpty(tmdbPerson.getAka())) {
                    String birthName = tmdbPerson.getAka().get(0);
                    person.setBirthName(birthName, SCANNER_ID);
                }
            }

            if (OverrideTools.checkOverwriteDeathDay(person, SCANNER_ID)) {
                Date parsedDate = MetadataTools.parseToDate(tmdbPerson.getDeathday());
                person.setDeathDay(parsedDate, SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteBiography(person, SCANNER_ID)) {
                person.setBiography(cleanBiography(tmdbPerson.getBiography()), SCANNER_ID);
            }

        } catch (MovieDbException ex) {
            LOG.warn("Failed to get information on {}-'{}', error: {}", id, person.getName(), ex.getMessage());
            return ScanResult.ERROR;
        }

        LOG.debug("Successfully processed person: {}-'{}'", id, person.getName());
        return ScanResult.OK;
    }

    /**
     * Remove unneeded text from the biography
     *
     * @param bio
     * @return
     */
    private String cleanBiography(final String bio) {
        String newBio = StringUtils.trimToNull(bio);
        if (newBio == null) {
            return null;
        }

        newBio = newBio.replaceAll("\\s+", " ");

        int pos = StringUtils.indexOfIgnoreCase(newBio, FROM_WIKIPEDIA);
        if (pos >= 0) {
            // We've found the text, so remove it
            LOG.trace("Removing start wikipedia text from bio");
            newBio = newBio.substring(pos + FROM_WIKIPEDIA.length() + 1);
        }

        pos = StringUtils.indexOfIgnoreCase(newBio, WIKIPEDIA_DESCRIPTION_ABOVE);
        if (pos >= 0) {
            LOG.trace("Removing end wikipedia text from bio");
            newBio = newBio.substring(0, pos);
        }

        return newBio.trim();
    }


    @Override
    public boolean isFilmographyScanEnabled() {
        return configServiceWrapper.getBooleanProperty("themoviedb.person.filmography", false);
    }

    @Override
    public ScanResult scanFilmography(Person person) {
        String id = getPersonId(person);
        if (StringUtils.isBlank(id) || !StringUtils.isNumeric(id)) {
            return ScanResult.MISSING_ID;
        }
        
        try {
            TmdbResultsList<PersonCredit> credits = this.tmdbApi.getPersonCredits(Integer.parseInt(id));
            
            Set<FilmParticipation> newFilmography = new HashSet<FilmParticipation>();
            for (PersonCredit credit : credits.getResults()) {
                JobType jobType = this.retrieveJobType(credit.getPersonType(), credit.getDepartment());
                if (jobType == null) {
                    // job type must be present
                    continue;
                }
                Date releaseDate = MetadataTools.parseToDate(credit.getReleaseDate());
                if (releaseDate == null) {
                    // release date must be present
                    continue;
                }
                
                // NOTE: until now just movies possible; no TV credits
                FilmParticipation filmo = new FilmParticipation();
                filmo.setParticipationType(ParticipationType.MOVIE);
                filmo.setSourceDb(SCANNER_ID);
                filmo.setSourceDbId(String.valueOf(credit.getMovieId()));
                filmo.setPerson(person);
                filmo.setJobType(jobType);;
                if (JobType.ACTOR == jobType) {
                    filmo.setRole(credit.getCharacter());
                }
                filmo.setTitle(credit.getMovieTitle());
                filmo.setTitleOriginal(StringUtils.trimToNull(credit.getMovieOriginalTitle()));
                filmo.setReleaseDate(releaseDate);
                filmo.setYear(MetadataTools.extractYearAsInt(releaseDate));
                newFilmography.add(filmo);
                
            }
            person.setNewFilmography(newFilmography);
            
            return ScanResult.OK;
        } catch (MovieDbException ex) {
            LOG.error("Failed retrieving TheMovieDb filmography for '{}', error: {}", person.getName(), ex.getMessage());
            return ScanResult.ERROR;
        }
    }
    
    private JobType retrieveJobType(PersonType personType, String department) {
        if (personType == PersonType.CAST) {
            return JobType.ACTOR;
        }
        
        if (personType == PersonType.CREW) {
            if ("writing".equalsIgnoreCase(department)) {
                return JobType.WRITER;
            }
            if ("directing".equalsIgnoreCase(department)) {
                return JobType.DIRECTOR;
            }
            if ("production".equalsIgnoreCase(department)) {
                return JobType.PRODUCER;
            }
            if ("sound".equalsIgnoreCase(department)) {
                return JobType.SOUND;
            }
            if ("camera".equalsIgnoreCase(department)) {
                return JobType.CAMERA;
            }
            if ("art".equalsIgnoreCase(department)) {
                return JobType.ART;
            }
            if ("editing".equalsIgnoreCase(department)) {
                return JobType.EDITING;
            }
            if ("costume & make-up".equalsIgnoreCase(department)) {
                return JobType.COSTUME_MAKEUP;
            }
            if ("crew".equalsIgnoreCase(department)) {
                return JobType.CREW;
            }
            if ("visual effects".equalsIgnoreCase(department)) {
                return JobType.EFFECTS;
            }
            if ("lighting".equalsIgnoreCase(department)) {
                return JobType.LIGHTING;
            }

            LOG.debug("Unknown department '{}'", department);
            return JobType.UNKNOWN;
        }

        LOG.debug("Unknown person type: '{}'", personType);
        return null;
    }

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return Boolean.TRUE;
        }
        
        LOG.trace("Scanning NFO for TheMovieDb ID");

        try {
            int beginIndex = nfoContent.indexOf("/movie/");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$");
                String sourceId = st.nextToken();
                LOG.debug("Allocine ID found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return Boolean.TRUE;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }
        
        LOG.debug("No TheMovieDb ID found in NFO");
        return Boolean.FALSE;
    }
}
