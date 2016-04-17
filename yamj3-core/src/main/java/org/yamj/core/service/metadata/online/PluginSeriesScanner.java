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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Series;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.metadata.SeriesScanner;

public class PluginSeriesScanner implements ISeriesScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginSeriesScanner.class);
    private final SeriesScanner seriesScanner;
    private final LocaleService localeService;
    
    public PluginSeriesScanner(SeriesScanner seriesScanner, LocaleService localeService) {
        this.seriesScanner = seriesScanner;
        this.localeService = localeService;
    }
    
    @Override
    public String getScannerName() {
        return seriesScanner.getScannerName();
    }
    
    @Override
    public String getSeriesId(Series series) {
        return getSeriesId(series, false);
    }

    private String getSeriesId(Series series, boolean throwTempError) {
        String seriesId = series.getSourceDbId(getScannerName());
        if (StringUtils.isBlank(seriesId)) {
            seriesId = seriesScanner.getSeriesId(series.getTitle(), series.getTitleOriginal(), series.getStartYear(), series.getSourceDbIdMap(), throwTempError);
            series.setSourceDbId(getScannerName(), seriesId);
        }
        return seriesId;
    }

    @Override
    public ScanResult scanSeries(Series series, boolean throwTempError) {
        String seriesId = getSeriesId(series, throwTempError);
        if (StringUtils.isBlank(seriesId)) {
            LOG.debug("{} id not available '{}'", getScannerName(), series.getIdentifier());
            return ScanResult.MISSING_ID;
        }
        
        final org.yamj.plugin.api.metadata.Series tvSeries = new org.yamj.plugin.api.metadata.Series().setIds(series.getSourceDbIdMap());
        //TODO fill in season and episodes
        
        final boolean scanned = seriesScanner.scanSeries(tvSeries, throwTempError);
        if (!scanned) {
            LOG.error("Can't find {} informations for series '{}'", getScannerName(), series.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        // set possible scanned movie IDs only if not set before   
        for (Entry<String,String> entry : tvSeries.getIds().entrySet()) {
            if (StringUtils.isBlank(series.getSourceDbId(entry.getKey()))) {
                series.setSourceDbId(entry.getKey(), entry.getValue());
            }
        }

        if (OverrideTools.checkOverwriteTitle(series, getScannerName())) {
            series.setTitle(tvSeries.getTitle(), getScannerName());
        }

        if (OverrideTools.checkOverwriteOriginalTitle(series, getScannerName())) {
            series.setTitleOriginal(tvSeries.getOriginalTitle(), getScannerName());
        }

        if (OverrideTools.checkOverwriteYear(series, getScannerName())) {
            series.setStartYear(tvSeries.getStartYear(), getScannerName());
            series.setEndYear(tvSeries.getEndYear(), getScannerName());
        }

        if (OverrideTools.checkOverwritePlot(series, getScannerName())) {
            series.setPlot(tvSeries.getPlot(), getScannerName());
        }

        if (OverrideTools.checkOverwriteOutline(series, getScannerName())) {
            series.setOutline(tvSeries.getOutline(), getScannerName());
        }

        series.addRating(getScannerName(), tvSeries.getRating());

        if (OverrideTools.checkOverwriteGenres(series, getScannerName())) {
            series.setGenreNames(tvSeries.getGenres(), getScannerName());
        }

        if (OverrideTools.checkOverwriteStudios(series, getScannerName())) {
            series.setStudioNames(tvSeries.getStudios(), getScannerName());
        }

        if (OverrideTools.checkOverwriteCountries(series, getScannerName())) {
            Set<String> countryCodes = new HashSet<>(tvSeries.getCountries().size());
            for (String country : tvSeries.getCountries()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(countryCode);
            }
            series.setCountryCodes(countryCodes, getScannerName());
        }

        // TODO fill season and episodes
        
        return ScanResult.OK;
    }
    
    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(getScannerName()))) {
            return true;
        }

        LOG.trace("Scanning NFO for {} ID", getScannerName());
        try {
            String id = seriesScanner.scanNFO(nfoContent);
            if (StringUtils.isNotBlank(id)) {
                LOG.debug("{} ID found in NFO: {}", getScannerName(), id);
                dto.addId(getScannerName(), id);
                return true;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No {} ID found in NFO", getScannerName());
        return false;
    }
}
