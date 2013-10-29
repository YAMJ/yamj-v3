/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package org.yamj.core.tools.web;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.yamj.api.common.http.DefaultPoolingHttpClient;
import org.yamj.common.tools.PropertyTools;

public class PoolingHttpClient extends DefaultPoolingHttpClient implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(PoolingHttpClient.class);
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private final Map<String, Integer> groupLimits = new HashMap<String, Integer>();
    private final List<String> routedHosts = new ArrayList<String>();

    public PoolingHttpClient() {
        this(null, null);
    }

    public PoolingHttpClient(ClientConnectionManager conman) {
        this(conman, null);
    }

    public PoolingHttpClient(HttpParams params) {
        this(null, params);
    }

    public PoolingHttpClient(ClientConnectionManager connectionManager, HttpParams httpParams) {
        super(connectionManager, httpParams);

        // First we have to read/create the rules
        // Default, can be overridden
        groupLimits.put(".*", 1);
        String limitsProperty = PropertyTools.getProperty("yamj3.http.maxDownloadSlots", ".*=1");
        LOG.debug("Using download limits: {}", limitsProperty);

        Pattern pattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
        Matcher matcher = pattern.matcher(limitsProperty);
        while (matcher.find()) {
            String group = matcher.group(1);
            try {
                Pattern.compile(group);
                groupLimits.put(group, Integer.parseInt(matcher.group(2)));
            } catch (Exception error) {
                LOG.debug("Rule '{}' is not valid regexp, ignored", group);
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        ClientConnectionManager clientManager = super.getConnectionManager();
        if (clientManager != null) {
            clientManager.shutdown();
        }
    }

    @Override
    public String requestContent(HttpGet httpGet, Charset charset) throws IOException {
        // set route (if not set before)
        setRoute(httpGet);

        HttpResponse response = execute(httpGet);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new RuntimeException("Unexpected status " + statusCode + " for uri " + httpGet.getURI());
        }

        if (charset == null) {
            // use UTF8 char set if none charset given
            return readContent(response, UTF8_CHARSET);
        }
        // use given charset
        return readContent(response, charset);
    }

    private void setRoute(HttpUriRequest request) throws ClientProtocolException {
        HttpHost httpHost = determineTarget(request);
        String key = httpHost.toString();

        synchronized (routedHosts) {
            if (!routedHosts.contains(key)) {
                String group = ".*";
                for (String searchGroup : groupLimits.keySet()) {
                    if (key.matches(searchGroup) && searchGroup.length() > group.length()) {
                        group = searchGroup;

                    }
                }
                int maxRequests = groupLimits.get(group);

                LOG.debug("IO download host: {}; rule: {}, maxRequests: {}", key, group, maxRequests);
                routedHosts.add(key);

                HttpRoute httpRoute = new HttpRoute(httpHost);

                ClientConnectionManager conMan = this.getConnectionManager();
                if (conMan instanceof PoolingClientConnectionManager) {
                    PoolingClientConnectionManager poolMan = (PoolingClientConnectionManager) conMan;
                    poolMan.setMaxPerRoute(httpRoute, maxRequests);
                }
            }
        }
    }

    private static HttpHost determineTarget(HttpUriRequest request) throws ClientProtocolException {
        HttpHost target = null;
        URI requestURI = request.getURI();
        if (requestURI.isAbsolute()) {
            target = URIUtils.extractHost(requestURI);
            if (target == null) {
                throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI);
            }
        }
        return target;
    }
}