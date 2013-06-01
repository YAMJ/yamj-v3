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