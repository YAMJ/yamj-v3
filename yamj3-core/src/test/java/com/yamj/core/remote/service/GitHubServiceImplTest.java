package com.yamj.core.remote.service;

import javax.annotation.Resource;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHubServiceImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubServiceImplTest.class);
    @Resource(name = "githubService")
    private GitHubServiceImpl github;

    public GitHubServiceImplTest() {
        BasicConfigurator.configure();
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
        String expResult = "";
        String result = github.pushDate();
        System.out.println("Got result: '" + result + "'");
//        assertEquals(expResult, result);
    }
}