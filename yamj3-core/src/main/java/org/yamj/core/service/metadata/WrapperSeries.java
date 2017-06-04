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
package org.yamj.core.service.metadata;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.OnlineScanner;
import org.yamj.plugin.api.model.ISeason;
import org.yamj.plugin.api.model.ISeries;

public class WrapperSeries implements ISeries {

    private final Series series;
    private final LocaleService localeService;
    private final IdentifierService identifierService;
    private String scannerName;
    private List<ISeason> seasons;

    public WrapperSeries(Series series, LocaleService localeService, IdentifierService identifierService) {
        this.series = series;
        this.localeService = localeService;
        this.identifierService = identifierService;
    }

    public WrapperSeries setScanner(OnlineScanner scanner) {
        this.scannerName = scanner.getScannerName();
        return this;
    }

    public String getScannerName() {
        return this.scannerName;
    }
    
    @Override
    public String getId(String source) {
        return series.getSourceDbId(source);
    }

    @Override
    public void addId(String source, String id) {
        if (scannerName.equalsIgnoreCase(source)) { 
            series.setSourceDbId(source, id);
        } else if (StringUtils.isBlank(series.getSourceDbId(source))) {
            series.setSourceDbId(source, id);
        }
    }

    @Override
    public String getTitle() {
        return series.getTitle();
    }

    @Override
    public void setTitle(String title) {
        if (OverrideTools.checkOverwriteTitle(series, scannerName)) {
            series.setTitle(title, scannerName);
        }
    }

    @Override
    public String getOriginalTitle() {
        return series.getTitleOriginal();
    }

    @Override
    public void setOriginalTitle(String originalTitle) {
        if (OverrideTools.checkOverwriteOriginalTitle(series, scannerName)) {
            series.setTitleOriginal(originalTitle, scannerName);
        }
    }

    @Override
    public int getStartYear() {
        return series.getStartYear();
    }

    @Override
    public void setStartYear(int startYear) {
        if (OverrideTools.checkOverwriteYear(series, scannerName)) {
            series.setStartYear(startYear, scannerName);
        }
    }

    @Override
    public int getEndYear() {
        return series.getEndYear();
    }

    @Override
    public void setEndYear(int endYear) {
        if (OverrideTools.checkOverwriteYear(series, scannerName)) {
            series.setEndYear(endYear, scannerName);
        }
    }

    @Override
    public void setPlot(String plot) {
        if (OverrideTools.checkOverwritePlot(series, scannerName)) {
            series.setPlot(plot, scannerName);
        }
    }

    @Override
    public void setOutline(String outline) {
        if (OverrideTools.checkOverwriteOutline(series, scannerName)) {
            series.setOutline(outline, scannerName);
        }
    }

    @Override
    public void setRating(int rating) {
        series.addRating(scannerName, rating);
    }

    @Override
    public void setStudios(Collection<String> studios) {
        if (OverrideTools.checkOverwriteStudios(series, scannerName)) {
            series.setStudioNames(studios, scannerName);
        }
    }
	
	// add setLibraries
	@Override
    public void setLibraries(Collection<String> libraries) {
        if (OverrideTools.checkOverwriteLibraries(series, scannerName)) {
            series.setLibraryNames(libraries, scannerName);
        }
    }
	// end library

    @Override
    public void setGenres(Collection<String> genres) {
        if (OverrideTools.checkOverwriteGenres(series, scannerName)) {
            series.setGenreNames(genres, scannerName);
        }
    }

    @Override
    public void setCountries(Collection<String> countries) {
        if (countries != null && OverrideTools.checkOverwriteCountries(series, scannerName)) {
            final Set<String> countryCodes = new HashSet<>(countries.size());
            for (String country : countries) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) {
                    countryCodes.add(countryCode);
                }
            }
            series.setCountryCodes(countryCodes, scannerName);
        }
    }

    @Override
    public void addCertification(String country, String certificate) {
        String countryCode = localeService.findCountryCode(country);
        series.addCertificationInfo(countryCode, certificate);
    }

    @Override
    public void addAward(String event, String category, int year) {
        addAward(event, category, year, true, false);
    }

    @Override
    public void addAward(String event, String category, int year, boolean won, boolean nominated) {
        series.addAwardDTO(scannerName, event, category, year, won, nominated);
    }

    @Override
    public Collection<ISeason> getSeasons() {
        if (this.seasons == null) {
            this.seasons = new ArrayList<>();
            for (Season season : series.getSeasons()) {
                WrapperSeason wrapper = new WrapperSeason(this, season, localeService, identifierService);
                this.seasons.add(wrapper);
            }
        }
        return this.seasons;
    }
}
