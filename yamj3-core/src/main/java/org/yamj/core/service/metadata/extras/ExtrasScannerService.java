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
package org.yamj.core.service.metadata.extras;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.metadata.WrapperMovie;
import org.yamj.core.service.metadata.WrapperSeries;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.plugin.api.extras.ExtrasScanner;
import org.yamj.plugin.api.extras.MovieExtrasScanner;
import org.yamj.plugin.api.extras.SeriesExtrasScanner;

@Service("extrasScannerService")
public class ExtrasScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtrasScannerService.class);
    
    private final Set<MovieExtrasScanner> registeredMovieExtrasScanner = new HashSet<>();
    private final Set<SeriesExtrasScanner> registeredSeriesExtrasScanner = new HashSet<>();
    
    @Autowired
    private LocaleService localeService;
    @Autowired
    private IdentifierService identifierService;
    @Autowired
    private TraktTvIdScanner traktTvIdScanner;
    
    @PostConstruct
    public void init() {
        LOG.debug("Initialize extras scanner");
        registerExtraScanner(traktTvIdScanner);
    }
        
    /**
     * Register an extra scanner
     *
     * @param extraScanner
     */
    public void registerExtraScanner(ExtrasScanner extrasScanner) {
        if (extrasScanner instanceof MovieExtrasScanner) {
            LOG.trace("Registered movie extras scanner: {}", extrasScanner.getScannerName().toLowerCase());
            registeredMovieExtrasScanner.add((MovieExtrasScanner)extrasScanner);
        }
        if (extrasScanner instanceof SeriesExtrasScanner) {
            LOG.trace("Registered series extras scanner: {}", extrasScanner.getScannerName().toLowerCase());
            registeredSeriesExtrasScanner.add((SeriesExtrasScanner)extrasScanner);
        }
    }
    
    /**
     * Scan a movie.
     * 
     * @param videoData
     */
    public void scanMovie(VideoData videoData) {
        if (videoData.isAllScansSkipped()) {
            // all scans skipped
            return;
        }

        final WrapperMovie wrapper = new WrapperMovie(videoData, localeService, identifierService);
        
        for (MovieExtrasScanner extrasScanner : registeredMovieExtrasScanner) {
            if (extrasScanner.isEnabled()) {
                if (videoData.isSkippedScan(extrasScanner.getScannerName())) {
                    LOG.info("Movie scan skipped for '{}' using {}", videoData.getTitle(), extrasScanner.getScannerName());
                } else {
                    LOG.debug("Scanning movie extras for '{}' using {}", videoData.getTitle(), extrasScanner.getScannerName());
                    try {
                        wrapper.setScannerName(extrasScanner.getScannerName());
                        extrasScanner.scanExtras(wrapper);
                    } catch (Exception error) {
                        LOG.error("Failed scanning movie with {} scanner", extrasScanner.getScannerName());
                        LOG.warn("Scanning error", error);
                    }
                }
            }
		}       
    }

    /**
     * Scan a series.
     * 
     * @param series
     */
    public void scanSeries(Series series) {
        if (series.isAllScansSkipped()) {
            // all scans skipped
            return;
        }

        final WrapperSeries wrapper = new WrapperSeries(series, localeService, identifierService);

        for (SeriesExtrasScanner extrasScanner : registeredSeriesExtrasScanner) {
            if (extrasScanner.isEnabled()) {
                if (series.isSkippedScan(extrasScanner.getScannerName())) {
                    LOG.info("Series scan skipped for '{}' using {}", series.getTitle(), extrasScanner.getScannerName());
                } else {
                    LOG.debug("Scanning series extras for '{}' using {}", series.getTitle(), extrasScanner.getScannerName());
                    try {
                        wrapper.setScannerName(extrasScanner.getScannerName());
                        extrasScanner.scanExtras(wrapper);
                    } catch (Exception error) {
                        LOG.error("Failed scanning series with {} scanner", extrasScanner.getScannerName());
                        LOG.warn("Scanning error", error);
                    }
                }
            }
        }       
    }
}
