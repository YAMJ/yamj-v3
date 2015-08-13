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

import com.omertron.imdbapi.ImdbApi;
import com.omertron.imdbapi.model.ImdbImage;
import com.omertron.imdbapi.model.ImdbPerson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.metadata.online.ImdbScanner;
import org.yamj.core.web.PoolingHttpClient;

@Service("imdbArtworkScanner")
public class ImdbArtworkScanner implements IMoviePosterScanner, IMovieFanartScanner, IPhotoScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ImdbArtworkScanner.class);

    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private ImdbScanner imdbScanner;
    @Autowired
    private ImdbApi imdbApi;
    @Autowired
    private Cache imdbArtworkCache;
    @Autowired
    private PoolingHttpClient httpClient;

    @Override
    public String getScannerName() {
        return ImdbScanner.SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize IMDb artwork scanner");

        // register this scanner
        artworkScannerService.registerArtworkScanner(this);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(VideoData videoData) {
        String imdbId = imdbScanner.getMovieId(videoData);
        return getArtworks(imdbId, ArtworkType.POSTER);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(VideoData videoData) {
        String imdbId = imdbScanner.getMovieId(videoData);
        return getArtworks(imdbId, ArtworkType.FANART);
    }

    @Override
    public List<ArtworkDetailDTO> getPhotos(Person person) {
        String imdbId = imdbScanner.getPersonId(person);
        if (StringUtils.isBlank(imdbId)) {
            return null;
        }
        
        ImdbPerson imdbPerson = imdbApi.getActorDetails(imdbId);
        if (imdbPerson == null || imdbPerson.getImage() == null) {
            return null;
        }
        
        ArtworkDetailDTO dto = new ArtworkDetailDTO(getScannerName(), imdbPerson.getImage().getUrl(), imdbId);
        return Collections.singletonList(dto);
    }

    private List<ArtworkDetailDTO> getArtworks(String imdbId, ArtworkType artworkType) {
        if (StringUtils.isBlank(imdbId)) {
            return null;
        }
        
        List<ImdbArtwork> artworks = this.getImdbArtwork(imdbId);
        if (CollectionUtils.isEmpty(artworks)) {
            return null;
        }

        List<ArtworkDetailDTO> dtos = new ArrayList<>();
        for (ImdbArtwork artwork : artworks) {
            if (artworkType == artwork.getArtworkType()) {
                dtos.add(new ArtworkDetailDTO(getScannerName(), artwork.getUrl(), artwork.getHashCode()));
            }
        }
        return dtos;
    }
    
    private List<ImdbArtwork> getImdbArtwork(String imdbId) {
        List<ImdbArtwork> artwork = imdbArtworkCache.get(imdbId, List.class);
        if (artwork == null) {
            List<ImdbImage> titlePhotos = imdbApi.getTitlePhotos(imdbId);
            if (CollectionUtils.isNotEmpty(titlePhotos)) {
                artwork = new ArrayList<>();
                for (ImdbImage image : titlePhotos) {
                    ImdbArtwork ia = buildImdbArtwork(image);
                    if (ia != null) artwork.add(ia);
                }
                Collections.sort(artwork);
                imdbArtworkCache.put(imdbId, artwork);
            }
        }
        return artwork;
    }

    private static ImdbArtwork buildImdbArtwork(ImdbImage image) {
        if (image.getImage() == null) return null;
        if (!"presskit".equalsIgnoreCase(image.getSource())) return null;
        if (StringUtils.isBlank(image.getImage().getUrl())) return null;
        if (StringUtils.startsWithIgnoreCase(image.getCaption(), "Still of")) return null;
        final int width = image.getImage().getWidth();
        final int height = image.getImage().getHeight();
        
        ArtworkType artworkType;
        if (width > height) {
            artworkType = ArtworkType.FANART;
            if (width > (2*height)) artworkType = ArtworkType.BANNER;
        } else if (height == width) {
            return null;
        } else  {
            artworkType = ArtworkType.POSTER;
            if (height > (2*width)) return null;
        }
        
        // build hash code from caption
        String hashCode = null;
        int beginIndex = StringUtils.indexOf(image.getLink(), "/rm");
        if (beginIndex != -1) {
            int endIndex = image.getLink().indexOf('/', beginIndex+1);
            if (endIndex != -1) {
                hashCode = image.getLink().substring(beginIndex+1, endIndex);
            }
        }
        
        final int size = (image.getImage().getWidth() * image.getImage().getHeight());
        return new ImdbArtwork(artworkType, image.getImage().getUrl(), hashCode, size);
    }
    
    private static class ImdbArtwork implements Comparable<ImdbArtwork>{
        private final ArtworkType artworkType;
        private final String url;
        private final String hashCode;
        private final int size;
        
        public ImdbArtwork(ArtworkType artworkType, String url, String hashCode, int size) {
            this.artworkType = artworkType;
            this.url = url;
            this.hashCode = hashCode;
            this.size = size;
        }

        public ArtworkType getArtworkType() {
            return artworkType;
        }

        public String getUrl() {
            return url;
        }

        public String getHashCode() {
            return hashCode;
        }

        @Override
        public int compareTo(ImdbArtwork obj) {
            if (size > obj.size) return -1;
            if (size < obj.size) return 1;
            return 0;
        }
    }
}
