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

import org.yamj.plugin.api.web.TemporaryUnavailableException;

import com.omertron.moviemeter.MovieMeterApi;
import com.omertron.moviemeter.MovieMeterException;
import com.omertron.moviemeter.model.FilmInfo;
import com.omertron.moviemeter.model.SearchResult;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.tools.ResponseTools;

@Service
public class MovieMeterApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterApiWrapper.class);
    private static final String API_ERROR = "MovieMeter error";

    @Autowired
    private MovieMeterApi movieMeterApi;

    public String getMovieIdByIMDbId(String imdbId, boolean throwTempError) {
        try {
            FilmInfo filmInfo = movieMeterApi.getFilm(imdbId);
            return String.valueOf(filmInfo.getId());
        } catch (MovieMeterException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get film info using IMDb ID {}: {}", imdbId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return null;
    }

    public String getMovieIdByTitleAndYear(String title, int year, boolean throwTempError) {
        LOG.trace("Looking for MovieMeter ID for '{}' ({})", title, year);
        
        List<SearchResult> searchResults = null;
        try {
            searchResults = movieMeterApi.search(title);
        } catch (MovieMeterException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get MovieMeter ID by title '{}': {}", title, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }

        if (searchResults == null || searchResults.isEmpty()) {
            // failed retrieving any results
            return null;
        }

        double maxMatch = 0.0;
        Integer id = null;
        for (SearchResult searchResult : searchResults) {
            // if we have a year, check that first
            if (year > 0 && searchResult.getYear() != year) {
                continue;
            }

            // check for best text similarity
            double result = StringUtils.getJaroWinklerDistance(title, searchResult.getTitle());
            if (result > maxMatch) {
                LOG.trace("Better match found for {} ({}) = {} ({}) [{}]", title, year, searchResult.getTitle(), searchResult.getYear(), maxMatch);
                maxMatch = result;
                id = searchResult.getId();
            }
        }

        if (id != null) {
            LOG.debug("MovieMeter ID {} found for '{}' ({}): match confidence = {}", id, title, year, maxMatch);
            return id.toString();
        }

        return null;
    }

    public FilmInfo getFilmInfo(String movieMeterId, boolean throwTempError) {
        FilmInfo filmInfo = null;
        try {
            filmInfo = movieMeterApi.getFilm(NumberUtils.toInt(movieMeterId));
        } catch (MovieMeterException ex) {
            checkTempError(throwTempError, ex);
            LOG.error("Failed to get film info using MovieMeter ID {}: {}", movieMeterId, ex.getMessage());
            LOG.trace(API_ERROR, ex);
        }
        return filmInfo;
    }

    private static void checkTempError(boolean throwTempError, MovieMeterException ex) {
        if (throwTempError && ResponseTools.isTemporaryError(ex)) {
            throw new TemporaryUnavailableException("MovieMeter service temporary not available: " + ex.getResponseCode(), ex);
        }
    }
}
