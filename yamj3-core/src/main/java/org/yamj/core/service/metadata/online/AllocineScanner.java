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

import com.moviejukebox.allocine.model.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.database.model.type.ParticipationType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.web.HTMLTools;
import org.yamj.core.web.PoolingHttpClient;
import org.yamj.core.web.apis.AllocineApiWrapper;
import org.yamj.core.web.apis.ImdbSearchEngine;
import org.yamj.core.web.apis.SearchEngineTools;

@Service("allocineScanner")
public class AllocineScanner implements IMovieScanner, ISeriesScanner, IPersonScanner, IFilmographyScanner {

    private static final String SCANNER_ID = "allocine";
    private static final Logger LOG = LoggerFactory.getLogger(AllocineScanner.class);

    private final Lock searchEngingeLock = new ReentrantLock(true);
    private SearchEngineTools searchEngineTools;
    
    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private AllocineApiWrapper allocineApiWrapper;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper; 
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private LocaleService localeService;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize Allocine scanner");
        
        searchEngineTools = new SearchEngineTools(httpClient, Locale.FRANCE);
        
        // register this scanner
        onlineScannerService.registerMetadataScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String allocineId = videoData.getSourceDbId(SCANNER_ID);
        
        if (StringUtils.isBlank(allocineId)) {
            allocineId = allocineApiWrapper.getAllocineMovieId(videoData.getTitle(), videoData.getYear(), throwTempError);

            if (StringUtils.isBlank(allocineId) && StringUtils.isNotBlank(videoData.getTitleOriginal())) {
                // try with original title
                allocineId = allocineApiWrapper.getAllocineMovieId(videoData.getTitleOriginal(), videoData.getYear(), throwTempError);
            }
            
            if (StringUtils.isBlank(allocineId)) {
                // try search engines
                searchEngingeLock.lock();
                try {
                    searchEngineTools.setSearchSuffix("/fichefilm_gen_cfilm");
                    String url = searchEngineTools.searchURL(videoData.getTitle(), videoData.getYear(), "www.allocine.fr/film", throwTempError);
                    allocineId = HTMLTools.extractTag(url, "fichefilm_gen_cfilm=", ".html");
                } finally {
                    searchEngingeLock.unlock();
                }
            }
            
            videoData.setSourceDbId(SCANNER_ID, allocineId);
        }

        // we also get IMDb id for extra infos
        String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isBlank(imdbId) && StringUtils.isNotBlank(videoData.getTitleOriginal())) {
            boolean searchImdb = configServiceWrapper.getBooleanProperty("allocine.search.imdb", false);
            if (searchImdb) {
                imdbId = imdbSearchEngine.getImdbId(videoData.getTitleOriginal(), videoData.getYear(), false, false);
                if (StringUtils.isNotBlank(imdbId)) {
                    LOG.debug("Found IMDb id {} for movie '{}'", imdbId, videoData.getTitleOriginal());
                    videoData.setSourceDbId(ImdbScanner.SCANNER_ID, imdbId);
                }
            }
        }

        return allocineId;
    }


    @Override
    public String getSeriesId(Series series) {
        return getSeriesId(series, false);
    }
    
    private String getSeriesId(Series series, boolean throwTempError) {
        String allocineId = series.getSourceDbId(SCANNER_ID);
        
        if (StringUtils.isBlank(allocineId)) {
            allocineId = allocineApiWrapper.getAllocineSeriesId(series.getTitle(), series.getYear(), throwTempError);

            if (StringUtils.isBlank(allocineId) && StringUtils.isNotBlank(series.getTitleOriginal())) {
                // try with original title
                allocineId = allocineApiWrapper.getAllocineSeriesId(series.getTitleOriginal(), series.getYear(), throwTempError);
            }

            if (StringUtils.isBlank(allocineId)) {
                // try search engines
                searchEngingeLock.lock();
                try {
                    searchEngineTools.setSearchSuffix("/ficheserie_gen_cserie");
                    String url = searchEngineTools.searchURL(series.getTitle(), series.getYear(), "www.allocine.fr/series", throwTempError);
                    allocineId = HTMLTools.extractTag(url, "ficheserie_gen_cserie=", ".html");
                } finally {
                    searchEngingeLock.unlock();
                }
            }
          
            series.setSourceDbId(SCANNER_ID, allocineId);
        }

        // we also get IMDb id for extra infos
        String imdbId = series.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isBlank(imdbId) && StringUtils.isNotBlank(series.getTitleOriginal())) {
            boolean searchImdb = configServiceWrapper.getBooleanProperty("allocine.search.imdb", false);
            if (searchImdb) {
                imdbId = imdbSearchEngine.getImdbId(series.getTitleOriginal(), series.getYear(), true, false);
                if (StringUtils.isNotBlank(imdbId)) {
                    LOG.debug("Found IMDb id {} for series '{}'", imdbId, series.getTitleOriginal());
                    series.setSourceDbId(ImdbScanner.SCANNER_ID, imdbId);
                }
            }
        }

        return allocineId;
    }

    @Override
    public String getPersonId(Person person) {
        return getPersonId(person, false);
    }

    private String getPersonId(Person person, boolean throwTempError) {
        String allocineId = person.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNotBlank(allocineId)) {
            return allocineId;
        }
  
        if (StringUtils.isNotBlank(person.getName())) {
            allocineId = allocineApiWrapper.getAllocinePersonId(person.getName(), throwTempError);
  
            if (StringUtils.isBlank(allocineId)) {
                // try search engines
                searchEngingeLock.lock();
                try {
                    searchEngineTools.setSearchSuffix("/fichepersonne_gen_cpersonne");
                    String url = searchEngineTools.searchURL(person.getName(), -1, "www.allocine.fr/personne", throwTempError);
                    allocineId = HTMLTools.extractTag(url, "fichepersonne_gen_cpersonne=", ".html");
                } finally {
                    searchEngingeLock.unlock();
                }
            }
            
            person.setSourceDbId(SCANNER_ID, allocineId);
        }
        
        return allocineId;
    }

    @Override
    public ScanResult scanMovie(VideoData videoData) {
        MovieInfos movieInfos = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("allocine.throwError.tempUnavailable", Boolean.TRUE);
            String allocineId = getMovieId(videoData, throwTempError);

            if (StringUtils.isBlank(allocineId)) {
                LOG.debug("Allocine id not available '{}'", videoData.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            movieInfos = allocineApiWrapper.getMovieInfos(allocineId, throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("allocine.maxRetries.movie", 0);
            if (videoData.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        
        if (movieInfos.isNotValid()) {
            LOG.error("Can't find informations for movie '{}'", videoData.getIdentifier());
            return ScanResult.NO_RESULT;
        }

        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(movieInfos.getTitle(), SCANNER_ID);
        }
        
        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
            videoData.setTitleOriginal(movieInfos.getOriginalTitle(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
            int year = movieInfos.getProductionYear();
            videoData.setPublicationYear(year, SCANNER_ID);
        }
            
        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
            videoData.setPlot(movieInfos.getSynopsis(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
            videoData.setOutline(movieInfos.getSynopsisShort(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
            final String releaseCountryCode = localeService.findCountryCode(movieInfos.getReleaseCountry());
            final Date releaseDate = MetadataTools.parseToDate(movieInfos.getReleaseDate());
            videoData.setRelease(releaseCountryCode, releaseDate, SCANNER_ID);
        }
                
        if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            videoData.setGenreNames(movieInfos.getGenres(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
            final String studioName = movieInfos.getDistributor();
            if (StringUtils.isNotBlank(studioName)) {
                Set<String> studioNames = Collections.singleton(studioName);
                videoData.setStudioNames(studioNames, SCANNER_ID);
            }
        }
        
        if (CollectionUtils.isNotEmpty(movieInfos.getNationalities()) && OverrideTools.checkOverwriteCountries(videoData, SCANNER_ID)) {
            final Set<String> countryCodes = new HashSet<>();
            for (String country : movieInfos.getNationalities()) {
                String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(countryCode);
            }
            videoData.setCountryCodes(countryCodes, SCANNER_ID);
        }
      
        // certification
        videoData.addCertificationInfo(Locale.FRANCE.getCountry(), movieInfos.getCertification());

        // allocine rating
        videoData.addRating(SCANNER_ID, movieInfos.getUserRating());

        // DIRECTORS
        if (configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
            for (MoviePerson person : movieInfos.getDirectors()) {
                videoData.addCreditDTO(createCredit(person, JobType.DIRECTOR));
            }
        }
        
        // WRITERS
        if (configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
            for (MoviePerson person : movieInfos.getWriters()) {
                videoData.addCreditDTO(createCredit(person, JobType.WRITER));
            }
        }
        
        // ACTORS
        if (configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            for (MoviePerson person : movieInfos.getActors()) {
                CreditDTO credit = createCredit(person, JobType.ACTOR);
                credit.setRole(person.getRole());
                videoData.addCreditDTO(credit);
            }
        }
        
        // CAMERA    
        if (configServiceWrapper.isCastScanEnabled(JobType.CAMERA)) {
            for (MoviePerson person : movieInfos.getCamera()) {
                videoData.addCreditDTO(createCredit(person, JobType.CAMERA));
            }
        }
        
        // PRODUCERS        
        if (configServiceWrapper.isCastScanEnabled(JobType.PRODUCER)) {
            for (MoviePerson person : movieInfos.getProducers()) {
                videoData.addCreditDTO(createCredit(person, JobType.PRODUCER));
            }
        }

        // add awards
        if (configServiceWrapper.getBooleanProperty("allocine.movie.awards", Boolean.FALSE)) {
            if (CollectionUtils.isNotEmpty(movieInfos.getFestivalAwards())) {
                for (FestivalAward festivalAward : movieInfos.getFestivalAwards()) {
                    videoData.addAwardDTO(festivalAward.getFestival(), festivalAward.getName(), SCANNER_ID, festivalAward.getYear());
                }
            }
        }
        
        return ScanResult.OK;
    }

    @Override
    public ScanResult scanSeries(Series series) {
        TvSeriesInfos tvSeriesInfos = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("allocine.throwError.tempUnavailable", Boolean.TRUE);
            String allocineId = getSeriesId(series, throwTempError);

            if (StringUtils.isBlank(allocineId)) {
                LOG.debug("Allocine id not available '{}'", series.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            tvSeriesInfos = allocineApiWrapper.getTvSeriesInfos(allocineId, throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("allocine.maxRetries.tvshow", 0);
            if (series.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        
        if (tvSeriesInfos.isNotValid()) {
            LOG.error("Can't find informations for series '{}'", series.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
            series.setTitle(tvSeriesInfos.getTitle(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(series, SCANNER_ID)) {
            series.setTitleOriginal(tvSeriesInfos.getOriginalTitle(), SCANNER_ID);
        }
        
        if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
            series.setStartYear(tvSeriesInfos.getYearStart(), SCANNER_ID);
            series.setEndYear(tvSeriesInfos.getYearEnd(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
            series.setPlot(tvSeriesInfos.getSynopsis(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
            series.setOutline(tvSeriesInfos.getSynopsisShort(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            series.setGenreNames(tvSeriesInfos.getGenres(), SCANNER_ID);
        }

        if (tvSeriesInfos.getOriginalChannel() != null && OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            Set<String> studioNames = Collections.singleton(tvSeriesInfos.getOriginalChannel());
            series.setStudioNames(studioNames, SCANNER_ID);
        }

        if (CollectionUtils.isNotEmpty(tvSeriesInfos.getNationalities()) && OverrideTools.checkOverwriteCountries(series, SCANNER_ID)) {
            Set<String> countryCodes = new HashSet<>();
            for (String country : tvSeriesInfos.getNationalities()) {
                String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(countryCode);
            }
            series.setCountryCodes(countryCodes, SCANNER_ID);
        }

        // allocine rating
        series.addRating(SCANNER_ID, tvSeriesInfos.getUserRating());

        // add awards
        if (configServiceWrapper.getBooleanProperty("allocine.tvshow.awards", Boolean.FALSE)) {
            if (CollectionUtils.isNotEmpty(tvSeriesInfos.getFestivalAwards())) {
                for (FestivalAward festivalAward : tvSeriesInfos.getFestivalAwards()) {
                    series.addAwardDTO(festivalAward.getFestival(), festivalAward.getName(), SCANNER_ID, festivalAward.getYear());
                }
            }
        }

        // SCAN SEASONS
        scanSeasons(series, tvSeriesInfos);

        return ScanResult.OK;
    }
     
    private void scanSeasons(Series series, TvSeriesInfos tvSeriesInfos) {

        for (Season season : series.getSeasons()) {

            TvSeasonInfos tvSeasonInfos = null;
            if (season.getSeason() <= tvSeriesInfos.getSeasonCount()) {
                int seasonCode = tvSeriesInfos.getSeasonCode(season.getSeason());
                if (seasonCode > 0) {
                    tvSeasonInfos = allocineApiWrapper.getTvSeasonInfos(String.valueOf(seasonCode));
                }
            }

            if (!season.isTvSeasonDone(SCANNER_ID)) {

                if (tvSeasonInfos == null || tvSeasonInfos.isNotValid()) {
                    // mark season as not found
                    season.removeOverrideSource(SCANNER_ID);
                    season.removeSourceDbId(SCANNER_ID);
                    season.setTvSeasonNotFound();
                } else {
                    season.setSourceDbId(SCANNER_ID, String.valueOf(tvSeasonInfos.getCode()));

                    if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                        season.setTitle(tvSeriesInfos.getTitle(), SCANNER_ID);
                    }
                    
                    if (OverrideTools.checkOverwriteOriginalTitle(season, SCANNER_ID)) {
                        season.setTitle(tvSeriesInfos.getOriginalTitle(), SCANNER_ID);
                    }
                    
                    if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                        season.setPlot(tvSeriesInfos.getSynopsis(), SCANNER_ID);
                    }
                    
                    if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                        season.setOutline(tvSeriesInfos.getSynopsisShort(), SCANNER_ID);
                    }

                    if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                        season.setPublicationYear(tvSeasonInfos.getSeason().getYearStart(), SCANNER_ID);
                    }
                       
                    // mark season as done
                    season.setTvSeasonDone();
                }
            }
            
            // scan episodes
            scanEpisodes(season, tvSeasonInfos);
        }
    }

    private void scanEpisodes(Season season, TvSeasonInfos tvSeasonInfos) {
        for (VideoData videoData : season.getVideoDatas()) {
            
            if (videoData.isTvEpisodeDone(SCANNER_ID)) {
                // nothing to do if episode already done
                continue;
            }

            // get the episode
            String allocineId = videoData.getSourceDbId(SCANNER_ID);
            if (StringUtils.isBlank(allocineId) && tvSeasonInfos != null && tvSeasonInfos.isValid()) {
                Episode episode = tvSeasonInfos.getEpisode(videoData.getEpisode());
                if (episode != null && episode.getCode() > 0) {
                    allocineId = String.valueOf(episode.getCode());
                }
            }

            EpisodeInfos episodeInfos = allocineApiWrapper.getEpisodeInfos(allocineId);
            if (episodeInfos == null || episodeInfos.isNotValid()) {
                // mark episode as not found
                videoData.removeOverrideSource(SCANNER_ID);
                videoData.removeSourceDbId(SCANNER_ID);
                videoData.setTvEpisodeNotFound();
                continue;
            }
            
            videoData.setSourceDbId(SCANNER_ID, String.valueOf(episodeInfos.getCode()));

            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                videoData.setTitle(episodeInfos.getTitle(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
                videoData.setTitleOriginal(episodeInfos.getOriginalTitle(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                videoData.setPlot(episodeInfos.getSynopsis(), SCANNER_ID);
            }

            if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                videoData.setOutline(episodeInfos.getSynopsisShort(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                Date releaseDate = MetadataTools.parseToDate(episodeInfos.getOriginalBroadcastDate());
                videoData.setRelease(releaseDate, SCANNER_ID);
            }

            //  parse credits
            parseCredits(videoData, episodeInfos.getEpisode().getCastMember());

            // mark episode as done
            videoData.setTvEpisodeDone();
        }
    }

    private void parseCredits(VideoData videoData, List<CastMember> castMembers) {

        if (CollectionUtils.isNotEmpty(castMembers)) {
            for (CastMember member: castMembers) {
                if (member.getShortPerson() == null) {
                    continue;
                }
                
                if (member.isActor() && configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
                    CreditDTO credit;
                    if (member.isLeadActor()) {
                        credit = createCredit(member, JobType.ACTOR);
                    } else {
                        credit = createCredit(member, JobType.GUEST_STAR);
                    }
                    credit.setRole(MetadataTools.cleanRole(member.getRole()));
                    credit.setVoice(MetadataTools.isVoiceRole(member.getRole()));
                    videoData.addCreditDTO(credit);
                } else if (member.isDirector() && configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
                    videoData.addCreditDTO(createCredit(member, JobType.DIRECTOR));
                } else if (member.isWriter() && configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
                    videoData.addCreditDTO(createCredit(member, JobType.WRITER));
                } else if (member.isCamera() && configServiceWrapper.isCastScanEnabled(JobType.CAMERA)) {
                    videoData.addCreditDTO(createCredit(member, JobType.CAMERA));
                } else if (member.isProducer() && configServiceWrapper.isCastScanEnabled(JobType.PRODUCER)) {
                    videoData.addCreditDTO(createCredit(member, JobType.PRODUCER));
                }
            }
        }
    }
    
    private static CreditDTO createCredit(CastMember member, JobType jobType) {
        String sourceId = (member.getShortPerson().getCode() > 0 ?  String.valueOf(member.getShortPerson().getCode()) : null);
        return new CreditDTO(SCANNER_ID, sourceId, jobType, member.getShortPerson().getName());
    }

    private static CreditDTO createCredit(MoviePerson person, JobType jobType) {
        String sourceId = (person.getCode() > 0 ?  String.valueOf(person.getCode()) : null);
        return new CreditDTO(SCANNER_ID, sourceId, jobType, person.getName());
    }

    @Override
    public ScanResult scanPerson(Person person) {
        PersonInfos  personInfos = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("allocine.throwError.tempUnavailable", Boolean.TRUE);
            String allocineId = getPersonId(person, throwTempError);

            if (StringUtils.isBlank(allocineId)) {
                LOG.debug("Allocine id not available '{}'", person.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            personInfos = allocineApiWrapper.getPersonInfos(allocineId, throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("allocine.maxRetries.person", 0);
            if (person.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        
        if (personInfos.isNotValid()) {
            LOG.error("Can't find informations for person '{}'", person.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        // fill in data

        if (OverrideTools.checkOverwriteName(person, SCANNER_ID)) {
            person.setName(personInfos.getFullName(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteFirstName(person, SCANNER_ID)) {
            person.setFirstName(personInfos.getFirstName(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteLastName(person, SCANNER_ID)) {
            person.setLastName(personInfos.getLastName(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteBirthDay(person, SCANNER_ID)) {
            Date parsedDate = MetadataTools.parseToDate(personInfos.getBirthDate());
            person.setBirthDay(parsedDate, SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteBirthPlace(person, SCANNER_ID)) {
            person.setBirthPlace(personInfos.getBirthPlace(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteBirthName(person, SCANNER_ID)) {
            person.setBirthName(personInfos.getRealName(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteDeathDay(person, SCANNER_ID)) {
            Date parsedDate = MetadataTools.parseToDate(personInfos.getDeathDate());
            person.setDeathDay(parsedDate, SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteDeathPlace(person, SCANNER_ID)) {
            person.setDeathPlace(personInfos.getDeathPlace(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteBiography(person, SCANNER_ID)) {
            person.setBiography(personInfos.getBiography(), SCANNER_ID);
        }

        return ScanResult.OK;
    }

    @Override
    public ScanResult scanFilmography(Person person) {
        FilmographyInfos  filmographyInfos = null;
        try {
            boolean throwTempError = configServiceWrapper.getBooleanProperty("allocine.throwError.tempUnavailable", Boolean.TRUE);
            String allocineId = getPersonId(person, throwTempError);

            if (StringUtils.isBlank(allocineId)) {
                LOG.debug("Allocine id not available '{}'", person.getIdentifier());
                return ScanResult.MISSING_ID;
            }

            filmographyInfos = allocineApiWrapper.getFilmographyInfos(allocineId, throwTempError);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("allocine.maxRetries.filmography", 0);
            if (person.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        
        if (filmographyInfos == null || filmographyInfos.isNotValid() || CollectionUtils.isEmpty(filmographyInfos.getParticipances())) {
            LOG.trace("No filmography present for person '{}'", person.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        Set<FilmParticipation> newFilmography = new HashSet<>();
        for (Participance participance : filmographyInfos.getParticipances()) {
            FilmParticipation filmo = new FilmParticipation();
            filmo.setSourceDb(SCANNER_ID);
            filmo.setSourceDbId(String.valueOf(participance.getCode()));
            filmo.setPerson(person);
            
            if (participance.isActor()) {
                filmo.setJobType(JobType.ACTOR);
                filmo.setRole(StringUtils.trimToNull(participance.getRole()));
            } else if (participance.isDirector()) {
                filmo.setJobType(JobType.DIRECTOR);
            } else if (participance.isWriter()) {
                filmo.setJobType(JobType.WRITER);
            } else if (participance.isCamera()) {
                filmo.setJobType(JobType.CAMERA);
            } else if (participance.isProducer()) {
                filmo.setJobType(JobType.PRODUCER);
            } else {
                // no entries with unknown job type
                continue;
            }

            if (participance.isTvShow()) {
                filmo.setParticipationType(ParticipationType.SERIES);
                filmo.setYear(participance.getYearStart());
                filmo.setYearEnd(participance.getYearEnd());
            } else {
                filmo.setParticipationType(ParticipationType.MOVIE);
                filmo.setYear(participance.getYear());
            }
            
            filmo.setTitle(participance.getTitle());
            filmo.setTitleOriginal(StringUtils.trimToNull(participance.getOriginalTitle()));
            filmo.setDescription(StringUtils.trimToNull(participance.getSynopsisShort()));
            filmo.setReleaseDate(MetadataTools.parseToDate(participance.getReleaseDate()));
            String releaseCountryCode = localeService.findCountryCode(participance.getReleaseCountry());
            filmo.setReleaseCountryCode(releaseCountryCode);
            newFilmography.add(filmo);
        }
        
        person.setNewFilmography(newFilmography);
        
        return ScanResult.OK;
    }

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(SCANNER_ID))) {
            return Boolean.TRUE;
        }
        
        // scan for IMDb ID
        ImdbScanner.scanImdbID(nfoContent, dto, ignorePresentId);

        LOG.trace("Scanning NFO for Allocine ID");
        
        // http://www.allocine.fr/...=XXXXX.html
        try {
            int beginIndex = StringUtils.indexOfIgnoreCase(nfoContent, "http://www.allocine.fr/");
            if (beginIndex != -1) {
                int beginIdIndex = nfoContent.indexOf('=', beginIndex);
                if (beginIdIndex != -1) {
                    int endIdIndex = nfoContent.indexOf('.', beginIdIndex);
                    if (endIdIndex != -1) {
                        String sourceId = nfoContent.substring(beginIdIndex + 1, endIdIndex);
                        LOG.debug("Allocine ID found in NFO: {}", sourceId);
                        dto.addId(SCANNER_ID, sourceId);
                        return Boolean.TRUE;
                    }
                }
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }
        
        LOG.debug("No Allocine ID found in NFO");
        return Boolean.FALSE;
    }
}

