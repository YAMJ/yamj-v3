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

import com.omertron.themoviedbapi.model.credits.CreditBasic;
import com.omertron.themoviedbapi.model.credits.CreditMovieBasic;
import com.omertron.themoviedbapi.model.credits.MediaCreditCast;
import com.omertron.themoviedbapi.model.credits.MediaCreditCrew;
import com.omertron.themoviedbapi.model.media.MediaCreditList;
import com.omertron.themoviedbapi.model.movie.MovieInfo;
import com.omertron.themoviedbapi.model.movie.ProductionCompany;
import com.omertron.themoviedbapi.model.movie.ProductionCountry;
import com.omertron.themoviedbapi.model.person.PersonCreditList;
import com.omertron.themoviedbapi.model.person.PersonInfo;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.configuration.ConfigServiceWrapper;
import org.yamj.core.database.model.FilmParticipation;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.database.model.type.ParticipationType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.tools.PersonNameDTO;
import org.yamj.core.tools.web.TemporaryUnavailableException;

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
    private TheMovieDbApiWrapper tmdbApiWrapper;

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize TheMovieDb scanner");

        // register this scanner
        onlineScannerService.registerMovieScanner(this);
        onlineScannerService.registerPersonScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String tmdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(tmdbId)) {
            return tmdbId;
        }

        String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isNotBlank(imdbId)) {
            // Search based on IMDb ID
            LOG.debug("Using IMDb id {} for '{}'", imdbId, videoData.getTitle());
            MovieInfo movieDb = tmdbApiWrapper.getMovieInfoByIMDB(imdbId, throwTempError);
            if (movieDb != null && movieDb.getId() != 0) {
                tmdbId = String.valueOf(movieDb.getId());
            }
        }

        if (!StringUtils.isNumeric(tmdbId)) {
            LOG.debug("No TMDb id found for '{}', searching title with year {}", videoData.getTitle(), videoData.getPublicationYear());
            tmdbId = tmdbApiWrapper.getMovieDbId(videoData.getTitle(), videoData.getPublicationYear(), throwTempError);
            videoData.setSourceDbId(SCANNER_ID, tmdbId);
        }

        if (!StringUtils.isNumeric(tmdbId) && StringUtils.isNotBlank(videoData.getTitleOriginal())) {
            LOG.debug("No TMDb id found for '{}', searching original title with year {}", videoData.getTitleOriginal(), videoData.getPublicationYear());
            tmdbId = tmdbApiWrapper.getMovieDbId(videoData.getTitleOriginal(), videoData.getPublicationYear(), throwTempError);
            videoData.setSourceDbId(SCANNER_ID, tmdbId);
        }

        if (StringUtils.isNumeric(tmdbId)) {
            videoData.setSourceDbId(SCANNER_ID, tmdbId);
            return tmdbId;
        }

        return null;
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        MovieInfo movieDb = null;
        MediaCreditList movieCasts = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("themoviedb.throwError.tempUnavailable", Boolean.TRUE);
            String tmdbId = getMovieId(videoData, throwTempError);

            if (!StringUtils.isNumeric(tmdbId)) {
                LOG.debug("TMDb id not available '{}'", videoData.getTitle());
                return ScanResult.MISSING_ID;
            }

            movieDb = tmdbApiWrapper.getMovieInfoByTMDB(Integer.parseInt(tmdbId), throwTempError);
            movieCasts = tmdbApiWrapper.getMovieCredits(movieDb.getId(), throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("themoviedb.maxRetries.movie", 0);
            if (videoData.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }

        if (movieDb == null || movieCasts == null) {
            LOG.error("Can't find informations for movie '{}'", videoData.getTitle());
            return ScanResult.ERROR;
        }

        // fill in data
        videoData.setSourceDbId(ImdbScanner.SCANNER_ID, StringUtils.trim(movieDb.getImdbID()));

        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(StringUtils.trim(movieDb.getTitle()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
            videoData.setPlot(StringUtils.trim(movieDb.getOverview()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
            videoData.setOutline(StringUtils.trim(movieDb.getOverview()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteTagline(videoData, SCANNER_ID)) {
            videoData.setOutline(StringUtils.trim(movieDb.getTagline()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteCountries(videoData, SCANNER_ID)) {
            Set<String> countryNames = new LinkedHashSet<>();
            for (ProductionCountry country : movieDb.getProductionCountries()) {
                countryNames.add(country.getName());
            }
            videoData.setCountryNames(countryNames, SCANNER_ID);
        }

        String releaseDateString = movieDb.getReleaseDate();
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

        if (CollectionUtils.isNotEmpty(movieDb.getGenres())) {
            if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
                Set<String> genreNames = new HashSet<>();
                for (com.omertron.themoviedbapi.model.Genre genre : movieDb.getGenres()) {
                    if (StringUtils.isNotBlank(genre.getName())) {
                        genreNames.add(StringUtils.trim(genre.getName()));
                    }
                }
                videoData.setGenreNames(genreNames, SCANNER_ID);
            }
        }

        if (CollectionUtils.isNotEmpty(movieDb.getProductionCompanies())) {
            if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
                Set<String> studioNames = new HashSet<>();
                for (ProductionCompany company : movieDb.getProductionCompanies()) {
                    if (StringUtils.isNotBlank(company.getName())) {
                        studioNames.add(StringUtils.trim(company.getName()));
                    }
                }
                videoData.setStudioNames(studioNames, SCANNER_ID);
            }
        }

        // CAST
        if (!this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            for (MediaCreditCast person : movieCasts.getCast()) {
                CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), JobType.ACTOR, person.getName(), person.getCharacter());
                videoData.addCreditDTO(credit);
            }
        }

        // GUEST STARS
        if (!this.configServiceWrapper.isCastScanEnabled(JobType.GUEST_STAR)) {
            for (MediaCreditCast person : movieCasts.getGuestStars()) {
                CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), JobType.GUEST_STAR, person.getName(), person.getCharacter());
                videoData.addCreditDTO(credit);
            }
        }

        // CREW
        for (MediaCreditCrew person : movieCasts.getCrew()) {
            JobType jobType = retrieveJobType(person.getDepartment());
            if (!this.configServiceWrapper.isCastScanEnabled(jobType)) {
                // scan not enabled for that job
                continue;
            }

            CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), jobType, person.getName(), person.getJob());
            videoData.addCreditDTO(credit);
        }

        return ScanResult.OK;
    }

    @Override
    public String getPersonId(Person person) {
        return getPersonId(person, false);
    }

    private String getPersonId(Person person, boolean throwTempError) {
        String tmdbId = person.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(tmdbId)) {
            return tmdbId;
        }

        if (StringUtils.isNotBlank(person.getName())) {
            tmdbId = tmdbApiWrapper.getPersonId(person.getName(), throwTempError);
            person.setSourceDbId(SCANNER_ID, tmdbId);
        }

        return tmdbId;
    }

    @Override
    public ScanResult scan(Person person) {
        PersonInfo tmdbPerson = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("themoviedb.throwError.tempUnavailable", Boolean.TRUE);
            String tmdbId = getPersonId(person, throwTempError);

            if (!StringUtils.isNumeric(tmdbId)) {
                LOG.debug("TMDb id not available '{}'", person.getName());
                return ScanResult.MISSING_ID;
            }

            tmdbPerson = tmdbApiWrapper.getPersonInfo(Integer.parseInt(tmdbId), throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("themoviedb.maxRetries.person", 0);
            if (person.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }

        if (tmdbPerson == null) {
            LOG.error("Can't find information for person '{}'", person.getName());
            return ScanResult.ERROR;
        }

        // fill in data
        person.setSourceDbId(ImdbScanner.SCANNER_ID, StringUtils.trim(tmdbPerson.getImdbId()));

        if (OverrideTools.checkOverwritePersonNames(person, SCANNER_ID)) {
            // split person names
            PersonNameDTO nameDTO = MetadataTools.splitFullName(tmdbPerson.getName());
            if (OverrideTools.checkOverwriteName(person, SCANNER_ID)) {
                person.setName(nameDTO.getName(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteName(person, SCANNER_ID)) {
                person.setFirstName(nameDTO.getFirstName(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteLastName(person, SCANNER_ID)) {
                person.setLastName(nameDTO.getLastName(), SCANNER_ID);
            }
        }

        if (OverrideTools.checkOverwriteBirthDay(person, SCANNER_ID)) {
            Date parsedDate = MetadataTools.parseToDate(tmdbPerson.getBirthday());
            person.setBirthDay(parsedDate, SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteBirthPlace(person, SCANNER_ID)) {
            person.setBirthPlace(StringUtils.trimToNull(tmdbPerson.getPlaceOfBirth()), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteBirthName(person, SCANNER_ID)) {
            if (CollectionUtils.isNotEmpty(tmdbPerson.getAlsoKnownAs())) {
                String birthName = tmdbPerson.getAlsoKnownAs().get(0);
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

        return ScanResult.OK;
    }

    /**
     * Remove unneeded text from the biography
     *
     * @param bio
     * @return
     */
    private static String cleanBiography(final String bio) {
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
        PersonCreditList<CreditBasic> credits = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("themoviedb.throwError.tempUnavailable", Boolean.TRUE);
            String tmdbId = getPersonId(person, throwTempError);

            if (!StringUtils.isNumeric(tmdbId)) {
                LOG.debug("TMDb id not available '{}'", person.getName());
                return ScanResult.MISSING_ID;
            }

            credits = tmdbApiWrapper.getPersonCredits(Integer.parseInt(tmdbId), throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("themoviedb.maxRetries.filmography", 0);
            if (person.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }

        if (credits == null) {
            LOG.error("Can't find filmography for person '{}'", person.getName());
            return ScanResult.ERROR;
        }

        // fill in data
        Set<FilmParticipation> newFilmography = new HashSet<>();
        for (CreditBasic credit : credits.getCast()) {
            JobType jobType = retrieveJobType(credit.getDepartment());
            if (jobType == null) {
                // job type must be present
                continue;
            }

            FilmParticipation filmo = null;
            switch (credit.getMediaType()) {
                case MOVIE:
                    filmo = convertMovieCreditToFilm((CreditMovieBasic) credit, person, jobType);
                    break;
                case TV:
                    LOG.debug("TV credit information for {} ({}) not used: {}", person.getName(), jobType, credit.toString());
                    break;
                case EPISODE:
                    LOG.debug("TV Episode credit information for {} ({}) not used: {}", person.getName(), jobType, credit.toString());
                    break;
                default:
                    LOG.debug("Unknown media type '{}' for credit {}", credit.getMediaType(), credit.toString());
            }

            if (filmo != null) {
                newFilmography.add(filmo);
            }
        }
        person.setNewFilmography(newFilmography);

        return ScanResult.OK;
    }

    private FilmParticipation convertMovieCreditToFilm(CreditMovieBasic credit, Person person, JobType jobType) {
        Date releaseDate = MetadataTools.parseToDate(credit.getReleaseDate());
        if (releaseDate == null) {
            // release date must be present
            return null;
        }

        FilmParticipation filmo = new FilmParticipation();
        filmo.setParticipationType(ParticipationType.MOVIE);
        filmo.setSourceDb(SCANNER_ID);
        filmo.setSourceDbId(String.valueOf(credit.getId()));
        filmo.setPerson(person);
        filmo.setJobType(jobType);
        if (JobType.ACTOR == jobType) {
            filmo.setRole(credit.getCharacter());
        }
        filmo.setTitle(credit.getTitle());
        filmo.setTitleOriginal(StringUtils.trimToNull(credit.getOriginalTitle()));
        filmo.setReleaseDate(releaseDate);
        filmo.setYear(MetadataTools.extractYearAsInt(releaseDate));
        return filmo;
    }

    private static JobType retrieveJobType(String department) {
        switch (department.toLowerCase()) {
            case "writing":
                return JobType.WRITER;
            case "directing":
                return JobType.DIRECTOR;
            case "production":
                return JobType.PRODUCER;
            case "sound":
                return JobType.SOUND;
            case "camera":
                return JobType.CAMERA;
            case "art":
                return JobType.ART;
            case "editing":
                return JobType.EDITING;
            case "costume & make-up":
                return JobType.COSTUME_MAKEUP;
            case "crew":
                return JobType.CREW;
            case "visual effects":
                return JobType.EFFECTS;
            case "lighting":
                return JobType.LIGHTING;
            default:
                LOG.debug("Unknown department '{}'", department);
                return JobType.UNKNOWN;
        }
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
