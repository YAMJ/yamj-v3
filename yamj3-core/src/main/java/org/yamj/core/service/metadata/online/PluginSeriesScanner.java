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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.core.service.metadata.WrapperSeries;
import org.yamj.plugin.api.metadata.NfoScanner;
import org.yamj.plugin.api.metadata.SeriesScanner;
import org.yamj.plugin.api.model.IdMap;

public class PluginSeriesScanner implements NfoScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginSeriesScanner.class);
    private final SeriesScanner seriesScanner;

    public PluginSeriesScanner(SeriesScanner seriesScanner) {
        this.seriesScanner = seriesScanner;
    }
    
    public SeriesScanner getSeriesScanner() {
        return seriesScanner;
    }

    @Override
    public String getScannerName() {
        return seriesScanner.getScannerName();
    }

    public ScanResult scanSeries(WrapperSeries wrapper, boolean throwTempError) {
        // set actual scanner
        wrapper.setScannerName(seriesScanner.getScannerName());

        // get the series id
        String seriesId = seriesScanner.getSeriesId(wrapper, throwTempError);
        if (!seriesScanner.isValidSeriesId(seriesId)) {
            LOG.debug("{} id not available '{}'", getScannerName(), wrapper.getTitle());
            return ScanResult.MISSING_ID;
        }
        
        final boolean scanned = seriesScanner.scanSeries(wrapper, throwTempError);
        if (!scanned) {
            LOG.error("Can't find {} informations for series '{}'", getScannerName(), wrapper.getTitle());
            return ScanResult.NO_RESULT;
        }
        
        return ScanResult.OK;
    }

    @Override
    public boolean scanNFO(String nfoContent, IdMap idMap) {
        try {
            return seriesScanner.scanNFO(nfoContent, idMap);
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
            return false;
        }
    }
}
