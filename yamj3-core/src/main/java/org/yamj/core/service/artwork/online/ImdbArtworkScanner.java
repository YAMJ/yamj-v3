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

import static org.yamj.plugin.api.common.Constants.SOURCE_IMDB;

import com.omertron.imdbapi.model.ImdbImage;
import com.omertron.imdbapi.model.ImdbPerson;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.service.metadata.online.ImdbScanner;
import org.yamj.core.web.apis.ImdbApiWrapper;
import org.yamj.plugin.api.artwork.ArtworkDTO;

@Service("imdbArtworkScanner")
public class ImdbArtworkScanner implements IMovieArtworkScanner, IPersonArtworkScanner {

    @Autowired
    private ImdbScanner imdbScanner;
    @Autowired
    private ImdbApiWrapper imdbApiWrapper;

    @Override
    public String getScannerName() {
        return SOURCE_IMDB;
    }

    @Override
    public List<ArtworkDTO> getPosters(VideoData videoData) {
        String imdbId = imdbScanner.getMovieId(videoData);
        return getArtworks(imdbId, ArtworkType.POSTER);
    }

    @Override
    public List<ArtworkDTO> getFanarts(VideoData videoData) {
        String imdbId = imdbScanner.getMovieId(videoData);
        return getArtworks(imdbId, ArtworkType.FANART);
    }

    @Override
    public List<ArtworkDTO> getPhotos(Person person) {
        String imdbId = imdbScanner.getPersonId(person);
        if (StringUtils.isBlank(imdbId)) {
            return Collections.emptyList();
        }
        
        ImdbPerson imdbPerson = imdbApiWrapper.getPerson(imdbId, Locale.US, false);
        if (imdbPerson == null || imdbPerson.getImage() == null) {
            return Collections.emptyList();
        }
        
        final ArtworkDTO dto = new ArtworkDTO(getScannerName(), imdbPerson.getImage().getUrl(), imdbId);
        return Collections.singletonList(dto);
    }

    private List<ArtworkDTO> getArtworks(String imdbId, ArtworkType artworkType) {
        if (StringUtils.isBlank(imdbId)) {
            return Collections.emptyList();
        }

        List<ArtworkDTO> dtos = new ArrayList<>();
        for (ImdbArtwork artwork : this.getImdbArtwork(imdbId, artworkType)) {
            dtos.add(new ArtworkDTO(getScannerName(), artwork.getUrl(), artwork.getHashCode()));
        }
        return dtos;
    }
    
    private Set<ImdbArtwork> getImdbArtwork(String imdbId, ArtworkType artworkType) {
        // use TreeSet just for ordering according to size
        TreeSet<ImdbArtwork> result = new TreeSet<>();
        for (ImdbImage image : imdbApiWrapper.getTitlePhotos(imdbId, Locale.US)) {
            ImdbArtwork ia = buildImdbArtwork(image, artworkType);
            if (ia != null) {
                result.add(ia);
            }
        }
        return result;
    }

    private static ImdbArtwork buildImdbArtwork(ImdbImage image, ArtworkType artworkType) {
        if (image.getImage() == null ||
            StringUtils.isBlank(image.getImage().getUrl()) ||
            !"presskit".equalsIgnoreCase(image.getSource()) ||
            StringUtils.startsWithIgnoreCase(image.getCaption(), "Still of"))
        {
            return null;
        }
        
        final int width = image.getImage().getWidth();
        final int height = image.getImage().getHeight();
        
        ArtworkType imdbArtworkType;
        if (width > height) {
            imdbArtworkType = ArtworkType.FANART;
            if (width > (2*height)) {
                imdbArtworkType = ArtworkType.BANNER;
            }
        } else if (height == width) {
            return null;
        } else  {
            imdbArtworkType = ArtworkType.POSTER;
            if (height > (2*width)) {
                return null;
            }
        }
        
        if (imdbArtworkType != artworkType) {
            return null;
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
        
        return new ImdbArtwork(image.getImage().getUrl(), hashCode, image.getImage().getWidth() * image.getImage().getHeight());
    }
    
    private static class ImdbArtwork implements Comparable<ImdbArtwork>{
        private final String url;
        private final String hashCode;
        private final int size;
        
        public ImdbArtwork(String url, String hashCode, int size) {
            this.url = url;
            this.hashCode = hashCode;
            this.size = size;
        }

        public String getUrl() {
            return url;
        }

        public String getHashCode() {
            return hashCode;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(url)
                    .append(hashCode)
                    .append(size)
                    .toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof ImdbArtwork)) {
                return false;
            }
            final ImdbArtwork other = (ImdbArtwork) obj;
            return new EqualsBuilder()
                    .append(url, other.url)
                    .append(hashCode, other.hashCode)
                    .append(size, other.size)
                    .isEquals();
        }
        
        @Override
        public int compareTo(ImdbArtwork obj) {
            if (size > obj.size) {
                return -1;
            }
            if (size < obj.size) {
                return 1;
            }
            return 0;
        }
    }
}
