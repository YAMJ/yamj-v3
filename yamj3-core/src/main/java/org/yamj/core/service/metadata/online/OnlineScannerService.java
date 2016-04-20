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

import java.util.*;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.*;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.plugin.api.common.PluginMetadataService;
import org.yamj.plugin.api.metadata.*;
import org.yamj.plugin.api.web.TemporaryUnavailableException;
import ro.fortsoft.pf4j.PluginManager;

@Service("onlineScannerService")
@DependsOn("pluginManager")
public class OnlineScannerService implements PluginMetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(OnlineScannerService.class);
    public static final Set<String> MOVIE_SCANNER = PropertyTools.getPropertyAsOrderedSet("yamj3.sourcedb.scanner.movie", "tmdb,imdb");
    public static final Set<String> SERIES_SCANNER = PropertyTools.getPropertyAsOrderedSet("yamj3.sourcedb.scanner.series", "tvdb,tmdb");
    public static final Set<String> PERSON_SCANNER = PropertyTools.getPropertyAsOrderedSet("yamj3.sourcedb.scanner.person", "tmdb,imdb");
    public static final Set<String> FILMOGRAPHY_SCANNER = PropertyTools.getPropertyAsOrderedSet("yamj3.sourcedb.scanner.filmography", "tmdb");
    private static final String SCANNING_ERROR = "Scanning error";
    
    private final HashMap<String, IMovieScanner> registeredMovieScanner = new HashMap<>();
    private final HashMap<String, ISeriesScanner> registeredSeriesScanner = new HashMap<>();
    private final HashMap<String, IPersonScanner> registeredPersonScanner = new HashMap<>();
    private final HashMap<String, IFilmographyScanner> registeredFilmographyScanner = new HashMap<>();

    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private PoolingHttpClient poolingHttpClient;
    @Autowired
    private PluginManager pluginManager;
    @Autowired
    private IdentifierService identifierService;
    @Autowired
    private AllocineScanner allocineScanner;
    @Autowired
    private ImdbScanner imdbScanner;
    @Autowired
    private TheMovieDbScanner theMovieDbScanner;
    @Autowired
    private TheTVDbScanner theTVDbScanner;
    
    @PostConstruct
    public void init() {
        LOG.debug("Initialize online scanner");
        this.registerMetadataScanner(allocineScanner);
        this.registerMetadataScanner(imdbScanner);
        this.registerMetadataScanner(theMovieDbScanner);
        this.registerMetadataScanner(theTVDbScanner);
        
        // add movie scanner to online scanner service
        for (MovieScanner movieScanner : pluginManager.getExtensions(MovieScanner.class)) {
            movieScanner.init(configServiceWrapper, this, localeService, poolingHttpClient);
            PluginMovieScanner scanner = new PluginMovieScanner(movieScanner, localeService, identifierService);
            this.registerMetadataScanner(scanner);
        }
        
        // add series scanner to online scanner service
        for (SeriesScanner seriesScanner : pluginManager.getExtensions(SeriesScanner.class)) {
            seriesScanner.init(configServiceWrapper, this, localeService, poolingHttpClient);
            PluginSeriesScanner scanner = new PluginSeriesScanner(seriesScanner, localeService, identifierService);
            this.registerMetadataScanner(scanner);
        }

        // add person scanner to online scanner service
        for (PersonScanner personScanner : pluginManager.getExtensions(PersonScanner.class)) {
            personScanner.init(configServiceWrapper, this, localeService, poolingHttpClient);
            PluginPersonScanner scanner = new PluginPersonScanner(personScanner);
            this.registerMetadataScanner(scanner);
        }

        // add filmography scanner to online scanner service
        for (FilmographyScanner filmographyScanner : pluginManager.getExtensions(FilmographyScanner.class)) {
            filmographyScanner.init(configServiceWrapper, this, localeService, poolingHttpClient);
            PluginFilmographyScanner scanner = new PluginFilmographyScanner(filmographyScanner, localeService);
            this.registerMetadataScanner(scanner);
        }
    }

    /**
     * Register a metadata scanner
     *
     * @param metadataScanner
     */
    private void registerMetadataScanner(IMetadataScanner metadataScanner) {
        final String scannerName =  metadataScanner.getScannerName().toLowerCase();
        
        if (metadataScanner instanceof IMovieScanner) {
            LOG.trace("Registered movie scanner: {}", scannerName);
            registeredMovieScanner.put(scannerName, (IMovieScanner)metadataScanner);
        }
        if (metadataScanner instanceof ISeriesScanner) {
            LOG.trace("Registered series scanner: {}", scannerName);
            registeredSeriesScanner.put(scannerName, (ISeriesScanner)metadataScanner);
        }
        if (metadataScanner instanceof IPersonScanner) {
            LOG.trace("Registered person scanner: {}", scannerName);
            registeredPersonScanner.put(scannerName, (IPersonScanner)metadataScanner);
        }
        if (metadataScanner instanceof IFilmographyScanner) {
            LOG.trace("Registered filmography scanner: {}", scannerName);
            registeredFilmographyScanner.put(scannerName, (IFilmographyScanner)metadataScanner);
        }
    }
    
    /**
     * Scan a movie.
     * 
     * @param videoData
     */
    public void scanMovie(VideoData videoData) {
        if (videoData.isAllScansSkipped()) {
            LOG.info("All movie scans skipped for {}-'{}'", videoData.getId(), videoData.getTitle());
            videoData.setRetries(0);
            videoData.setStatus(StatusType.DONE);
            return;
        }
        
        final boolean useAlternate = this.configServiceWrapper.getBooleanProperty("yamj3.sourcedb.scanner.movie.alternate.always", false);
        final boolean throwTempError = configServiceWrapper.getBooleanProperty("yamj3.error.throwTempUnavailableError", true);
        ScanResult scanResult = null;
        
    	loop: for (String scanner : MOVIE_SCANNER) {
    	    // holds the inner scan result
    	    ScanResult innerResult = ScanResult.NO_RESULT;
    	    
    		IMovieScanner movieScanner = registeredMovieScanner.get(scanner);
            if (movieScanner == null) {
                LOG.error("Movie scanner '{}' not registered", scanner);
            } else {
                // scan video data
                try {
                    if (videoData.isSkippedScan(movieScanner.getScannerName())) {
                        LOG.info("Movie scan skipped for '{}' using {}", videoData.getTitle(), movieScanner.getScannerName());
                        innerResult = ScanResult.SKIPPED;
                    } else {
                        LOG.info("Scanning movie data for '{}' using {}", videoData.getTitle(), movieScanner.getScannerName());
                        innerResult = movieScanner.scanMovie(videoData, throwTempError);
                    }
                } catch (TemporaryUnavailableException ex) {
                    // check retry
                    if (scanResult == null && videoData.getRetries() < configServiceWrapper.getIntProperty("yamj3.error.maxRetries.movie", 0)) {
                        LOG.info("{} service temporary not available; trigger retry: '{}'", movieScanner.getScannerName(), videoData.getIdentifier());
                        innerResult = ScanResult.RETRY;
                    } else {
                        LOG.error("Temporary scanning error for movie '{}' with {} scanner", videoData.getIdentifier(), movieScanner.getScannerName());
                        LOG.warn(SCANNING_ERROR, ex);
                    }
                } catch (Exception ex) {
                    LOG.error("Failed scanning movie '{}' with {} scanner", videoData.getIdentifier(), movieScanner.getScannerName());
                    LOG.warn(SCANNING_ERROR, ex);
                }
            }

            if (ScanResult.OK.equals(innerResult)) {
                // scanned OK
                scanResult = ScanResult.OK;
                // no alternate scanning then break the loop
                if (!useAlternate) {
                    break loop;
                }
            } else if (ScanResult.SKIPPED.equals(innerResult)) {
                // change nothing if scan skipped and force next scan
            } else {
                // just set scan result to inner result if no scan result before
                scanResult = (scanResult == null) ? innerResult : scanResult;
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
        if (series.isAllScansSkipped()) {
            LOG.info("All series scans skipped for {}-'{}', skipped", series.getId(), series.getTitle());
            skipSeries(series);
            return;
        }
        
        final boolean useAlternate = this.configServiceWrapper.getBooleanProperty("yamj3.sourcedb.scanner.series.alternate.always", false);
        final boolean throwTempError = this.configServiceWrapper.getBooleanProperty("yamj3.error.throwTempUnavailableError", true);
		ScanResult scanResult = null;

    	loop: for (String scanner : SERIES_SCANNER) {
            // holds the inner scan result
            ScanResult innerResult = ScanResult.NO_RESULT;
            
    		ISeriesScanner seriesScanner = registeredSeriesScanner.get(scanner);
            if (seriesScanner == null) {
                LOG.error("Series scanner '{}' not registered", scanner);
            } else {
                // scan series
                try {
                    if (series.isSkippedScan(seriesScanner.getScannerName())) {
                        LOG.info("Series scan skipped for '{}' using {}", series.getTitle(), seriesScanner.getScannerName());
                        innerResult = ScanResult.SKIPPED;
                    } else {
                        LOG.info("Scanning series data for '{}' using {}", series.getTitle(), seriesScanner.getScannerName());
                        innerResult = seriesScanner.scanSeries(series, throwTempError);
                    }
                } catch (TemporaryUnavailableException ex) {
                    // check retry
                    if (scanResult == null && series.getRetries() < configServiceWrapper.getIntProperty("yamj3.error.maxRetries.tvshow", 0)) {
                        LOG.info("{} service temporary not available; trigger retry: '{}'", seriesScanner.getScannerName(), series.getIdentifier());
                        innerResult = ScanResult.RETRY;
                    } else {
                        LOG.error("Temporary scanning error for series '{}' with {} scanner", series.getIdentifier(), seriesScanner.getScannerName());
                        LOG.warn(SCANNING_ERROR, ex);
                    }
                } catch (Exception error) {
                    LOG.error("Failed scanning series '{}' with {} scanner", series.getIdentifier(), seriesScanner.getScannerName());
                    LOG.warn(SCANNING_ERROR, error);
                }
            }
            
            if (ScanResult.OK.equals(innerResult)) {
                // scanned OK
                scanResult = ScanResult.OK;
                // no alternate scanning then break the loop
                if (!useAlternate) {
                    break loop;
                }
            } else if (ScanResult.SKIPPED.equals(innerResult)) {
                // change nothing if scan skipped and force next scan
            } else {
                // just set scan result to inner result if no scan result before
                scanResult = (scanResult == null) ? innerResult : scanResult;
            }
    	}

        // evaluate scan result
        if (ScanResult.OK.equals(scanResult)) {
            LOG.debug("Series {}-'{}', scanned OK", series.getId(), series.getTitle());
            series.setRetries(0);
            series.setStatus(StatusType.DONE);
        } else if (ScanResult.SKIPPED.equals(scanResult)) {
            LOG.info("Series {}-'{}', skipped", series.getId(), series.getTitle());
            skipSeries(series);
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
    
    private static void skipSeries(Series series) {
        series.setRetries(0);
        series.setStatus(StatusType.DONE);
        // set all seasons and episodes to DONE
        for (Season season : series.getSeasons()) {
            season.setTvSeasonDone();
            for (VideoData videoData : season.getVideoDatas()) {
                videoData.setTvEpisodeDone();
            }
        }
    }

    /**
     * Scan a person.
     * 
     * @param person
     */
    public void scanPerson(Person person) {
        if (person.isAllScansSkipped()) {
            LOG.info("All person scans skipped for {}-'{}'", person.getId(), person.getName());
            person.setRetries(0);
            person.setStatus(StatusType.DONE);
            person.setFilmographyStatus(StatusType.DONE);
            return;
        }
        
        final boolean useAlternate = this.configServiceWrapper.getBooleanProperty("yamj3.sourcedb.scanner.person.alternate.always", false);
        final boolean throwTempError = this.configServiceWrapper.getBooleanProperty("yamj3.error.throwTempUnavailableError", true);
    	ScanResult scanResult = null;
        
    	loop: for (String scanner : PERSON_SCANNER) {
            // holds the inner scan result
            ScanResult innerResult = ScanResult.NO_RESULT;
            
    		IPersonScanner personScanner = registeredPersonScanner.get(scanner);
            if (personScanner == null) {
                LOG.error("Person scanner '{}' not registered", scanner);
            } else {
                // scan person data
                try {
                    if (person.isSkippedScan(personScanner.getScannerName())) {
                        LOG.info("Person scan skipped for '{}' using {}", person.getName(), personScanner.getScannerName());
                        innerResult = ScanResult.SKIPPED;
                    } else {
                        LOG.info("Scanning person data for '{}' using {}", person.getName(), personScanner.getScannerName());
                        innerResult = personScanner.scanPerson(person, throwTempError);
                    }
                } catch (TemporaryUnavailableException ex) {
                    // check retry
                    if (scanResult == null && person.getRetries() < configServiceWrapper.getIntProperty("yamj3.error.maxRetries.person", 0)) {
                        LOG.info("{} service temporary not available; trigger retry: '{}'", personScanner.getScannerName(), person.getName());
                        innerResult = ScanResult.RETRY;
                    } else {
                        LOG.error("Temporary scanning error for person '{}' with {} scanner", person.getName(), personScanner.getScannerName());
                        LOG.warn(SCANNING_ERROR, ex);
                    }
                } catch (Exception error) {
                    LOG.error("Failed scanning person '{}' with {} scanner", person.getName(), personScanner.getScannerName());
                    LOG.warn(SCANNING_ERROR, error);
                }
            }
            
            if (ScanResult.OK.equals(innerResult)) {
                // scanned OK
                scanResult = ScanResult.OK;
                // no alternate scanning then break the loop
                if (!useAlternate) {
                    break loop;
                }
            } else if (ScanResult.SKIPPED.equals(innerResult)) {
                // change nothing if scan skipped and force next scan
            } else {
                // just set scan result to inner result if no scan result before
                scanResult = (scanResult == null) ? innerResult : scanResult;
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
        if (person.isAllScansSkipped()) {
            LOG.info("All filmography scans skipped for {}-'{}'", person.getId(), person.getName());
            person.setRetries(0);
            person.setFilmographyStatus(StatusType.DONE);
            return;
        }

        final boolean throwTempError = configServiceWrapper.getBooleanProperty("yamj3.error.throwTempUnavailableError", true);
        ScanResult scanResult = null;

        for (String scanner : FILMOGRAPHY_SCANNER) {
            // holds the inner scan result
            ScanResult innerResult = ScanResult.ERROR;
            
            IFilmographyScanner filmographyScanner = registeredFilmographyScanner.get(scanner);
            if (filmographyScanner == null) {
                LOG.error("Filmography scanner '{}' not registered", scanner);
            } else {
                // scan filmography
                try {
                    if (person.isSkippedScan(filmographyScanner.getScannerName())) {
                        LOG.info("Filmography scan skipped for '{}' using {}", person.getName(), filmographyScanner.getScannerName());
                        innerResult = ScanResult.SKIPPED;
                    } else {
                        LOG.info("Scanning filmography data for '{}' using {}", person.getName(), filmographyScanner.getScannerName());
                        innerResult = filmographyScanner.scanFilmography(person, throwTempError);
                    }
                } catch (TemporaryUnavailableException ex) {
                    // check retry
                    if (scanResult == null && person.getRetries() < configServiceWrapper.getIntProperty("yamj3.error.maxRetries.filmography", 0)) {
                        LOG.info("{} service temporary not available; trigger retry: '{}'", filmographyScanner.getScannerName(), person.getName());
                        innerResult = ScanResult.RETRY;
                    } else {
                        LOG.error("Temporary scanning error for filmography of '{}' with {} scanner", person.getName(), filmographyScanner.getScannerName());
                        LOG.warn(SCANNING_ERROR, ex);
                    }
                } catch (Exception ex) {
                    LOG.error("Failed scanning filmography of '{}' with {} scanner", person.getName(), filmographyScanner.getScannerName());
                    LOG.warn(SCANNING_ERROR, ex);
                }
            }
            
            if (ScanResult.OK.equals(innerResult)) {
                // scan OK
                scanResult = ScanResult.OK;
                break;
            } else if (ScanResult.SKIPPED.equals(innerResult)) {
                // change nothing if scan skipped and force next scan
            } else {
                // just set scan result to inner result if no scan result before
                scanResult = (scanResult == null) ? innerResult : scanResult;
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
        INfoScanner nfoScanner = null;
        if (dto.isTvShow()) {
            Iterator<String> iter = SERIES_SCANNER.iterator();
            if (iter.hasNext()) nfoScanner = this.registeredSeriesScanner.get(iter.next());
        } else {
            Iterator<String> iter = MOVIE_SCANNER.iterator();
            if (iter.hasNext()) nfoScanner = this.registeredMovieScanner.get(iter.next());
        }

        boolean autodetect = this.configServiceWrapper.getBooleanProperty("nfo.autodetect.scanner", false);
        boolean ignorePresentId = this.configServiceWrapper.getBooleanProperty("nfo.ignore.present.id", false);

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
    
    public boolean isKnownScanner(MetaDataType type, String source) {
        switch(type) {
            case SERIES:
            case SEASON:
            case EPISODE:
                if (registeredSeriesScanner.containsKey(source)) {
                    return true;
                }
                return false;
            case MOVIE:
                if (registeredMovieScanner.containsKey(source)) {
                    return true;
                }
                return false;
            case PERSON:
                if (registeredPersonScanner.containsKey(source)) {
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public MovieScanner getMovieScanner(String source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SeriesScanner getSeriesScanner(String source) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PersonScanner getPersonScanner(String source) {
        // TODO Auto-generated method stub
        return null;
    }
}
