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
package org.yamj.core.tools.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolingHttpClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PoolingHttpClientBuilder.class);

    private boolean systemProperties = false;
    private int connectionTimeout = 25000;
    private int connectionRequestTimeout = 15000;
    private int socketTimeout = 90000;
    private String proxyHost;
    private int proxyPort = 0;
    private String proxyUsername;
    private String proxyPassword;
    private int connectionsMaxTotal = 20;
    private int connectionsMaxPerRoute = 1;
    private String maxDownloadSlots;
    
    protected PoolingHttpClientBuilder() {}
    
    public static PoolingHttpClientBuilder create() {
        return new PoolingHttpClientBuilder();
    }
    
    public void setSystemProperties(boolean systemProperties) {
        this.systemProperties = systemProperties;
    }
  
    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }
  
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
  
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
  
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }
  
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
  
    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }
  
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }
    
    public void setConnectionsMaxTotal(int connectionsMaxTotal) {
      this.connectionsMaxTotal = connectionsMaxTotal;
    }

    public void setConnectionsMaxPerRoute(int connectionsMaxPerRoute) {
      this.connectionsMaxPerRoute = connectionsMaxPerRoute;
    }
    
    public void setMaxDownloadSlots(String maxDownloadSlots) {
      this.maxDownloadSlots = maxDownloadSlots;
    }

    @SuppressWarnings("resource")
    public PoolingHttpClient build() {
        // create proxy
        HttpHost proxy = null;
        CredentialsProvider credentialsProvider = null;
        
        if (StringUtils.isNotBlank(proxyHost) && proxyPort > 0) {
            proxy = new HttpHost(proxyHost, proxyPort);
          
            if (StringUtils.isNotBlank(proxyUsername) && StringUtils.isNotBlank(proxyPassword)) {
                if (systemProperties) {
                    credentialsProvider = new SystemDefaultCredentialsProvider();
                } else {
                    credentialsProvider = new BasicCredentialsProvider();
                }
                credentialsProvider.setCredentials(
                        new AuthScope(proxyHost, proxyPort),
                        new UsernamePasswordCredentials(proxyUsername, proxyPassword));
            }
        }
      
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeout).build());
        connManager.setMaxTotal(connectionsMaxTotal);
        connManager.setDefaultMaxPerRoute(connectionsMaxPerRoute);
        
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .setProxy(proxy)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(connectionRequestTimeout)
                        .setConnectTimeout(connectionTimeout)
                        .setSocketTimeout(socketTimeout)
                        .setProxy(proxy)
                        .build());
                

        // use system properties
        if (systemProperties) {
            builder.useSystemProperties();
        }

        // build the client
        PoolingHttpClient wrapper = new PoolingHttpClient(builder.build(), connManager);
        wrapper.addGroupLimit(".*", 1); // default limit, can be overwritten
        
        if (StringUtils.isNotBlank(maxDownloadSlots)) {
            LOG.debug("Using download limits: {}", maxDownloadSlots);
  
            Pattern pattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
            Matcher matcher = pattern.matcher(maxDownloadSlots);
            while (matcher.find()) {
                String group = matcher.group(1);
                try {
                    final Integer maxResults = Integer.valueOf(matcher.group(2));
                    wrapper.addGroupLimit(group, maxResults);
                    LOG.trace("Added download slot '{}' with max results {}", group, maxResults);
                } catch (NumberFormatException error) {
                    LOG.debug("Rule '{}' is no valid regexp, ignored", group);
                }
            }
        }
        
        return wrapper;
    }
}
