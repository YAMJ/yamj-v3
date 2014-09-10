/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.core.service.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.yamj.core.service.metadata.online.ScanResult;
import org.yamj.core.service.metadata.online.TheTVDbScanner;

import javax.annotation.Resource;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.yamj.core.database.model.Series;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class TheTVDbScannerTest extends AbstractJUnit4SpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbScannerTest.class);
    private static final String PLUGIN_ID = "tvdb";

    @Resource(name = "tvdbScanner")
    private TheTVDbScanner tvdbScanner;

    public TheTVDbScannerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getScannerName method, of class TheTVDbScanner.
     */
    @Test
    public void testGetScannerName() {
        LOG.info("getScannerName");
        String result = tvdbScanner.getScannerName();
        assertEquals("Changed scanner name", PLUGIN_ID, result);
    }

    /**
     * Test of afterPropertiesSet method, of class TheTVDbScanner.
     *
     * @throws java.lang.Exception
     */
    @Ignore("Does not need to be tested")
    public void testAfterPropertiesSet() throws Exception {
        LOG.info("afterPropertiesSet");
    }

    /**
     * Test of getSeriesId method, of class TheTVDbScanner.
     */
    @Test
    public void testGetSeriesId_Series() {
        LOG.info("getSeriesId");
        Series series = new Series();
        series.setTitle("Chuck", PLUGIN_ID);
        series.setStartYear(2007);
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
        String title = "Chuck";
        int year = 2007;
        String expResult = "80348";
        String result = tvdbScanner.getSeriesId(title, year);
        assertEquals("Wrong ID returned", expResult, result);
    }

    /**
     * Test of scan method, of class TheTVDbScanner.
     */
    @Test
    public void testScan() {
        LOG.info("scan");
        Series series = new Series();

        // Test that we get an error when scanning without an ID
        ScanResult result = tvdbScanner.scan(series);
        assertEquals("Wrong ScanResult returned", ScanResult.MISSING_ID, result);

        series = new Series();
        series.setSourceDbId(PLUGIN_ID, "70726");
        result = tvdbScanner.scan(series);

        LOG.info("***** SERIES {} *****", ToStringBuilder.reflectionToString(series, ToStringStyle.MULTI_LINE_STYLE));
        assertEquals("Wrong ScanResult returned", ScanResult.OK, result);
        assertEquals("Wrong series ID returned", "70726", series.getSourceDbId(PLUGIN_ID));
        assertEquals("Wrong title", "Babylon 5", series.getTitle());
        assertFalse("No Genres found", series.getGenreNames().isEmpty());

    }

}
