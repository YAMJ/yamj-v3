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

import javax.annotation.Resource;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.service.metadata.online.imdb.ImdbScanner;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class ImdbScannerTest extends AbstractJUnit4SpringContextTests {


    @Resource(name = "imdbScanner")
    private ImdbScanner imdbScanner;

    @Test
    public void testMovie() {
        VideoData videoData = new VideoData();
        videoData.setSourceDbId(imdbScanner.getScannerName(), "tt0499549");
        imdbScanner.scan(videoData);

        assertEquals("Avatar - Aufbruch nach Pandora", videoData.getTitle());
        assertEquals("Avatar", videoData.getTitleOriginal());
        assertEquals(2009, videoData.getPublicationYear());
        assertEquals("USA", videoData.getCountry());
        assertEquals("When his brother is killed in a robbery, paraplegic Marine Jake Sully decides to take his place in a mission on the distant world of Pandora. There he learns of greedy corporate figurehead Parker Selfridge's intentions of driving off the native humanoid \"Na'vi\" in order to mine for the precious material scattered throughout their rich woodland. In exchange for the spinal surgery that will fix his legs, Jake gathers intel for the cooperating military unit spearheaded by gung-ho Colonel Quaritch, while simultaneously attempting to infiltrate the Na'vi people with the use of an \"avatar\" identity. While Jake begins to bond with the native tribe and quickly falls in love with the beautiful alien Neytiri, the restless Colonel moves forward with his ruthless extermination tactics, forcing the soldier to take a stand - and fight back in an epic battle for the fate of Pandora.", videoData.getPlot());
        assertNotNull(videoData.getOutline());
        assertTrue(videoData.getGenreNames().contains("Adventure"));
        assertTrue(videoData.getGenreNames().contains("Action"));
        assertTrue(videoData.getGenreNames().contains("Fantasy"));
        assertTrue(videoData.getStudioNames().contains("Twentieth Century Fox Film Corporation"));
        assertTrue(videoData.getStudioNames().contains("Lightstorm Entertainment"));
        
        for (CreditDTO credit : videoData.getCreditDTOS()) {
            String role = credit.getRole() == null ? "" : " (" + credit.getRole() + ")";
            System.err.println(credit.getJobType() +": " + credit.getName() + role);
        }
    }

    @Test
    public void testSeries() {
        Series series = new Series();
        series.setSourceDbId(imdbScanner.getScannerName(), "tt0944947");
        
        Season season = new Season();
        season.setSeason(1);
        season.setSeries(series);
        series.getSeasons().add(season);
        
        VideoData episode1 = new VideoData("GOT_1");
        episode1.setEpisode(1);
        episode1.setSeason(season);
        season.getVideoDatas().add(episode1);

        VideoData episode2 = new VideoData("GOT_2");
        episode2.setEpisode(2);
        episode2.setSeason(season);
        season.getVideoDatas().add(episode2);
        
        imdbScanner.scan(series);

        assertEquals("Game of Thrones - Das Lied von Eis und Feuer", series.getTitle());
        assertEquals("Game of Thrones", series.getTitleOriginal());
        assertEquals(2011, series.getStartYear());
        assertEquals(-1, series.getEndYear());
        
        for (VideoData videoData : season.getVideoDatas()) {
            assertNotNull(videoData.getTitle());
            assertNotNull(videoData.getReleaseDate());
            assertNotNull(videoData.getPlot());
            
            for (CreditDTO credit : videoData.getCreditDTOS()) {
                String role = credit.getRole() == null ? "" : " (" + credit.getRole() + ")";
                System.err.println(credit.getJobType() +": " + credit.getName() + role);
            }
        }
    }

    @Test
    public void testPerson() {
        Person person = new Person();
        person.setSourceDbId(imdbScanner.getScannerName(), "nm0001352");
        imdbScanner.scan(person);

        assertEquals("Terence Hill", person.getName());
        assertEquals("Mario Girotti", person.getBirthName());
        assertNotNull(person.getBiography());
        assertEquals("Venice, Veneto, Italy", person.getBirthPlace());
    }
}