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
import org.yamj.core.configuration.ConfigServiceWrapper;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.database.model.type.ParticipationType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.tools.web.HTMLTools;
import org.yamj.core.tools.web.PoolingHttpClient;
import org.yamj.core.tools.web.SearchEngineTools;
import org.yamj.core.tools.web.TemporaryUnavailableException;

@Service("allocineScanner")
public class AllocineScanner implements IMovieScanner, ISeriesScanner, IFilmographyScanner {

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

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() throws Exception {
        LOG.info("Initialize Allocine scanner");
        
        searchEngineTools = new SearchEngineTools(httpClient, "fr");
       
        onlineScannerService.registerMovieScanner(this);
        onlineScannerService.registerSeriesScanner(this);
        onlineScannerService.registerPersonScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String allocineId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(allocineId)) {
            allocineId = getMovieId(videoData.getTitle(), videoData.getYear());
            videoData.setSourceDbId(SCANNER_ID, allocineId);
        }

        // we also get IMDb id for extra infos
        String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isBlank(imdbId) && StringUtils.isNotBlank(videoData.getTitleOriginal())) {
            boolean searchImdb = configServiceWrapper.getBooleanProperty("allocine.search.imdb", false);
            if (searchImdb) {
                imdbId = imdbSearchEngine.getImdbId(videoData.getTitleOriginal(), videoData.getYear(), false);
                if (StringUtils.isNotBlank(imdbId)) {
                    LOG.debug("Found IMDb id {} for movie '{}'", imdbId, videoData.getTitleOriginal());
                    videoData.setSourceDbId(ImdbScanner.SCANNER_ID, imdbId);
                }
            }
        }

        return allocineId;
    }

    @Override
    public String getMovieId(String title, int year) {
        String allocineId = allocineApiWrapper.getAllocineMovieId(title, year);

        if (StringUtils.isBlank(allocineId)) {
            // try search engines
            searchEngingeLock.lock();
            try {
                searchEngineTools.setSearchSuffix("/fichefilm_gen_cfilm");
                String url = searchEngineTools.searchURL(title, year, "www.allocine.fr/film");
                allocineId = HTMLTools.extractTag(url, "fichefilm_gen_cfilm=", ".html");
            } finally {
                searchEngingeLock.unlock();
            }
        }
       
        return allocineId;
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        MovieInfos movieInfos = null;
        try {
            String allocineId = getMovieId(videoData);

            if (StringUtils.isBlank(allocineId)) {
                LOG.debug("Allocine id not available '{}'", videoData.getTitle());
                return ScanResult.MISSING_ID;
            }

            movieInfos = allocineApiWrapper.getMovieInfos(allocineId);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("allocine.maxRetries.movie", 0);
            if (videoData.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        if (movieInfos == null || movieInfos.isNotValid()) {
            LOG.error("Can't find informations for movie '{}'", videoData.getTitle());
            return ScanResult.ERROR;
        }

        // fill in data
        
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
            Date releaseDate = MetadataTools.parseToDate(movieInfos.getReleaseDate());
            videoData.setReleaseDate(releaseDate, SCANNER_ID);
        }
                
        if (OverrideTools.checkOverwriteCountry(videoData, SCANNER_ID)) {
            Set<String> nationalities = movieInfos.getNationalities();
            if (CollectionUtils.isNotEmpty(nationalities)) {
                // TODO more countries
                String country = nationalities.iterator().next();
                videoData.setCountry(country, SCANNER_ID);
            }
        }
        
        if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            videoData.setGenreNames(movieInfos.getGenres(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
            String studioName = movieInfos.getDistributor();
            if (StringUtils.isNotBlank(studioName)) {
                Set<String> studioNames = Collections.singleton(studioName);
                videoData.setStudioNames(studioNames, SCANNER_ID);
            }
        }
        
        // certification
        videoData.addCertificationInfo("France", movieInfos.getCertification());

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
        
        // add poster URLs
        if (CollectionUtils.isNotEmpty(movieInfos.getPosterUrls()))  {
            for (String posterURL : movieInfos.getPosterUrls()) {
                videoData.addPosterURL(posterURL, SCANNER_ID);
            }
        }

        return ScanResult.OK;
    }

    @Override
    public String getSeriesId(Series series) {
        String allocineId = series.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(allocineId)) {
            allocineId = getSeriesId(series.getTitle(), series.getYear());
            series.setSourceDbId(SCANNER_ID, allocineId);
        }

        // we also get IMDb id for extra infos
        String imdbId = series.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isBlank(imdbId) && StringUtils.isNotBlank(series.getTitleOriginal())) {
            boolean searchImdb = configServiceWrapper.getBooleanProperty("allocine.search.imdb", false);
            if (searchImdb) {
                imdbId = imdbSearchEngine.getImdbId(series.getTitleOriginal(), series.getYear(), true);
                if (StringUtils.isNotBlank(imdbId)) {
                    LOG.debug("Found IMDb id {} for series '{}'", imdbId, series.getTitleOriginal());
                    series.setSourceDbId(ImdbScanner.SCANNER_ID, imdbId);
                }
            }
        }

        return allocineId;
    }

    @Override
    public String getSeriesId(String title, int year) {
        String allocineId = allocineApiWrapper.getAllocineSeriesId(title, year);

        if (StringUtils.isBlank(allocineId)) {
            // try search engines
            searchEngingeLock.lock();
            try {
                searchEngineTools.setSearchSuffix("/ficheserie_gen_cserie");
                String url = searchEngineTools.searchURL(title, year, "www.allocine.fr/series");
                allocineId = HTMLTools.extractTag(url, "ficheserie_gen_cserie=", ".html");
            } finally {
                searchEngingeLock.unlock();
            }
        }
        
        return allocineId;
    }

    @Override
    public String getPersonId(Person person) {
        String id = person.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNotBlank(id)) {
            return id;
        } else if (StringUtils.isNotBlank(person.getName())) {
            return getPersonId(person.getName());
        }
        LOG.error("No ID or Name found for {}", person.toString());
        return StringUtils.EMPTY;
    }

    @Override
    public String getPersonId(String name) {
        String allocineId = allocineApiWrapper.getAllocinePersonId(name);

        if (StringUtils.isBlank(allocineId)) {
            // try search engines
            searchEngingeLock.lock();
            try {
                searchEngineTools.setSearchSuffix("/fichepersonne_gen_cpersonne");
                String url = searchEngineTools.searchURL(name, -1, "www.allocine.fr/personne");
                allocineId = HTMLTools.extractTag(url, "fichepersonne_gen_cpersonne=", ".html");
            } finally {
                searchEngingeLock.unlock();
            }
        }
        
        return allocineId;
    }

    @Override
    public ScanResult scan(Series series) {
        TvSeriesInfos tvSeriesInfos = null;
        try {
            String allocineId = getSeriesId(series);

            if (StringUtils.isBlank(allocineId)) {
                LOG.debug("Allocine id not available '{}'", series.getTitle());
                return ScanResult.MISSING_ID;
            }

            tvSeriesInfos = allocineApiWrapper.getTvSeriesInfos(allocineId);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("allocine.maxRetries.tvshow", 0);
            if (series.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        if (tvSeriesInfos == null || tvSeriesInfos.isNotValid()) {
            LOG.error("Can't find informations for series '{}'", series.getTitle());
            return ScanResult.ERROR;
        }
        
        // fill in data
        
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

        if (OverrideTools.checkOverwriteStudios(series, SCANNER_ID)) {
            if (tvSeriesInfos.getOriginalChannel() != null) {
                Set<String> studioNames = Collections.singleton(tvSeriesInfos.getOriginalChannel());
                series.setStudioNames(studioNames, SCANNER_ID);
            }
        }

        // TODO series country
        
        if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
            series.setGenreNames(tvSeriesInfos.getGenres(), SCANNER_ID);
        }

        // allocine rating
        series.addRating(SCANNER_ID, tvSeriesInfos.getUserRating());

        // add poster URLs
        if (CollectionUtils.isNotEmpty(tvSeriesInfos.getPosterUrls()))  {
            for (String posterURL : tvSeriesInfos.getPosterUrls()) {
                series.addPosterURL(posterURL, SCANNER_ID);
            }
        }

        // SCAN SEASONS
        scanSeasons(series, tvSeriesInfos);

        return ScanResult.OK;
    }

    private void scanSeasons(Series series, TvSeriesInfos tvSeriesInfos) {

        for (Season season : series.getSeasons()) {

            TvSeasonInfos tvSeasonInfos = allocineApiWrapper.getTvSeasonInfos(tvSeriesInfos, season.getSeason());

            // use values from series
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

            if (tvSeasonInfos != null && tvSeasonInfos.isValid()) {
                
                if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                    season.setPublicationYear(tvSeasonInfos.getYearStart(), SCANNER_ID);
                }
                
                season.setSourceDbId(SCANNER_ID, String.valueOf(tvSeasonInfos.getCode()));
            }

            // mark season as done
            season.setTvSeasonDone();

            // scan episodes
            scanEpisodes(season, tvSeasonInfos);
        }
    }

    private void scanEpisodes(Season season, TvSeasonInfos tvSeasonInfos) {
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
            Episode episode = null;
            if (tvSeasonInfos != null && tvSeasonInfos.isValid()) {
                episode = tvSeasonInfos.getEpisode(videoData.getEpisode());
            }
            
            if (episode == null) {
                // mark episode as not found
                videoData.setTvEpisodeNotFound();
            } else {

                String allocineId = videoData.getSourceDbId(SCANNER_ID);
                if (episode.getCode() > 0) {
                    allocineId = String.valueOf(episode.getCode());
                }
                videoData.setSourceDbId(SCANNER_ID, allocineId);

                List<CastMember> castMembers = null;
                EpisodeInfos episodeInfos = allocineApiWrapper.getEpisodeInfos(allocineId);
                if (episodeInfos == null || episodeInfos.isNotValid()) {
                    // fix episode from season info
                    episode.setSynopsis(HTMLTools.replaceHtmlTags(episode.getSynopsis(), " "));

                    episodeInfos = new EpisodeInfos();
                    episodeInfos.setEpisode(episode);
                    
                    // use members from season
                    if (tvSeasonInfos.getSeason() != null) {
                        castMembers = tvSeasonInfos.getSeason().getCastMember();
                    }
                } else {
                    // use members from episode
                    castMembers = episodeInfos.getEpisode().getCastMember();
                }

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
                    videoData.setReleaseDate(releaseDate, SCANNER_ID);
                }
                
                //  add credits
                videoData.addCreditDTOS(parseCredits(castMembers));

                // mark episode as done
                videoData.setTvEpisodeDone();
            }
        }
    }

    private Set<CreditDTO> parseCredits(List<CastMember> castMembers) {
        Set<CreditDTO> result = new LinkedHashSet<>();

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
                    credit.setRole(member.getRole());
                    result.add(credit);
                } else if (member.isDirector() && configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
                    result.add(createCredit(member, JobType.DIRECTOR));
                } else if (member.isWriter() && configServiceWrapper.isCastScanEnabled(JobType.WRITER)) {
                    result.add(createCredit(member, JobType.WRITER));
                } else if (member.isCamera() && configServiceWrapper.isCastScanEnabled(JobType.CAMERA)) {
                    result.add(createCredit(member, JobType.CAMERA));
                } else if (member.isProducer() && configServiceWrapper.isCastScanEnabled(JobType.PRODUCER)) {
                    result.add(createCredit(member, JobType.PRODUCER));
                }
            }
        }
        return result;
    }
    
    private static CreditDTO createCredit(CastMember member, JobType jobType) {
        CreditDTO credit = new CreditDTO(SCANNER_ID, jobType, member.getShortPerson().getName());
        if (member.getShortPerson().getCode() > 0) {
            credit.addPersonId(SCANNER_ID, String.valueOf(member.getShortPerson().getCode()));
        }
        if (member.getPicture() != null) {
            credit.addPhotoURL(member.getPicture().getHref(), SCANNER_ID);
        }
        return credit;
    }

    private static CreditDTO createCredit(MoviePerson person, JobType jobType) {
        CreditDTO credit = new CreditDTO(SCANNER_ID, jobType, person.getName());
        if (person.getCode() > 0) {
            credit.addPersonId(SCANNER_ID, String.valueOf(person.getCode()));
        }
        credit.addPhotoURL(person.getPhotoURL(), SCANNER_ID);
        return credit;
    }

    @Override
    public ScanResult scan(Person person) {
        PersonInfos  personInfos = null;
        try {
            String allocineId = getPersonId(person);

            if (StringUtils.isBlank(allocineId)) {
                LOG.debug("Allocine id not available '{}'", person.getName());
                return ScanResult.MISSING_ID;
            }

            personInfos = allocineApiWrapper.getPersonInfos(allocineId);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("allocine.maxRetries.person", 0);
            if (person.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        if (personInfos == null || personInfos.isNotValid()) {
            LOG.error("Can't find informations for person '{}'", person.getName());
            return ScanResult.ERROR;
        }
        
        // fill in data

        if (OverrideTools.checkOverwriteName(person, SCANNER_ID)) {
            person.setName(personInfos.getFullName(), SCANNER_ID);
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
        
        // add poster URL
        person.addPhotoURL(personInfos.getPhotoURL(), SCANNER_ID);

        return ScanResult.OK;
    }

    @Override
    public boolean isFilmographyScanEnabled() {
        return configServiceWrapper.getBooleanProperty("allocine.person.filmography", false);
    }

    @Override
    public ScanResult scanFilmography(Person person) {
        FilmographyInfos  filmographyInfos = null;
        try {
            String allocineId = getPersonId(person);

            if (StringUtils.isBlank(allocineId)) {
                LOG.debug("Allocine id not available '{}'", person.getName());
                return ScanResult.MISSING_ID;
            }

            filmographyInfos = allocineApiWrapper.getFilmographyInfos(allocineId);
        } catch (TemporaryUnavailableException ex) {
            // check retry
            int maxRetries = configServiceWrapper.getIntProperty("allocine.maxRetries.filmography", 0);
            if (person.getRetries() < maxRetries) {
                return ScanResult.RETRY;
            }
        }
        if (filmographyInfos == null || filmographyInfos.isNotValid()) {
            LOG.error("Can't find filmography for person '{}'", person.getName());
            return ScanResult.ERROR;
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
            filmo.setReleaseState(StringUtils.trimToNull(participance.getReleaseState()));
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

