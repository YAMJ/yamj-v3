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
package org.yamj.core.service.artwork.common;

import com.omertron.fanarttvapi.FanartTvApi;
import com.omertron.fanarttvapi.FanartTvException;
import com.omertron.fanarttvapi.enumeration.FTArtworkType;
import com.omertron.fanarttvapi.model.FTArtwork;
import com.omertron.fanarttvapi.model.FTMovie;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.IMetadata;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.artwork.fanart.IMovieFanartScanner;
import org.yamj.core.service.artwork.poster.IMoviePosterScanner;
import org.yamj.core.service.metadata.online.TheMovieDbApiWrapper;

@Service("fanartTvArtworkScanner")
public class FanartTvScanner implements IMoviePosterScanner, IMovieFanartScanner {

    private static final Logger LOG = LoggerFactory.getLogger(FanartTvScanner.class);
    public static final String SCANNER_ID = "fanarttv";
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private FanartTvApi fanarttvApi;
    @Autowired
    private TheMovieDbApiWrapper tmdbApiWrapper;

    @PostConstruct
    public void init() {
        LOG.info("Initialize FanartTV artwork scanner");

        // register this scanner
        artworkScannerService.registerMoviePosterScanner(this);
        artworkScannerService.registerMovieFanartScanner(this);
    }

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @Override
    public String getId(String title, int year) {
        // Use TheMovieDB scanner to get the id
        return tmdbApiWrapper.getMovieDbId(title, year, false);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String title, int year) {
        String id = this.getId(title, year);
        return this.getPosters(id);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String id) {
        return getMovieArtworkType(id, FTArtworkType.MOVIEPOSTER);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(IMetadata metadata) {
        String id = getId(metadata);
        if (StringUtils.isNotBlank(id)) {
            return getPosters(id);
        }
        return null;
    }

    @Override
    public String getId(IMetadata metadata) {
        String id;
        if (metadata.isMovie()) {
            // Scan for a movie ID
            id = tmdbApiWrapper.getId(metadata);
        } else {
            // Scan for a TV ID
            id = null;
        }

        LOG.info("{} {} ID for {} ({}), ID: {}",
                StringUtils.isBlank(id) ? "Failed to get" : "Got",
                metadata.isMovie() ? "Movie" : "TV",
                metadata.getTitle(), metadata.getYear(),
                id);

        return id;
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(String title, int year) {
        String id = this.getId(title, year);
        return getMovieArtworkType(id, FTArtworkType.MOVIEBACKGROUND);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(String id) {
        return getMovieArtworkType(id, FTArtworkType.MOVIEBACKGROUND);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(IMetadata metadata) {
        String id = getId(metadata);
        if (StringUtils.isNotBlank(id)) {
            return getFanarts(id);
        }
        return null;
    }

    /**
     * Generic routine to get the artwork type from the FanartTV based on the
     * passed type
     *
     * @param id ID of the artwork to get
     * @param artworkType Type of the artwork to get
     * @return List of the appropriate artwork
     */
    private List<ArtworkDetailDTO> getMovieArtworkType(String id, FTArtworkType artworkType) {
        List<ArtworkDetailDTO> artworkList = new ArrayList<>();

        try {
            FTMovie ftm = fanarttvApi.getMovieArtwork(id);

            for (FTArtwork artwork : ftm.getArtwork(artworkType)) {
                ArtworkDetailDTO aDto = new ArtworkDetailDTO(SCANNER_ID, artwork.getUrl());
                aDto.setLanguage(artwork.getLanguage());
                artworkList.add(aDto);
            }
        } catch (FanartTvException ex) {
            LOG.warn("Failed to get {} artwork from FanartTV for ID '{}', error: {}", artworkType, id, ex.getMessage(), ex);
        }
        return artworkList;
    }
}
