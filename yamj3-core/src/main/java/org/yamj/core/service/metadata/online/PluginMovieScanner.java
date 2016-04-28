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
import org.yamj.core.service.metadata.WrapperMovie;
import org.yamj.plugin.api.metadata.MovieScanner;
import org.yamj.plugin.api.metadata.NfoScanner;
import org.yamj.plugin.api.model.IdMap;

public class PluginMovieScanner implements NfoScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginMovieScanner.class);
    private final MovieScanner movieScanner;
    
    public PluginMovieScanner(MovieScanner movieScanner) {
        this.movieScanner = movieScanner;
    }
    
    public MovieScanner getMovieScanner() {
        return movieScanner;
    }
    
    @Override
    public String getScannerName() {
        return movieScanner.getScannerName();
    }
    
    public ScanResult scanMovie(WrapperMovie wrapper, boolean throwTempError) {
        // set actual scanner
        wrapper.setScannerName(movieScanner.getScannerName());

        // get the movie id
        String movieId = movieScanner.getMovieId(wrapper, throwTempError);
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
