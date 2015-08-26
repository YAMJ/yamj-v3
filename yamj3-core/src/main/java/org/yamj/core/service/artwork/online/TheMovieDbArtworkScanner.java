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
import com.omertron.themoviedbapi.model.movie.MovieBasic;
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
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.*;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.metadata.online.TheMovieDbScanner;
import org.yamj.core.tools.CommonTools;
import org.yamj.core.tools.MetadataTools;

@Service("tmdbArtworkScanner")
public class TheMovieDbArtworkScanner implements
        IMoviePosterScanner, IMovieFanartScanner, IPhotoScanner,
        IBoxedSetPosterScanner, IBoxedSetFanartScanner,
        ITvShowPosterScanner, ITvShowFanartScanner, ITvShowVideoImageScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbArtworkScanner.class);
    private static final String DEFAULT_SIZE = "original";
    private static final String NO_LANGUAGE = StringUtils.EMPTY;
    private static final String LANGUAGE_EN = "en";
    
    @Autowired
    private ConfigService configService;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private TheMovieDbScanner tmdbScanner;
    @Autowired
    private TheMovieDbApi tmdbApi;
    @Autowired
    private Cache tmdbArtworkCache;

    private String getDefaultLanguage() {
        return localeService.getLocaleForConfig("themoviedb").getLanguage();
    }

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
        return getFilteredArtwork(tmdbId, getDefaultLanguage(), MetaDataType.MOVIE, ArtworkType.POSTER, DEFAULT_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(VideoData videoData) {
        String tmdbId = tmdbScanner.getMovieId(videoData);
        return getFilteredArtwork(tmdbId, getDefaultLanguage(), MetaDataType.MOVIE, ArtworkType.BACKDROP, DEFAULT_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Season season) {
        String tmdbId = tmdbScanner.getSeriesId(season.getSeries());
        return getFilteredArtwork(tmdbId, season.getSeason(), -1, getDefaultLanguage(), MetaDataType.SEASON, ArtworkType.POSTER, DEFAULT_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(Season season) {
        String tmdbId = tmdbScanner.getSeriesId(season.getSeries());
        return getFilteredArtwork(tmdbId, season.getSeason(), -1, getDefaultLanguage(), MetaDataType.SEASON, ArtworkType.BACKDROP, DEFAULT_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Series series) {
        String tmdbId = tmdbScanner.getSeriesId(series);
        return getFilteredArtwork(tmdbId, getDefaultLanguage(), MetaDataType.SERIES, ArtworkType.POSTER, DEFAULT_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(Series series) {
        String tmdbId = tmdbScanner.getSeriesId(series);
        return getFilteredArtwork(tmdbId, getDefaultLanguage(), MetaDataType.SERIES, ArtworkType.BACKDROP, DEFAULT_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getVideoImages(VideoData videoData) {
        String tmdbId = tmdbScanner.getSeriesId(videoData.getSeason().getSeries());
        return getFilteredArtwork(tmdbId, videoData.getSeason().getSeason(), videoData.getEpisode(), getDefaultLanguage(), MetaDataType.EPISODE, ArtworkType.STILL, DEFAULT_SIZE);
    }
    
    @Override
    public List<ArtworkDetailDTO> getPhotos(Person person) {
        String tmdbId = tmdbScanner.getPersonId(person);
        return getFilteredArtwork(tmdbId, NO_LANGUAGE, MetaDataType.PERSON, ArtworkType.PROFILE, DEFAULT_SIZE);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(BoxedSet boxedSet) {
        String tmdbId = boxedSet.getSourceDbId(getScannerName());
        String defaultLanguage = getDefaultLanguage();
        
        if (StringUtils.isNumeric(tmdbId)) {
            return getFilteredArtwork(tmdbId, defaultLanguage, MetaDataType.BOXSET, ArtworkType.POSTER, DEFAULT_SIZE);
        }
        
        MovieBasic movieBasic = findCollection(boxedSet, defaultLanguage);
        if (movieBasic != null) {
            boxedSet.setSourceDbId(getScannerName(), Integer.toString(movieBasic.getId()));
            // TODO rename collection?
            return this.getFilteredArtwork(movieBasic.getId(), defaultLanguage, MetaDataType.BOXSET, ArtworkType.POSTER, DEFAULT_SIZE);
        }
        
        return Collections.emptyList();
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(BoxedSet boxedSet) {
        String tmdbId = boxedSet.getSourceDbId(getScannerName());
        String defaultLanguage = getDefaultLanguage();
        
        if (StringUtils.isNumeric(tmdbId)) {
            return getFilteredArtwork(tmdbId, defaultLanguage, MetaDataType.BOXSET, ArtworkType.BACKDROP, DEFAULT_SIZE);
        }
        
        MovieBasic movieBasic = findCollection(boxedSet, defaultLanguage);
        if (movieBasic != null) {
            boxedSet.setSourceDbId(getScannerName(), Integer.toString(movieBasic.getId()));
            // TODO rename collection?
            return this.getFilteredArtwork(movieBasic.getId(), defaultLanguage, MetaDataType.BOXSET, ArtworkType.BACKDROP, DEFAULT_SIZE);
        }
        
        return Collections.emptyList();
    }

    public MovieBasic findCollection(BoxedSet boxedSet, String language) {
        try {
            ResultList<MovieBasic> resultList = tmdbApi.searchCollection(boxedSet.getName(), 0, language);
            if (resultList.isEmpty() && !StringUtils.equalsIgnoreCase(language, "en")) {
                resultList = tmdbApi.searchCollection(boxedSet.getName(), 0, "en");
            }

            for (MovieBasic movieBasic : resultList.getResults()) {
                // 1. check name
                String boxedSetName = MetadataTools.cleanIdentifier(boxedSet.getName());
                String collectionName = MetadataTools.cleanIdentifier(movieBasic.getTitle());
                if (StringUtils.equalsIgnoreCase(boxedSetName, collectionName)) {
                    // found matching collection
                    return movieBasic;
                }

                
                // 2. TODO find matching collection based on the collection members (not supported by TMDbApi until now)
            }
        } catch (MovieDbException ex) {
            LOG.error("Failed retrieving collection for boxed set: {}", boxedSet.getName());
            LOG.warn("TheMovieDb error", ex);
        }
        return null;
    }

    /**
     * Get a list of the artwork for a movie.
     *
     * This will get all the artwork for a specified language and the blank
     * languages as well
     *
     * @param tmdbId
     * @param language
     * @param metaDataType
     * @param artworkType
     * @param artworkSize
     * @return
     */
    private List<ArtworkDetailDTO> getFilteredArtwork(String tmdbId, String language, MetaDataType metaDataType, ArtworkType artworkType, String artworkSize) {
        return this.getFilteredArtwork(tmdbId, -1, -1, language, metaDataType, artworkType, artworkSize);
    }

    /**
     * Get a list of the artwork for a movie.
     *
     * This will get all the artwork for a specified language and the blank
     * languages as well
     * 
     * @param tmdbId
     * @param season
     * @param episode
     * @param language
     * @param metaDataType
     * @param artworkType
     * @param artworkSize
     * @return
     */
    private List<ArtworkDetailDTO> getFilteredArtwork(String tmdbId, int season, int episode, String language, MetaDataType metaDataType, ArtworkType artworkType, String artworkSize) {
        if (StringUtils.isNumeric(tmdbId)) {
            return this.getFilteredArtwork(Integer.parseInt(tmdbId), season, episode, language, metaDataType, artworkType, artworkSize);
        }
        return Collections.emptyList();
    }

    /**
     * Get a list of the artwork for a movie.
     *
     * This will get all the artwork for a specified language and the blank
     * languages as well
     *
     * @param tmdbId
     * @param season
     * @param episode
     * @param language
     * @param metaDataType
     * @param artworkType
     * @param artworkSize
     * @return
     */
    private List<ArtworkDetailDTO> getFilteredArtwork(int tmdbId, String language, MetaDataType metaDataType, ArtworkType artworkType, String artworkSize) {
        return this.getFilteredArtwork(tmdbId, -1, -1, language, metaDataType, artworkType, artworkSize);
    }
    
    /**
     * Get a list of the artwork for a movie.
     *
     * This will get all the artwork for a specified language and the blank
     * languages as well
     *
     * @param tmdbId
     * @param season
     * @param episode
     * @param language
     * @param metaDataType
     * @param artworkType
     * @param artworkSize
     * @return
     */
    private List<ArtworkDetailDTO> getFilteredArtwork(int tmdbId, int season, int episode, String language, MetaDataType metaDataType, ArtworkType artworkType, String artworkSize) {
        List<ArtworkDetailDTO> dtos = new ArrayList<>();
        try {
            ResultList<Artwork> results = getArtworksFromTMDb(tmdbId, season, episode, metaDataType);
            
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
        return dtos;
    }
    
    private void addArtworkDTO(List<ArtworkDetailDTO> dtos, Artwork artwork, ArtworkType artworkType, String artworkSize) throws MovieDbException {
        URL artworkURL = tmdbApi.createImageUrl(artwork.getFilePath(), artworkSize);
        if (artworkURL == null || artworkURL.toString().endsWith("null")) {
            LOG.warn("{} URL is invalid and will not be used: {}", artworkType, artworkURL);
        } else {
            String url = artworkURL.toString();
            dtos.add(new ArtworkDetailDTO(getScannerName(), url, CommonTools.getPartialHashCode(url)));
        }
    }

    private ResultList<Artwork> getArtworksFromTMDb(int tmdbId, int season, int episode, MetaDataType metaDataType) throws MovieDbException {
        ResultList<Artwork> results;
        if (MetaDataType.PERSON == metaDataType) {
            String cacheKey = "person###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                results = tmdbApi.getPersonImages(tmdbId);
            }
            tmdbArtworkCache.put(cacheKey, results);
        } else if (MetaDataType.BOXSET == metaDataType) {
            String cacheKey = "boxset###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                // use an empty language to get all artwork and then filter it
                results = tmdbApi.getCollectionImages(tmdbId, NO_LANGUAGE);
            }
            tmdbArtworkCache.put(cacheKey, results);
        } else if (MetaDataType.SERIES == metaDataType) {
            String cacheKey = "series###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                // use an empty language to get all artwork and then filter it
                results = tmdbApi.getTVImages(tmdbId, NO_LANGUAGE);
            }
            tmdbArtworkCache.put(cacheKey, results);
        } else if (MetaDataType.SEASON == metaDataType) {
            String cacheKey = "season###"+season+"###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                // use an empty language to get all artwork and then filter it
                results = tmdbApi.getSeasonImages(tmdbId, season, NO_LANGUAGE);
            }
            tmdbArtworkCache.put(cacheKey, results);
        } else if (MetaDataType.EPISODE == metaDataType) {
            String cacheKey = "episode###"+season+"###"+episode+"###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                // use an empty language to get all artwork and then filter it
                results = tmdbApi.getEpisodeImages(tmdbId, season, episode);
            }
            tmdbArtworkCache.put(cacheKey, results);
        } else {
            String cacheKey = "movie###"+tmdbId;
            results = tmdbArtworkCache.get(cacheKey, ResultList.class);
            if (results == null || results.isEmpty()) {
                // use an empty language to get all artwork and then filter it
                results = tmdbApi.getMovieImages(tmdbId, NO_LANGUAGE);
            }
            tmdbArtworkCache.put(cacheKey, results);
        }
        return results;
    }
}
