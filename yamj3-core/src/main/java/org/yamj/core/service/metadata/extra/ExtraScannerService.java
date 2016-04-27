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
package org.yamj.core.service.metadata.extra;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;

@Service("extraScannerService")
public class ExtraScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtraScannerService.class);
    
    private final Set<IExtraMovieScanner> registeredExtraMovieScanner = new HashSet<>();
    private final Set<IExtraSeriesScanner> registeredExtraSeriesScanner = new HashSet<>();
    
    /**
     * Register an extra scanner
     *
     * @param extraScanner
     */
    public void registerExtraScanner(IExtraScanner extraScanner) {
        if (extraScanner instanceof IExtraMovieScanner) {
            LOG.trace("Registered extra scanner: {}", extraScanner.getScannerName().toLowerCase());
            registeredExtraMovieScanner.add((IExtraMovieScanner)extraScanner);
        }
        if (extraScanner instanceof IExtraSeriesScanner) {
            LOG.trace("Registered series scanner: {}", extraScanner.getScannerName().toLowerCase());
            registeredExtraSeriesScanner.add((IExtraSeriesScanner)extraScanner);
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

        for (IExtraMovieScanner extraScanner : registeredExtraMovieScanner) {
            if (extraScanner.isEnabled()) {
                if (videoData.isSkippedScan(extraScanner.getScannerName())) {
                    LOG.info("Movie scan skipped for '{}' using {}", videoData.getTitle(), extraScanner.getScannerName());
                } else {
                    LOG.debug("Scanning movie extras for '{}' using {}", videoData.getTitle(), extraScanner.getScannerName());
                    try {
                        extraScanner.scanMovie(videoData);
                    } catch (Exception error) {
                        LOG.error("Failed scanning movie with {} scanner", extraScanner.getScannerName());
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
        
        for (IExtraSeriesScanner extraScanner : registeredExtraSeriesScanner) {
            if (extraScanner.isEnabled()) {
                if (series.isSkippedScan(extraScanner.getScannerName())) {
                    LOG.info("Series scan skipped for '{}' using {}", series.getTitle(), extraScanner.getScannerName());
                } else {
                    LOG.debug("Scanning series extras for '{}' using {}", series.getTitle(), extraScanner.getScannerName());
                    try {
                        extraScanner.scanSeries(series);
                    } catch (Exception error) {
                        LOG.error("Failed scanning series with {} scanner", extraScanner.getScannerName());
                        LOG.warn("Scanning error", error);
                    }
                }
            }
        }       
    }
}
