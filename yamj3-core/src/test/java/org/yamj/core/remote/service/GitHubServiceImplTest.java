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
package org.yamj.core.remote.service;

import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

public class GitHubServiceImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubServiceImplTest.class);
    @Resource(name = "githubService")
    private GitHubServiceImpl github;

    public GitHubServiceImplTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        BasicConfigurator.configure();
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
     * Test of pushDate method, of class GitHubServiceImpl.
     */
    @Ignore("No need to test this")
    public void testPushDate_String_String() {
    }

    /**
     * Test of pushDate method, of class GitHubServiceImpl.
     */
    @Test
    public void testPushDate() {
        LOG.info("pushDate");
        String result = github.pushDate();
        LOG.info("Got result: '{}'", result);
        assertTrue("Returned date is blank", StringUtils.isNotBlank(result));
    }
}