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
package org.yamj.core.service.artwork.poster;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.tools.web.PoolingHttpClient;

@Service("yahooPosterScanner")
public class YahooPosterScanner extends AbstractMoviePosterScanner
        implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(YahooPosterScanner.class);

    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private PoolingHttpClient httpClient;

    @Override
    public String getScannerName() {
        return "yahoo";
    }

    @Override
    public void afterPropertiesSet() {
        artworkScannerService.registerMoviePosterScanner(this);
    }

    @Override
    public String getId(String title, int year) {
        // Yahoo has no ID, so return the title
        return title;
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String title, int year) {
        List<ArtworkDetailDTO> dtos = new ArrayList<ArtworkDetailDTO>();

        try {
            StringBuilder sb = new StringBuilder("http://fr.images.search.yahoo.com/search/images?p=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

            String xml = httpClient.requestContent(sb.toString());

            // TODO scan more posters at once
            int beginIndex = xml.indexOf("imgurl=");
            if (beginIndex > 0) {
                int endIndex = xml.indexOf("rurl=", beginIndex);
                if (endIndex > 0) {
                    String url = URLDecoder.decode(xml.substring(beginIndex + 7, endIndex - 1), "UTF-8");
                    dtos.add(new ArtworkDetailDTO(getScannerName(), url));
                } else {
                    String url = URLDecoder.decode(xml.substring(beginIndex + 7), "UTF-8");
                    dtos.add(new ArtworkDetailDTO(getScannerName(), url));
                }
            }
        } catch (IOException error) {
            LOG.error("Failed retreiving poster URL from yahoo images : {}", title, error);
        }

        return dtos;
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String id) {
        return getPosters(id, -1);
    }
}
