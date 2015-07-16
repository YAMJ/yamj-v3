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
package org.yamj.core.service.artwork.online;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.artwork.ArtworkTools.HashCodeType;
import org.yamj.core.service.metadata.online.ImdbScanner;
import org.yamj.core.service.metadata.online.ImdbSearchEngine;
import org.yamj.core.web.PoolingHttpClient;
import org.yamj.core.web.ResponseTools;

@Service("imdbPosterScanner")
public class ImdbPosterScanner implements IMoviePosterScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbPosterScanner.class);

    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private PoolingHttpClient httpClient;

    @Override
    public String getScannerName() {
        return ImdbScanner.SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize IMDb poster scanner");

        // register this scanner
        artworkScannerService.registerArtworkScanner(this);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(VideoData videoData) {
        List<ArtworkDetailDTO> dtos = new ArrayList<>();
      
        String imdbId = videoData.getSourceDbId(getScannerName());
        if (StringUtils.isBlank(imdbId)) {
            imdbId = imdbSearchEngine.getImdbId(videoData.getTitle(), videoData.getPublicationYear(), false, false);
            if (StringUtils.isBlank(imdbId)) {
                return dtos;
            }
        }

        try {
            DigestedResponse response = this.httpClient.requestContent("http://www.imdb.com/title/" + imdbId);
            if (ResponseTools.isOK(response)) {
                String metaImageString = "<meta property='og:image' content=\"";
                int beginIndex = response.getContent().indexOf(metaImageString);
                if (beginIndex > 0) {
                    beginIndex = beginIndex + metaImageString.length();
                    int endIndex =  response.getContent().indexOf("\"", beginIndex);
                    if (endIndex > 0) {
                        String url = response.getContent().substring(beginIndex, endIndex);
                        dtos.add(new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART));
                    }
                }
            } else {
                LOG.warn("Requesting IMDb poster for '{}' failed with status {}", imdbId, response.getStatusCode());
            }
        } catch (Exception ex) {
            LOG.error("Failed retrieving poster URL from IMDb images for id {}: {}", imdbId, ex.getMessage());
            LOG.trace("IMDb service error", ex);
        }

        return dtos;
    }
}
