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
package org.yamj.core.web;

import com.moviejukebox.allocine.AllocineApi;
import com.omertron.fanarttvapi.FanartTvApi;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.thetvdbapi.TheTVDBApi;
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
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.yamj.api.common.http.WebBrowserUserAgentSelector;

@Configuration
public class WebConfiguration  {

    private static final Logger LOG = LoggerFactory.getLogger(WebConfiguration.class);

    @Value("${yamj3.http.systemProperties:false}")
    private boolean systemProperties;

    @Value("${yamj3.http.proxyHost:null}")
    private String proxyHost;
  
    @Value("${yamj3.http.proxyPort:0}")
    private int proxyPort;

    @Value("${yamj3.http.proxyUsername:null}")
    private String proxyUsername;

    @Value("${yamj3.http.proxyPassword:null}")
    private String proxyPassword;

    @Value("${yamj3.http.connectionTimeout:25000}")
    private int connectionTimeout;

    @Value("${yamj3.http.connectionRequestTimeout:15000}")
    private int connectionRequestTimeout;
    
    @Value("${yamj3.http.socketTimeout:90000}")
    private int socketTimeout;
    
    @Value("${yamj3.http.connections.maxTotal:20}")
    private int connectionsMaxTotal;
    
    @Value("${yamj3.http.connections.maxPerRoute:1}")
    private int connectionsMaxPerRoute;

    @Value("${yamj3.http.maxDownloadSlots:null}")
    private String maxDownloadSlots;

    @Value("${APIKEY.themoviedb}")
    private String theMovieDbApiKey;

    @Value("${APIKEY.tvdb}")
    private String theTVDBApiKey;

    @Value("${APIKEY.fanarttv.apiKey}")
    private String fanartTvApiKey;

    @Value("${APIKEY.fanarttv.clientKey}")
    private String fanartTvApiClientKey;

    @Value("${APIKEY.allocine.partnerKey}")
    private String allocineApiPartnerKey;

    @Value("${APIKEY.allocine.secretKey}")
    private String allocineApiSecretKey;

    @Scope
    @Bean(destroyMethod="close")
    @SuppressWarnings("resource")
    public PoolingHttpClient poolingHttpClient() {
        LOG.trace("Create new pooling http client");
        
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
        
        CacheConfig cacheConfig = CacheConfig.custom()
                        .setMaxCacheEntries(1000)
                        .setMaxObjectSize(8192)
                        .build();
        
        HttpClientBuilder builder = CachingHttpClientBuilder.create()
                .setCacheConfig(cacheConfig)
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
        wrapper.setUserAgentSelector(new WebBrowserUserAgentSelector());
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

    @Bean
    public TheMovieDbApi theMovieDbApi() throws Exception  {
        LOG.trace("Initialize TheMovieDbApi");
        return new TheMovieDbApi(theMovieDbApiKey, poolingHttpClient());
    }

    @Bean
    public TheTVDBApi theTvDbApi() {
        LOG.trace("Initialize TheTVDBApi");
        return new TheTVDBApi(theTVDBApiKey, poolingHttpClient());
    }

    @Bean
    public FanartTvApi fanartTvApi() throws Exception  {
        LOG.trace("Initialize FanartTvApi");
        return new FanartTvApi(fanartTvApiKey, fanartTvApiClientKey, poolingHttpClient());
    }

    @Bean
    public AllocineApi allocineApi() throws Exception {
        LOG.trace("Initialize AllocineApi");
        return new AllocineApi(allocineApiPartnerKey, allocineApiSecretKey, poolingHttpClient());
    }
}

