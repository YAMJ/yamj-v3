/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.StatusType;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.metadata.nfo.InfoDTO;

@Service("onlineScannerService")
public class OnlineScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(OnlineScannerService.class);
    public static final String MOVIE_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie", "tmdb");
    public static final String MOVIE_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie.alternate", "");
    public static final String SERIES_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.series", "tvdb");
    public static final String SERIES_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.series.alternate", "");
    public static final String PERSON_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.person", "tmdb");
    public static final String PERSON_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.person.alternate", "");
    
    private final HashMap<String, IMovieScanner> registeredMovieScanner = new HashMap<String, IMovieScanner>();
    private final HashMap<String, ISeriesScanner> registeredSeriesScanner = new HashMap<String, ISeriesScanner>();
    private final HashMap<String, IPersonScanner> registeredPersonScanner = new HashMap<String, IPersonScanner>();
    private final HashMap<String, IFilmographyScanner> registeredFilmographyScanner = new HashMap<String, IFilmographyScanner>();

    @Autowired
    private ConfigService configService;
    
    /**
     * Register a movie scanner
     *
     * @param movieScanner
     */
    public void registerMovieScanner(IMovieScanner movieScanner) {
        LOG.trace("Registered movie scanner: {}", movieScanner.getScannerName().toLowerCase());
        registeredMovieScanner.put(movieScanner.getScannerName().toLowerCase(), movieScanner);
    }

    /**
     * Register TV series scanner
     *
     * @param seriesScanner
     */
    public void registerSeriesScanner(ISeriesScanner seriesScanner) {
        LOG.trace("Registered series scanner: {}", seriesScanner.getScannerName().toLowerCase());
        registeredSeriesScanner.put(seriesScanner.getScannerName().toLowerCase(), seriesScanner);
    }

    /**
     * Register person scanner
     *
     * @param personScanner
     */
    public void registerPersonScanner(IPersonScanner personScanner) {
        LOG.trace("Registered person scanner: {}", personScanner.getScannerName().toLowerCase());
        registeredPersonScanner.put(personScanner.getScannerName().toLowerCase(), personScanner);
        
        if (personScanner instanceof IFilmographyScanner) {
            IFilmographyScanner filmographyScanner = (IFilmographyScanner)personScanner;
            LOG.trace("Registered filmography scanner: {}", filmographyScanner.getScannerName().toLowerCase());
            registeredFilmographyScanner.put(filmographyScanner.getScannerName().toLowerCase(), filmographyScanner);
        }
    }
    
    /**
     * Scan a movie.
     * 
     * @param videoData
     */
    public void scanMovie(VideoData videoData) {
        ScanResult scanResult;

        IMovieScanner movieScanner = registeredMovieScanner.get(MOVIE_SCANNER);
        if (movieScanner == null) {
            LOG.error("Movie scanner '{}' not registered", MOVIE_SCANNER);
            scanResult = ScanResult.ERROR;
        } else {
            LOG.info("Scanning movie data for '{}' using {}", videoData.getTitle(), MOVIE_SCANNER);

            // scan video data
            try {
                if (videoData.isSkippedOnlineScan(movieScanner.getScannerName())) {
                    scanResult = ScanResult.SKIPPED;
                } else {
                    scanResult = movieScanner.scan(videoData);
                }
            } catch (Exception error) {
                scanResult = ScanResult.ERROR;
                LOG.error("Failed scanning movie with {} scanner", MOVIE_SCANNER);
                LOG.warn("Scanning error", error);
            }
        }
        
        // alternate scanning if main scanner failed or not registered
        boolean useAlternate = this.configService.getBooleanProperty("yamj3.sourcedb.scanner.movie.alternate.always", Boolean.FALSE);
        if (!ScanResult.OK.equals(scanResult) || useAlternate) {
            movieScanner = registeredMovieScanner.get(MOVIE_SCANNER_ALT);

            if (movieScanner != null && !videoData.isSkippedOnlineScan(movieScanner.getScannerName())) {
                LOG.debug("Alternate scanning movie data for '{}' using {}", videoData.getTitle(), MOVIE_SCANNER_ALT);

                try {
                    movieScanner.scan(videoData);
                } catch (Exception error) {
                    LOG.error("Failed scanning movie with {} alternate scanner", MOVIE_SCANNER_ALT);
                    LOG.warn("Alternate scanning error", error);
                }
            }
        }

        // evaluate scan result
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Movie {}-'{}', scanned OK", videoData.getId(), videoData.getTitle());
            videoData.setRetries(0);
            videoData.setStatus(StatusType.DONE);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.warn("Movie {}-'{}', skipped", videoData.getId(), videoData.getTitle());
            videoData.setRetries(0);
            videoData.setStatus(StatusType.DONE);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Movie {}-'{}', not found", videoData.getId(), videoData.getTitle());
            videoData.setRetries(0);
            videoData.setStatus(StatusType.NOTFOUND);
        } else if (ScanResult.RETRY.equals(scanResult)) {
            LOG.warn("Movie {}-'{}', will be retried", videoData.getId(), videoData.getTitle());
            videoData.setRetries(videoData.getRetries()+1);
            videoData.setStatus(StatusType.UPDATED);
        } else {
            videoData.setRetries(0);
            videoData.setStatus(StatusType.ERROR);
        }
    }

    /**
     * Scan a series.
     * 
     * @param series
     */
    public void scanSeries(Series series) {
        ScanResult scanResult;

        ISeriesScanner seriesScanner = registeredSeriesScanner.get(SERIES_SCANNER);
        if (seriesScanner == null) {
            LOG.error("Series scanner '{}' not registered", SERIES_SCANNER);
            scanResult = ScanResult.ERROR;
        } else {
            LOG.info("Scanning series data for '{}' using {}", series.getTitle(), SERIES_SCANNER);

            // scan series
            try {
                if (series.isSkippedOnlineScan(seriesScanner.getScannerName())) {
                    scanResult = ScanResult.SKIPPED;
                } else {
                    scanResult = seriesScanner.scan(series);
                }
            } catch (Exception error) {
                scanResult = ScanResult.ERROR;
                LOG.error("Failed scanning series data with {} scanner", SERIES_SCANNER);
                LOG.warn("Scanning error", error);
            }
        }
        
        // alternate scanning if main scanner failed
        boolean useAlternate = this.configService.getBooleanProperty("yamj3.sourcedb.scanner.series.alternate.always", Boolean.FALSE);
        if (!ScanResult.OK.equals(scanResult) || useAlternate) {
            seriesScanner = registeredSeriesScanner.get(SERIES_SCANNER_ALT);

            if (seriesScanner != null && !series.isSkippedOnlineScan(seriesScanner.getScannerName())) {
                LOG.debug("Alternate scanning series data for '{}' using {}", series.getTitle(), SERIES_SCANNER_ALT);

                try {
                    seriesScanner.scan(series);
                } catch (Exception error) {
                    LOG.error("Failed scanning series data with {} alternate scanner", SERIES_SCANNER_ALT);
                    LOG.warn("Alternate scanning error", error);
                }
            }
        }

        // evaluate scan result
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Series {}-'{}', scanned OK", series.getId(), series.getTitle());
            series.setRetries(0);
            series.setStatus(StatusType.DONE);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.warn("Series {}-'{}', skipped", series.getId(), series.getTitle());
            series.setRetries(0);
            series.setStatus(StatusType.DONE);
       } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Series {}-'{}', not found", series.getId(), series.getTitle());
            series.setRetries(0);
            series.setStatus(StatusType.NOTFOUND);
       } else if (ScanResult.RETRY.equals(scanResult)) {
           LOG.warn("Series {}-'{}', not found", series.getId(), series.getTitle());
           series.setRetries(series.getRetries()+1);
           series.setStatus(StatusType.UPDATED);
        } else {
            series.setRetries(0);
            series.setStatus(StatusType.ERROR);
        }
    }

    /**
     * Scan a person.
     * 
     * @param person
     */
    public void scanPerson(Person person) {
        ScanResult scanResult;
        
        IPersonScanner personScanner = registeredPersonScanner.get(PERSON_SCANNER);
        if (personScanner == null) {
            LOG.error("Person scanner '{}' not registered", PERSON_SCANNER);
            scanResult = ScanResult.ERROR;
        } else {
            LOG.info("Scanning for information on person {}-'{}' using {}", person.getId(), person.getName(), PERSON_SCANNER);
    
            // scan person data
            try {
                scanResult = personScanner.scan(person);
            } catch (Exception error) {
                scanResult = ScanResult.ERROR;
                LOG.error("Failed scanning person (ID '{}') data with scanner {} ", person.getId(), PERSON_SCANNER);
                LOG.warn("Scanning error", error);
            }
        }
        
        // alternate scanning if main scanner failed or not registered
        boolean useAlternate = this.configService.getBooleanProperty("yamj3.sourcedb.scanner.person.alternate.always", Boolean.FALSE);
        if (!ScanResult.OK.equals(scanResult) || useAlternate) {
            personScanner = registeredPersonScanner.get(PERSON_SCANNER_ALT);

            if (personScanner != null) {
                LOG.info("Alternate scanning for information on person {}-'{}' using {}", person.getId(), person.getName(), PERSON_SCANNER_ALT);

                try {
                    personScanner.scan(person);
                } catch (Exception error) {
                    LOG.error("Failed scanning person (ID '{}') data with alternate scanner {}", person.getId(), PERSON_SCANNER_ALT);
                    LOG.warn("Alternate scanning error", error);
                }
            }
        }

        // evaluate status
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Person {}-'{}', scanned OK", person.getId(), person.getName());
            person.setRetries(0);
            person.setStatus(StatusType.DONE);
            person.setFilmographyStatus(StatusType.NEW);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.warn("Person {}-'{}', skipped", person.getId(), person.getName());
            person.setRetries(0);
            person.setStatus(StatusType.DONE);
            person.setFilmographyStatus(StatusType.NEW);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Person {}-'{}', not found", person.getId(), person.getName());
            person.setRetries(0);
            person.setStatus(StatusType.NOTFOUND);
        } else if (ScanResult.RETRY.equals(scanResult)) {
            LOG.warn("Person {}-'{}', will be retried", person.getId(), person.getName());
            person.setRetries(person.getRetries()+1);
            person.setStatus(StatusType.UPDATED);
        } else {
            person.setRetries(0);
            person.setStatus(StatusType.ERROR);
        }
    }

    /**
     * Check if filmography scan is enabled.
     * 
     * @return true, if filmography scanner has been set, else false
     */
    public boolean isFilmographyScanEnabled() {
        IFilmographyScanner filmographyScanner = registeredFilmographyScanner.get(PERSON_SCANNER);
        if (filmographyScanner == null) {
            return false;
        }
        return filmographyScanner.isFilmographyScanEnabled();
    }

    /**
     * Scan a person.
     * 
     * @param person
     */
    public void scanFilmography(Person person) {
        ScanResult scanResult;
        
        IFilmographyScanner filmographyScanner = registeredFilmographyScanner.get(PERSON_SCANNER);
        if (filmographyScanner == null) {
            LOG.error("Filmography scanner '{}' not registered", PERSON_SCANNER);
            scanResult = ScanResult.ERROR;
        } else {
            LOG.info("Scanning for filmography of person {}-'{}' using {}", person.getId(), person.getName(), PERSON_SCANNER);
    
            // scan person data
            try {
                scanResult = filmographyScanner.scanFilmography(person);
            } catch (Exception error) {
                scanResult = ScanResult.ERROR;
                LOG.error("Failed scanning person filmography (ID '{}') data with scanner {} ", person.getId(), PERSON_SCANNER);
                LOG.warn("Scanning error", error);
            }
        }
        
        // evaluate status
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Person filmography {}-'{}', scanned OK", person.getId(), person.getName());
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.DONE);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.warn("Person filmography {}-'{}', skipped", person.getId(), person.getName());
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.DONE);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Person filmography {}-'{}', not found", person.getId(), person.getName());
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.NOTFOUND);
        } else if (ScanResult.RETRY.equals(scanResult)) {
            LOG.warn("Person filmography {}-'{}', will be retried", person.getId(), person.getName());
            person.setRetries(person.getRetries()+1);
            person.setFilmographyStatus(StatusType.UPDATED);
        } else {
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.ERROR);
        }
    }

    public boolean scanNFO(String nfoContent, InfoDTO dto) {
        INfoScanner nfoScanner;
        if (dto.isTvShow()) {
            nfoScanner = this.registeredSeriesScanner.get(SERIES_SCANNER);
        } else {
            nfoScanner = this.registeredMovieScanner.get(MOVIE_SCANNER);
        }

        boolean autodetect = this.configService.getBooleanProperty("nfo.autodetect.scanner", Boolean.FALSE);
        boolean ignorePresentId = this.configService.getBooleanProperty("nfo.ignore.present.id", Boolean.FALSE);

        boolean foundInfo = false;
        if (nfoScanner != null) {
            foundInfo = nfoScanner.scanNFO(nfoContent, dto, ignorePresentId);
        }
        
        if (autodetect && !foundInfo) {
            Set<INfoScanner> nfoScanners = new HashSet<INfoScanner>();
            if (dto.isTvShow()) {
                nfoScanners.addAll(this.registeredSeriesScanner.values());
            } else {
                nfoScanners.addAll(this.registeredMovieScanner.values());
            }
            
            for (INfoScanner autodetectScanner : nfoScanners) {
                foundInfo = autodetectScanner.scanNFO(nfoContent, dto, ignorePresentId);
                if (foundInfo) {
                    // set auto-detected scanner
                    dto.setOnlineScanner(autodetectScanner.getScannerName());
                    break;
                }
            }
        }
        
        return foundInfo;
    }
}
