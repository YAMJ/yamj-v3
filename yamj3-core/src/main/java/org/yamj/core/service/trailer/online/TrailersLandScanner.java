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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package org.yamj.core.service.trailer.online;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.TrailerDTO;
import org.yamj.core.database.model.type.ContainerType;
import org.yamj.core.service.trailer.TrailerScannerService;

@Service("trailersLandScanner")
public class TrailersLandScanner implements IMovieTrailerScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TrailersLandScanner.class);
    private static final String SCANNER_ID = "trailersland";
    private static final String TL_BASE_URL = "http://www.trailersland.com/";
    private static final String TL_SEARCH_URL = "cerca?ricerca=";
    private static final String TL_MOVIE_URL = "film/";
    private static final String TL_TRAILER_URL = "trailer/";
    private static final String TL_TRAILER_FILE_URL = "wrapping/tls.php?";

    @Autowired
    private TrailerScannerService trailerScannerService;
    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private ConfigService configService;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }
    
    @PostConstruct
    public void init() {
        LOG.info("Initialize TrailersLand trailer scanner");
        
        // register this scanner
        trailerScannerService.registerTrailerScanner(this);
    }

    @Override
    public List<TrailerDTO> getTrailers(VideoData videoData) {
        String trailersLandId = getTrailersLandId(videoData);
        if (StringUtils.isBlank(trailersLandId)) {
            return Collections.emptyList();
        }

        return getTrailerDTOS(trailersLandId);
    }

    private String getTrailersLandId(VideoData videoData) {
        String trailersLandId = videoData.getSourceDbId(SCANNER_ID);
        
        if (StringUtils.isBlank(trailersLandId)) {
            trailersLandId = getTrailersLandId(videoData.getTitle());
        }

        if (StringUtils.isBlank(trailersLandId) && 
            StringUtils.isNotBlank(videoData.getTitleOriginal()) &&
            videoData.getTitle().equalsIgnoreCase(videoData.getTitleOriginal())) 
        {
            trailersLandId = getTrailersLandId(videoData.getTitleOriginal());
        }

        videoData.setSourceDbId(SCANNER_ID, trailersLandId);
        return trailersLandId;
    }

    private String getTrailersLandId(String title) {
        String searchUrl;
        try {
            searchUrl = TL_BASE_URL + TL_SEARCH_URL + URLEncoder.encode(title, "ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            LOG.error("Unsupported encoding, cannot build search URL", ex);
            return null;
        }

        LOG.debug("Searching for movie at URL {}", searchUrl);

        String xml;
        try {
            DigestedResponse response = httpClient.requestContent(searchUrl);
            if (ResponseTools.isNotOK(response)) {
                LOG.warn("Failed to search for movie '{}'; status={}", title, response.getStatusCode());
                return null;
            }
            xml = response.getContent();
        } catch (IOException ex) {
            LOG.error("Failed retrieving TrailersLand ID for movie '{}': {}", title, ex.getMessage());
            LOG.trace("TrailersLand error", ex);
            return null;
        }
       
        String trailersLandId = null;
        int indexRes = xml.indexOf("<span class=\"info\"");
        if (indexRes >= 0) {
            int indexMovieUrl = xml.indexOf(TL_BASE_URL + TL_MOVIE_URL, indexRes + 1);
            if (indexMovieUrl >= 0) {
                int endMovieUrl = xml.indexOf('"', indexMovieUrl + 1);
                if (endMovieUrl >= 0) {
                    trailersLandId = xml.substring(indexMovieUrl + TL_BASE_URL.length() + TL_MOVIE_URL.length(), endMovieUrl);
                    LOG.debug("Found Trailers Land ID '{}'", trailersLandId);
                }
            } else {
                LOG.warn("Got search result but no movie; layout may be changed");
            }
        } else {
            LOG.debug("No movie found with title '{}'", title);
        }
        return trailersLandId;

    }

    /**
     * Scrape the web page for trailer URLs
     */
    private List<TrailerDTO> getTrailerDTOS(String trailersLandId) {
        String xml;
        try {
            DigestedResponse response = httpClient.requestContent(TL_BASE_URL + TL_MOVIE_URL + trailersLandId);
            if (ResponseTools.isNotOK(response)) {
                LOG.warn("Failed to search for movie ID {}; status={}", trailersLandId, response.getStatusCode());
                return Collections.emptyList();
            }
            xml = response.getContent();
        } catch (IOException ex) {
            LOG.error("Failed retrieving movie details for ID {}: {}", trailersLandId, ex.getMessage());
            LOG.trace("TrailersLand error", ex);
            return Collections.emptyList();
        }

        String preferredLanguages = this.configService.getProperty("trailersland.preferredLanguages", "it,fr,en");
        String preferredTypes = this.configService.getProperty("trailersland.preferredTypes", "trailer,teaser");
                        
        int indexVideo = xml.indexOf("<div class=\"trailer_container\">");
        int indexEndVideo = xml.indexOf("<div id=\"sidebar\">", indexVideo + 1);

        List<TrailersLandTrailer> trailerList = new ArrayList<>();
        if (indexVideo >= 0 && indexVideo < indexEndVideo) {
            int nextIndex = xml.indexOf(TL_BASE_URL + TL_TRAILER_URL, indexVideo);
            while (nextIndex >= 0 && nextIndex < indexEndVideo) {
                int endIndex = xml.indexOf('"', nextIndex + 1);
                String trailerPageUrl = xml.substring(nextIndex, endIndex);

                TrailersLandTrailer tr = new TrailersLandTrailer(trailerPageUrl, nextIndex, preferredLanguages, preferredTypes);
                if (tr.isValidLanguage() && tr.isValidType()) {
                    if (trailerList.contains(tr)) {
                        LOG.trace("Duplicate trailer page ignored - URL {}", trailerPageUrl);
                    } else {
                        LOG.debug("Found trailer page - URL {}", trailerPageUrl);
                        trailerList.add(tr);
                    }
                } else {
                    LOG.trace("Discarding page - URL {}", trailerPageUrl);
                }

                nextIndex = xml.indexOf(TL_BASE_URL + TL_TRAILER_URL, endIndex + 1);
            }
        } else {
            LOG.warn("Video section not found; layout may be changed");
        }

        Collections.sort(trailerList);
        LOG.debug("Found {} trailers pages", trailerList.size());

        List<TrailerDTO> result = new ArrayList<>();
        for (TrailersLandTrailer trailer : trailerList) {
            try {
                DigestedResponse response = httpClient.requestContent(trailer.getPageUrl());
                if (ResponseTools.isOK(response)) {
                    xml = response.getContent();
                } else {
                    LOG.error("Failed retrieving trailer details for ID {}; status={}", trailersLandId);
                    xml = null;
                }
            } catch (IOException ex) {
                LOG.error("Failed retrieving trailer details for ID {}: {}", trailersLandId, ex.getMessage());
                LOG.trace("TrailersLand error", ex);
                xml = null;
            }

            if (StringUtils.isBlank(xml)) {
                continue;
            }
            
            @SuppressWarnings("null")
            int nextIndex = xml.indexOf(TL_BASE_URL + TL_TRAILER_FILE_URL);
            while (nextIndex >= 0) {
                int endIndex = xml.indexOf('"', nextIndex);
                String url = xml.substring(nextIndex, endIndex);

                if (trailer.candidateUrl(url)) {
                    result.add(new TrailerDTO(SCANNER_ID, trailer.getContainerType(), trailer.getUrl()));
                }

                nextIndex = xml.indexOf(TL_BASE_URL + TL_TRAILER_FILE_URL, endIndex + 1);
            }
        }

        LOG.info("Found {} trailers", result.size());
        return result;
    }

    public class TrailersLandTrailer implements Comparable<TrailersLandTrailer> {

        private final String pageUrl;
        private final int index;
        private final String preferredLanguages;
        private final String preferredTypes;
        private final String type;
        private final String language;
        private ContainerType containerType;
        private String resolution;
        private String url;
        
        public TrailersLandTrailer(String pageUrl, int index, String preferredLanguages, String preferredTypes) {
            this.pageUrl = pageUrl;
            this.index = index;
            this.preferredLanguages = preferredLanguages;
            this.preferredTypes = preferredTypes;

            int nameIndex = TL_BASE_URL.length() + TL_TRAILER_URL.length();

            // some typo are present...
            if (pageUrl.indexOf("teaser", nameIndex) >= 0 || pageUrl.indexOf("tesaer", nameIndex) >= 0) {
                this.type = "teaser";
            } else if (pageUrl.indexOf("trailer", nameIndex) >= 0) {
                this.type = "trailer";
            } else {
                this.type = StringUtils.EMPTY;
            }
            
            if (pageUrl.indexOf("sottotitolato", nameIndex) >= 0) {
                this.language = Locale.ITALIAN.getLanguage();
            } else if (pageUrl.indexOf("italiano", nameIndex) >= 0) {
                this.language = Locale.ITALIAN.getLanguage();
            } else if (pageUrl.indexOf("francese", nameIndex) >= 0) {
                this.language = Locale.FRENCH.getLanguage();
            } else {
                this.language = Locale.ENGLISH.getLanguage();
            }
        }

        public String getPageUrl() {
            return pageUrl;
        }

        public int getIndex() {
            return index;
        }
        
        public String getType() {
            return type;
        }

        public String getLanguage() {
            return language;
        }

        public ContainerType getContainerType() {
            return containerType;
        }

        public String getResolution() {
            return resolution;
        }

        public String getUrl() {
            return url;
        }

        private boolean isBetterResolution(String res) {
            if (resolution == null) {
                return true;
            }
            if (resolution.equals(RESOLUTION_1080P)) {
                return false;
            }
            if (resolution.equals(RESOLUTION_720P) && res.equals(RESOLUTION_1080P)) {
                return true;
            }
            return resolution.equals(RESOLUTION_SD) && (res.equals(RESOLUTION_1080P) || res.equals(RESOLUTION_720P));
        }

        public boolean candidateUrl(String url) {
            int startIndex = url.indexOf("url=");
            if (startIndex < 0) {
                return false;
            }
            
            final String fileUrl = url.substring(startIndex + 4);

            LOG.trace("Evaluating candidate URL {}", fileUrl);
            String ext = FilenameUtils.getExtension(fileUrl);
            for (ContainerType type : ContainerType.values()) {
                if (type.name().equalsIgnoreCase(ext)) {
                    this.containerType = type;
                    break;
                }
            }
            if (containerType == null) {
                return false;
            }

            String params = url.substring(0, startIndex - 1);

            String resolution;
            if (params.contains("sd_file")) {
                resolution = RESOLUTION_SD;
            } else if (params.contains("480")) {
                resolution = RESOLUTION_SD;
            } else if (params.contains("720")) {
                resolution = RESOLUTION_720P;
            } else if (params.contains("1080")) {
                resolution = RESOLUTION_1080P;
            } else {
                LOG.info("Cannot guess trailer resolution for params '{}'", params);
                return false;
            }

            LOG.trace("Resolution is {}", resolution);
            if (!this.isBetterResolution(resolution)) {
                LOG.trace("Discarding '{}' as it's not better than actual resolution", fileUrl);
                return false;
            }

            this.url = fileUrl;
            this.resolution = resolution;
            return true;
        }


        public boolean isValidLanguage() {
            return evaluateAgainstList(language, preferredLanguages) > 0;
        }

        public boolean isValidType() {
            return evaluateAgainstList(type, preferredTypes) > 0;
        }

        private int evaluateAgainstList(String what, String list) {
            if (list.indexOf(',') < 0) {
                return what.equalsIgnoreCase(list) ? 1 : -1;
            }

            StringTokenizer st = new StringTokenizer(list, ",");
            int w = 1;
            while (st.hasMoreTokens()) {
                if (what.equalsIgnoreCase(st.nextToken())) {
                    return w;
                }
                w++;
            }
            return -1;
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof TrailersLandTrailer) {
                final TrailersLandTrailer other = (TrailersLandTrailer) obj;
                return new EqualsBuilder()
                        .append(resolution, other.resolution)
                        .append(type, other.type)
                        .append(language, other.language)
                        .isEquals();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(resolution)
                    .append(type)
                    .append(language)
                    .toHashCode();
        }

        @Override
        public int compareTo(TrailersLandTrailer other) {
            int diff = evaluateAgainstList(other.language, preferredLanguages) - evaluateAgainstList(this.language, preferredLanguages);
            if (diff == 0) {
                diff = evaluateAgainstList(other.type, preferredTypes) - evaluateAgainstList(this.type, preferredTypes);
                if (diff == 0) {
                    diff = other.index - this.index;
                }
            }
            return diff;
        }
    }
}
