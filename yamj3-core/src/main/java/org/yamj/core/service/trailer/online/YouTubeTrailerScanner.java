/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
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
package org.yamj.core.service.trailer.online;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.TrailerDTO;
import org.yamj.core.database.model.type.ContainerType;
import org.yamj.core.service.trailer.TrailerScannerService;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

@Service("youTubeTrailerScanner")
public class YouTubeTrailerScanner implements IMovieTrailerScanner {

    private static final Logger LOG = LoggerFactory.getLogger(YouTubeTrailerScanner.class);
    public static final String SCANNER_ID = "youtube";

    @Value("${APIKEY.youtube:null}")
    private String youtubeApiKey;

    @Autowired
    private TrailerScannerService trailerScannerService;
    @Autowired
    private ConfigService configService;
    
    private YouTube youtube;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }
    
    @PostConstruct
    public void init() {
        LOG.info("Initialize YouTube trailer scanner");
        
        if (youtubeApiKey == null) {
            LOG.warn("No YouTube api key provided");
            return;
        }
        
        // this object is used to make YouTube Data API requests
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(),
            new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {
                    // nothing to do
                }
            })
            .setApplicationName("youtube-yamj3-search")
            .build();
        
        // register this scanner
        trailerScannerService.registerTrailerScanner(this);
    }

    @Override
    public List<TrailerDTO> getTrailers(VideoData videoData) {
        try {
            StringBuilder query = new StringBuilder();
            query.append(videoData.getTitle());
            if (videoData.getPublicationYear() > 0) {
                query.append(" ").append(videoData.getPublicationYear());
            }
            query.append(" trailer");

            String additionalSearch = configService.getProperty("youtube.trailer.additionalSearch");
            if (StringUtils.isNotBlank(additionalSearch)) {
                query.append(" ").append(additionalSearch);
            }

            // define the API request for retrieving search results
            YouTube.Search.List search = youtube.search().list("id,snippet");
            search.setKey(youtubeApiKey);
            search.setQ(query.toString());
            search.setType("video");
            search.setMaxResults(configService.getLongProperty("youtube.trailer.maxResults", 5));
            
            if (configService.getBooleanProperty("youtube.trailer.hdwanted", Boolean.TRUE)) {
                search.setVideoDefinition("high");
            }
            
            String regionCode = configService.getProperty("youtube.trailer.regionCode");
            if (StringUtils.isNotBlank(regionCode)) {
                search.setRegionCode(regionCode);
            }

            String relevanceLanguage = configService.getProperty("youtube.trailer.relevanceLanguage");
            if (StringUtils.isNotBlank(relevanceLanguage)) {
                search.setRelevanceLanguage(relevanceLanguage);
            }
            
            SearchListResponse searchResponse = search.execute();
            if (CollectionUtils.isEmpty(searchResponse.getItems())) {
                LOG.trace("Found no trailers for movie '{}'", videoData.getTitle());
            } else {
                LOG.trace("Found {} trailers for movie '{}'", searchResponse.getItems().size(), videoData.getTitle());
                
                List<TrailerDTO> trailers = new ArrayList<>(searchResponse.getItems().size());
                for (SearchResult item : searchResponse.getItems()) {
                    ResourceId resourceId = item.getId();
                    if (resourceId.getKind().equals("youtube#video")) {
                        trailers.add(new TrailerDTO(SCANNER_ID, ContainerType.MP4,
                                        YouTubeDownloadParser.TRAILER_BASE_URL + resourceId.getVideoId(),
                                        item.getSnippet().getTitle(),
                                        resourceId.getVideoId()));
                    }
                }
                return trailers;
            }
        } catch (Exception e) {
            LOG.error("YouTube trailer scanner error: '" + videoData.getTitle() + "'", e);
        }
        return null;
    }
}