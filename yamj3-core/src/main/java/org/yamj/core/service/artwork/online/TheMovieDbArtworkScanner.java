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

import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.enumeration.ArtworkType;
import com.omertron.themoviedbapi.model.artwork.Artwork;
import com.omertron.themoviedbapi.results.ResultList;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.BoxedSet;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.artwork.ArtworkTools.HashCodeType;
import org.yamj.core.service.metadata.online.TheMovieDbScanner;

@Service("tmdbArtworkScanner")
public class TheMovieDbArtworkScanner implements
        IMoviePosterScanner, IMovieFanartScanner, IPhotoScanner,
        IBoxedSetPosterScanner, IBoxedSetFanartScanner {

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
    @Autowired
    private TheMovieDbScanner tmdbScanner;
    @Autowired
    private TheMovieDbApi tmdbApi;
    @Autowired
    private Cache tmdbArtworkCache;

    @Override
    public String getScannerName() {
        return TheMovieDbScanner.SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize TheMovieDb artwork scanner");

        // register this scanner
        artworkScannerService.registerArtworkScanner(this);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(VideoData videoData) {
        String tmdbId = tmdbScanner.getMovieId(videoData);
        String defaultLanguage = configService.getProperty("themoviedb.language", LANGUAGE_EN);
        return getFilteredArtwork(tmdbId, defaultLanguage, MetaDataType.MOVIE, ArtworkType.POSTER, DEFAULT_POSTER_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(VideoData videoData) {
        String tmdbId = tmdbScanner.getMovieId(videoData);
        String defaultLanguage = configService.getProperty("themoviedb.language", LANGUAGE_EN);
        return getFilteredArtwork(tmdbId, defaultLanguage, MetaDataType.MOVIE, ArtworkType.BACKDROP, DEFAULT_FANART_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getPhotos(Person person) {
        String tmdbId = tmdbScanner.getPersonId(person);
        return getFilteredArtwork(tmdbId, LANGUAGE_NONE, MetaDataType.PERSON, ArtworkType.PROFILE, DEFAULT_PHOTO_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(BoxedSet boxedSet) {
        String tmdbId = boxedSet.getSourceDbId(getScannerName());
        if (StringUtils.isNumeric(tmdbId)) {
            String defaultLanguage = configService.getProperty("themoviedb.language", LANGUAGE_EN);
            return getFilteredArtwork(tmdbId, defaultLanguage, MetaDataType.BOXSET, ArtworkType.POSTER, DEFAULT_POSTER_SIZE);
        }
        // TODO get boxed set by checking movie/series
        return Collections.emptyList();
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(BoxedSet boxedSet) {
        String tmdbId = boxedSet.getSourceDbId(getScannerName());
        if (StringUtils.isNumeric(tmdbId)) {
            String defaultLanguage = configService.getProperty("themoviedb.language", LANGUAGE_EN);
            return getFilteredArtwork(tmdbId, defaultLanguage, MetaDataType.BOXSET, ArtworkType.BACKDROP, DEFAULT_POSTER_SIZE);
        }
        // TODO get boxed set by checking movie/series
        return Collections.emptyList();
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
    private List<ArtworkDetailDTO> getFilteredArtwork(String id, String language, MetaDataType metaDataType, ArtworkType artworkType, String artworkSize) {
        List<ArtworkDetailDTO> dtos = new ArrayList<>();
        if (StringUtils.isNumeric(id)) {
            int tmdbId = Integer.parseInt(id);
            try {
                ResultList<Artwork> results = getArtworksFromTMDb(tmdbId, metaDataType);
                
                if (results == null || results.isEmpty()) {
                    LOG.debug("Got no {} artworks from TMDb for id {}", artworkType, tmdbId);
                } else {
                    List<Artwork> artworkList = results.getResults();
                    LOG.debug("Got {} {} artworks from TMDb for id {}", artworkList.size(), artworkType, tmdbId);
                    
                    for (Artwork artwork : artworkList) {
                        if (artwork.getArtworkType() == artworkType
                                && (StringUtils.isBlank(artwork.getLanguage())
                                || StringUtils.equalsIgnoreCase(artwork.getLanguage(), language))) 
                        {
                            this.addArtworkDTO(dtos, artwork, artworkType, artworkSize);
                        }
                    }
                    
                    if (dtos.isEmpty() && !StringUtils.equalsIgnoreCase(language, LANGUAGE_EN)) {
                        // retrieve by english language
                        for (Artwork artwork : artworkList) {
                            if (artwork.getArtworkType() == artworkType && StringUtils.equalsIgnoreCase(artwork.getLanguage(), LANGUAGE_EN)) {
                                this.addArtworkDTO(dtos, artwork, artworkType, artworkSize);
                            }
                        }
                    }
                    LOG.debug("Found {} {} artworks for TMDb id {} and language '{}'", dtos.size(), artworkType, tmdbId, language);
                }
            } catch (MovieDbException ex) {
                LOG.error("Failed retrieving {} artworks for movie id {}: {}", artworkType, tmdbId, ex.getMessage());
                LOG.warn("TheMovieDb error", ex);
            }
        }
        return dtos;
    }
    
    private void addArtworkDTO(List<ArtworkDetailDTO> dtos, Artwork artwork, ArtworkType artworkType, String artworkSize) throws MovieDbException {
        URL artworkURL = tmdbApi.createImageUrl(artwork.getFilePath(), artworkSize);
        if (artworkURL == null || artworkURL.toString().endsWith("null")) {
            LOG.warn("{} URL is invalid and will not be used: {}", artworkType, artworkURL);
        } else {
            String url = artworkURL.toString();
            dtos.add(new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART));
        }
    }
    
    private ResultList<Artwork> getArtworksFromTMDb(int tmdbId, MetaDataType metaDataType) throws MovieDbException {
        ResultList<Artwork> results;
        if (MetaDataType.PERSON == metaDataType) {
            String cacheKey = "person###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                results = tmdbApi.getPersonImages(tmdbId);
            }
            tmdbArtworkCache.putIfAbsent(cacheKey, results);
        } else if (MetaDataType.BOXSET == metaDataType) {
            String cacheKey = "boxset###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                // use an empty language to get all artwork and then filter it
                results = tmdbApi.getCollectionImages(tmdbId, LANGUAGE_NONE);
            }
            tmdbArtworkCache.putIfAbsent(cacheKey, results);
        } else {
            String cacheKey = "movie###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                // use an empty language to get all artwork and then filter it
                results = tmdbApi.getMovieImages(tmdbId, LANGUAGE_NONE);
            }
            tmdbArtworkCache.putIfAbsent(cacheKey, results);
        }
        return results;
    }
}
