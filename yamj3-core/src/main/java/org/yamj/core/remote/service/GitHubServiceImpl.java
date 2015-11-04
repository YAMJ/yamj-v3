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
package org.yamj.core.remote.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HTTP;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.common.remote.service.GitHubService;
import org.yamj.common.tools.ClassTools;
import org.yamj.common.tools.DateTimeTools;

/**
 * Calls GitHub to determine the last code update
 *
 * @author stuart.boston
 */
@Service("githubService")
public class GitHubServiceImpl implements GitHubService {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubServiceImpl.class);
    private static final String GH_API = "https://api.github.com/repos/";
    private static final String GH_OWNER = "YAMJ";
    private static final String GH_REPO = "yamj-v3";
    private static final String ACCEPT = "Accept";
    private static final String GH_USER_AGENT = "GitHubJava/2.1.0";
    private static final String GH_ACCEPT = "application/vnd.github.beta+json";
    private static final long MILLISECONDS_PER_DAY = 24L * 60 * 60 * 1000;
    @Autowired
    private PoolingHttpClient httpClient;

    /**
     * Get the date of the last push to a repository
     *
     * @param owner
     * @param repository
     * @return
     */
    @Override
    public String pushDate(String owner, String repository) {
        if (StringUtils.isBlank(owner) || StringUtils.isBlank(repository)) {
            LOG.error("Owner '{}' or repository '{}' cannot be blank", owner, repository);
            throw new IllegalArgumentException("Owner or repository cannot be blank");
        }

        String returnDate = StringUtils.EMPTY;

        StringBuilder url = new StringBuilder(GH_API);
        url.append(owner).append("/").append(repository);

        try {
            HttpGet httpGet = new HttpGet(url.toString());
            httpGet.setHeader(HTTP.USER_AGENT, GH_USER_AGENT);
            httpGet.addHeader(ACCEPT, GH_ACCEPT);

            URL newUrl = new URL(url.toString());
            httpGet.setURI(newUrl.toURI());

            DigestedResponse response = httpClient.requestContent(httpGet);
            if (ResponseTools.isNotOK(response)) {
                LOG.warn("Request for GitHub informations failed with status {}", response.getStatusCode());
                return returnDate;
            }

            // This is ugly and a bit of a hack, but I don't need to unmarshal the whole object just for a date
            String jsonData = response.getContent();
            int posStart = jsonData.indexOf("pushed_at");
            posStart = jsonData.indexOf("20", posStart);
            int posEnd = jsonData.indexOf('\"', posStart);
            returnDate = jsonData.substring(posStart, posEnd);
            LOG.info("Date: '{}'", returnDate);
        } catch (IOException | RuntimeException | URISyntaxException ex) {
            LOG.error("Unable to get GitHub information, error: {}", ex.getMessage());
            LOG.warn(ClassTools.getStackTrace(ex));
            return returnDate;
        }
        return returnDate;
    }

    /**
     * Get the last push date for the default YAMJ repository
     */
    @Override
    public String pushDate() {
        return pushDate(GH_OWNER, GH_REPO);
    }

    /**
     * Check the installation date of the default repository
     *
     * @param buildDate
     * @param maxAgeDays
     * @return
     */
    @Override
    public boolean checkInstallationDate(DateTime buildDate, int maxAgeDays) {
        return checkInstallationDate(GH_OWNER, GH_REPO, buildDate, maxAgeDays);
    }

    /**
     * Compare the installation date of the build against the push date of the repository. <br>
     * If the difference is greater than maxAgeDays, return false
     *
     * @param owner
     * @param repository
     * @param buildDate
     * @param maxAgeDays
     * @return
     */
    @Override
    public boolean checkInstallationDate(String owner, String repository, DateTime buildDate, int maxAgeDays) {
        String ghDate = pushDate(owner, repository);
        LOG.debug("GitHub Date: {}", ghDate);
        LOG.debug("Build Date : {}", buildDate);

        if (StringUtils.isBlank(ghDate) || buildDate == null) {
            LOG.debug("Invalid (blank) date, check skipped");
            return Boolean.TRUE;
        }

        DateTime dt1 = DateTimeTools.parseDate(ghDate, DateTimeTools.ISO8601_FORMAT);
        long diff = DateTimeTools.getDuration(dt1, buildDate);

        LOG.debug("Difference : {}", diff, DateTimeTools.formatDurationColon(diff));
        if (diff > (maxAgeDays * MILLISECONDS_PER_DAY)) {
            LOG.warn("Your installation is older than () days! Please update it", maxAgeDays);
            return Boolean.FALSE;
        } else if (diff > 0) {
            LOG.debug("Your installation is only {} old.", DateTimeTools.formatDurationText(diff));
        } else {
            LOG.debug("Your installation is up to date");
        }
        return Boolean.TRUE;
    }
}
