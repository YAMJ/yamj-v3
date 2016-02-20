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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.yamj.core.AbstractTest;

import javax.annotation.Resource;
import org.junit.Test;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;

public class ComingSoonScannerTest extends AbstractTest {

    @Resource(name = "comingSoonScanner")
    private ComingSoonScanner comingSoonScanner;

    @Test
    public void testGetMovieId() {
        VideoData videoData = new VideoData();
        videoData.setTitle("Avatar", comingSoonScanner.getScannerName());
        videoData.setPublicationYear(2009, comingSoonScanner.getScannerName());
        String id = comingSoonScanner.getMovieId(videoData);
        assertEquals("846", id);
    }

    @Test
    public void testScanMovie() {
        VideoData videoData = new VideoData();
        videoData.setSourceDbId(comingSoonScanner.getScannerName(), "846");
        comingSoonScanner.scanMovie(videoData);

        assertEquals("Avatar", videoData.getTitle());
        assertEquals("Avatar", videoData.getTitleOriginal());
        assertEquals(2009, videoData.getPublicationYear());
        assertEquals("Entriamo in questo mondo alieno attraverso gli occhi di Jake Sully, un ex Marine costretto a vivere sulla sedia a rotelle. Nonostante il suo corpo martoriato, Jake nel profondo è ancora un combattente. E' stato reclutato per viaggiare anni luce sino all'avamposto umano su Pandora, dove alcune società stanno estraendo un raro minerale che è la chiave per risolvere la crisi energetica sulla Terra. Poiché l'atmosfera di Pandora è tossica, è stato creato il Programma Avatar, in cui i \"piloti\" umani collegano le loro coscienze ad un avatar, un corpo organico controllato a distanza che può sopravvivere nell'atmosfera letale. Questi avatar sono degli ibridi geneticamente sviluppati dal DNA umano unito al DNA dei nativi di Pandora... i Na’vi.Rinato nel suo corpo di Avatar, Jake può camminare nuovamente. Gli viene affidata la missione di infiltrarsi tra i Na'vi che sono diventati l'ostacolo maggiore per l'estrazione del prezioso minerale. Ma una bellissima donna Na'vi, Neytiri, salva la vita a Jake, e questo cambia tutto.", videoData.getPlot());
        assertNotNull(videoData.getOutline());
        assertTrue(videoData.getGenreNames().contains("Fantascienza"));
        assertTrue(videoData.getGenreNames().contains("Avventura"));
        assertTrue(videoData.getGenreNames().contains("Azione"));
        assertTrue(videoData.getGenreNames().contains("Thriller"));
    }

    @Test
    public void testGetSeriesId() {
        Series series = new Series();
        series.setTitle("Two and a half men", comingSoonScanner.getScannerName());
        series.setStartYear(2003, comingSoonScanner.getScannerName());
        String id = comingSoonScanner.getSeriesId(series);
        assertEquals("28", id);
    }

    @Test
    public void testScanSeries() {
        Series series = new Series();
        series.setSourceDbId(comingSoonScanner.getScannerName(), "28");
        
        Season season = new Season();
        season.setSeason(1);
        season.setSeries(series);
        series.getSeasons().add(season);
        
        VideoData episode1 = new VideoData("TwoAndAHalfMen_1");
        episode1.setEpisode(1);
        episode1.setSeason(season);
        season.getVideoDatas().add(episode1);

        VideoData episode2 = new VideoData("TwoAndAHalfMen_2");
        episode2.setEpisode(2);
        episode2.setSeason(season);
        season.getVideoDatas().add(episode2);

        comingSoonScanner.scanSeries(series);

        assertEquals("Due Uomini E Mezzo", series.getTitle());
        assertEquals("Two and a Half Men", series.getTitleOriginal());
        assertNotNull(series.getPlot());
        logCredits(season, getClass());
    }
}