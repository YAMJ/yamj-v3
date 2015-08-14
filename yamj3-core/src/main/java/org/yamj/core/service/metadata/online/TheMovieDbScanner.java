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
        LOG.info("Initialize TheMovieDb scanner");

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
            MovieInfo movieDb = tmdbApiWrapper.getMovieInfoByIMDB(imdbId, tmdbLocale, throwTempError);
            if (movieDb != null && movieDb.getId() != 0) {
                tmdbId = String.valueOf(movieDb.getId());
            }
        }

        if (!StringUtils.isNumeric(tmdbId)) {
            LOG.debug("No TMDb id found for '{}', searching title with year {}", videoData.getTitle(), videoData.getPublicationYear());
            tmdbId = tmdbApiWrapper.getMovieId(videoData.getTitle(), videoData.getPublicationYear(), tmdbLocale, throwTempError);
        }

        if (!StringUtils.isNumeric(tmdbId) && StringUtils.isNotBlank(videoData.getTitleOriginal())) {
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
    public ScanResult scan(VideoData videoData) {
        // locale for TMDb
        final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");

        MovieInfo movieInfo = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("themoviedb.throwError.tempUnavailable", Boolean.TRUE);
            String tmdbId = getMovieId(videoData, tmdbLocale, throwTempError);

            if (!StringUtils.isNumeric(tmdbId)) {
                LOG.debug("TMDb id not available '{}'", videoData.getTitle());
                return ScanResult.MISSING_ID;
            }

            movieInfo = tmdbApiWrapper.getMovieInfoByTMDB(Integer.parseInt(tmdbId), tmdbLocale, throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("themoviedb.maxRetries.movie", 0);
            if (videoData.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }

        if (movieInfo == null) {
            LOG.error("Can't find informations for movie '{}'", videoData.getTitle());
            return ScanResult.ERROR;
        }
                        
        // fill in data
        videoData.setSourceDbId(ImdbScanner.SCANNER_ID, StringUtils.trim(movieInfo.getImdbID()));

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
        
        if (movieInfo.getVoteAverage() > 0) {
            videoData.addRating(SCANNER_ID, Float.valueOf(movieInfo.getVoteAverage() * 10).intValue());
        }

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
        if (releaseDate != null) {
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                videoData.setRelease(releaseCountryCode, releaseDate, SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
                videoData.setPublicationYear(MetadataTools.extractYearAsInt(releaseDate), SCANNER_ID);
            }
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
            final Set<String> studioNames = new HashSet<>();
            for (ProductionCompany company : movieInfo.getProductionCompanies()) {
                if (StringUtils.isNotBlank(company.getName())) {
                    studioNames.add(StringUtils.trim(company.getName()));
                }
            }
            videoData.setStudioNames(studioNames, SCANNER_ID);
        }

        // CAST
        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            boolean skipUncredited = configServiceWrapper.getBooleanProperty("yamj3.castcrew.skip.uncredited", Boolean.TRUE);
            
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
        com.omertron.themoviedbapi.model.collection.Collection collection = movieInfo.getBelongsToCollection();
        if (collection != null) {
            videoData.addBoxedSetDTO(SCANNER_ID, collection.getName(), -1, String.valueOf(collection.getId()));
        }
        
        return ScanResult.OK;
    }

    @Override
    public String getSeriesId(Series series) {
        // locale for TMDb
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
    public String getSeasonId(Season season) {
        String tmdbId = season.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(tmdbId)) {
            return tmdbId;
        }

        String seriesId = season.getSeries().getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(seriesId)) {
            // get season id from series
            final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");
            TVSeasonInfo seasonInfo = tmdbApiWrapper.getSeasonInfo(Integer.parseInt(seriesId), season.getSeason(), tmdbLocale, false);
            if (seasonInfo != null) {
                tmdbId = String.valueOf(seasonInfo.getId());
                season.setSourceDbId(SCANNER_ID, tmdbId);
            }
        }
        
        return tmdbId;
    }
        
    @Override
    public String getEpisodeId(VideoData videoData) {
        String tmdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(tmdbId)) {
            return tmdbId;
        }

        String seriesId = videoData.getSeason().getSeries().getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(seriesId)) {
            // get episode id from series and season
            final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");
            TVEpisodeInfo episodeInfo = tmdbApiWrapper.getEpisodeInfo(Integer.parseInt(seriesId), videoData.getSeason().getSeason(), videoData.getEpisode(), tmdbLocale, false);
            if (episodeInfo != null) {
                tmdbId = String.valueOf(episodeInfo.getId());
                videoData.setSourceDbId(SCANNER_ID, tmdbId);
            }
        }
        
        return tmdbId;
    }
    
    @Override
    public ScanResult scan(Series series) {
        // locale for TMDb
        final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");
        
        TVInfo tvInfo = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("themoviedb.throwError.tempUnavailable", Boolean.TRUE);
            String tmdbId = getSeriesId(series, tmdbLocale, throwTempError);

            if (!StringUtils.isNumeric(tmdbId)) {
                LOG.debug("TMDb id not available '{}'", series.getTitle());
                return ScanResult.MISSING_ID;
            }

            tvInfo = tmdbApiWrapper.getSeriesInfo(Integer.parseInt(tmdbId), tmdbLocale, throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("themoviedb.maxRetries.tvshow", 0);
            if (series.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }

        if (tvInfo == null) {
            LOG.error("Can't find informations for series '{}'", series.getTitle());
            return ScanResult.NO_RESULT;
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
            series.addRating(SCANNER_ID, Float.valueOf(tvInfo.getVoteAverage() * 10).intValue());
        }

        if (CollectionUtils.isNotEmpty(tvInfo.getOriginCountry()) && OverrideTools.checkOverwriteCountries(series, SCANNER_ID)) {
            Set<String> countryCodes = new HashSet<>();
            for (String country : tvInfo.getOriginCountry()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(country);
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

            TVSeasonInfo seasonInfo = tmdbApiWrapper.getSeasonInfo(tvInfo.getId(), season.getSeason(), tmdbLocale, false);
            MediaCreditList mediaCreditList = tmdbApiWrapper.getSeasonCredits(tvInfo.getId(), season.getSeason(), false);

            // use values from series
            if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                if (seasonInfo == null) {
                    season.setTitle(tvInfo.getName(), SCANNER_ID);
                } else {
                    season.setTitle(seasonInfo.getName(), SCANNER_ID);
                }
            }
            if (OverrideTools.checkOverwriteOriginalTitle(season, SCANNER_ID)) {
                season.setTitle(tvInfo.getOriginalName(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                if (seasonInfo == null) {
                    season.setPlot(tvInfo.getOverview(), SCANNER_ID);
                } else {
                    season.setPlot(seasonInfo.getOverview(), SCANNER_ID);
                }
            }
            if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                if (seasonInfo == null) {
                    season.setOutline(tvInfo.getOverview(), SCANNER_ID);
                } else {
                    season.setOutline(seasonInfo.getOverview(), SCANNER_ID);
                }
            }

            if (seasonInfo != null) {
                
                if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                    final Date date = parseTMDbDate(seasonInfo.getAirDate());
                    season.setPublicationYear(MetadataTools.extractYearAsInt(date), SCANNER_ID);
                }
                
                season.setSourceDbId(SCANNER_ID, String.valueOf(seasonInfo.getId()));
            }

            // mark season as done
            season.setTvSeasonDone();

            // scan episodes
            scanEpisodes(season, seasonInfo, mediaCreditList);
        }
    }
    
    private void scanEpisodes(Season season, TVSeasonInfo seasonInfo, MediaCreditList mediaCreditList) {
        if (season.isTvEpisodesScanned(SCANNER_ID)) {
            // nothing to do anymore
            return;
        }

        for (VideoData videoData : season.getVideoDatas()) {
            if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                // nothing to do if already done
                continue;
            }

            // get the episode
            TVEpisodeInfo episode = null;
            if (seasonInfo != null) {
                for (TVEpisodeInfo check : seasonInfo.getEpisodes()) {
                    if (check.getSeasonNumber() == season.getSeason() && check.getEpisodeNumber() == videoData.getEpisode()) {
                        episode = check;
                        break;
                    }
                }
            }
            
            if (episode == null) {
                // mark episode as not found
                videoData.setTvEpisodeNotFound();
            } else {
                videoData.setSourceDbId(SCANNER_ID, String.valueOf(episode.getId()));

                if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                    videoData.setTitle(episode.getName(), SCANNER_ID);
                }

                if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                    videoData.setPlot(episode.getOverview(), SCANNER_ID);
                }

                if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                    videoData.setOutline(episode.getOverview(), SCANNER_ID);
                }
                
                if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                    Date releaseDate = parseTMDbDate(episode.getAirDate());
                    videoData.setRelease(releaseDate, SCANNER_ID);
                }

                // CAST
                if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
                    if (mediaCreditList != null && CollectionUtils.isNotEmpty(mediaCreditList.getCast())) {
                        for (MediaCreditCast person : mediaCreditList.getCast()) {
                            CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), JobType.ACTOR, person.getName(), person.getCharacter());
                            videoData.addCreditDTO(credit);
                        }
                    }
                }
                
                // GUEST STARS
                if (this.configServiceWrapper.isCastScanEnabled(JobType.GUEST_STAR)) {
                    if (mediaCreditList != null && CollectionUtils.isNotEmpty(mediaCreditList.getGuestStars())) {
                        for (MediaCreditCast person : mediaCreditList.getGuestStars()) {
                            CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), JobType.GUEST_STAR, person.getName(), person.getCharacter());
                            videoData.addCreditDTO(credit);
                        }
                    }
                    for (MediaCreditCast person : episode.getGuestStars()) {
                        CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), JobType.GUEST_STAR, person.getName(), person.getCharacter());
                        videoData.addCreditDTO(credit);
                    }
                }
                
                // CREW
                if (mediaCreditList != null && CollectionUtils.isNotEmpty(mediaCreditList.getCrew())) {
                    for (MediaCreditCrew person : mediaCreditList.getCrew()) {
                        JobType jobType = retrieveJobType(person.getName(), person.getDepartment());
                        if (!this.configServiceWrapper.isCastScanEnabled(jobType)) {
                            // scan not enabled for that job
                            continue;
                        }
                        CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), jobType, person.getName(), person.getJob());
                        videoData.addCreditDTO(credit);
                    }
                }
                
                for (MediaCreditCrew person : episode.getCrew()) {
                    JobType jobType = retrieveJobType(person.getName(), person.getDepartment());
                    if (!this.configServiceWrapper.isCastScanEnabled(jobType)) {
                        // scan not enabled for that job
                        continue;
                    }
                    CreditDTO credit = new CreditDTO(SCANNER_ID, String.valueOf(person.getId()), jobType, person.getName(), person.getJob());
                    videoData.addCreditDTO(credit);
                }

                // mark episode as done
                videoData.setTvEpisodeDone();
            }
        }
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
            if (OverrideTools.checkOverwriteFirstName(person, SCANNER_ID)) {
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
            person.setBiography(MetadataTools.cleanBiography(tmdbPerson.getBiography()), SCANNER_ID);
        }

        return ScanResult.OK;
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

            // locale for TMDb
            final Locale tmdbLocale = localeService.getLocaleForConfig("themoviedb");

            credits = tmdbApiWrapper.getPersonCredits(Integer.parseInt(tmdbId), tmdbLocale, throwTempError);
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
        for (CreditBasic credit : credits.getCast()) {
            JobType jobType = retrieveJobType(person.getName(), credit.getDepartment());

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
            filmo.setRole(credit.getCharacter());
        }
        filmo.setTitle(credit.getTitle());
        filmo.setTitleOriginal(StringUtils.trimToNull(credit.getOriginalTitle()));
        filmo.setReleaseDate(releaseDate);
        filmo.setYear(MetadataTools.extractYearAsInt(releaseDate));
        return filmo;
    }

    private static JobType retrieveJobType(String personName, String department) {
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
