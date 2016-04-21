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
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.api.common.tools.ResponseTools;
import org.yamj.core.database.model.VideoData;
import org.yamj.plugin.api.artwork.ArtworkDTO;
import org.yamj.plugin.api.web.HTMLTools;

@Service("yahooPosterScanner")
public class YahooPosterScanner implements IMovieArtworkScanner {

    private static final Logger LOG = LoggerFactory.getLogger(YahooPosterScanner.class);

    @Autowired
    private PoolingHttpClient httpClient;

    @Override
    public String getScannerName() {
        return "yahoo";
    }

    @Override
    public List<ArtworkDTO> getPosters(VideoData videoData) {
        List<ArtworkDTO> dtos = new ArrayList<>(1);

        try {
            StringBuilder sb = new StringBuilder("http://fr.images.search.yahoo.com/search/images?p=");
            sb.append(HTMLTools.encodeUrl(videoData.getTitle()));
            sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

            DigestedResponse response = httpClient.requestContent(sb.toString());
            if (ResponseTools.isOK(response)) {
                // TODO scan more posters at once
                int beginIndex = response.getContent().indexOf("imgurl=");
                if (beginIndex > 0) {
                    int endIndex = response.getContent().indexOf("rurl=", beginIndex);
                    if (endIndex > 0) {
                        String url = HTMLTools.decodeUrl(response.getContent().substring(beginIndex + 7, endIndex - 1));
                        dtos.add(new ArtworkDTO(getScannerName(), url));
                    } else {
                        String url = HTMLTools.decodeUrl(response.getContent().substring(beginIndex + 7));
                        dtos.add(new ArtworkDTO(getScannerName(), url));
                    }
                }
            } else {
                LOG.warn("Requesting yahoo poster for '{}' failed with status {}", videoData.getTitle(), response.getStatusCode());
            }
        } catch (Exception ex) {
            LOG.error("Failed retrieving poster URL from yahoo images '{}': {}", videoData.getTitle(), ex.getMessage());
            LOG.trace("Yahoo poster scanner error", ex);
        }

        return dtos;
    }

    @Override
    public List<ArtworkDTO> getFanarts(VideoData videoData) {
        return Collections.emptyList();
    }
}
