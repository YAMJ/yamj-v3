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
import org.yamj.core.AbstractTest;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.plugin.api.artwork.ArtworkDTO;

public class TheMovieDbArtworkScannerTest extends AbstractTest {

    @Resource(name = "tmdbArtworkScanner")
    private TheMovieDbArtworkScanner tmdbArtworkScanner;

    @Test
    public void testSeriesPoster() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        
        List<ArtworkDTO> dtos = tmdbArtworkScanner.getPosters(series);
        logArtworks(ArtworkType.POSTER, dtos, getClass());
    }

    @Test
    public void testSeriesFanart() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        
        List<ArtworkDTO> dtos = tmdbArtworkScanner.getFanarts(series);
        logArtworks(ArtworkType.FANART, dtos, getClass());
    }

    @Test
    public void testSeasonPoster() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        Season season = new Season();
        season.setSeason(1);
        season.setSeries(series);
        series.getSeasons().add(season);
        
        List<ArtworkDTO> dtos = tmdbArtworkScanner.getPosters(season);
        logArtworks(ArtworkType.POSTER, dtos, getClass());
    }

    @Test
    public void testSeasonFanart() {
        Series series = new Series();
        series.setSourceDbId(tmdbArtworkScanner.getScannerName(), "1399");
        Season season = new Season();
        season.setSeason(1);
        season.setSeries(series);
        series.getSeasons().add(season);
        
        List<ArtworkDTO> dtos = tmdbArtworkScanner.getFanarts(season);
        logArtworks(ArtworkType.FANART, dtos, getClass());
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
        
        List<ArtworkDTO> dtos = tmdbArtworkScanner.getVideoImages(episode);
        logArtworks(ArtworkType.VIDEOIMAGE, dtos, getClass());
    }
}