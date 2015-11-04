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
import static org.junit.Assert.assertTrue;

import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.core.web.apis.SearchEngineTools;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = {"classpath:spring-test.xml"})
public class SearchEngineToolsTest extends AbstractJUnit4SpringContextTests {

    private static final Logger LOG = LoggerFactory.getLogger(SearchEngineToolsTest.class);

    @Autowired
    private PoolingHttpClient httpClient;
    
    @Test
    public void roundTripIMDB() {
        LOG.info("roundTripIMDB");
        SearchEngineTools search = new SearchEngineTools(httpClient);

        // movie
        for (int i = 0; i < search.countSearchSites(); i++) {
            String engine = search.getCurrentSearchEngine();
            LOG.info("Testing " + engine);
            String url = search.searchURL("Avatar", 2009, "www.imdb.com/title", false);
            url = StringUtils.removeEnd(url, "/");
            assertEquals("Search engine '" + engine + "' failed", "http://www.imdb.com/title/tt0499549", url);
        }

        // TV show, must leave out the year and search for TV series
        for (int i = 0; i < search.countSearchSites(); i++) {
            String engine = search.getCurrentSearchEngine();
            LOG.info("Testing " + engine);
            String url = search.searchURL("Two and a Half Men", -1, "www.imdb.com/title", "TV series", false);
            url = StringUtils.removeEnd(url, "/");
            assertEquals("Search engine '" + engine + "' failed", "http://www.imdb.com/title/tt0369179", url);
        }
    }

    @Test
    public void roundTripOFDB() {
        LOG.info("roundTripOFDB");
        SearchEngineTools search = new SearchEngineTools(httpClient, Locale.GERMANY);
        search.setSearchSites("google");
        
        // movie
        for (int i = 0; i < search.countSearchSites(); i++) {
            String engine = search.getCurrentSearchEngine();
            LOG.info("Testing " + engine);
            String url = search.searchURL("Avatar", 2009, "www.ofdb.de/film", false);
            assertEquals("Search engine '" + engine + "' failed", "http://www.ofdb.de/film/188514,Avatar---Aufbruch-nach-Pandora", url);
        }

        // TV show
        for (int i = 0; i < search.countSearchSites(); i++) {
            String engine = search.getCurrentSearchEngine();
            LOG.info("Testing " + engine);
            String url = search.searchURL("Two and a Half Men", 2003, "www.ofdb.de/film", false);
            assertEquals("Search engine '" + engine + "' failed", "http://www.ofdb.de/film/66192,Mein-cooler-Onkel-Charlie", url);
        }
    }

    @Test
    public void roundTripAllocine() {
        LOG.info("roundTripAllocine");
        SearchEngineTools search = new SearchEngineTools(httpClient, Locale.FRANCE);

        // movie, must set search suffix
        search.setSearchSuffix("/fichefilm_gen_cfilm");
        for (int i = 0; i < search.countSearchSites(); i++) {
            String engine = search.getCurrentSearchEngine();
            LOG.info("Testing " + engine);
            String url = search.searchURL("Avatar", 2009, "www.allocine.fr/film", false);
            assertEquals("Search engine '" + engine + "' failed", "http://www.allocine.fr/film/fichefilm_gen_cfilm=61282.html", url);
        }
        // TV show, must set search suffix
        search.setSearchSuffix("/ficheserie_gen_cserie");
        for (int i = 0; i < search.countSearchSites(); i++) {
            String engine = search.getCurrentSearchEngine();
            LOG.info("Testing " + engine);
            String url = search.searchURL("Two and a Half Men", 2003, "www.allocine.fr/series", false);
            assertTrue("Search engine '" + engine + "' failed: " + url, url.startsWith("http://www.allocine.fr/series/ficheserie_gen_cserie=132.html"));
        }
    }
}
