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

import static org.yamj.core.tools.Constants.LANGUAGE_EN;

import com.omertron.fanarttvapi.enumeration.FTArtworkType;
import com.omertron.fanarttvapi.model.FTArtwork;
import com.omertron.fanarttvapi.model.FTMovie;
import com.omertron.fanarttvapi.model.FTSeries;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.metadata.online.ImdbScanner;
import org.yamj.core.service.metadata.online.TheTVDbScanner;
import org.yamj.core.web.apis.FanartTvApiWrapper;

@Service("fanartTvScanner")
public class FanartTvScanner implements IMoviePosterScanner, IMovieFanartScanner,
    ITvShowFanartScanner, ITvShowPosterScanner, ITvShowBannerScanner 
{

    private static final Logger LOG = LoggerFactory.getLogger(FanartTvScanner.class);
    private static final String SCANNER_ID = "fanarttv";
    private static final String LANGUAGE_NONE = "00";
    
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private FanartTvApiWrapper fanartTvApiWrapper;
    @Autowired
    private ImdbScanner imdbScanner;
    @Autowired
    private TheTVDbScanner tvdbScanner;

    @PostConstruct
    public void init() {
        LOG.trace("Initialize FanartTV artwork scanner");

        // register this scanner
        artworkScannerService.registerArtworkScanner(this);
    }

    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(VideoData videoData) {
        String imdbId = imdbScanner.getMovieId(videoData);
        return getMovieArtworkType(imdbId, FTArtworkType.MOVIEPOSTER);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(VideoData videoData) {
        String imdbId = imdbScanner.getMovieId(videoData);
        return getMovieArtworkType(imdbId, FTArtworkType.MOVIEBACKGROUND);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Series series) {
        String tvdbId = tvdbScanner.getSeriesId(series);
        return getSeriesArtworkType(tvdbId, FTArtworkType.TVPOSTER, -1);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(Series series) {
        String tvdbId = tvdbScanner.getSeriesId(series);
        return getSeriesArtworkType(tvdbId, FTArtworkType.SHOWBACKGROUND, -1);
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(Series series) {
        String tvdbId = tvdbScanner.getSeriesId(series);
        return getSeriesArtworkType(tvdbId, FTArtworkType.TVBANNER, -1);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Season season) {
        String tvdbId = tvdbScanner.getSeriesId(season.getSeries());
        return getSeriesArtworkType(tvdbId, FTArtworkType.SEASONPOSTER, season.getSeason());
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(Season season) {
        String tvdbId = tvdbScanner.getSeriesId(season.getSeries());
        return getSeriesArtworkType(tvdbId, FTArtworkType.SHOWBACKGROUND, -1);
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(Season season) {
        String tvdbId = tvdbScanner.getSeriesId(season.getSeries());
        return getSeriesArtworkType(tvdbId, FTArtworkType.SEASONBANNER, season.getSeason());
    }

    /**
     * Generic routine to get the artwork type from the FanartTV based on the passed type.
     *
     * @param id the ID of the movie to get
     * @param artworkType type of the artwork to get
     * @return list of the appropriate artwork
     */
    private List<ArtworkDetailDTO> getMovieArtworkType(String id, FTArtworkType artworkType) {
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }
        
        FTMovie ftMovie = fanartTvApiWrapper.getFanartMovie(id);
        if (ftMovie == null) {
            return Collections.emptyList();
        }
        
        final String language = localeService.getLocaleForConfig("fanarttv").getLanguage();
        return getArtworkList(ftMovie.getArtwork(artworkType), language, -1);
    }

    /**
     * Generic routine to get the artwork type from the FanartTV based on the passed type.
     *
     * @param id the ID of the movie to get
     * @param artworkType type of the artwork to get
     * @return list of the appropriate artwork
     */
    private List<ArtworkDetailDTO> getSeriesArtworkType(String id, FTArtworkType artworkType, int seasonNumber) {
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }
        
        FTSeries ftSeries = fanartTvApiWrapper.getFanartSeries(id);
        if (ftSeries == null) {
            return Collections.emptyList();
        }

        final String language = localeService.getLocaleForConfig("fanarttv").getLanguage();
        return getArtworkList(ftSeries.getArtwork(artworkType), language, seasonNumber);
    }
    
    private static List<ArtworkDetailDTO> getArtworkList(List<FTArtwork> ftArtwork, String language, int seasonNumber) {
        List<ArtworkDetailDTO> artworkList = new ArrayList<>();
        final String season = Integer.toString(seasonNumber);
        
        // first try for default language
        for (FTArtwork artwork : ftArtwork) {
            if (!season.equals(artwork.getSeason())) {
                continue;
            }
            
            if (language.equalsIgnoreCase(artwork.getLanguage())) {
                ArtworkDetailDTO aDto = new ArtworkDetailDTO(SCANNER_ID, artwork.getUrl());
                aDto.setLanguageCode(artwork.getLanguage());
                artworkList.add(aDto);
            } 
        }

        // try with English if nothing found with default language
        if (artworkList.isEmpty() && !LANGUAGE_EN.equalsIgnoreCase(language)) {
            for (FTArtwork artwork : ftArtwork) {
                if (!season.equals(artwork.getSeason())) {
                    continue;
                }

                if (LANGUAGE_EN.equalsIgnoreCase(artwork.getLanguage())) {
                    ArtworkDetailDTO aDto = new ArtworkDetailDTO(SCANNER_ID, artwork.getUrl());
                    aDto.setLanguageCode(artwork.getLanguage());
                    artworkList.add(aDto);
                }
            }
        }

        // add artwork without language
        for (FTArtwork artwork : ftArtwork) {
            if (!season.equals(artwork.getSeason())) {
                continue;
            }

            if (LANGUAGE_NONE.equalsIgnoreCase(artwork.getLanguage())) {
                artworkList.add(new ArtworkDetailDTO(SCANNER_ID, artwork.getUrl()));
            }
        }

        return artworkList;
    }
}