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
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Series;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.plugin.api.metadata.SeriesScanner;
import org.yamj.plugin.api.model.IdMap;

public class PluginSeriesScanner implements ISeriesScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginSeriesScanner.class);
    private final SeriesScanner seriesScanner;
    private final LocaleService localeService;
    private final IdentifierService identifierService;

    public PluginSeriesScanner(SeriesScanner seriesScanner, LocaleService localeService, IdentifierService identifierService) {
        this.seriesScanner = seriesScanner;
        this.localeService = localeService;
        this.identifierService = identifierService;
    }
    
    public SeriesScanner getSeriesScanner() {
        return seriesScanner;
    }

    @Override
    public String getScannerName() {
        return seriesScanner.getScannerName();
    }
    
    @Override
    public String getSeriesId(Series series) {
        // create series wrapper
        WrapperSeries wrapper = new WrapperSeries(series, localeService, identifierService);
        wrapper.setScannerName(seriesScanner.getScannerName());
        
        return getSeriesId(wrapper, false);
    }

    private String getSeriesId(WrapperSeries wrapper, boolean throwTempError) {
        String seriesId = seriesScanner.getSeriesId(wrapper, throwTempError);
        wrapper.addId(getScannerName(), seriesId);
        return seriesId;
    }

    @Override
    public ScanResult scanSeries(Series series, boolean throwTempError) {
        // create series wrapper
        WrapperSeries wrapper = new WrapperSeries(series, localeService, identifierService);
        wrapper.setScannerName(seriesScanner.getScannerName());

        String seriesId = getSeriesId(wrapper, throwTempError);
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
