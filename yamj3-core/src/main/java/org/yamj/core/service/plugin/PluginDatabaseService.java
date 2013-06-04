package org.yamj.core.service.plugin;

import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.dao.MediaDao;
import org.yamj.core.database.dao.PersonDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.database.model.type.MetaDataType;

@Service("pluginDatabaseService")
public class PluginDatabaseService {

    private static final Logger LOG = LoggerFactory.getLogger(PluginDatabaseService.class);
    public static final String VIDEO_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie", "tmdb");
    public static final String VIDEO_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie.alternate", "");
    public static final String SERIES_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.series", "tvdb");
    public static final String SERIES_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.series.alternate", "");
    private static final String PERSON_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.person", "tmdb");

    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private CommonDao commonDao;

    private HashMap<String, IMovieScanner> registeredMovieScanner = new HashMap<String, IMovieScanner>();
    private HashMap<String, ISeriesScanner> registeredSeriesScanner = new HashMap<String, ISeriesScanner>();
    private HashMap<String, IPersonScanner> registeredPersonScanner = new HashMap<String, IPersonScanner>();

    public void registerMovieScanner(IMovieScanner movieScanner) {
        registeredMovieScanner.put(movieScanner.getScannerName().toLowerCase(), movieScanner);
    }

    public void registerSeriesScanner(ISeriesScanner seriesScanner) {
        registeredSeriesScanner.put(seriesScanner.getScannerName().toLowerCase(), seriesScanner);
    }

    public void registerPersonScanner(IPersonScanner personScanner) {
        registeredPersonScanner.put(personScanner.getScannerName().toLowerCase(), personScanner);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void scanVideoData(Long id) {
        VideoData videoData = mediaDao.getVideoData(id);

        // SCAN MOVIE
        LOG.debug("Scanning movie data for '{}' using {}", videoData.getTitle(), VIDEO_SCANNER);

        IMovieScanner movieScanner = registeredMovieScanner.get(VIDEO_SCANNER);
        if (movieScanner == null) {
            LOG.error("Video data scanner not registered '{}'", VIDEO_SCANNER);
            videoData.setStatus(StatusType.ERROR);
            mediaDao.updateEntity(videoData);
            return;
        }

        // scan video data
        ScanResult scanResult;
        try {
            scanResult = movieScanner.scan(videoData);
        } catch (Exception error) {
            scanResult = ScanResult.ERROR;
            LOG.error("Failed scanning video data with {} scanner", VIDEO_SCANNER);
            LOG.warn("Scanning error", error);
        }

        // alternate scanning if main scanner failed
        if (!ScanResult.OK.equals(scanResult)) {
            movieScanner = registeredMovieScanner.get(VIDEO_SCANNER_ALT);

            if (movieScanner != null) {
                try {
                    movieScanner.scan(videoData);
                } catch (Exception error) {
                    LOG.error("Failed scanning video data with {} alternate scanner", VIDEO_SCANNER_ALT);
                    LOG.warn("Alternate scanning error", error);
                }
            }
        }

        // STORAGE
        updateGenres(videoData);
        updateCastCrew(videoData);

        // update video data and reset status
        if (ScanResult.OK.equals(scanResult)) {
            videoData.setStatus(StatusType.DONE);
        } else {
            videoData.setStatus(StatusType.ERROR);
        }
        mediaDao.updateEntity(videoData);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void scanSeries(Long id) {
        Series series = mediaDao.getSeries(id);

        // SCAN SERIES
        LOG.debug("Scanning series data for '{}' using {}", series.getTitle(), SERIES_SCANNER);

        ISeriesScanner seriesScanner = registeredSeriesScanner.get(SERIES_SCANNER);
        if (seriesScanner == null) {
            LOG.error("Series scanner '{}' not registered", SERIES_SCANNER);
            series.setStatus(StatusType.ERROR);
            mediaDao.updateEntity(series);
            return;
        }

        // scan series
        ScanResult scanResult;
        try {
            scanResult = seriesScanner.scan(series);
        } catch (Exception error) {
            scanResult = ScanResult.ERROR;
            LOG.error("Failed scanning series data with {} scanner", SERIES_SCANNER);
            LOG.warn("Scanning error", error);
        }

        // alternate scanning if main scanner failed
        if (!ScanResult.OK.equals(scanResult)) {
            seriesScanner = registeredSeriesScanner.get(SERIES_SCANNER_ALT);

            if (seriesScanner != null) {
                try {
                    seriesScanner.scan(series);
                } catch (Exception error) {
                    LOG.error("Failed scanning series data with {} alternate scanner", SERIES_SCANNER_ALT);
                    LOG.warn("Alternate scanning error", error);
                }
            }
        }

        // update underlying seasons and episodes
        for (Season season : series.getSeasons()) {
            if (StatusType.PROCESSED.equals(season.getStatus())) {
                season.setStatus(StatusType.DONE);
                mediaDao.updateEntity(season);
            }
            for (VideoData videoData : season.getVideoDatas()) {
                if (StatusType.PROCESSED.equals(videoData.getStatus())) {
                    updateGenres(videoData);
                    updateCastCrew(videoData);
                    videoData.setStatus(StatusType.DONE);
                    mediaDao.updateEntity(videoData);
                }
            }
        }

        // update series and reset status
        if (ScanResult.OK.equals(scanResult)) {
            series.setStatus(StatusType.DONE);
        } else {
            series.setStatus(StatusType.ERROR);
        }
        mediaDao.updateEntity(series);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        if (queueElement.isMetadataType(MetaDataType.VIDEODATA)) {
            VideoData videoData = mediaDao.getVideoData(queueElement.getId());
            if (videoData != null) {
                videoData.setStatus(StatusType.ERROR);
                mediaDao.updateEntity(videoData);
            }
        } else if (queueElement.isMetadataType(MetaDataType.SERIES)) {
            Series series = mediaDao.getSeries(queueElement.getId());
            if (series != null) {
                series.setStatus(StatusType.ERROR);
                mediaDao.updateEntity(series);
            }
        } else if (queueElement.isMetadataType(MetaDataType.PERSON)) {
            Person person = mediaDao.getPerson(queueElement.getId());
            if (person != null) {
                person.setStatus(StatusType.ERROR);
                mediaDao.updateEntity(person);
            }
        }
    }

    private void updateGenres(VideoData videoData) {
        HashSet<Genre> genres = new HashSet<Genre>(0);
        for (Genre genre : videoData.getGenres()) {
            Genre stored = commonDao.getGenre(genre.getName());
            if (stored == null) {
                commonDao.saveEntity(genre);
                genres.add(genre);
            } else {
                genres.add(stored);
            }
        }
        videoData.setGenres(genres);
    }

    private void updateCastCrew(VideoData videoData) {
        for (CreditDTO dto : videoData.getCreditDTOS()) {
            Person person = null;
            CastCrew castCrew = null;

            for (CastCrew credit : videoData.getCredits()) {
                if ((credit.getJobType() == dto.getJobType()) && StringUtils.equalsIgnoreCase(dto.getName(), credit.getPerson().getName())) {
                    castCrew = credit;
                    person = credit.getPerson();
                    break;
                }
            }

            // find person if not found
            if (person == null) {
                LOG.info("Attempting to retrieve information on '{}' from database", dto.getName());
                person = personDao.getPerson(dto.getName());
            } else {
                LOG.debug("Found '{}' in cast table", person.getName());
            }

            if (person != null) {
                // update person id
                if (StringUtils.isNotBlank(dto.getSourcedb()) && StringUtils.isNotBlank(dto.getSourcedbId())) {
                    person.setPersonId(dto.getSourcedb(), dto.getSourcedbId());
                    personDao.updateEntity(person);
                }
            } else {
                // create new person
                person = new Person();
                person.setName(dto.getName());
                if (StringUtils.isNotBlank(dto.getSourcedb()) && StringUtils.isNotBlank(dto.getSourcedbId())) {
                    person.setPersonId(dto.getSourcedb(), dto.getSourcedbId());
                }
                person.setStatus(StatusType.NEW);
                personDao.saveEntity(person);
            }

            if (castCrew == null) {
                castCrew = new CastCrew();
                castCrew.setPerson(person);
                castCrew.setJobType(dto.getJobType());
                if (StringUtils.isNotBlank(dto.getRole())) {
                    castCrew.setRole(dto.getRole());
                }
                castCrew.setVideoData(videoData);
                videoData.addCredit(castCrew);
                personDao.saveEntity(castCrew);
            } else {
                // update role
                if (StringUtils.isBlank(castCrew.getRole())
                        && JobType.ACTOR.equals(castCrew.getJobType())
                        && StringUtils.isNotBlank(dto.getRole())) {
                    castCrew.setRole(dto.getRole());
                    personDao.updateEntity(castCrew);
                }
            }
        }
    }

    /**
     * Scan the data site for information on the person
     */
    public void scanPerson(Long id) {
        String scannerName = PERSON_SCANNER;
        IPersonScanner personScanner = registeredPersonScanner.get(scannerName);
        Person person = personDao.getRequiredPerson(id);
        
        LOG.info("Scanning for information on person {}-'{}' using {}", id, person.getName(), scannerName);

        if (personScanner == null) {
            LOG.error("Person scanner '{}' not registered", scannerName);
            person.setStatus(StatusType.ERROR);
            personDao.updateEntity(person);
            return;
        }

        // Scan series data
        ScanResult scanResult;
        try {
            scanResult = personScanner.scan(person);
        } catch (Exception error) {
            scanResult = ScanResult.ERROR;
            LOG.error("Failed scanning person (ID '{}') data with {} scanner", id, scannerName);
            LOG.warn("Scanning error", error);
        }

        // update person and reset status
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Person {}-'{}', scanned OK", id, person.getName());
            person.setStatus(StatusType.DONE);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            person.setStatus(StatusType.MISSING);
        } else {
            person.setStatus(StatusType.ERROR);
        }
        personDao.updateEntity(person);
    }
}
