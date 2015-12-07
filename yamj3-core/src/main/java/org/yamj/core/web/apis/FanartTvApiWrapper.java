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
package org.yamj.core.web.apis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yamj.core.CachingNames;

import com.omertron.fanarttvapi.FanartTvApi;
import com.omertron.fanarttvapi.FanartTvException;
import com.omertron.fanarttvapi.model.FTMovie;
import com.omertron.fanarttvapi.model.FTSeries;

@Service("fanartTvApiWrapper")
public class FanartTvApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(FanartTvApiWrapper.class);

    @Autowired
    private FanartTvApi fanarttvApi;

    @Cacheable(value=CachingNames.API_FANARTTV, key="{#root.methodName, #id}", unless="#result==null")
    public FTMovie getFanartMovie(String id) { 
        FTMovie ftMovie = null;
        try {
            ftMovie = fanarttvApi.getMovieArtwork(id);
        } catch (FanartTvException ex) {
            LOG.error("Failed to get artwork from FanartTV for id {}: {}", id, ex.getMessage());
            LOG.trace("FanartTV scanner error", ex);
        }
        return ftMovie;
    }
    

    @Cacheable(value=CachingNames.API_FANARTTV, key="{#root.methodName, #id}", unless="#result==null")
    public FTSeries getFanartSeries(String id) { 
        FTSeries ftSeries = null;
        try {
            ftSeries = fanarttvApi.getTvArtwork(id);
        } catch (FanartTvException ex) {
            LOG.error("Failed to get artwork from FanartTV for id {}: {}", id, ex.getMessage());
            LOG.trace("FanartTV scanner error", ex);
        }
        return ftSeries;
    }
}
