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

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.metadata.online.ImdbScanner;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class RottenTomatoesScannerTest extends AbstractJUnit4SpringContextTests {

    @Resource(name = "rottenTomatoesScanner")
    private RottenTomatoesScanner rottenTomatoesScanner;

    @Test
    public void testScanMovie() {
        VideoData videoData = new VideoData();
        videoData.setTitle("Avatar", ImdbScanner.SCANNER_ID);
        videoData.setPublicationYear(2009, ImdbScanner.SCANNER_ID);
        rottenTomatoesScanner.scanMovie(videoData);
        
        assertEquals(83, videoData.getRating(rottenTomatoesScanner.getScannerName()));
    }
}