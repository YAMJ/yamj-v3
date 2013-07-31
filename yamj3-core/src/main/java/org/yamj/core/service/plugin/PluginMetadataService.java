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

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.service.MetadataStorageService;

@Service("pluginMetadataService")
public class PluginMetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(PluginMetadataService.class);
    public static final String MOVIE_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie", "tmdb");
    public static final String MOVIE_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie.alternate", "");
    public static final String SERIES_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.series", "tvdb");
    public static final String SERIES_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.series.alternate", "");
    private static final String PERSON_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.person", "tmdb");
    // locks for calling database methods synchronized
    private static final ReentrantLock STORE_GENRE_LOCK = new ReentrantLock();
    private static final ReentrantLock STORE_PERSON_LOCK = new ReentrantLock();

    @Autowired
    private MetadataStorageService metadataStorageService;

    private HashMap<String, IMovieScanner> registeredMovieScanner = new HashMap<String, IMovieScanner>();
    private HashMap<String, ISeriesScanner> registeredSeriesScanner = new HashMap<String, ISeriesScanner>();
    private HashMap<String, IPersonScanner> registeredPersonScanner = new HashMap<String, IPersonScanner>();

    public void registerMovieScanner(IMovieScanner movieScanner) {
        LOG.info("Registered movie scanner: {}", movieScanner.getScannerName().toLowerCase());
        registeredMovieScanner.put(movieScanner.getScannerName().toLowerCase(), movieScanner);
    }

    public void registerSeriesScanner(ISeriesScanner seriesScanner) {
        LOG.info("Registered series scanner: {}", seriesScanner.getScannerName().toLowerCase());
        registeredSeriesScanner.put(seriesScanner.getScannerName().toLowerCase(), seriesScanner);
    }

    public void registerPersonScanner(IPersonScanner personScanner) {
        LOG.info("Registered person scanner: {}", personScanner.getScannerName().toLowerCase());
        registeredPersonScanner.put(personScanner.getScannerName().toLowerCase(), personScanner);
    }

    public void scanMovie(Long id) {
        IMovieScanner movieScanner = registeredMovieScanner.get(MOVIE_SCANNER);
        if (movieScanner == null) {
            throw new RuntimeException("Movie scanner '" + MOVIE_SCANNER  + "' not registered");
        }

        VideoData videoData = metadataStorageService.getRequiredVideoData(id);
        LOG.debug("Scanning movie data for '{}' using {}", videoData.getTitle(), MOVIE_SCANNER);

        // scan video data
        ScanResult scanResult;
        try {
            scanResult = movieScanner.scan(videoData);
        } catch (Exception error) {
            scanResult = ScanResult.ERROR;
            LOG.error("Failed scanning movie with {} scanner", MOVIE_SCANNER);
            LOG.warn("Scanning error", error);
        }

        // alternate scanning if main scanner failed
        if (!ScanResult.OK.equals(scanResult)) {
            movieScanner = registeredMovieScanner.get(MOVIE_SCANNER_ALT);

            if (movieScanner != null) {
                LOG.debug("Alternate scanning movie data for '{}' using {}", videoData.getTitle(), MOVIE_SCANNER_ALT);

                try {
                    movieScanner.scan(videoData);
                } catch (Exception error) {
                    LOG.error("Failed scanning movie with {} alternate scanner", MOVIE_SCANNER_ALT);
                    LOG.warn("Alternate scanning error", error);
                }
            }
        }

        // store associated entities
        if (!storeAssociatedEntities(videoData)) {
            // exit if associated entities couldn't be stored
            return;
        }

        // update data in database
        try {
            if (ScanResult.OK.equals(scanResult)) {
                LOG.debug("Movie {}-'{}', scanned OK", id, videoData.getTitle());
                videoData.setStatus(StatusType.DONE);
            } else if (ScanResult.MISSING_ID.equals(scanResult)){
                LOG.warn("Movie {}-'{}', not found", id, videoData.getTitle());
                videoData.setStatus(StatusType.NOTFOUND);
            } else {
                videoData.setStatus(StatusType.ERROR);
            }

            LOG.debug("Update video data in database: {}-'{}'", videoData.getId(), videoData.getTitle());
            metadataStorageService.updateVideoData(videoData);
        } catch (Exception error) {
            // NOTE: status will not be changed
            LOG.error("Failed storing movie {}-'{}'", id, videoData.getTitle());
            LOG.warn("Storage error", error);
        }
    }

    public void scanSeries(Long id) {
        ISeriesScanner seriesScanner = registeredSeriesScanner.get(SERIES_SCANNER);
        if (seriesScanner == null) {
            throw new RuntimeException("Series scanner '" + SERIES_SCANNER  + "' not registered");
        }

        Series series = metadataStorageService.getRequiredSeries(id);
        LOG.debug("Scanning series data for '{}' using {}", series.getTitle(), SERIES_SCANNER);

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
        // TODO enable alter scanning if requested

        if (!ScanResult.OK.equals(scanResult)) {
            seriesScanner = registeredSeriesScanner.get(SERIES_SCANNER_ALT);

            if (seriesScanner != null) {
                LOG.debug("Alternate scanning series data for '{}' using {}", series.getTitle(), SERIES_SCANNER_ALT);

                try {
                    seriesScanner.scan(series);
                } catch (Exception error) {
                    LOG.error("Failed scanning series data with {} alternate scanner", SERIES_SCANNER_ALT);
                    LOG.warn("Alternate scanning error", error);
                }
            }
        }

        // store associated entities
        boolean result = true;
        for (Season season : series.getSeasons()) {
            for (VideoData videoData : season.getVideoDatas()) {
                if (!storeAssociatedEntities(videoData)) {
                    result = false;
                }
            }
        }
        if (!result) {
            // exit if associated entities couldn't be stored
            return;
        }

        // update data in database
        try {
            if (ScanResult.OK.equals(scanResult)) {
                LOG.debug("Series {}-'{}', scanned OK", id, series.getTitle());
                series.setStatus(StatusType.DONE);
            } else if (ScanResult.MISSING_ID.equals(scanResult)) {
                LOG.warn("Series {}-'{}', not found", id, series.getTitle());
                series.setStatus(StatusType.NOTFOUND);
            } else {
                series.setStatus(StatusType.ERROR);
            }

            LOG.debug("Update series in database: {}-'{}'", series.getId(), series.getTitle());
            metadataStorageService.updateSeries(series);
        } catch (Exception error) {
            // NOTE: status will not be changed
            LOG.error("Failed storing series {}-'{}'", id, series.getTitle());
            LOG.warn("Storage error", error);
        }
    }

    private boolean storeAssociatedEntities(VideoData videoData) {
        boolean result = true;

        // store genres
        for (String genreName : videoData.getGenreNames()) {
            STORE_GENRE_LOCK.lock();
            try {
                metadataStorageService.storeGenre(genreName);
            } catch (Exception error) {
                LOG.error("Failed to store genre '{}'", genreName);
                LOG.warn("Storage error", error);
                result = false;
            } finally {
                STORE_GENRE_LOCK.unlock();
            }
        }

        // store persons
        for (CreditDTO creditDTO : videoData.getCreditDTOS()) {
            STORE_PERSON_LOCK.lock();
            try {
                metadataStorageService.storePerson(creditDTO);
            } catch (Exception error) {
                LOG.error("Failed to store person '{}'", creditDTO.getName());
                LOG.warn("Storage error", error);
                result = false;
            } finally {
                STORE_PERSON_LOCK.unlock();
            }
        }

        return result;
    }

    /**
     * Scan the data site for information on the person
     */
    public void scanPerson(Long id) {
        IPersonScanner personScanner = registeredPersonScanner.get(PERSON_SCANNER);
        if (personScanner == null) {
            throw new RuntimeException("Person scanner '" + PERSON_SCANNER  + "' not registered");
        }

        Person person = metadataStorageService.getRequiredPerson(id);
        LOG.info("Scanning for information on person {}-'{}' using {}", id, person.getName(), PERSON_SCANNER);

        // scan person data
        ScanResult scanResult;
        try {
            scanResult = personScanner.scan(person);
        } catch (Exception error) {
            scanResult = ScanResult.ERROR;
            LOG.error("Failed scanning person (ID '{}') data with {} scanner", id, PERSON_SCANNER);
            LOG.warn("Scanning error", error);
        }

        // set status
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Person {}-'{}', scanned OK", id, person.getName());
            person.setStatus(StatusType.DONE);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Person {}-'{}', not found", id, person.getName());
            person.setStatus(StatusType.NOTFOUND);
        } else {
            person.setStatus(StatusType.ERROR);
        }

        // update data in database
        try {
            LOG.debug("Update person in database: {}-'{}'", person.getId(), person.getName());
            metadataStorageService.updatePerson(person);
        } catch (Exception error) {
            // NOTE: status will not be changed
            LOG.error("Failed storing person {}-'{}'", id, person.getName());
            LOG.warn("Storage error", error);
        }
    }

    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        if (queueElement.isMetadataType(MetaDataType.MOVIE)) {
            metadataStorageService.errorVideoData(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.SERIES)) {
            metadataStorageService.errorSeries(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.PERSON)) {
            metadataStorageService.errorPerson(queueElement.getId());
        }
    }
}
