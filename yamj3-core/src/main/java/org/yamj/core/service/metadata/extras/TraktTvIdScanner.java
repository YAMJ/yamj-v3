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

import static org.yamj.plugin.api.Constants.*;

import javax.annotation.PostConstruct;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.service.trakttv.TraktTvService;
import org.yamj.plugin.api.extras.MovieExtrasScanner;
import org.yamj.plugin.api.extras.SeriesExtrasScanner;
import org.yamj.plugin.api.metadata.MetadataTools;
import org.yamj.plugin.api.model.IMovie;
import org.yamj.plugin.api.model.ISeries;

@Service("traktTvIdScanner")
public class TraktTvIdScanner implements MovieExtrasScanner, SeriesExtrasScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TraktTvIdScanner.class);
    private static final int NO_ID = -1;

    @Autowired
    private TraktTvService traktTvService;
    
    private boolean enabled;
    
    @PostConstruct
    public void init() {
        LOG.trace("Initialize Trakt.TV ID scanner");

        // can be a global property cause set in static properties
        enabled = traktTvService.isSynchronizationEnabled();
    }
    
    @Override
    public String getScannerName() {
        return SOURCE_TRAKTTV;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void scanExtras(IMovie movie) {
        int traktId = NumberUtils.toInt(movie.getId(SOURCE_TRAKTTV), NO_ID);
        if (traktId > NO_ID) {
            // nothing to do anymore cause Trakt.TV id already present
            return;
        }

        LOG.trace("Search for Trakt.TV movie ID: {}", movie.getTitle());
        
        // try IMDB id
        Integer found = traktTvService.searchMovieIdByIMDB(movie.getId(SOURCE_IMDB));
        if (found != null && found.intValue() > NO_ID) {
            movie.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // try TheMovieDB id
        found = traktTvService.searchMovieIdByTMDB(movie.getId(SOURCE_TMDB));
        if (found != null && found.intValue() > NO_ID) {
            movie.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }
        
        // search by original title first
        if (MetadataTools.isOriginalTitleScannable(movie)) {
            found = traktTvService.searchMovieByTitleAndYear(movie.getOriginalTitle(), movie.getYear());
            if (found != null && found.intValue() > NO_ID) {
                movie.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
                // nothing to do anymore cause Trakt.TV id found
                return;
            }
        }
        
        // search by title if still not found
        found = traktTvService.searchMovieByTitleAndYear(movie.getTitle(), movie.getYear());
        if (found != null && found.intValue() > NO_ID) {
            movie.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // no Trakt.TV ID found at this point
        LOG.info("No Trakt.TV ID found for movie '{}'-{}", movie.getTitle(), movie.getYear());
    }

    @Override
    public void scanExtras(ISeries series) {
        int traktId = NumberUtils.toInt(series.getId(SOURCE_TRAKTTV), NO_ID);
        if (traktId > NO_ID) {
            // nothing to do anymore cause Trakt.TV id already present
            return;
        }

        LOG.trace("Search for Trakt.TV series ID: {}", series.getTitle());

        // try TheTVDb id
        Integer found = traktTvService.searchShowIdByTVDB(series.getId(SOURCE_TVDB));
        if (found != null && found.intValue() > NO_ID) {
            series.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // try TVRage id
        found = traktTvService.searchShowIdByTVDB(series.getId(SOURCE_TVRAGE));
        if (found != null && found.intValue() > NO_ID) {
            series.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // try IMDB id
        found = traktTvService.searchShowIdByIMDB(series.getId(SOURCE_IMDB));
        if (found != null && found.intValue() > NO_ID) {
            series.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // try TheMovieDB id
        found = traktTvService.searchShowIdByTMDB(series.getId(SOURCE_TMDB));
        if (found != null && found.intValue() > NO_ID) {
            series.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }
        
        // search by original title first
        if (MetadataTools.isOriginalTitleScannable(series)) {
            found = traktTvService.searchShowByTitleAndYear(series.getOriginalTitle(), series.getStartYear());
            if (found != null && found.intValue() > NO_ID) {
                series.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
                // nothing to do anymore cause Trakt.TV id found
                return;
            }
        }
        
        // search by title if still not found
        found = traktTvService.searchShowByTitleAndYear(series.getTitle(), series.getStartYear());
        if (found != null && found.intValue() > NO_ID) {
            series.addId(SOURCE_TRAKTTV, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // no Trakt.TV ID found at this point
        LOG.info("No Trakt.TV ID found for series '{}'-{}", series.getTitle(), series.getStartYear());
    }
}
