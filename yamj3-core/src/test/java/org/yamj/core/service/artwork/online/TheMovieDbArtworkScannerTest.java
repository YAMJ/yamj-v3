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
package org.yamj.core.service.artwork.online;

import java.util.List;
import javax.annotation.Resource;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class TheMovieDbArtworkScannerTest extends AbstractJUnit4SpringContextTests {

    @Resource(name = "tmdbArtworkScanner")
    private TheMovieDbArtworkScanner tmdbArtworkScanner;

    @Test
    public void testSeriesPoster() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        
        List<ArtworkDetailDTO> dtos = tmdbArtworkScanner.getPosters(series);
        if (dtos != null) {
            for (ArtworkDetailDTO dto : dtos) {
                System.err.println(dto);
            }
        }
    }

    @Test
    public void testSeriesFanart() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        
        List<ArtworkDetailDTO> dtos = tmdbArtworkScanner.getFanarts(series);
        if (dtos != null) {
            for (ArtworkDetailDTO dto : dtos) {
                System.err.println(dto);
            }
        }
    }

    @Test
    public void testSeasonPoster() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        Season season = new Season();
        season.setSeason(1);
        season.setSeries(series);
        series.getSeasons().add(season);
        
        List<ArtworkDetailDTO> dtos = tmdbArtworkScanner.getPosters(season);
        if (dtos != null) {
            for (ArtworkDetailDTO dto : dtos) {
                System.err.println(dto);
            }
        }
    }

    @Test
    public void testSeasonFanart() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        Season season = new Season();
        season.setSeason(1);
        season.setSeries(series);
        series.getSeasons().add(season);
        
        List<ArtworkDetailDTO> dtos = tmdbArtworkScanner.getFanarts(season);
        if (dtos != null) {
            for (ArtworkDetailDTO dto : dtos) {
                System.err.println(dto);
            }
        }
    }

    @Test
    public void testVideoImages() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        Season season = new Season();
        season.setSeason(4);
        season.setSeries(series);
        series.getSeasons().add(season);
        VideoData episode = new VideoData();
        episode.setEpisode(2);
        episode.setSeason(season);
        season.getVideoDatas().add(episode);
        
        List<ArtworkDetailDTO> dtos = tmdbArtworkScanner.getVideoImages(episode);
        if (dtos != null) {
            for (ArtworkDetailDTO dto : dtos) {
                System.err.println(dto);
            }
        }
    }
}