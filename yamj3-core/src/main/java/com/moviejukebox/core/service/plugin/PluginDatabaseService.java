package com.moviejukebox.core.service.plugin;

import com.moviejukebox.core.database.dao.CommonDao;
import com.moviejukebox.core.database.dao.MediaDao;
import com.moviejukebox.core.database.dao.PersonDao;
import com.moviejukebox.core.database.model.*;
import com.moviejukebox.core.database.model.dto.CreditDTO;
import com.moviejukebox.core.database.model.dto.QueueDTO;
import com.moviejukebox.core.database.model.type.JobType;
import com.moviejukebox.common.type.StatusType;
import com.moviejukebox.common.tools.PropertyTools;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("pluginDatabaseService")
public class PluginDatabaseService {

    private static final Logger LOG = LoggerFactory.getLogger(PluginDatabaseService.class);
    private static final String VIDEO_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie", "tmdb");
    private static final String VIDEO_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie.alternate", "");
    private static final String SERIES_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.series", "tvdb");
    private static final String SERIES_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.series.alternate", "");
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private CommonDao commonDao;
    private HashMap<String, IMovieScanner> registeredMovieScanner = new HashMap<String, IMovieScanner>();
    private HashMap<String, ISeriesScanner> registeredSeriesScanner = new HashMap<String, ISeriesScanner>();

    public void registerMovieScanner(IMovieScanner movieScanner) {
        registeredMovieScanner.put(movieScanner.getScannerName().toLowerCase(), movieScanner);
    }

    public void registerSeriesScanner(ISeriesScanner seriesScanner) {
        registeredSeriesScanner.put(seriesScanner.getScannerName().toLowerCase(), seriesScanner);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void scanMetadata(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        if (queueElement.isVideoDataElement()) {
            this.scanVideoData(queueElement.getId());
        } else if (queueElement.isSeriesElement()) {
            this.scanSeries(queueElement.getId());
        } else {
            LOG.error("No valid element for scanning metadata '{}'", queueElement);
        }
    }

    private void scanVideoData(Long id) {
        VideoData videoData = mediaDao.getVideoData(id);

        // SCAN MOVIE
        String scannerName = VIDEO_SCANNER;
        LOG.debug("Scanning movie data for '{}' using {}", videoData.getTitle(), scannerName);

        IMovieScanner movieScanner = registeredMovieScanner.get(scannerName);
        if (movieScanner == null) {
            LOG.error("Movie scanner not registered '{}'", scannerName);
            videoData.setStatus(StatusType.ERROR);
            mediaDao.updateEntity(videoData);
            return;
        }

        // scan video data
        ScanResult scanResult = ScanResult.ERROR;
        try {
            scanResult = movieScanner.scan(videoData);
        } catch (Exception error) {
            LOG.error("Failed scanning video data with {} scanner", scannerName);
            LOG.warn("Scanning error", error);
        }

        // SCAN ALTERNATE
        // TODO alternate scanning

        if (!ScanResult.OK.equals(scanResult)) {
            movieScanner = null;

            scannerName = VIDEO_SCANNER_ALT;
            if (StringUtils.isNotBlank(scannerName)) {
                movieScanner = registeredMovieScanner.get(scannerName);
            }

            if (movieScanner != null) {
                try {
                    movieScanner.scan(videoData);
                } catch (Exception error) {
                    LOG.error("Failed scanning video data with {} alternate scanner", scannerName);
                    LOG.warn("Alternate scanning error", error);
                }
            }
        }

        // STORAGE

        // update genres
        HashSet<Genre> genres = new HashSet<Genre>(0);
        for (Genre genre : videoData.getGenres()) {
            Genre stored = commonDao.getGenre(genre.getName());
            if (stored != null) {
                genres.add(stored);
            } else {
                commonDao.saveEntity(genre);
                genres.add(genre);
            }
        }
        videoData.setGenres(genres);

        // update cast and crew
        updateCastCrew(videoData);

        // update video data and reset status
        if (ScanResult.OK.equals(scanResult)) {
            videoData.setStatus(StatusType.DONE);
        } else {
            videoData.setStatus(StatusType.ERROR);
        }
        mediaDao.updateEntity(videoData);
    }

    private void scanSeries(Long id) {
        Series series = mediaDao.getSeries(id);

        // SCAN SERIES
        String scannerName = SERIES_SCANNER;
        LOG.debug("Scanning series data for '{}' using {}", series.getTitle(), scannerName);

        ISeriesScanner seriesScanner = registeredSeriesScanner.get(scannerName);
        if (seriesScanner == null) {
            LOG.error("Series scanner '{}' not registered", scannerName);
            series.setStatus(StatusType.ERROR);
            mediaDao.updateEntity(series);
        }

        // Scan series data
        ScanResult scanResult = ScanResult.ERROR;
        try {
            scanResult = seriesScanner.scan(series);
        } catch (Exception error) {
            LOG.error("Failed scanning series data with {} scanner", scannerName);
            LOG.warn("Scanning error", error);
        }

        // SCAN ALTERNATE
        // TODO alternate scanning

        // STORAGE

        // update genres
        // TODO: Add genres to Series

        // update cast and crew
        // TODO: Add cast & crew to Series

        // update video data and reset status
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

        if (queueElement.isVideoDataElement()) {
            VideoData videoData = mediaDao.getVideoData(queueElement.getId());
            if (videoData != null) {
                videoData.setStatus(StatusType.ERROR);
                mediaDao.updateEntity(videoData);
            }
        }
        // TODO series and season
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
                person = personDao.getPerson(dto.getName());
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
                personDao.saveEntity(person);
            }

            if (castCrew != null) {
                // update role
                if (StringUtils.isBlank(castCrew.getRole())
                        && JobType.ACTOR.equals(castCrew.getJobType())
                        && StringUtils.isNotBlank(dto.getRole())) {
                    castCrew.setRole(dto.getRole());
                    personDao.updateEntity(castCrew);
                }
            } else {
                castCrew = new CastCrew();
                castCrew.setPerson(person);
                castCrew.setVideoData(videoData);
                castCrew.setJobType(dto.getJobType());
                if (StringUtils.isNotBlank(dto.getRole())) {
                    castCrew.setRole(dto.getRole());
                }
                videoData.addCredit(castCrew);
                personDao.saveEntity(castCrew);
            }
        }
    }
}
