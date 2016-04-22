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
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.metadata.*;

public class PluginMovieScanner implements IMovieScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginMovieScanner.class);
    private final MovieScanner movieScanner;
    private final LocaleService localeService;
    private final IdentifierService identifierService;
    
    public PluginMovieScanner(MovieScanner movieScanner, LocaleService localeService, IdentifierService identifierService) {
        this.movieScanner = movieScanner;
        this.localeService = localeService;
        this.identifierService = identifierService;
    }
    
    public MovieScanner getMovieScanner() {
        return movieScanner;
    }
    
    @Override
    public String getScannerName() {
        return movieScanner.getScannerName();
    }
    
    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String movieId = movieScanner.getMovieId(videoData.getTitle(), videoData.getTitleOriginal(), videoData.getPublicationYear(), videoData.getIdMap(), throwTempError);
        videoData.setSourceDbId(getScannerName(), movieId);
        return movieId;
    }
    
    @Override
    public ScanResult scanMovie(VideoData videoData, boolean throwTempError) {
        String movieId = getMovieId(videoData, throwTempError);
        if (StringUtils.isBlank(movieId)) {
            LOG.debug("{} id not available '{}'", getScannerName(), videoData.getIdentifier());
            return ScanResult.MISSING_ID;
        }
        
        final MovieDTO movie = new MovieDTO(videoData.getIdMap()).setTitle(videoData.getTitle());
        final boolean scanned = movieScanner.scanMovie(movie, throwTempError);
        if (!scanned) {
            LOG.error("Can't find {} informations for movie '{}'", getScannerName(), videoData.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        // set  IDs only if not set before   
        for (Entry<String,String> entry : movie.getIds().entrySet()) {
            if (getScannerName().equalsIgnoreCase(entry.getKey())) {
                videoData.setSourceDbId(entry.getKey(), entry.getValue());
            } else if (StringUtils.isBlank(videoData.getSourceDbId(entry.getKey()))) {
                videoData.setSourceDbId(entry.getKey(), entry.getValue());
            }
        }

        if (OverrideTools.checkOverwriteTitle(videoData, getScannerName())) {
            videoData.setTitle(movie.getTitle(), getScannerName());
        }

        if (OverrideTools.checkOverwriteOriginalTitle(videoData, getScannerName())) {
            videoData.setTitleOriginal(movie.getOriginalTitle(), getScannerName());
        }

        if (OverrideTools.checkOverwriteYear(videoData, getScannerName())) {
            videoData.setPublicationYear(movie.getYear(), getScannerName());
        }

        if (OverrideTools.checkOverwritePlot(videoData, getScannerName())) {
            videoData.setPlot(movie.getPlot(), getScannerName());
        }

        if (OverrideTools.checkOverwriteOutline(videoData, getScannerName())) {
            videoData.setOutline(movie.getOutline(), getScannerName());
        }

        if (OverrideTools.checkOverwriteTagline(videoData, getScannerName())) {
            videoData.setTagline(movie.getTagline(), getScannerName());
        }

        if (OverrideTools.checkOverwriteQuote(videoData, getScannerName())) {
            videoData.setTagline(movie.getQuote(), getScannerName());
        }

        if (OverrideTools.checkOverwriteReleaseDate(videoData, getScannerName())) {
            String releaseCountryCode = localeService.findCountryCode(movie.getReleaseCountry());
            videoData.setRelease(releaseCountryCode, movie.getReleaseDate(), getScannerName());
        }

        if (OverrideTools.checkOverwriteGenres(videoData, getScannerName())) {
            videoData.setGenreNames(movie.getGenres(), getScannerName());
        }

        if (OverrideTools.checkOverwriteStudios(videoData, getScannerName())) {
            videoData.setStudioNames(movie.getStudios(), getScannerName());
        }

        if (movie.getCountries() != null && OverrideTools.checkOverwriteCountries(videoData, getScannerName())) {
            Set<String> countryCodes = new HashSet<>(movie.getCountries().size());
            for (String country : movie.getCountries()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(countryCode);
            }
            videoData.setCountryCodes(countryCodes, getScannerName());
        }

        for (Entry<String,String> certification : movie.getCertifications().entrySet()) {
            String countryCode = localeService.findCountryCode(certification.getKey());
            videoData.addCertificationInfo(countryCode, certification.getValue());
        }

        if (movie.getCredits() != null) {
            for (CreditDTO credit : movie.getCredits()) {
                videoData.addCreditDTO(identifierService.createCredit(credit));
            }
        }
        
        videoData.addRating(getScannerName(), movie.getRating());

        videoData.addAwardDTOS(movie.getAwards());
        
        return ScanResult.OK;
    }
    
    @Override
    public boolean scanNFO(String nfoContent, IdMap idMap) {
        try {
            return movieScanner.scanNFO(nfoContent, idMap);
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
            return false;
        }
    }
}
