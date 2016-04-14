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
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.metadata.Credit;
import org.yamj.plugin.api.metadata.Movie;
import org.yamj.plugin.api.metadata.MovieScanner;

public class PluginMovieScanner implements IMovieScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginMovieScanner.class);
    private final MovieScanner movieScanner;
    private final LocaleService localeService;
    
    public PluginMovieScanner(MovieScanner movieScanner, LocaleService localeService) {
        this.movieScanner = movieScanner;
        this.localeService = localeService;
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
        String movieId = videoData.getSourceDbId(getScannerName());
        if (StringUtils.isBlank(movieId)) {
            movieId = movieScanner.getMovieId(videoData.getTitle(), videoData.getTitleOriginal(), videoData.getYear(), videoData.getSourceDbIdMap(), throwTempError);
            videoData.setSourceDbId(getScannerName(), movieId);
        }
        return movieId;
    }
    
    @Override
    public ScanResult scanMovie(VideoData videoData, boolean throwTempError) {
        String movieId = getMovieId(videoData, throwTempError);
        if (StringUtils.isBlank(movieId)) {
            LOG.debug("{} id not available '{}'", getScannerName(), videoData.getIdentifier());
            return ScanResult.MISSING_ID;
        }
        
        Movie movie = movieScanner.scanMovie(movieId, throwTempError);
        if (movie == null) {
            LOG.error("Can't find {} informations for movie '{}'", getScannerName(), videoData.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        // set possible scanned movie IDs only if not set before   
        for (Entry<String,String> entry : movie.getIds().entrySet()) {
            if (StringUtils.isBlank(videoData.getSourceDbId(entry.getKey()))) {
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
            final String countryCode = localeService.findCountryCode(movie.getReleaseCountry());
            videoData.setRelease(countryCode, movie.getReleaseDate(), getScannerName());
        }

        videoData.addRating(getScannerName(), movie.getRating());

        if (OverrideTools.checkOverwriteGenres(videoData, getScannerName())) {
            videoData.setGenreNames(movie.getGenres(), getScannerName());
        }

        if (OverrideTools.checkOverwriteStudios(videoData, getScannerName())) {
            videoData.setGenreNames(movie.getStudios(), getScannerName());
        }

        if (OverrideTools.checkOverwriteCountries(videoData, getScannerName())) {
            Set<String> countryCodes = new HashSet<>(movie.getCountries().size());
            for (String country : movie.getCountries()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(countryCode);
            }
            videoData.setCountryCodes(countryCodes, getScannerName());
        }

        for (Credit credit : movie.getCredits()) {
            videoData.addCreditDTO(new CreditDTO(getScannerName(), credit));
        }
        
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
            String id = movieScanner.scanForIdInNFO(nfoContent);
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
