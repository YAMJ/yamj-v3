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

import com.omertron.rottentomatoesapi.RottenTomatoesApi;
import com.omertron.rottentomatoesapi.RottenTomatoesException;
import com.omertron.rottentomatoesapi.model.RTMovie;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.VideoData;

@Service("rottenTomatoesRatingScanner")
public class RottenTomatoesRatingScanner implements IExtraMovieScanner {

    private static final String SCANNER_ID = "rottentomatoes";
    private static final Logger LOG = LoggerFactory.getLogger(RottenTomatoesRatingScanner.class);

    @Autowired
    private RottenTomatoesApi rottenTomatoesApi;
    @Autowired
    private ConfigService configService;
    @Autowired
    private ExtraScannerService extraScannerService;
    
    @PostConstruct
    public void init() {
        LOG.trace("Initialize RottenTomatoes rating scanner");

        // register this scanner
        extraScannerService.registerExtraScanner(this);
    }
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @Override
    public boolean isEnabled() {
        return configService.getBooleanProperty("rottentomatoes.rating.enabled", false);
    }

    @Override
    public void scanMovie(VideoData videoData) {
        RTMovie rtMovie = null;
        int rtId = NumberUtils.toInt(videoData.getSourceDbId(SCANNER_ID));

        if (rtId > 0) {
            try {
                rtMovie = rottenTomatoesApi.getDetailedInfo(rtId);
            } catch (RottenTomatoesException ex) {
                LOG.warn("Failed to get RottenTomatoes information: {}", ex.getMessage());
            }
        } else {
            try {
                List<RTMovie> rtMovies = rottenTomatoesApi.getMoviesSearch(videoData.getTitle());
                for (RTMovie tmpMovie : rtMovies) {
                    if (videoData.getTitle().equalsIgnoreCase(tmpMovie.getTitle()) && (videoData.getPublicationYear() == tmpMovie.getYear())) {
                        rtId = tmpMovie.getId();
                        rtMovie = tmpMovie;
                        videoData.setSourceDbId(SCANNER_ID, String.valueOf(rtId));
                        break;
                    }
                }
            } catch (RottenTomatoesException ex) {
                LOG.warn("Failed to get RottenTomatoes information: {}", ex.getMessage());
            }
        }

        if (rtMovie != null) {
            for (String type : configService.getPropertyAsList("rottentomatoes.rating.priority", "critics_score,audience_score,critics_rating,audience_rating")) {
                int rating = NumberUtils.toInt(rtMovie.getRatings().get(type));
                if (rating > 0) {
                    LOG.debug("{} - {} found: {}", videoData.getTitle(), type, rating);
                    videoData.addRating(SCANNER_ID, rating);
                    return;
                }
            }
        }
        
        LOG.debug("No RottenTomatoes rating found for '{}'", videoData.getTitle());
    }
}
