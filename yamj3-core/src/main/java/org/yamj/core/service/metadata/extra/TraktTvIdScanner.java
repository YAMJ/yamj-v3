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

import static org.yamj.plugin.api.common.Constants.*;

import javax.annotation.PostConstruct;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.metadata.ExtraScannerService;
import org.yamj.core.service.metadata.online.TraktTvScanner;
import org.yamj.core.service.trakttv.TraktTvService;
import org.yamj.plugin.api.metadata.tools.MetadataTools;

@Service("traktTvIdScanner")
public class TraktTvIdScanner implements IExtraMovieScanner, IExtraSeriesScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TraktTvIdScanner.class);
    private static final int NO_ID = -1;

    @Autowired
    private TraktTvService traktTvService;
    @Autowired
    private ExtraScannerService extraScannerService;
    
    private boolean enabled;
    
    @PostConstruct
    public void init() {
        LOG.trace("Initialize Trakt.TV ID scanner");

        // can be a global property cause set in static properties
        enabled = traktTvService.isSynchronizationEnabled();

        // register this scanner
        extraScannerService.registerExtraScanner(this);
    }
    
    @Override
    public String getScannerName() {
        return TraktTvScanner.SCANNER_ID;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void scanMovie(VideoData videoData) {
        int traktId = NumberUtils.toInt(videoData.getSourceDbId(TraktTvScanner.SCANNER_ID), NO_ID);
        if (traktId > NO_ID) {
            // nothing to do anymore cause Trakt.TV id already present
            return;
        }

        LOG.trace("Search for Trakt.TV movie ID: {}", videoData.getIdentifier());
        
        // try IMDB id
        Integer found = traktTvService.searchMovieIdByIMDB(videoData.getSourceDbId(SOURCE_IMDB));
        if (found != null && found.intValue() > NO_ID) {
            videoData.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // try TheMovieDB id
        found = traktTvService.searchMovieIdByTMDB(videoData.getSourceDbId(SOURCE_TMDB));
        if (found != null && found.intValue() > NO_ID) {
            videoData.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }
        
        // search by original title first
        if (MetadataTools.isOriginalTitleScannable(videoData.getTitle(), videoData.getTitleOriginal())) {
            found = traktTvService.searchMovieByTitleAndYear(videoData.getTitleOriginal(), videoData.getYear());
            if (found != null && found.intValue() > NO_ID) {
                videoData.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
                // nothing to do anymore cause Trakt.TV id found
                return;
            }
        }
        
        // search by title if still not found
        found = traktTvService.searchMovieByTitleAndYear(videoData.getTitle(), videoData.getYear());
        if (found != null && found.intValue() > NO_ID) {
            videoData.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // no Trakt.TV ID found at this point
        LOG.info("No Trakt.TV ID found for movie '{}'-{}", videoData.getTitle(), videoData.getPublicationYear());
    }

    @Override
    public void scanSeries(Series series) {
        int traktId = NumberUtils.toInt(series.getSourceDbId(TraktTvScanner.SCANNER_ID), NO_ID);
        if (traktId > NO_ID) {
            // nothing to do anymore cause Trakt.TV id already present
            return;
        }

        LOG.trace("Search for Trakt.TV series ID: {}", series.getIdentifier());

        // try TheTVDb id
        Integer found = traktTvService.searchShowIdByTVDB(series.getSourceDbId(SOURCE_TVDB));
        if (found != null && found.intValue() > NO_ID) {
            series.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // try TVRage id
        found = traktTvService.searchShowIdByTVDB(series.getSourceDbId(SOURCE_TVRAGE));
        if (found != null && found.intValue() > NO_ID) {
            series.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // try IMDB id
        found = traktTvService.searchShowIdByIMDB(series.getSourceDbId(SOURCE_IMDB));
        if (found != null && found.intValue() > NO_ID) {
            series.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // try TheMovieDB id
        found = traktTvService.searchShowIdByTMDB(series.getSourceDbId(SOURCE_TMDB));
        if (found != null && found.intValue() > NO_ID) {
            series.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }
        
        // search by original title first
        if (MetadataTools.isOriginalTitleScannable(series.getTitle(), series.getTitleOriginal())) {
            found = traktTvService.searchShowByTitleAndYear(series.getTitleOriginal(), series.getStartYear());
            if (found != null && found.intValue() > NO_ID) {
                series.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
                // nothing to do anymore cause Trakt.TV id found
                return;
            }
        }
        
        // search by title if still not found
        found = traktTvService.searchShowByTitleAndYear(series.getTitle(), series.getStartYear());
        if (found != null && found.intValue() > NO_ID) {
            series.setSourceDbId(TraktTvScanner.SCANNER_ID, Integer.toString(found.intValue()));
            // nothing to do anymore cause Trakt.TV id found
            return;
        }

        // no Trakt.TV ID found at this point
        LOG.info("No Trakt.TV ID found for series '{}'-{}", series.getTitle(), series.getStartYear());
    }
}
