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
package org.yamj.core.service.artwork.poster;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.tools.web.PoolingHttpClient;
import org.yamj.core.tools.web.ResponseTools;

@Service("yahooPosterScanner")
public class YahooPosterScanner extends AbstractMoviePosterScanner {

    private static final Logger LOG = LoggerFactory.getLogger(YahooPosterScanner.class);

    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private PoolingHttpClient httpClient;

    @Override
    public String getScannerName() {
        return "yahoo";
    }

    @PostConstruct
    public void init() throws Exception {
        LOG.info("Initialize Yahoo poster scanner");

        artworkScannerService.registerMoviePosterScanner(this);
    }

    @Override
    public String getId(String title, int year) {
        // Yahoo has no ID, so return the title
        return title;
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String title, int year) {
        List<ArtworkDetailDTO> dtos = new ArrayList<>();

        try {
            StringBuilder sb = new StringBuilder("http://fr.images.search.yahoo.com/search/images?p=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

            DigestedResponse response = httpClient.requestContent(sb.toString());
            if (ResponseTools.isOK(response)) {
                // TODO scan more posters at once
                int beginIndex = response.getContent().indexOf("imgurl=");
                if (beginIndex > 0) {
                    int endIndex = response.getContent().indexOf("rurl=", beginIndex);
                    if (endIndex > 0) {
                        String url = URLDecoder.decode(response.getContent().substring(beginIndex + 7, endIndex - 1), "UTF-8");
                        dtos.add(new ArtworkDetailDTO(getScannerName(), url));
                    } else {
                        String url = URLDecoder.decode(response.getContent().substring(beginIndex + 7), "UTF-8");
                        dtos.add(new ArtworkDetailDTO(getScannerName(), url));
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed retrieving poster URL from yahoo images '{}': {}", title, ex.getMessage());
            LOG.trace("Scanner error", ex);
        }

        return dtos;
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String id) {
        return getPosters(id, -1);
    }
}
