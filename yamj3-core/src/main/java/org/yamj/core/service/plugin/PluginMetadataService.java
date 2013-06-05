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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.MetaDataType;
import org.yamj.core.database.service.MetadataStorageService;

@Service("pluginMetadataService")
public class PluginMetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(PluginMetadataService.class);
    public static final String VIDEO_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie", "tmdb");
    public static final String VIDEO_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie.alternate", "");
    public static final String SERIES_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.series", "tvdb");
    public static final String SERIES_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.series.alternate", "");
    private static final String PERSON_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.person", "tmdb");

    @Autowired
    private MetadataStorageService metadataStorageService;

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

    public void scanVideoData(Long id) {
        VideoData videoData = metadataStorageService.getRequiredVideoData(id);

        // SCAN MOVIE
        LOG.debug("Scanning movie data for '{}' using {}", videoData.getTitle(), VIDEO_SCANNER);

        IMovieScanner movieScanner = registeredMovieScanner.get(VIDEO_SCANNER);
        if (movieScanner == null) {
            LOG.error("Video data scanner not registered '{}'", VIDEO_SCANNER);
            videoData.setStatus(StatusType.ERROR);
            metadataStorageService.update(videoData);
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

        // set status
        if (ScanResult.OK.equals(scanResult)) {
            videoData.setStatus(StatusType.DONE);
        } else if (ScanResult.MISSING_ID.equals(scanResult)){
            videoData.setStatus(StatusType.MISSING);
        } else {
            videoData.setStatus(StatusType.ERROR);
        }

        // storage
        metadataStorageService.store(videoData);
    }

    public void scanSeries(Long id) {
        Series series = metadataStorageService.getRequiredSeries(id);

        // SCAN SERIES
        LOG.debug("Scanning series data for '{}' using {}", series.getTitle(), SERIES_SCANNER);

        ISeriesScanner seriesScanner = registeredSeriesScanner.get(SERIES_SCANNER);
        if (seriesScanner == null) {
            LOG.error("Series scanner '{}' not registered", SERIES_SCANNER);
            series.setStatus(StatusType.ERROR);
            metadataStorageService.update(series);
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

        // set status
        if (ScanResult.OK.equals(scanResult)) {
            series.setStatus(StatusType.DONE);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            series.setStatus(StatusType.MISSING);
        } else {
            series.setStatus(StatusType.ERROR);
        }

        // storage
        metadataStorageService.store(series);
    }

    /**
     * Scan the data site for information on the person
     */
    public void scanPerson(Long id) {
        String scannerName = PERSON_SCANNER;
        IPersonScanner personScanner = registeredPersonScanner.get(scannerName);
        Person person = metadataStorageService.getRequiredPerson(id);

        LOG.info("Scanning for information on person {}-'{}' using {}", id, person.getName(), scannerName);

        if (personScanner == null) {
            LOG.error("Person scanner '{}' not registered", scannerName);
            person.setStatus(StatusType.ERROR);
            metadataStorageService.update(person);
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

        // storage
        metadataStorageService.store(person);
    }

    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        if (queueElement.isMetadataType(MetaDataType.VIDEODATA)) {
            metadataStorageService.errorVideoData(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.SERIES)) {
            metadataStorageService.errorSeries(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.PERSON)) {
            metadataStorageService.errorPerson(queueElement.getId());
        }
    }
}
