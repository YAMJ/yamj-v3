package com.moviejukebox.core.scanner.moviedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.moviejukebox.core.service.moviedb.OfdbScanner;

import com.moviejukebox.core.database.model.Genre;
import com.moviejukebox.core.database.model.VideoData;
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
        String id = ofdbScanner.getMoviedbId("Avatar", 2009);
        assertEquals("http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora", id);
    }

    @Test
    public void testScan() {
        VideoData videoData = new VideoData();
        videoData.setMoviedbId(OfdbScanner.OFDB_SCANNER_ID,"http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora");
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