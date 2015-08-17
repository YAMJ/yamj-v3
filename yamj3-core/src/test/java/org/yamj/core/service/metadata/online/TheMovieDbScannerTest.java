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

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.CreditDTO;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class TheMovieDbScannerTest extends AbstractJUnit4SpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbScannerTest.class);
    private static final String SCANNER_ID = "tmdb";

    @Resource(name = "tmdbScanner")
    private TheMovieDbScanner tmdbScanner;

    /**
     * Test of getScannerName method, of class TheTVDbScanner.
     */
    @Test
    public void testGetScannerName() {
        LOG.info("testGetScannerName");
        String result = tmdbScanner.getScannerName();
        assertEquals("Changed scanner name", SCANNER_ID, result);
    }

    @Test
    public void testScanMovie() {
        LOG.info("testScanMovie");
        VideoData videoData = new VideoData();
        videoData.setSourceDbId(SCANNER_ID, "19995");
        tmdbScanner.scanMovie(videoData);
    }

    @Test
    public void testScanFilmography() {
        LOG.info("testScanFilmography");
        Person person = new Person();
        person.setSourceDbId(SCANNER_ID, "12795");

        // Test that we get an error when scanning without an ID
        tmdbScanner.scanFilmography(person);
        assertEquals(Boolean.FALSE, person.getNewFilmography().isEmpty());
        for (FilmParticipation p : person.getNewFilmography()) {
            System.err.println(p);
            
        }
    }

    @Test
    public void testGetSeriesId() {
        LOG.info("testGetSeriesId");
        Series series = new Series();
        series.setTitle("Game Of Thrones - Das Lied von Eis und Feuer", SCANNER_ID);
        String id = tmdbScanner.getSeriesId(series);
        assertEquals("1399", id);
    }

    @Test
    public void testScanSeries() {
        LOG.info("testScanSeries");
        Series series = new Series();
        series.setSourceDbId(SCANNER_ID, "1399");
        Season season = new Season();
        season.setSeason(1);
        season.setSeries(series);
        series.getSeasons().add(season);
        VideoData episode = new VideoData();
        episode.setEpisode(5);
        episode.setSeason(season);
        season.getVideoDatas().add(episode);
        tmdbScanner.scanSeries(series);
        
        System.err.println(episode.getTitle());
        for (CreditDTO credit : episode.getCreditDTOS()) {
            System.err.println(credit);
        }
        
    }
}
