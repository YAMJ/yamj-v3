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

import com.omertron.themoviedbapi.model.collection.Collection;
import com.omertron.themoviedbapi.model.credits.*;
import com.omertron.themoviedbapi.model.media.MediaCreditList;
import com.omertron.themoviedbapi.model.movie.*;
import com.omertron.themoviedbapi.model.person.PersonCreditList;
import com.omertron.themoviedbapi.model.person.PersonInfo;
import com.omertron.themoviedbapi.model.tv.TVEpisodeInfo;
import com.omertron.themoviedbapi.model.tv.TVInfo;
import com.omertron.themoviedbapi.model.tv.TVSeasonInfo;
import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.database.model.type.ParticipationType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.tools.PersonNameDTO;
import org.yamj.core.web.apis.TheMovieDbApiWrapper;

@Service("tmdbScanner")
public class TheMovieDbScanner implements IMovieScanner, ISeriesScanner, IPersonScanner, IFilmographyScanner {

    public static final String SCANNER_ID = "tmdb";
    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbScanner.class);
                    
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private TheMovieDbApiWrapper tmdbApiWrapper;
    @Autowired
    private LocaleService localeService;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.trace("Initialize TheMovieDb scanner");

        // register this scanner
        onlineScannerService.registerMetadataScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        // locale for TMDb
        final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");

        return getMovieId(videoData, tmdbLocale, false);
    }

    private String getMovieId(VideoData videoData, Locale tmdbLocale, boolean throwTempError) {
        String tmdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(tmdbId)) {
            return tmdbId;
        }

        String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isNotBlank(imdbId)) {
            // Search based on IMDb ID
            LOG.debug("Using IMDb id {} for '{}'", imdbId, videoData.getTitle());
            MovieInfo movieInfo = tmdbApiWrapper.getMovieInfoByIMDB(imdbId, tmdbLocale, throwTempError);
            if (movieInfo != null && movieInfo.getId() > 0) {
                tmdbId = String.valueOf(movieInfo.getId());
            }
        }

        if (!StringUtils.isNumeric(tmdbId)) {
            LOG.debug("No TMDb id found for '{}', searching title with year {}", videoData.getTitle(), videoData.getPublicationYear());
            tmdbId = tmdbApiWrapper.getMovieId(videoData.getTitle(), videoData.getPublicationYear(), tmdbLocale, throwTempError);
        }

        if (!StringUtils.isNumeric(tmdbId) && videoData.isTitleOriginalScannable()) {
            LOG.debug("No TMDb id found for '{}', searching original title with year {}", videoData.getTitleOriginal(), videoData.getPublicationYear());
            tmdbId = tmdbApiWrapper.getMovieId(videoData.getTitleOriginal(), videoData.getPublicationYear(), tmdbLocale, throwTempError);
        }

        if (StringUtils.isNumeric(tmdbId)) {
            videoData.setSourceDbId(SCANNER_ID, tmdbId);
            return tmdbId;
        }

        return null;
    }

    @Override
    public String getSeriesId(Series series) {
        final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");
        return getSeriesId(series, tmdbLocale, false);
    }

    private String getSeriesId(Series series, Locale tmdbLocale, boolean throwTempError) {
        String tmdbId = series.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(tmdbId)) {
            return tmdbId;
        }

        LOG.debug("No TMDb id found for '{}', searching title with year {}", series.getTitle(), series.getStartYear());
        tmdbId = tmdbApiWrapper.getSeriesId(series.getTitle(), series.getStartYear(), tmdbLocale, throwTempError);

        if (!StringUtils.isNumeric(tmdbId) && StringUtils.isNotBlank(series.getTitleOriginal())) {
            LOG.debug("No TMDb id found for '{}', searching original title with year {}", series.getTitleOriginal(), series.getStartYear());
            tmdbId = tmdbApiWrapper.getMovieId(series.getTitleOriginal(), series.getStartYear(), tmdbLocale, throwTempError);
        }

        if (StringUtils.isNumeric(tmdbId)) {
            series.setSourceDbId(SCANNER_ID, tmdbId);
            return tmdbId;
        }

        return null;
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
    public ScanResult scanMovie(VideoData videoData, boolean throwTempError) {
        final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");
        
        // get movie id
        String tmdbId = getMovieId(videoData, tmdbLocale, throwTempError);
        if (!StringUtils.isNumeric(tmdbId)) {
            LOG.debug("TMDb id not available '{}'", videoData.getIdentifier());
            return ScanResult.MISSING_ID;
        }

        // get movie info
        MovieInfo movieInfo = tmdbApiWrapper.getMovieInfoByTMDB(Integer.parseInt(tmdbId), tmdbLocale, throwTempError);
        if (movieInfo == null || movieInfo.getId() <= 0) {
            LOG.error("Can't find informations for movie '{}'", videoData.getIdentifier());
            return ScanResult.NO_RESULT;
        }
                        
        // set IMDb id if not set before
        String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = StringUtils.trim(movieInfo.getImdbID());
            videoData.setSourceDbId(ImdbScanner.SCANNER_ID, imdbId);
        }

        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(movieInfo.getTitle(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
            videoData.setPlot(movieInfo.getOverview(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
            videoData.setOutline(movieInfo.getOverview(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteTagline(videoData, SCANNER_ID)) {
            videoData.setTagline(movieInfo.getTagline(), SCANNER_ID);
        }
        
        videoData.addRating(SCANNER_ID, MetadataTools.parseRating(movieInfo.getVoteAverage()));

        // RELEASE DATE
        Date releaseDate = null;
        String releaseCountryCode = null;
        if (CollectionUtils.isNotEmpty(movieInfo.getReleases())) {
            for (ReleaseInfo releaseInfo : movieInfo.getReleases()) {
                if (tmdbLocale.getCountry().equalsIgnoreCase(releaseInfo.getCountry())) {
                    releaseDate = parseTMDbDate(releaseInfo.getReleaseDate());
                    if (releaseDate != null) {
                        releaseCountryCode = releaseInfo.getCountry();
                        break;
                    }
                }
            }
            if (releaseDate == null) {
                // use primary release date
                for (ReleaseInfo releaseInfo : movieInfo.getReleases()) {
                    if (releaseInfo.isPrimary()) {
                        releaseDate = parseTMDbDate(releaseInfo.getReleaseDate());
                        if (releaseDate != null) {
                            releaseCountryCode = releaseInfo.getCountry();
                        }
                        break;
                    }
                }
            }
        }
        if (releaseDate == null) {
            releaseDate = parseTMDbDate(movieInfo.getReleaseDate());
        }
        if (releaseDate != null && OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
            videoData.setRelease(releaseCountryCode, releaseDate, SCANNER_ID);
        }
        
        if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
            videoData.setPublicationYear(MetadataTools.extractYearAsInt(movieInfo.getReleaseDate()), SCANNER_ID);
        }

        // CERTIFICATIONS
        if (CollectionUtils.isNotEmpty(movieInfo.getReleases())) {
            for (String countryCode : localeService.getCertificationCountryCodes(tmdbLocale)) {
                for (ReleaseInfo releaseInfo : movieInfo.getReleases()) {
                    if (countryCode.equalsIgnoreCase(releaseInfo.getCountry())) {
                        videoData.addCertificationInfo(countryCode, releaseInfo.getCertification());
                        break;
                    }
                }
            }
        }
        
        // COUNTRIES
        if (CollectionUtils.isNotEmpty(movieInfo.getProductionCountries()) && OverrideTools.checkOverwriteCountries(videoData, SCANNER_ID)) {
            final Set<String> countryCodes = new HashSet<>(movieInfo.getProductionCountries().size());
            for (ProductionCountry country : movieInfo.getProductionCountries()) {
                countryCodes.add(country.getCountry());
            }
            videoData.setCountryCodes(countryCodes, SCANNER_ID);
        }

        // GENRES
        if (CollectionUtils.isNotEmpty(movieInfo.getGenres()) && OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            final Set<String> genreNames = new HashSet<>(movieInfo.getGenres().size());
            for (com.omertron.themoviedbapi.model.Genre genre : movieInfo.getGenres()) {
                if (StringUtils.isNotBlank(genre.getName())) {
                    genreNames.add(StringUtils.trim(genre.getName()));
                }
            }
            videoData.setGenreNames(genreNames, SCANNER_ID);
        }

        // COMPANIES
        if (CollectionUtils.isNotEmpty(movieInfo.getProductionCompanies()) && OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
            final Set<String> studioNames = new HashSet<>(movieInfo.getProductionCompanies().size());
            for (ProductionCompany company : movieInfo.getProductionCompanies()) {
                if (StringUtils.isNotBlank(company.getName())) {
                    studioNames.add(StringUtils.trim(company.getName()));
                }
            }
            videoData.setStudioNames(studioNames, SCANNER_ID);
        }

        // CAST
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            boolean skipUncredited = configServiceWrapper.getBooleanProperty("yamj3.castcrew.skip.uncredited", true);
            
            for (MediaCreditCast person : movieInfo.getCast()) {
                // skip person without credit
                if (skipUncredited && StringUtils.indexOf(person.getCharacter(), "uncredited") > 0) {
                    continue;
                }
                
                CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), JobType.ACTOR, person.getName(), person.getCharacter());
                videoData.addCreditDTO(credit);
            }
        }

        // CREW
        for (MediaCreditCrew person : movieInfo.getCrew()) {
            JobType jobType = retrieveJobType(person.getName(), person.getDepartment());
            if (!this.configServiceWrapper.isCastScanEnabled(jobType)) {
                // scan not enabled for that job
                continue;
            }

            CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), jobType, person.getName(), person.getJob());
            videoData.addCreditDTO(credit);
        }

        // store collection as boxed set
        if (this.configServiceWrapper.getBooleanProperty("themoviedb.include.collection", false)) {
            Collection collection = movieInfo.getBelongsToCollection();
            if (collection != null && collection.getName() != null) {
                videoData.addBoxedSetDTO(SCANNER_ID, collection.getName(), Integer.valueOf(-1), Integer.toString(collection.getId()));
            }
        }
        
        return ScanResult.OK;
    }
    
    @Override
    public ScanResult scanSeries(Series series, boolean throwTempError) { //NOSONAR
        final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");

        // get series id
        String tmdbId = getSeriesId(series, tmdbLocale, throwTempError);
        if (!StringUtils.isNumeric(tmdbId)) {
            LOG.debug("TMDb id not available '{}'", series.getIdentifier());
            return ScanResult.MISSING_ID;
        }

        // get series info
        TVInfo tvInfo = tmdbApiWrapper.getSeriesInfo(Integer.parseInt(tmdbId), tmdbLocale, throwTempError);
        if (tvInfo == null || tvInfo.getId() <= 0) {
            LOG.error("Can't find informations for series '{}'", series.getIdentifier());
            return ScanResult.NO_RESULT;
        }

        // set external IDS if not set before
        if (tvInfo.getExternalIDs() != null) {
            if (StringUtils.isBlank(series.getSourceDbId(ImdbScanner.SCANNER_ID))) {
                series.setSourceDbId(ImdbScanner.SCANNER_ID, tvInfo.getExternalIDs().getImdbId());
            }
            if (StringUtils.isBlank(series.getSourceDbId(TheTVDbScanner.SCANNER_ID))) {
                series.setSourceDbId(TheTVDbScanner.SCANNER_ID, tvInfo.getExternalIDs().getTvdbId());
            }
            if (StringUtils.isBlank(series.getSourceDbId(TVRageScanner.SCANNER_ID))) {
                series.setSourceDbId(TVRageScanner.SCANNER_ID, tvInfo.getExternalIDs().getTvrageId());
            }
        }
        
        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            series.setTitleOriginal(tvInfo.getName(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(series, SCANNER_ID)) {
            series.setTitleOriginal(tvInfo.getOriginalName(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(tvInfo.getOverview(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(tvInfo.getOverview(), SCANNER_ID);
        }

        if (tvInfo.getVoteAverage() > 0) {
            series.addRating(SCANNER_ID, MetadataTools.parseRating(tvInfo.getVoteAverage()));
        }

        if (CollectionUtils.isNotEmpty(tvInfo.getOriginCountry()) && OverrideTools.checkOverwriteCountries(series, SCANNER_ID)) {
            Set<String> countryCodes = new HashSet<>();
            for (String country : tvInfo.getOriginCountry()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) {
                    countryCodes.add(country);
                }
            }
            series.setCountryCodes(countryCodes, SCANNER_ID);
        }

        if (CollectionUtils.isNotEmpty(tvInfo.getGenres()) && OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            final Set<String> genreNames = new HashSet<>(tvInfo.getGenres().size());
            for (com.omertron.themoviedbapi.model.Genre genre :  tvInfo.getGenres()) {
                genreNames.add(genre.getName());
            }
            series.setGenreNames(genreNames, SCANNER_ID);
        }
        
        if (CollectionUtils.isNotEmpty(tvInfo.getProductionCompanies()) && OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            final Set<String> studioNames = new HashSet<>(tvInfo.getProductionCompanies().size());
            for (ProductionCompany company : tvInfo.getProductionCompanies()) {
                studioNames.add(company.getName());
            }
            series.setStudioNames(studioNames, SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
            // first air date
            Date date = parseTMDbDate(tvInfo.getFirstAirDate());
            series.setStartYear(MetadataTools.extractYearAsInt(date), SCANNER_ID);
            // last air date
            date = parseTMDbDate(tvInfo.getLastAirDate());
            series.setEndYear(MetadataTools.extractYearAsInt(date), SCANNER_ID);
        }

        // SCAN SEASONS
        scanSeasons(series, tvInfo, tmdbLocale);
        
        return ScanResult.OK;
    }
    
    private void scanSeasons(Series series, TVInfo tvInfo, Locale tmdbLocale) {
        
        for (Season season : series.getSeasons()) {
            
            if (!season.isTvSeasonDone(SCANNER_ID)) {
                final String seriesId = series.getSourceDbId(SCANNER_ID);
                TVSeasonInfo seasonInfo = tmdbApiWrapper.getSeasonInfo(seriesId, season.getSeason(), tmdbLocale);
                
                if (seasonInfo == null || seasonInfo.getId() <= 0) {
                    // mark season as not found
                    season.removeOverrideSource(SCANNER_ID);
                    season.removeSourceDbId(SCANNER_ID);
                    season.setTvSeasonNotFound();
                } else {
                    // set source id
                    season.setSourceDbId(SCANNER_ID, String.valueOf(seasonInfo.getId()));
                    
                    if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                        season.setTitle(seasonInfo.getName(), SCANNER_ID);
                    }
                    if (OverrideTools.checkOverwriteOriginalTitle(season, SCANNER_ID)) {
                        season.setTitle(tvInfo.getOriginalName(), SCANNER_ID);
                    }
                    if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                        season.setPlot(seasonInfo.getOverview(), SCANNER_ID);
                    }
                    if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                        season.setOutline(seasonInfo.getOverview(), SCANNER_ID);
                    }
        
                    if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                        final Date date = parseTMDbDate(seasonInfo.getAirDate());
                        season.setPublicationYear(MetadataTools.extractYearAsInt(date), SCANNER_ID);
                    }
        
                    // mark season as done
                    season.setTvSeasonDone();
                }
            }
            
            // scan episodes
            scanEpisodes(season, tmdbLocale);
        }
    }
    
    private void scanEpisodes(Season season, Locale tmdbLocale) {
        for (VideoData videoData : season.getVideoDatas()) {
            
            if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                // nothing to do if already done
                continue;
            }

            // get the episode
            String seriesId = videoData.getSeason().getSeries().getSourceDbId(SCANNER_ID);
            TVEpisodeInfo episodeInfo = tmdbApiWrapper.getEpisodeInfo(seriesId, season.getSeason(), videoData.getEpisode(), tmdbLocale);
            if (episodeInfo == null || episodeInfo.getId() <= 0) {
                // mark episode as not found
                videoData.removeOverrideSource(SCANNER_ID);
                videoData.removeSourceDbId(SCANNER_ID);
                videoData.setTvEpisodeNotFound();
                continue;
            }
            
            // set source id
            videoData.setSourceDbId(SCANNER_ID, String.valueOf(episodeInfo.getId()));

            // set external IDS if not set before
            if (episodeInfo.getExternalIDs() != null) {
                if (StringUtils.isBlank(videoData.getSourceDbId(ImdbScanner.SCANNER_ID))) {
                    videoData.setSourceDbId(ImdbScanner.SCANNER_ID, episodeInfo.getExternalIDs().getImdbId());
                }
                if (StringUtils.isBlank(videoData.getSourceDbId(TheTVDbScanner.SCANNER_ID))) {
                    videoData.setSourceDbId(TheTVDbScanner.SCANNER_ID, episodeInfo.getExternalIDs().getTvdbId());
                }
                if (StringUtils.isBlank(videoData.getSourceDbId(TVRageScanner.SCANNER_ID))) {
                    videoData.setSourceDbId(TVRageScanner.SCANNER_ID, episodeInfo.getExternalIDs().getTvrageId());
                }
            }

            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                videoData.setTitle(episodeInfo.getName(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                videoData.setPlot(episodeInfo.getOverview(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                videoData.setOutline(episodeInfo.getOverview(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                Date releaseDate = parseTMDbDate(episodeInfo.getAirDate());
                videoData.setRelease(releaseDate, SCANNER_ID);
            }

            videoData.addRating(SCANNER_ID, MetadataTools.parseRating(episodeInfo.getVoteAverage()));
            
            // CAST & CREW
            MediaCreditList credits = episodeInfo.getCredits();
            if (credits != null) {

                if (CollectionUtils.isNotEmpty(credits.getCast()) && this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
                    for (MediaCreditCast person : credits.getCast()) {
                        CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), JobType.ACTOR, person.getName(), person.getCharacter());
                        videoData.addCreditDTO(credit);
                    }
                }
            
                // GUEST STARS
                if (CollectionUtils.isNotEmpty(credits.getGuestStars()) && this.configServiceWrapper.isCastScanEnabled(JobType.GUEST_STAR)) {
                    for (MediaCreditCast person : credits.getGuestStars()) {
                        CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), JobType.GUEST_STAR, person.getName(), person.getCharacter());
                        videoData.addCreditDTO(credit);
                    }
                }
            
                // CREW
                if (CollectionUtils.isNotEmpty(credits.getCrew())) {
                    for (MediaCreditCrew person : credits.getCrew()) {
                        final JobType jobType = retrieveJobType(person.getName(), person.getDepartment());
                        if (!this.configServiceWrapper.isCastScanEnabled(jobType)) {
                            // scan not enabled for that job
                            continue;
                        }
                        CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), jobType, person.getName(), person.getJob());
                        videoData.addCreditDTO(credit);
                    }
                }
            }

            // mark episode as done
            videoData.setTvEpisodeDone();
        }
    }
    
    @Override
    public ScanResult scanPerson(Person person, boolean throwTempError) {
        // get person id
        String tmdbId = getPersonId(person, throwTempError);
        if (!StringUtils.isNumeric(tmdbId)) {
            LOG.debug("TMDb id not available '{}'", person.getIdentifier());
            return ScanResult.MISSING_ID;
        }

        // get person info
        PersonInfo tmdbPerson = tmdbApiWrapper.getPersonInfo(Integer.parseInt(tmdbId), throwTempError);
        if (tmdbPerson == null || tmdbPerson.getId() <= 0) {
            LOG.error("Can't find information for person '{}'", person.getIdentifier());
            return ScanResult.NO_RESULT;
        }

        // fill in data
        person.setSourceDbId(ImdbScanner.SCANNER_ID, StringUtils.trim(tmdbPerson.getImdbId()));

        // split person names
        PersonNameDTO nameDTO = MetadataTools.splitFullName(tmdbPerson.getName());
        if (OverrideTools.checkOverwriteName(person, SCANNER_ID)) {
            person.setName(nameDTO.getName(), SCANNER_ID);
        }
        if (OverrideTools.checkOverwriteFirstName(person, SCANNER_ID)) {
            person.setFirstName(nameDTO.getFirstName(), SCANNER_ID);
        }
        if (OverrideTools.checkOverwriteLastName(person, SCANNER_ID)) {
            person.setLastName(nameDTO.getLastName(), SCANNER_ID);
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
            person.setBiography(MetadataTools.cleanBiography(tmdbPerson.getBiography()), SCANNER_ID);
        }

        return ScanResult.OK;
    }

    @Override
    public ScanResult scanFilmography(Person person, boolean throwTempError) {
        // get person id
        String tmdbId = getPersonId(person, throwTempError);
        if (!StringUtils.isNumeric(tmdbId)) {
            LOG.debug("TMDb id not available '{}'", person.getName());
            return ScanResult.MISSING_ID;
        }

        // get filmography
        final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");
        PersonCreditList<CreditBasic> credits = tmdbApiWrapper.getPersonCredits(Integer.parseInt(tmdbId), tmdbLocale, throwTempError);
        if (credits == null) {
            LOG.error("Can't find filmography for person '{}'", person.getName());
            return ScanResult.ERROR;
        } else if (CollectionUtils.isEmpty(credits.getCast())) {
            LOG.trace("No filmography present for person '{}'", person.getName());
            return ScanResult.NO_RESULT;
        }

        // Fill in cast data
        Set<FilmParticipation> newFilmography = new HashSet<>();
        for (CreditBasic credit : credits.getCast()) {
            FilmParticipation filmo = null;
            switch (credit.getMediaType()) {
                case MOVIE:
                    filmo = convertMovieCreditToFilm((CreditMovieBasic) credit, person, JobType.ACTOR);
                    break;
                case TV:
                    LOG.trace("TV credit information for {} ({}) not used: {}", person.getName(), JobType.ACTOR, credit.toString());
                    break;
                case EPISODE:
                    LOG.trace("TV Episode credit information for {} ({}) not used: {}", person.getName(), JobType.ACTOR, credit.toString());
                    break;
                default:
                    LOG.debug("Unknown media type '{}' for credit {}", credit.getMediaType(), credit.toString());
            }

            if (filmo != null) {
                newFilmography.add(filmo);
            }
        }

        // Fill in CREW data
        for (CreditBasic credit : credits.getCrew()) {
            final JobType jobType = retrieveJobType(person.getName(), credit.getDepartment());
            FilmParticipation filmo = null;
            switch (credit.getMediaType()) {
                case MOVIE:
                    filmo = convertMovieCreditToFilm((CreditMovieBasic) credit, person, jobType);
                    break;
                case TV:
                    LOG.trace("TV crew information for {} ({}) not used: {}", person.getName(), jobType, credit.toString());
                    break;
                case EPISODE:
                    LOG.trace("TV Episode crew information for {} ({}) not used: {}", person.getName(), jobType, credit.toString());
                    break;
                default:
                    LOG.debug("Unknown crew media type '{}' for credit {}", credit.getMediaType(), credit.toString());
            }

            if (filmo != null) {
                newFilmography.add(filmo);
            }
        }

        person.setNewFilmography(newFilmography);

        return ScanResult.OK;
    }

    private static FilmParticipation convertMovieCreditToFilm(CreditMovieBasic credit, Person person, JobType jobType) {
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
            filmo.setRole(MetadataTools.cleanRole(credit.getCharacter()));
            filmo.setVoiceRole(MetadataTools.isVoiceRole(credit.getCharacter()));
        }
        filmo.setTitle(credit.getTitle());
        filmo.setTitleOriginal(StringUtils.trimToNull(credit.getOriginalTitle()));
        filmo.setReleaseDate(releaseDate);
        filmo.setYear(MetadataTools.extractYearAsInt(releaseDate));
        return filmo;
    }

    private static JobType retrieveJobType(String personName, String department) { //NOSONAR
        if (StringUtils.isBlank(department)) {
            LOG.trace("No department found for person '{}'", personName);
            return JobType.UNKNOWN;
        }

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
                LOG.debug("Unknown department '{}' for person '{}'", department, personName);
                return JobType.UNKNOWN;
        }
    }

    private static Date parseTMDbDate(String date) {
        if (StringUtils.isNotBlank(date) && !"1900-01-01".equals(date)) {
            return MetadataTools.parseToDate(date);
        }
        return null;
    }
    
    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return true;
        }

        LOG.trace("Scanning NFO for TheMovieDb ID");

        try {
            int beginIndex = nfoContent.indexOf("/movie/");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$");
                String sourceId = st.nextToken();
                LOG.debug("TheMovieDb ID found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return true;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No TheMovieDb ID found in NFO");
        return false;
    }
}
