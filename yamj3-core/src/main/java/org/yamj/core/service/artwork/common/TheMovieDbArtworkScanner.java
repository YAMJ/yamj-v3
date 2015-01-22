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

import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.Artwork;
import com.omertron.themoviedbapi.model.ArtworkType;
import com.omertron.themoviedbapi.results.TmdbResultsList;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.IMetadata;
import org.yamj.core.database.model.Person;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.artwork.ArtworkTools.HashCodeType;
import org.yamj.core.service.artwork.fanart.IMovieFanartScanner;
import org.yamj.core.service.artwork.photo.IPhotoScanner;
import org.yamj.core.service.artwork.poster.IMoviePosterScanner;
import org.yamj.core.service.metadata.online.TheMovieDbApiWrapper;
import org.yamj.core.service.metadata.online.TheMovieDbScanner;

@Service("tmdbArtworkScanner")
public class TheMovieDbArtworkScanner implements
        IMoviePosterScanner, IMovieFanartScanner, IPhotoScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbArtworkScanner.class);
    private static final String DEFAULT_POSTER_SIZE = "original";
    private static final String DEFAULT_FANART_SIZE = "original";
    private static final String DEFAULT_PHOTO_SIZE = "original";
    private static final String LANGUAGE_NONE = "";
    private static final String LANGUAGE_EN = "en";
    @Autowired
    private ConfigService configService;
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Deprecated
    @Autowired
    private TheMovieDbApi tmdbApi;
    @Deprecated
    @Autowired
    private TheMovieDbApiWrapper tmdbApiWrapper;
    @Autowired
    private TheMovieDbScanner tmdbScanner;

    @Override
    public String getScannerName() {
        return TheMovieDbScanner.SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize TheMovieDb artwork scanner");

        // register this scanner
        artworkScannerService.registerMoviePosterScanner(this);
        artworkScannerService.registerMovieFanartScanner(this);
        artworkScannerService.registerPhotoScanner(this);
    }

    @Override
    public String getId(String title, int year) {
        return tmdbApiWrapper.getMovieDbId(title, year, false);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String title, int year) {
        String id = this.getId(title, year);
        return this.getPosters(id);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(String title, int year) {
        String id = this.getId(title, year);
        return this.getFanarts(id);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String id) {
        String defaultLanguage = configService.getProperty("themoviedb.language", LANGUAGE_EN);
        return getFilteredArtwork(id, defaultLanguage, ArtworkType.POSTER, DEFAULT_POSTER_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(String id) {
        String defaultLanguage = configService.getProperty("themoviedb.language", LANGUAGE_EN);
        return getFilteredArtwork(id, defaultLanguage, ArtworkType.BACKDROP, DEFAULT_FANART_SIZE);
    }

    /**
     * Get a list of the artwork for a movie.
     *
     * This will get all the artwork for a specified language and the blank
     * languages as well
     *
     * @param id
     * @param language
     * @param artworkType
     * @param artworkSize
     * @return
     */
    private List<ArtworkDetailDTO> getFilteredArtwork(String id, String language, ArtworkType artworkType, String artworkSize) {
        List<ArtworkDetailDTO> dtos = new ArrayList<>();
        if (StringUtils.isNumeric(id)) {
            int tmdbId = Integer.parseInt(id);
            try {
                // Use an empty language to get all artwork and then filter it.
                TmdbResultsList<Artwork> results;
                if (artworkType == ArtworkType.PROFILE) {
                    results = tmdbApi.getPersonImages(tmdbId);
                } else {
                    results = tmdbApi.getMovieImages(tmdbId, LANGUAGE_NONE);
                }

                List<Artwork> artworkList = results.getResults();
                for (Artwork artwork : artworkList) {
                    if (artwork.getArtworkType() == artworkType
                            && (StringUtils.isBlank(artwork.getLanguage())
                            || StringUtils.equalsIgnoreCase(artwork.getLanguage(), language))) {
                        URL artworkURL = tmdbApi.createImageUrl(artwork.getFilePath(), artworkSize);
                        if (artworkURL == null || artworkURL.toString().endsWith("null")) {
                            LOG.warn("{} URL is invalid and will not be used: {}", artworkType, artworkURL);
                        } else {
                            String url = artworkURL.toString();
                            dtos.add(new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART));
                        }
                    }
                }
                LOG.debug("Found {} {} artworks for TMDB ID '{}' and language '{}'", dtos.size(), artworkType, tmdbId, language);
            } catch (MovieDbException error) {
                LOG.warn("Failed to get the {} URL for TMDb ID {}", artworkType, id, error);
            }
        }
        return dtos;
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
    public List<ArtworkDetailDTO> getFanarts(IMetadata metadata) {
        String id = getId(metadata);
        if (StringUtils.isNotBlank(id)) {
            return getFanarts(id);
        }
        return null;
    }

    @Override
    public String getId(IMetadata metadata) {
        return tmdbApiWrapper.getId(metadata);
    }

    @Override
    public List<ArtworkDetailDTO> getPhotos(Person person) {
        String tmdbId = tmdbScanner.getPersonId(person);
        if (StringUtils.isNumeric(tmdbId)) {
            return getFilteredArtwork(tmdbId, LANGUAGE_NONE, ArtworkType.PROFILE, DEFAULT_PHOTO_SIZE);
        }
        return Collections.emptyList();
    }
}
