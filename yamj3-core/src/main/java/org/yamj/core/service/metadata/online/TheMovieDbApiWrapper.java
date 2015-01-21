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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.configuration.ConfigService;

import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;

@Service("tmdbApiWrapper")
public class TheMovieDbApiWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbApiWrapper.class);

    @Autowired
    private ConfigService configService;
    @Autowired
    private TheMovieDbApi tmdbApi;

    public String getMovieDbId(String title, int year) {
        String defaultLanguage = configService.getProperty("themoviedb.language", "en");
        boolean includeAdult = configService.getBooleanProperty("themoviedb.includeAdult", Boolean.FALSE);
        int searchMatch = configService.getIntProperty("themoviedb.searchMatch", 3);
        MovieDb moviedb = null;
  
        try {
            // Search using movie name
            List<MovieDb> movieList = tmdbApi.searchMovie(title, year, defaultLanguage, includeAdult, 0).getResults();
            LOG.info("Found {} potential matches for {} ({})", movieList.size(), title, year);
            // Iterate over the list until we find a match
            for (MovieDb m : movieList) {
                String relDate;
                if (StringUtils.isNotBlank(m.getReleaseDate()) && m.getReleaseDate().length() > 4) {
                    relDate = m.getReleaseDate().substring(0, 4);
                } else {
                    relDate = "";
                }
                LOG.debug("Checking " + m.getTitle() + " (" + relDate + ")");
                if (TheMovieDbApi.compareMovies(m, title, String.valueOf(year), searchMatch)) {
                    moviedb = m;
                    break;
                }
            }
        } catch (MovieDbException ex) {
            LOG.debug("Failed to get movie info for {}, error: {}", title, ex.getMessage());
        }
  
        if (moviedb != null) {
            LOG.info("TMDB ID found {} for '{}'", moviedb.getId(), title);
            return String.valueOf(moviedb.getId());
        }
        return null;
    }
}
