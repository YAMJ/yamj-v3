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

import static org.yamj.plugin.api.common.Constants.*;

import org.yamj.plugin.api.type.ParticipationType;

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
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.web.apis.TheMovieDbApiWrapper;
import org.yamj.plugin.api.metadata.tools.MetadataTools;
import org.yamj.plugin.api.metadata.tools.PersonName;
import org.yamj.plugin.api.type.JobType;

@Service("tmdbScanner")
public class TheMovieDbScanner implements IMovieScanner, ISeriesScanner, IPersonScanner, IFilmographyScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbScanner.class);
    
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private TheMovieDbApiWrapper tmdbApiWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private IdentifierService identifierService;
    
    @Override
    public String getScannerName() {
        return SOURCE_TMDB;
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
        String tmdbId = videoData.getSourceDbId(SOURCE_TMDB);
        if (StringUtils.isNumeric(tmdbId)) {
            return tmdbId;
        }

        String imdbId = videoData.getSourceDbId(SOURCE_IMDB);
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
            videoData.setSourceDbId(SOURCE_TMDB, tmdbId);
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
        String tmdbId = series.getSourceDbId(SOURCE_TMDB);
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
            series.setSourceDbId(SOURCE_TMDB, tmdbId);
            return tmdbId;
        }

        return null;
    }

    @Override
    public String getPersonId(Person person) {
        return getPersonId(person, false);
    }

    private String getPersonId(Person person, boolean throwTempError) {
        String tmdbId = person.getSourceDbId(SOURCE_TMDB);
        if (StringUtils.isNumeric(tmdbId)) {
            return tmdbId;
        }

        if (StringUtils.isNotBlank(person.getName())) {
            tmdbId = tmdbApiWrapper.getPersonId(person.getName(), throwTempError);
            person.setSourceDbId(SOURCE_TMDB, tmdbId);
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
        String imdbId = videoData.getSourceDbId(SOURCE_IMDB);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = StringUtils.trim(movieInfo.getImdbID());
            videoData.setSourceDbId(SOURCE_IMDB, imdbId);
        }

        if (OverrideTools.checkOverwriteTitle(videoData, SOURCE_TMDB)) {
            videoData.setTitle(movieInfo.getTitle(), SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwritePlot(videoData, SOURCE_TMDB)) {
            videoData.setPlot(movieInfo.getOverview(), SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteOutline(videoData, SOURCE_TMDB)) {
            videoData.setOutline(movieInfo.getOverview(), SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteTagline(videoData, SOURCE_TMDB)) {
            videoData.setTagline(movieInfo.getTagline(), SOURCE_TMDB);
        }
        
        videoData.addRating(SOURCE_TMDB, MetadataTools.parseRating(movieInfo.getVoteAverage()));

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
        if (releaseDate != null && OverrideTools.checkOverwriteReleaseDate(videoData, SOURCE_TMDB)) {
            videoData.setRelease(releaseCountryCode, releaseDate, SOURCE_TMDB);
        }
        
        if (OverrideTools.checkOverwriteYear(videoData, SOURCE_TMDB)) {
            videoData.setPublicationYear(MetadataTools.extractYearAsInt(movieInfo.getReleaseDate()), SOURCE_TMDB);
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
        if (CollectionUtils.isNotEmpty(movieInfo.getProductionCountries()) && OverrideTools.checkOverwriteCountries(videoData, SOURCE_TMDB)) {
            final Set<String> countryCodes = new HashSet<>(movieInfo.getProductionCountries().size());
            for (ProductionCountry country : movieInfo.getProductionCountries()) {
                countryCodes.add(country.getCountry());
            }
            videoData.setCountryCodes(countryCodes, SOURCE_TMDB);
        }

        // GENRES
        if (CollectionUtils.isNotEmpty(movieInfo.getGenres()) && OverrideTools.checkOverwriteGenres(videoData, SOURCE_TMDB)) {
            final Set<String> genreNames = new HashSet<>(movieInfo.getGenres().size());
            for (com.omertron.themoviedbapi.model.Genre genre : movieInfo.getGenres()) {
                if (StringUtils.isNotBlank(genre.getName())) {
                    genreNames.add(StringUtils.trim(genre.getName()));
                }
            }
            videoData.setGenreNames(genreNames, SOURCE_TMDB);
        }

        // COMPANIES
        if (CollectionUtils.isNotEmpty(movieInfo.getProductionCompanies()) && OverrideTools.checkOverwriteStudios(videoData, SOURCE_TMDB)) {
            final Set<String> studioNames = new HashSet<>(movieInfo.getProductionCompanies().size());
            for (ProductionCompany company : movieInfo.getProductionCompanies()) {
                if (StringUtils.isNotBlank(company.getName())) {
                    studioNames.add(StringUtils.trim(company.getName()));
                }
            }
            videoData.setStudioNames(studioNames, SOURCE_TMDB);
        }

        // CAST
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            boolean skipUncredited = configServiceWrapper.getBooleanProperty("yamj3.castcrew.skip.uncredited", true);
            
            for (MediaCreditCast person : movieInfo.getCast()) {
                // skip person without credit
                if (skipUncredited && StringUtils.indexOf(person.getCharacter(), "uncredited") > 0) {
                    continue;
                }
                
                CreditDTO credit = this.identifierService.createCredit(SOURCE_TMDB, String.valueOf(person.getId()), JobType.ACTOR, person.getName(), person.getCharacter());
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

            CreditDTO credit = this.identifierService.createCredit(SOURCE_TMDB, String.valueOf(person.getId()), jobType, person.getName(), person.getJob());
            videoData.addCreditDTO(credit);
        }

        // store collection as boxed set
        if (this.configServiceWrapper.getBooleanProperty("themoviedb.include.collection", false)) {
            Collection collection = movieInfo.getBelongsToCollection();
            if (collection != null && collection.getName() != null) {
                final String boxedSetIdentifier = identifierService.cleanIdentifier(collection.getName());
                videoData.addBoxedSetDTO(SOURCE_TMDB, boxedSetIdentifier, collection.getName(), Integer.valueOf(-1), Integer.toString(collection.getId()));
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
            if (StringUtils.isBlank(series.getSourceDbId(SOURCE_IMDB))) {
                series.setSourceDbId(SOURCE_IMDB, tvInfo.getExternalIDs().getImdbId());
            }
            if (StringUtils.isBlank(series.getSourceDbId(SOURCE_TVDB))) {
                series.setSourceDbId(SOURCE_TVDB, tvInfo.getExternalIDs().getTvdbId());
            }
            if (StringUtils.isBlank(series.getSourceDbId(SOURCE_TVRAGE))) {
                series.setSourceDbId(SOURCE_TVRAGE, tvInfo.getExternalIDs().getTvrageId());
            }
        }
        
        if (OverrideTools.checkOverwriteTitle(series, SOURCE_TMDB)) {
            series.setTitleOriginal(tvInfo.getName(), SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(series, SOURCE_TMDB)) {
            series.setTitleOriginal(tvInfo.getOriginalName(), SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwritePlot(series, SOURCE_TMDB)) {
            series.setPlot(tvInfo.getOverview(), SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteOutline(series, SOURCE_TMDB)) {
            series.setOutline(tvInfo.getOverview(), SOURCE_TMDB);
        }

        if (tvInfo.getVoteAverage() > 0) {
            series.addRating(SOURCE_TMDB, MetadataTools.parseRating(tvInfo.getVoteAverage()));
        }

        if (CollectionUtils.isNotEmpty(tvInfo.getOriginCountry()) && OverrideTools.checkOverwriteCountries(series, SOURCE_TMDB)) {
            Set<String> countryCodes = new HashSet<>();
            for (String country : tvInfo.getOriginCountry()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) {
                    countryCodes.add(country);
                }
            }
            series.setCountryCodes(countryCodes, SOURCE_TMDB);
        }

        if (CollectionUtils.isNotEmpty(tvInfo.getGenres()) && OverrideTools.checkOverwriteGenres(series, SOURCE_TMDB)) {
            final Set<String> genreNames = new HashSet<>(tvInfo.getGenres().size());
            for (com.omertron.themoviedbapi.model.Genre genre :  tvInfo.getGenres()) {
                genreNames.add(genre.getName());
            }
            series.setGenreNames(genreNames, SOURCE_TMDB);
        }
        
        if (CollectionUtils.isNotEmpty(tvInfo.getProductionCompanies()) && OverrideTools.checkOverwriteStudios(series, SOURCE_TMDB)) {
            final Set<String> studioNames = new HashSet<>(tvInfo.getProductionCompanies().size());
            for (ProductionCompany company : tvInfo.getProductionCompanies()) {
                studioNames.add(company.getName());
            }
            series.setStudioNames(studioNames, SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteYear(series, SOURCE_TMDB)) {
            // first air date
            Date date = parseTMDbDate(tvInfo.getFirstAirDate());
            series.setStartYear(MetadataTools.extractYearAsInt(date), SOURCE_TMDB);
            // last air date
            date = parseTMDbDate(tvInfo.getLastAirDate());
            series.setEndYear(MetadataTools.extractYearAsInt(date), SOURCE_TMDB);
        }

        // SCAN SEASONS
        scanSeasons(series, tvInfo, tmdbLocale);
        
        return ScanResult.OK;
    }
    
    private void scanSeasons(Series series, TVInfo tvInfo, Locale tmdbLocale) {
        
        for (Season season : series.getSeasons()) {
            
            if (!season.isTvSeasonDone(SOURCE_TMDB)) {
                final String seriesId = series.getSourceDbId(SOURCE_TMDB);
                TVSeasonInfo seasonInfo = tmdbApiWrapper.getSeasonInfo(seriesId, season.getSeason(), tmdbLocale);
                
                if (seasonInfo == null || seasonInfo.getId() <= 0) {
                    // mark season as not found
                    season.removeOverrideSource(SOURCE_TMDB);
                    season.removeSourceDbId(SOURCE_TMDB);
                    season.setTvSeasonNotFound();
                } else {
                    // set source id
                    season.setSourceDbId(SOURCE_TMDB, String.valueOf(seasonInfo.getId()));
                    
                    if (OverrideTools.checkOverwriteTitle(season, SOURCE_TMDB)) {
                        season.setTitle(seasonInfo.getName(), SOURCE_TMDB);
                    }
                    if (OverrideTools.checkOverwriteOriginalTitle(season, SOURCE_TMDB)) {
                        season.setTitle(tvInfo.getOriginalName(), SOURCE_TMDB);
                    }
                    if (OverrideTools.checkOverwritePlot(season, SOURCE_TMDB)) {
                        season.setPlot(seasonInfo.getOverview(), SOURCE_TMDB);
                    }
                    if (OverrideTools.checkOverwriteOutline(season, SOURCE_TMDB)) {
                        season.setOutline(seasonInfo.getOverview(), SOURCE_TMDB);
                    }
        
                    if (OverrideTools.checkOverwriteYear(season, SOURCE_TMDB)) {
                        final Date date = parseTMDbDate(seasonInfo.getAirDate());
                        season.setPublicationYear(MetadataTools.extractYearAsInt(date), SOURCE_TMDB);
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
            
            if (videoData.isTvEpisodeDone(SOURCE_TMDB)) {
                // nothing to do if already done
                continue;
            }

            // get the episode
            String seriesId = videoData.getSeason().getSeries().getSourceDbId(SOURCE_TMDB);
            TVEpisodeInfo episodeInfo = tmdbApiWrapper.getEpisodeInfo(seriesId, season.getSeason(), videoData.getEpisode(), tmdbLocale);
            if (episodeInfo == null || episodeInfo.getId() <= 0) {
                // mark episode as not found
                videoData.removeOverrideSource(SOURCE_TMDB);
                videoData.removeSourceDbId(SOURCE_TMDB);
                videoData.setTvEpisodeNotFound();
                continue;
            }
            
            // set source id
            videoData.setSourceDbId(SOURCE_TMDB, String.valueOf(episodeInfo.getId()));

            // set external IDS if not set before
            if (episodeInfo.getExternalIDs() != null) {
                if (StringUtils.isBlank(videoData.getSourceDbId(SOURCE_IMDB))) {
                    videoData.setSourceDbId(SOURCE_IMDB, episodeInfo.getExternalIDs().getImdbId());
                }
                if (StringUtils.isBlank(videoData.getSourceDbId(SOURCE_TVDB))) {
                    videoData.setSourceDbId(SOURCE_TVDB, episodeInfo.getExternalIDs().getTvdbId());
                }
                if (StringUtils.isBlank(videoData.getSourceDbId(SOURCE_TVRAGE))) {
                    videoData.setSourceDbId(SOURCE_TVRAGE, episodeInfo.getExternalIDs().getTvrageId());
                }
            }

            if (OverrideTools.checkOverwriteTitle(videoData, SOURCE_TMDB)) {
                videoData.setTitle(episodeInfo.getName(), SOURCE_TMDB);
            }

            if (OverrideTools.checkOverwritePlot(videoData, SOURCE_TMDB)) {
                videoData.setPlot(episodeInfo.getOverview(), SOURCE_TMDB);
            }

            if (OverrideTools.checkOverwriteOutline(videoData, SOURCE_TMDB)) {
                videoData.setOutline(episodeInfo.getOverview(), SOURCE_TMDB);
            }
            
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SOURCE_TMDB)) {
                Date releaseDate = parseTMDbDate(episodeInfo.getAirDate());
                videoData.setRelease(releaseDate, SOURCE_TMDB);
            }

            videoData.addRating(SOURCE_TMDB, MetadataTools.parseRating(episodeInfo.getVoteAverage()));
            
            // CAST & CREW
            MediaCreditList credits = episodeInfo.getCredits();
            if (credits != null) {

                if (CollectionUtils.isNotEmpty(credits.getCast()) && this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
                    for (MediaCreditCast person : credits.getCast()) {
                        CreditDTO credit = this.identifierService.createCredit(SOURCE_TMDB, String.valueOf(person.getId()), JobType.ACTOR, person.getName(), person.getCharacter());
                        videoData.addCreditDTO(credit);
                    }
                }
            
                // GUEST STARS
                if (CollectionUtils.isNotEmpty(credits.getGuestStars()) && this.configServiceWrapper.isCastScanEnabled(JobType.GUEST_STAR)) {
                    for (MediaCreditCast person : credits.getGuestStars()) {
                        CreditDTO credit = this.identifierService.createCredit(SOURCE_TMDB, String.valueOf(person.getId()), JobType.GUEST_STAR, person.getName(), person.getCharacter());
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
                        
                        CreditDTO credit = this.identifierService.createCredit(SOURCE_TMDB, String.valueOf(person.getId()), jobType, person.getName(), person.getJob());
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
        person.setSourceDbId(SOURCE_IMDB, StringUtils.trim(tmdbPerson.getImdbId()));

        // split person names
        PersonName personName = MetadataTools.splitFullName(tmdbPerson.getName());
        if (OverrideTools.checkOverwriteName(person, SOURCE_TMDB)) {
            person.setName(personName.getName(), SOURCE_TMDB);
        }
        if (OverrideTools.checkOverwriteFirstName(person, SOURCE_TMDB)) {
            person.setFirstName(personName.getFirstName(), SOURCE_TMDB);
        }
        if (OverrideTools.checkOverwriteLastName(person, SOURCE_TMDB)) {
            person.setLastName(personName.getLastName(), SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteBirthDay(person, SOURCE_TMDB)) {
            Date parsedDate = MetadataTools.parseToDate(tmdbPerson.getBirthday());
            person.setBirthDay(parsedDate, SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteBirthPlace(person, SOURCE_TMDB)) {
            person.setBirthPlace(StringUtils.trimToNull(tmdbPerson.getPlaceOfBirth()), SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteBirthName(person, SOURCE_TMDB)) {
            if (CollectionUtils.isNotEmpty(tmdbPerson.getAlsoKnownAs())) {
                String birthName = tmdbPerson.getAlsoKnownAs().get(0);
                person.setBirthName(birthName, SOURCE_TMDB);
            }
        }

        if (OverrideTools.checkOverwriteDeathDay(person, SOURCE_TMDB)) {
            Date parsedDate = MetadataTools.parseToDate(tmdbPerson.getDeathday());
            person.setDeathDay(parsedDate, SOURCE_TMDB);
        }

        if (OverrideTools.checkOverwriteBiography(person, SOURCE_TMDB)) {
            person.setBiography(MetadataTools.cleanBiography(tmdbPerson.getBiography()), SOURCE_TMDB);
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
        filmo.setSourceDb(SOURCE_TMDB);
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
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SOURCE_TMDB))) {
            return true;
        }

        LOG.trace("Scanning NFO for TheMovieDb ID");

        try {
            int beginIndex = nfoContent.indexOf("/movie/");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$");
                String sourceId = st.nextToken();
                LOG.debug("TheMovieDb ID found in NFO: {}", sourceId);
                dto.addId(SOURCE_TMDB, sourceId);
                return true;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No TheMovieDb ID found in NFO");
        return false;
    }
}
