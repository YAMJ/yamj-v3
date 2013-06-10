/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.yamj.core.database.model.Genre;
import org.yamj.core.database.model.VideoData;
import javax.annotation.Resource;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class OfdbScannerTest extends AbstractJUnit4SpringContextTests {

    @Resource(name = "ofdbScanner")
    private OfdbScanner ofdbScanner;

    @Test
    public void testGetMovieId() {
        String id = ofdbScanner.getMovieId("Avatar", 2009);
        assertEquals("http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora", id);
    }

    @Test
    public void testScan() {
        VideoData videoData = new VideoData();
        videoData.setSourceDbId(ofdbScanner.getScannerName(), "http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora");
        ofdbScanner.scan(videoData);

        assertEquals("Avatar - Aufbruch nach Pandora", videoData.getTitle());
        assertEquals("Avatar", videoData.getTitleOriginal());
        assertEquals(2009, videoData.getPublicationYear());
        assertEquals("Großbritannien", videoData.getCountry());
        assertNotNull(videoData.getPlot());
        assertNotNull(videoData.getOutline());
        assertTrue(videoData.getGenres().contains(new Genre("Abenteuer")));
        assertTrue(videoData.getGenres().contains(new Genre("Action")));
        assertTrue(videoData.getGenres().contains(new Genre("Science-Fiction")));

//        LinkedHashSet<String> testList = new LinkedHashSet<String>();
//        testList.add("James Cameron");
//        assertEquals(Arrays.asList(testList.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getDirectors().toArray(), 1)).toString());
//
//        testList.clear();
//        testList.add("Sam Worthington");
//        testList.add("Zoe Saldana");
//        assertEquals(Arrays.asList(testList.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getCast().toArray(), 2)).toString());
//
//        testList.clear();
//        testList.add("James Cameron");
//        assertEquals(Arrays.asList(testList.toArray()).toString(), Arrays.asList(Arrays.copyOf(movie.getWriters().toArray(), 1)).toString());
    }
}