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
import static org.junit.Assert.assertFalse;

import javax.annotation.Resource;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.core.AbstractTest;
import org.yamj.core.database.model.Series;

public class TheTVDbScannerTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbScannerTest.class);

    @Resource(name = "tvdbScanner")
    private TheTVDbScanner tvdbScanner;

    /**
     * Test of getScannerName method, of class TheTVDbScanner.
     */
    @Test
    public void testGetScannerName() {
        LOG.info("getScannerName");
        String result = tvdbScanner.getScannerName();
        assertEquals("Changed scanner name", tvdbScanner.getScannerName(), result);
    }

    /**
     * Test of getSeriesId method, of class TheTVDbScanner.
     */
    @Test
    public void testGetSeriesId_Series() {
        LOG.info("getSeriesId");
        Series series = new Series();
        series.setTitle("Chuck", tvdbScanner.getScannerName());
        series.setStartYear(2007, tvdbScanner.getScannerName());
        String expResult = "80348";
        String result = tvdbScanner.getSeriesId(series);
        assertEquals("Wrong ID returned", expResult, result);
    }

    /**
     * Test of getSeriesId method, of class TheTVDbScanner.
     */
    @Test
    public void testGetSeriesId_String_int() {
        LOG.info("getSeriesId");
        Series series = new Series();
        series.setTitle("Chuck", tvdbScanner.getScannerName());
        series.setStartYear(2007, tvdbScanner.getScannerName());
        String expResult = "80348";
        String result = tvdbScanner.getSeriesId(series);
        assertEquals("Wrong ID returned", expResult, result);
    }

    /**
     * Test of scan method, of class TheTVDbScanner.
     */
    @Ignore
    public void testScan() {
        LOG.info("scan");
        Series series = new Series();
        series.setSourceDbId(tvdbScanner.getScannerName(), "70726");
        ScanResult result = tvdbScanner.scanSeries(series, false);

        LOG.info("***** SERIES {} *****", ToStringBuilder.reflectionToString(series, ToStringStyle.MULTI_LINE_STYLE));
        assertEquals("Wrong ScanResult returned", ScanResult.OK, result);
        assertEquals("Wrong series ID returned", "70726", series.getSourceDbId(tvdbScanner.getScannerName()));
        assertEquals("Wrong title", "Babylon 5", series.getTitle());
        assertFalse("No Genres found", series.getGenreNames().isEmpty());
    }
}
