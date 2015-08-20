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

import com.omertron.moviemeter.MovieMeterApi;
import com.omertron.moviemeter.MovieMeterException;
import com.omertron.moviemeter.model.FilmInfo;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.service.metadata.online.TemporaryUnavailableException;
import org.yamj.core.web.ResponseTools;

@Service
public class MovieMeterApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterApiWrapper.class);
    
    @Autowired
    private MovieMeterApi movieMeterApi;

    public String getMovieIdByIMDbId(String imdbId, boolean throwTempError) {
        String moviemeterId = null;
        try {
            FilmInfo filmInfo = movieMeterApi.getFilm(imdbId);
            moviemeterId = String.valueOf(filmInfo.getId());
        } catch (MovieMeterException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("MovieMeter service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get film info using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace("MovieMeter error" , ex);
        }
        return moviemeterId;
    }

    public FilmInfo getFilmInfo(String moviemeterId, boolean throwTempError) {
        FilmInfo filmInfo = null;
        try {
            filmInfo = movieMeterApi.getFilm(NumberUtils.toInt(moviemeterId));
        } catch (MovieMeterException ex) {
            if (throwTempError && ResponseTools.isTemporaryError(ex)) {
                throw new TemporaryUnavailableException("MovieMeter service temporary not available: " + ex.getResponseCode(), ex);
            }
            LOG.error("Failed to get film info using MovieMeter ID {}: {}", moviemeterId, ex.getMessage());
            LOG.trace("MovieMeter error" , ex);
        }
        return filmInfo;
    }
}
