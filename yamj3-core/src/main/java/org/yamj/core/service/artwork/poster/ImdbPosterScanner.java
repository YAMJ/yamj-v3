/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.core.service.artwork.poster;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.artwork.ArtworkTools.HashCodeType;
import org.yamj.core.service.metadata.online.imdb.ImdbScanner;
import org.yamj.core.service.metadata.online.imdb.ImdbSearchEngine;
import org.yamj.core.tools.web.PoolingHttpClient;

@Service("imdbPosterScanner")
public class ImdbPosterScanner extends AbstractMoviePosterScanner {

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
    public void init() throws Exception {
        LOG.info("Initialize IMDb poster scanner");
        
        // register this scanner
        artworkScannerService.registerMoviePosterScanner(this);
    }

    @Override
    public String getId(String title, int year) {
        return imdbSearchEngine.getImdbId(title, year, false);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String title, int year) {
        String id = this.getId(title, year);
        return this.getPosters(id);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String id) {
        List<ArtworkDetailDTO> dtos = new ArrayList<ArtworkDetailDTO>();
        if (StringUtils.isBlank(id)) {
            return dtos;
        }

        try {
            String xml = this.httpClient.requestContent("http://www.imdb.com/title/" + id);
            
            String metaImageString = "<meta property='og:image' content=\"";
            int beginIndex = xml.indexOf(metaImageString);
            if (beginIndex > 0) {
                beginIndex = beginIndex + metaImageString.length();
                int endIndex =  xml.indexOf("\"", beginIndex);
                if (endIndex > 0) {
                    String url = xml.substring(beginIndex, endIndex);
                    dtos.add(new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART));
                }
            }
        } catch (Exception error) {
            LOG.error("Failed retrieving poster URL from imdb images: {}", id);
            LOG.warn("Scanner error", error);
        }

        return dtos;
    }
}
