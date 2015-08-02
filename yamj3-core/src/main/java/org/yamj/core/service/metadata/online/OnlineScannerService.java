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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.type.SourceType;
import org.yamj.core.service.metadata.nfo.InfoDTO;


@Service("onlineScannerService")
public class OnlineScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(OnlineScannerService.class);
    public static final String MOVIE_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie", "tmdb");
    public static final String MOVIE_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie.alternate", "");
    public static final String MOVIE_SCANNER_PRIO = PropertyTools.getProperty("yamj3.sourcedb.scanner.movie.priority", "");
    public static final String SERIES_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.series", "tvdb");
    public static final String SERIES_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.series.alternate", "");
    public static final String SERIES_SCANNER_PRIO = PropertyTools.getProperty("yamj3.sourcedb.scanner.series.priority", "");
    public static final String PERSON_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.person", "tmdb");
    public static final String PERSON_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.person.alternate", "");
    public static final String PERSON_SCANNER_PRIO = PropertyTools.getProperty("yamj3.sourcedb.scanner.person.priority", "");
    public static final String FILMOGRAPHY_SCANNER = PropertyTools.getProperty("yamj3.sourcedb.scanner.filmography", "tmdb");
    public static final String FILMOGRAPHY_SCANNER_ALT = PropertyTools.getProperty("yamj3.sourcedb.scanner.filmography.alternate", "");
    public static final String FILMOGRAPHY_SCANNER_PRIO = PropertyTools.getProperty("yamj3.sourcedb.filmography.person.priority", "");
    
    private final HashMap<String, IMovieScanner> registeredMovieScanner = new HashMap<>();
    private final HashMap<String, ISeriesScanner> registeredSeriesScanner = new HashMap<>();
    private final HashMap<String, IPersonScanner> registeredPersonScanner = new HashMap<>();
    private final HashMap<String, IFilmographyScanner> registeredFilmographyScanner = new HashMap<>();

    @Autowired
    private ConfigService configService;
    
    /**
     * Register a metadata scanner
     *
     * @param metadataScanner
     */
    public void registerMetadataScanner(IMetadataScanner metadataScanner) {
        if (metadataScanner instanceof IMovieScanner) {
            LOG.trace("Registered movie scanner: {}", metadataScanner.getScannerName().toLowerCase());
            registeredMovieScanner.put(metadataScanner.getScannerName().toLowerCase(), (IMovieScanner)metadataScanner);
        }
        if (metadataScanner instanceof ISeriesScanner) {
            LOG.trace("Registered series scanner: {}", metadataScanner.getScannerName().toLowerCase());
            registeredSeriesScanner.put(metadataScanner.getScannerName().toLowerCase(), (ISeriesScanner)metadataScanner);
        }
        if (metadataScanner instanceof IPersonScanner) {
            LOG.trace("Registered person scanner: {}", metadataScanner.getScannerName().toLowerCase());
            registeredPersonScanner.put(metadataScanner.getScannerName().toLowerCase(), (IPersonScanner)metadataScanner);
        }
        if (metadataScanner instanceof IFilmographyScanner) {
            LOG.trace("Registered filmography scanner: {}", metadataScanner.getScannerName().toLowerCase());
            registeredFilmographyScanner.put(metadataScanner.getScannerName().toLowerCase(), (IFilmographyScanner)metadataScanner);
        }
    }
    
    /**
     * Scan a movie.
     * 
     * @param videoData
     */
    public void scanMovie(VideoData videoData) {
        List<String> scanners = Arrays.asList(MOVIE_SCANNER_PRIO.split(","));
    	ScanResult scanResult = null;

    	for (String scanner : scanners) {
    		IMovieScanner movieScanner = registeredMovieScanner.get(scanner);
            if (movieScanner == null) {
                LOG.error("Movie scanner '{}' not registered", scanner);
                scanResult = ScanResult.ERROR;
            } else {
                // scan video data
                try {
                    if (videoData.isSkippedScan(movieScanner.getScannerName())) {
                        LOG.info("Movie scan skipped for '{}' using {}", videoData.getTitle(), movieScanner.getScannerName());
                        scanResult = ScanResult.SKIPPED;
                    } else {
                        LOG.info("Scanning movie data for '{}' using {}", videoData.getTitle(), movieScanner.getScannerName());
                        scanResult = movieScanner.scan(videoData);
                    }
                } catch (Exception error) {
                    scanResult = ScanResult.ERROR;
                    LOG.error("Failed scanning movie with {} scanner", movieScanner.getScannerName());
                    LOG.warn("Scanning error", error);
                }
            }
            if (ScanResult.OK.equals(scanResult)) {
            	break;
            }
		}       

        
        // evaluate scan result
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Movie {}-'{}', scanned OK", videoData.getId(), videoData.getTitle());
            videoData.setRetries(0);
            videoData.setStatus(StatusType.DONE);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.info("Movie {}-'{}', skipped", videoData.getId(), videoData.getTitle());
            videoData.setRetries(0);
            videoData.setStatus(StatusType.DONE);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Movie {}-'{}', not found", videoData.getId(), videoData.getTitle());
            videoData.setRetries(0);
            videoData.setStatus(StatusType.NOTFOUND);
        } else if (ScanResult.RETRY.equals(scanResult)) {
            LOG.debug("Movie {}-'{}', will be retried", videoData.getId(), videoData.getTitle());
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
    	List<String> scanners = Arrays.asList(SERIES_SCANNER_PRIO.split(","));
		ScanResult scanResult = null;

    	for (String scanner : scanners) {
    		ISeriesScanner seriesScanner = registeredSeriesScanner.get(scanner);
            if (seriesScanner == null) {
                LOG.error("Series scanner '{}' not registered", scanner);
                scanResult = ScanResult.ERROR;
            } else {

                // scan series
                try {
                    if (series.isSkippedScan(seriesScanner.getScannerName())) {
                        LOG.warn("Series scan skipped for '{}' using {}", series.getTitle(), seriesScanner.getScannerName());
                        scanResult = ScanResult.SKIPPED;
                    } else {
                        LOG.info("Scanning series data for '{}' using {}", series.getTitle(), seriesScanner.getScannerName());
                        scanResult = seriesScanner.scan(series);
                    }
                } catch (Exception error) {
                    scanResult = ScanResult.ERROR;
                    LOG.error("Failed scanning series data with {} scanner", seriesScanner.getScannerName());
                    LOG.warn("Scanning error", error);
                }
            }
            
            if (ScanResult.OK.equals(scanResult)) {
            	break;
            }
    	}


        // evaluate scan result
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Series {}-'{}', scanned OK", series.getId(), series.getTitle());
            series.setRetries(0);
            series.setStatus(StatusType.DONE);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.info("Series {}-'{}', skipped", series.getId(), series.getTitle());
            series.setRetries(0);
            series.setStatus(StatusType.DONE);
       } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Series {}-'{}', not found", series.getId(), series.getTitle());
            series.setRetries(0);
            series.setStatus(StatusType.NOTFOUND);
       } else if (ScanResult.RETRY.equals(scanResult)) {
           LOG.debug("Series {}-'{}', will be retried", series.getId(), series.getTitle());
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
    	List<String> scanners = Arrays.asList(PERSON_SCANNER_PRIO.split(","));
    	ScanResult scanResult = null;
        
    	for (String scanner : scanners) {
    		IPersonScanner personScanner = registeredPersonScanner.get(scanner);
            if (personScanner == null) {
                LOG.error("Person scanner '{}' not registered", scanner);
                scanResult = ScanResult.ERROR;
            } else {
                LOG.info("Scanning for information on person '{}' using {}", person.getName(), personScanner.getScannerName());
        
                // scan person data
                try {
                    if (person.isSkippedScan(personScanner.getScannerName())) {
                        LOG.info("Person scan skipped for '{}' using {}", person.getName(), personScanner.getScannerName());
                        scanResult = ScanResult.SKIPPED;
                    } else {
                        LOG.info("Scanning person data for '{}' using {}", person.getName(), personScanner.getScannerName());
                        scanResult = personScanner.scan(person);
                    }
                } catch (Exception error) {
                    scanResult = ScanResult.ERROR;
                    LOG.error("Failed scanning person (ID '{}') data with scanner {} ", person.getId(), personScanner.getScannerName());
                    LOG.warn("Scanning error", error);
                }
            }
            
            if (ScanResult.OK.equals(scanResult)){
            	break;
            }
		}
        
        // evaluate status
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Person {}-'{}', scanned OK", person.getId(), person.getName());
            person.setRetries(0);
            person.setStatus(StatusType.DONE);
            person.setFilmographyStatus(StatusType.NEW);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.info("Person {}-'{}', skipped", person.getId(), person.getName());
            person.setRetries(0);
            person.setStatus(StatusType.DONE);
            person.setFilmographyStatus(StatusType.NEW);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Person {}-'{}', not found", person.getId(), person.getName());
            person.setRetries(0);
            person.setStatus(StatusType.NOTFOUND);
        } else if (ScanResult.RETRY.equals(scanResult)) {
            LOG.debug("Person {}-'{}', will be retried", person.getId(), person.getName());
            person.setRetries(person.getRetries()+1);
            person.setStatus(StatusType.UPDATED);
        } else {
            person.setRetries(0);
            person.setStatus(StatusType.ERROR);
        }
    }

    /**
     * Scan a person.
     * 
     * @param person
     */
    public void scanFilmography(Person person) {
    	List<String> scanners = Arrays.asList(FILMOGRAPHY_SCANNER_PRIO.split(","));
        ScanResult scanResult = null;

        for (String scanner : scanners) {
        	IFilmographyScanner filmographyScanner = registeredFilmographyScanner.get(scanner);
            if (filmographyScanner == null) {
                LOG.error("Filmography scanner '{}' not registered", scanner);
                scanResult = ScanResult.ERROR;
            } else {
            	LOG.info("Scanning for filmography of person '{}' using {}", person.getName(), filmographyScanner.getScannerName());

                // scan filmography
                try {
                    if (person.isSkippedScan(filmographyScanner.getScannerName())) {
                        LOG.info("Filmography scan skipped for '{}' using {}", person.getName(), filmographyScanner.getScannerName());
                        scanResult = ScanResult.SKIPPED;
                    } else {
                        LOG.info("Scanning filmography data for '{}' using {}", person.getName(), filmographyScanner.getScannerName());
                        scanResult = filmographyScanner.scanFilmography(person);
                    }
                } catch (Exception error) {
                    scanResult = ScanResult.ERROR;
                    LOG.error("Failed scanning person filmography (ID '{}') data with scanner {} ", person.getId(), filmographyScanner.getScannerName());
                    LOG.warn("Scanning error", error);
                }
            }
            
            if (ScanResult.OK.equals(scanResult)) {
            	break;
            }
		}
    	

        // evaluate scan result
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Person filmography {}-'{}', scanned OK", person.getId(), person.getName());
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.DONE);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.info("Person filmography {}-'{}', skipped", person.getId(), person.getName());
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.DONE);
        } else if (ScanResult.NO_RESULT.equals(scanResult)) {
            LOG.info("Person filmography {}-'{}', no results", person.getId(), person.getName());
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.NOTFOUND);
        } else if (ScanResult.MISSING_ID.equals(scanResult)) {
            LOG.warn("Person filmography {}-'{}', not found", person.getId(), person.getName());
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.NOTFOUND);
        } else if (ScanResult.RETRY.equals(scanResult)) {
            LOG.debug("Person filmography {}-'{}', will be retried", person.getId(), person.getName());
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
            Set<INfoScanner> nfoScanners = new HashSet<>();
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
    
    public SourceType determineSourceType(MetaDataType type, String source) {
        switch(type) {
            case SERIES:
            case SEASON:
            case EPISODE:
                if (!registeredSeriesScanner.containsKey(source)) {
                    return SourceType.UNKNOWN;
                }
                if (SERIES_SCANNER.equals(source)) {
                    return SourceType.MAIN;
                }
                if (SERIES_SCANNER_ALT.equals(source)) {
                    return SourceType.ALTERNATE;
                }
                return SourceType.KNOWN;
                                
            case MOVIE:
                if (!registeredMovieScanner.containsKey(source)) {
                    return SourceType.UNKNOWN;
                }
                if (MOVIE_SCANNER.equals(source)) {
                    return SourceType.MAIN;
                }
                if (MOVIE_SCANNER_ALT.equals(source)) {
                    return SourceType.ALTERNATE;
                }
                return SourceType.KNOWN;
                
            case PERSON:
                if (!registeredPersonScanner.containsKey(source)) {
                    return SourceType.UNKNOWN;
                }
                if (PERSON_SCANNER.equals(source)) {
                    return SourceType.MAIN;
                }
                if (PERSON_SCANNER_ALT.equals(source)) {
                    return SourceType.ALTERNATE;
                }
                return SourceType.KNOWN;
                
            default:
                return SourceType.UNKNOWN;
        }
    }
}
