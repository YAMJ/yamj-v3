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
package org.yamj.common.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.joda.time.DateTime;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.tools.PropertyTools;

/**
 *
 * @author Stuart
 */
public class YamjInfoTest {

    private static final Logger LOG = LoggerFactory.getLogger(YamjInfoTest.class);
    private static YamjInfo YI;

    @BeforeClass
    public static void setUpClass() {
        BasicConfigurator.configure();
        YI = new YamjInfo(YamjInfoBuild.COMMON);
    }

    @AfterClass
    public static void tearDownClass() {
        // nothing to do
    }

    @Before
    public void setUp() {
        // nothing to do
    }

    @After
    public void tearDown() {
        // nothing to do
    }

    /**
     * Test of getProjectName method, of class YamjInfo.
     */
    @Test
    public void testGetProjectName() {
        LOG.info("getProjectName");
        assertTrue(StringUtils.isNotBlank(YI.getProjectName()));
    }

    /**
     * Test of getProjectVersion method, of class YamjInfo.
     */
    @Test
    public void testGetProjectVersion() {
        LOG.info("getProjectVersion");
        assertTrue(StringUtils.isNotBlank(YI.getProjectVersion()));
    }

    /**
     * Test of getBuildDateTime method, of class YamjInfo.
     */
    @Test
    public void testGetBuildDateTime() {
        LOG.info("getBuildDateTime");
        DateTime result = YI.getBuildDateTime();
        assertNotNull(result);
    }

    /**
     * Test of getStartUpDateTime method, of class YamjInfo.
     */
    @Test
    public void testGetStartUpDateTime() {
        LOG.info("getStartUpDateTime");
        DateTime result = YI.getStartUpDateTime();
        assertNotNull(result);
    }

    /**
     * Test of getBuildRevision method, of class YamjInfo.
     */
    @Test
    public void testGetBuildRevision() {
        LOG.info("getBuildRevision");
        assertTrue(StringUtils.isNotBlank(YI.getBuildRevision()));
    }

    /**
     * Test of getModuleName method, of class YamjInfo.
     */
    @Test
    public void testGetModuleName() {
        LOG.info("getModuleName");
        assertTrue(StringUtils.isNotBlank(YI.getModuleName()));
    }

    /**
     * Test of getModuleDescription method, of class YamjInfo.
     */
    @Test
    public void testGetModuleDescription() {
        LOG.info("getModuleDescription");
        assertTrue(StringUtils.isNotBlank(YI.getModuleDescription()));
    }

    /**
     * Test of isBuildDirty method, of class YamjInfo.
     */
    @Test
    public void testIsBuildDirty() {
        LOG.info("isBuildDirty");
        Boolean result = YI.isBuildDirty();
        assertNotNull(result);
    }

    /**
     * Test of getProcessorCores method, of class YamjInfo.
     */
    @Test
    public void testGetProcessorCores() {
        LOG.info("getProcessorCores");
        assertTrue(YI.getProcessorCores() > 0);
    }

    /**
     * Test of getOsArch method, of class YamjInfo.
     */
    @Test
    public void testGetOsArch() {
        LOG.info("getOsArch");
        assertTrue(StringUtils.isNotBlank(YI.getOsArch()));
    }

    /**
     * Test of getOsName method, of class YamjInfo.
     */
    @Test
    public void testGetOsName() {
        LOG.info("getOsName");
        assertTrue(StringUtils.isNotBlank(YI.getOsName()));
    }

    /**
     * Test of getOsVersion method, of class YamjInfo.
     */
    @Test
    public void testGetOsVersion() {
        LOG.info("getOsVersion");
        assertTrue(StringUtils.isNotBlank(YI.getOsVersion()));
    }

    /**
     * Test of getBuildDate method, of class YamjInfo.
     */
    @Test
    public void testGetBuildDate() {
        LOG.info("getBuildDate");
        assertTrue(StringUtils.isNotBlank(YI.getBuildDate()));
    }

    /**
     * Test of getStartUpTime method, of class YamjInfo.
     */
    @Test
    public void testGetStartUpTime() {
        LOG.info("getStartUpTime");
        assertTrue(StringUtils.isNotBlank(YI.getStartUpTime()));
    }

    /**
     * Test of getUptime method, of class YamjInfo.
     */
    @Test
    public void testGetUptime() {
        LOG.info("getUptime");
        assertTrue(StringUtils.isNotBlank(YI.getUptime()));
    }

    /**
     * Test of getDatabaseName method, of class YamjInfo.
     */
    @Test
    public void testGetDatabaseName() {
        LOG.info("getDatabaseName");
        assertTrue(StringUtils.isNotBlank(YI.getDatabaseName()));
    }

    /**
     * Test of getDatabaseIp method, of class YamjInfo.
     */
    @Test
    public void testGetDatabaseIp() {
        LOG.info("getDatabaseIp");
        assertTrue(StringUtils.isNotBlank(YI.getDatabaseIp()));
    }

    /**
     * Test of getCoreIp method, of class YamjInfo.
     */
    @Test
    public void testGetCoreIp() {
        LOG.info("getCoreIp");

        assertTrue(StringUtils.isNotBlank(YI.getCoreIp()));

        // Rebuild the YamjInfo to pick up the property change
        PropertyTools.setProperty("yamj3.core.url", "www.test.com");
        YI = new YamjInfo(YamjInfoBuild.COMMON);

        assertEquals("www.test.com", YI.getCoreIp());
    }

    /**
     * Test of getCorePort method, of class YamjInfo.
     */
    @Test
    public void testGetCorePort() {
        LOG.info("getCorePort");
        assertTrue(YI.getCorePort() > 0);
    }

    /**
     * Test of getBaseArtworkUrl method, of class YamjInfo.
     */
    @Test
    public void testGetBaseArtworkUrl() {
        LOG.info("getBaseArtworkUrl");
        assertTrue(StringUtils.isNotBlank(YI.getBaseArtworkUrl()));
    }

    /**
     * Test of getBaseMediainfoUrl method, of class YamjInfo.
     */
    @Test
    public void testGetBaseMediainfoUrl() {
        LOG.info("getBaseMediainfoUrl");
        assertTrue(StringUtils.isNotBlank(YI.getBaseMediainfoUrl()));
    }

    /**
     * Test of getBasePhotoUrl method, of class YamjInfo.
     */
    @Test
    public void testGetBasePhotoUrl() {
        LOG.info("getBasePhotoUrl");
        assertTrue(StringUtils.isNotBlank(YI.getBasePhotoUrl()));
    }

    /**
     * Test of getSkinDir method, of class YamjInfo.
     */
    @Test
    public void testGetSkinDir() {
        LOG.info("getSkinDir");
        assertTrue(StringUtils.isNotBlank(YI.getSkinDir()));
    }

}
