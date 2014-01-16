/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.core.service.plugin;

import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("imdbScanner")
public class ImdbScanner implements IMovieScanner, ISeriesScanner, InitializingBean {

    public static final String SCANNER_ID = "imdb";
    private static final Logger LOG = LoggerFactory.getLogger(ImdbScanner.class);
//    @Autowired
//    private PoolingHttpClient httpClient;
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private PluginMetadataService pluginMetadataService;

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // register this scanner
        pluginMetadataService.registerMovieScanner(this);
        pluginMetadataService.registerSeriesScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String imdbId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = getMovieId(videoData.getTitle(), videoData.getPublicationYear());
            videoData.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getSeriesId(Series series) {
        String imdbId = series.getSourceDbId(SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            int year = -1; // TODO: get form firsAired value
            imdbId = getSeriesId(series.getTitle(), year);
            series.setSourceDbId(SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getMovieId(String title, int year) {
        return imdbSearchEngine.getImdbId(title, year, false);
    }

    @Override
    public String getSeriesId(String title, int year) {
        return imdbSearchEngine.getImdbId(title, year, true);
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        String imdbId = getMovieId(videoData);
        if (StringUtils.isBlank(imdbId)) {
            LOG.debug("IMDb id not available : {}", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        // TODO Auto-generated method stub
        return ScanResult.ERROR;
    }

    @Override
    public ScanResult scan(Series series) {
        String imdbId = getSeriesId(series);
        if (StringUtils.isBlank(imdbId)) {
            LOG.debug("IMDb id not available: {}", series.getTitle());
            return ScanResult.MISSING_ID;
        }

        // TODO Auto-generated method stub
        return ScanResult.ERROR;
    }
}