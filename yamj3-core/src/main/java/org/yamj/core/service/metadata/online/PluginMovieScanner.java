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
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.various.IdentifierService;
import org.yamj.plugin.api.metadata.MovieScanner;
import org.yamj.plugin.api.model.IdMap;

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
        // create movie wrapper
        WrapperMovie wrapper = new WrapperMovie(videoData, localeService, identifierService);
        wrapper.setScannerName(movieScanner.getScannerName());
        
        return getMovieId(wrapper, false);
    }

    private String getMovieId(WrapperMovie wrapper, boolean throwTempError) {
        return movieScanner.getMovieId(wrapper, throwTempError);
    }

    @Override
    public ScanResult scanMovie(VideoData videoData, boolean throwTempError) {
        // create movie wrapper
        WrapperMovie wrapper = new WrapperMovie(videoData, localeService, identifierService);
        wrapper.setScannerName(movieScanner.getScannerName());

        String movieId = getMovieId(wrapper, throwTempError);
        if (!movieScanner.isValidMovieId(movieId)) {
            LOG.debug("{} id not available '{}'", getScannerName(), wrapper.getTitle());
            return ScanResult.MISSING_ID;
        }
        
        final boolean scanned = movieScanner.scanMovie(wrapper, throwTempError);
        if (!scanned) {
            LOG.error("Can't find {} informations for movie '{}'", getScannerName(), wrapper.getTitle());
            return ScanResult.NO_RESULT;
        }
        
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
