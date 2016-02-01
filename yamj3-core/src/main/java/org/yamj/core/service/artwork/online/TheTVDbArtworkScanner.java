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

import com.omertron.thetvdbapi.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigService;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.metadata.online.TheTVDbScanner;
import org.yamj.core.tools.CommonTools;
import org.yamj.core.web.apis.TheTVDbApiWrapper;

@Service("tvdbArtworkScanner")
public class TheTVDbArtworkScanner implements ITvShowPosterScanner,
        ITvShowFanartScanner, ITvShowBannerScanner, ITvShowVideoImageScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbArtworkScanner.class);

    @Autowired
    private ConfigService configService;
    @Autowired
    private LocaleService localeService;
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private TheTVDbScanner tvdbScanner;
    @Autowired
    private TheTVDbApiWrapper tvdbApiWrapper;
    
    @Override
    public String getScannerName() {
        return TheTVDbScanner.SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize TheTVDb artwork scanner");

        // register this scanner
        artworkScannerService.registerArtworkScanner(this);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Season season) {
        String id = tvdbScanner.getSeriesId(season.getSeries());
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }

        LOG.debug("Scan posters for season {}-{}", id, season.getSeason());

        List<ArtworkDetailDTO> langDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> altLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> noLangDTOs = new ArrayList<>(5);

        String language = localeService.getLocaleForConfig("thetvdb").getLanguage();
        String altLanguage = configService.getProperty("thetvdb.language.alternate", language);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        if (bannerList != null) {
            // find posters
            for (Banner banner : bannerList.getSeasonList()) {
                if ((banner.getSeason() == season.getSeason()) && (banner.getBannerType2() == BannerType.SEASON)) {
                    if (StringUtils.isBlank(banner.getLanguage())) {
                        noLangDTOs.add(createArtworDetail(banner));
                    } else if (banner.getLanguage().equalsIgnoreCase(language)) {
                        langDTOs.add(createArtworDetail(banner));
                    } else if (banner.getLanguage().equalsIgnoreCase(altLanguage)) {
                        altLangDTOs.add(createArtworDetail(banner));
                    }
                }
            }
        }
        
        LOG.debug("Season {}-{}: Found {} posters for language '{}'", id, season, langDTOs.size(), altLanguage);
        if (!language.equalsIgnoreCase(altLanguage)) {
            LOG.debug("Season {}-{}: Found {} posters for alternate language '{}'", id, season, altLangDTOs.size(), altLanguage);
        }
        LOG.debug("Season {}-{}: Found {} posters without language", id, season, noLangDTOs.size());

        final List<ArtworkDetailDTO> returnDTOs;
        if (!langDTOs.isEmpty()) {
            LOG.info("Season {}-{}: Using posters with language '{}'", id, season, language);
            returnDTOs = langDTOs;
        } else if (!altLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No poster found for language '{}', using posters with language '{}'", id, season, language, altLanguage);
            returnDTOs = altLangDTOs;
        } else if (!noLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No poster found for language '{}', using posters with no language", id, season, language);
            returnDTOs = noLangDTOs;
        } else if (bannerList != null && CollectionUtils.isNotEmpty(bannerList.getPosterList())) {
            LOG.info("Season {}-{}: No poster found by language, using first series poster found", id, season);
            returnDTOs = new ArrayList<>(1);
            Banner banner = bannerList.getPosterList().get(0);
            returnDTOs.add(createArtworDetail(banner));
        } else {
            com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApiWrapper.getSeries(id, language);
            if (tvdbSeries == null || StringUtils.isBlank(tvdbSeries.getPoster())) {
                returnDTOs = Collections.emptyList();
            } else {
                LOG.info("Season {}-{}: Using default series poster", id, season);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), tvdbSeries.getPoster(), CommonTools.getPartialHashCode(tvdbSeries.getPoster()));
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Series series) {
        String id = tvdbScanner.getSeriesId(series);
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }
  
        LOG.debug("Scan posters for series {}", id);
      
        List<ArtworkDetailDTO> langDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> altLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> noLangDTOs = new ArrayList<>(5);

        String language = localeService.getLocaleForConfig("thetvdb").getLanguage();
        String altLanguage = configService.getProperty("thetvdb.language.alternate", language);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        if (bannerList != null) {
            // find posters
            for (Banner banner : bannerList.getPosterList()) {
                if (banner.getBannerType2() == BannerType.POSTER) {
                    if (StringUtils.isBlank(banner.getLanguage())) {
                        noLangDTOs.add(createArtworDetail(banner));
                    } else if (banner.getLanguage().equalsIgnoreCase(language)) {
                        langDTOs.add(createArtworDetail(banner));
                    } else if (banner.getLanguage().equalsIgnoreCase(altLanguage)) {
                        altLangDTOs.add(createArtworDetail(banner));
                    }
                }
            }
        }
        
        LOG.debug("Series {}: Found {} posters for language '{}'", id, langDTOs.size(), language);
        if (!language.equalsIgnoreCase(altLanguage)) {
            LOG.debug("Series {}: Found {} posters for alternate language '{}'", id, altLangDTOs.size(), altLanguage);
        }
        LOG.debug("Series {}: Found {} posters without language", id, noLangDTOs.size());

        final List<ArtworkDetailDTO> returnDTOs;
        if (!langDTOs.isEmpty()) {
            LOG.info("Series {}: Using posters with language '{}'", id, language);
            returnDTOs = langDTOs;
        } else if (!altLangDTOs.isEmpty()) {
            LOG.info("Series {}: No poster found for language '{}', using posters with language '{}'", id, language, altLanguage);
            returnDTOs = altLangDTOs;
        } else if (!noLangDTOs.isEmpty()) {
            LOG.info("Series {}: No poster found for language '{}', using posters with no language", id, language);
            returnDTOs = noLangDTOs;
        } else {
            com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApiWrapper.getSeries(id, language);
            if (tvdbSeries == null || StringUtils.isBlank(tvdbSeries.getPoster())) {
                returnDTOs = Collections.emptyList();
            } else {
                LOG.info("Series {}: Using default series poster", id);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), tvdbSeries.getPoster(), CommonTools.getPartialHashCode(tvdbSeries.getPoster()));
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    /**
     * NOTE: No explicit season fanart; so right now the same as series fanarts
     */
    @Override
    public List<ArtworkDetailDTO> getFanarts(Season season) {
        String id = tvdbScanner.getSeriesId(season.getSeries());
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }
  
        LOG.debug("Scan fanarts for season {}-{}", id, season.getSeason());
      
        List<ArtworkDetailDTO> hdDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> sdDTOs = new ArrayList<>(5);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        if (bannerList != null) {
            // find fanart
            for (Banner banner : bannerList.getFanartList()) {
                if (banner.getBannerType2() == BannerType.FANART_HD) {
                    // HD fanart
                    hdDTOs.add(createArtworDetail(banner));
                } else {
                    // SD fanart
                    sdDTOs.add(createArtworDetail(banner));
                }
            }
        }
        
        LOG.debug("Season {}-{}: Found {} HD fanart", id, season, hdDTOs.size());
        LOG.debug("Season {}-{}: Found {} SD fanart", id, season, sdDTOs.size());

        final List<ArtworkDetailDTO> returnDTOs;
        if (!hdDTOs.isEmpty()) {
            LOG.debug("Season {}-{}: Using HD fanart", id, season);
            returnDTOs = hdDTOs;
        } else if (!sdDTOs.isEmpty()) {
            LOG.debug("Season {}-{}: No HD fanart found; using SD fanart", id, season);
            returnDTOs = sdDTOs;
        } else {
            final String language = localeService.getLocaleForConfig("thetvdb").getLanguage();
            com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApiWrapper.getSeries(id, language);
            if (tvdbSeries == null || StringUtils.isBlank(tvdbSeries.getFanart())) {
                returnDTOs = Collections.emptyList();
            } else {
                LOG.debug("Season {}-{}: Using default series fanart", id, season);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), tvdbSeries.getFanart(), CommonTools.getPartialHashCode(tvdbSeries.getFanart()));
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(Series series) {
        String id = tvdbScanner.getSeriesId(series);
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }
  
        LOG.debug("Scan fanarts for series {}", id);

        List<ArtworkDetailDTO> hdDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> sdDTOs = new ArrayList<>(5);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        if (bannerList != null) {
            // find fanart
            for (Banner banner : bannerList.getFanartList()) {
                if (banner.getBannerType2() == BannerType.FANART_HD) {
                    // HD fanart
                    hdDTOs.add(createArtworDetail(banner));
                } else {
                    // SD fanart
                    sdDTOs.add(createArtworDetail(banner));
                }
            }
        }
        
        LOG.debug("Series {}: Found {} HD fanart", id, hdDTOs.size());
        LOG.debug("Series {}: Found {} SD fanart", id, sdDTOs.size());

        final List<ArtworkDetailDTO> returnDTOs;
        if (!hdDTOs.isEmpty()) {
            LOG.info("Series {}: Using HD fanart", id);
            returnDTOs = hdDTOs;
        } else if (!sdDTOs.isEmpty()) {
            LOG.info("Series {}: No HD fanart found; using SD fanart", id);
            returnDTOs = sdDTOs;
        } else {
            final String language = localeService.getLocaleForConfig("thetvdb").getLanguage();
            com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApiWrapper.getSeries(id, language);
            if (tvdbSeries == null || StringUtils.isBlank(tvdbSeries.getFanart())) {
                returnDTOs = Collections.emptyList();
            } else {
                LOG.info("Series {}: Using default series fanart", id);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), tvdbSeries.getFanart(), CommonTools.getPartialHashCode(tvdbSeries.getFanart()));
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(Season season) {
        String id = tvdbScanner.getSeriesId(season.getSeries());
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }
  
        LOG.debug("Scan banners for season {}-{}", id, season.getSeason());
      
        List<ArtworkDetailDTO> seasonLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seasonAltLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seasonNoLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seriesLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seriesAltLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seriesNoLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> blankDTOs = new ArrayList<>(5);

        String language = localeService.getLocaleForConfig("thetvdb").getLanguage();
        String altLanguage = configService.getProperty("thetvdb.language.alternate", language);
        final boolean seasonBannerOnlySeries = configService.getBooleanProperty("thetvdb.season.banner.onlySeries", Boolean.FALSE);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        if (bannerList != null) {
            // series banners
            if (!seasonBannerOnlySeries) {
                // season banners
                for (Banner banner : bannerList.getSeasonList()) {
                    if (banner.getBannerType2() == BannerType.SEASONWIDE) {
                        if (StringUtils.isBlank(banner.getLanguage())) {
                            seasonNoLangDTOs.add(createArtworDetail(banner));
                        } else if (banner.getLanguage().equalsIgnoreCase(language)) {
                            seasonLangDTOs.add(createArtworDetail(banner));
                        } else if (banner.getLanguage().equalsIgnoreCase(altLanguage)) {
                            seasonAltLangDTOs.add(createArtworDetail(banner));
                        }
                    }
                }
            }
            for (Banner banner : bannerList.getSeasonList()) {
                if (banner.getBannerType2() == BannerType.GRAPHICAL) {
                    if (StringUtils.isBlank(banner.getLanguage())) {
                        seriesNoLangDTOs.add(createArtworDetail(banner));
                    } else if (banner.getLanguage().equalsIgnoreCase(language)) {
                        seriesLangDTOs.add(createArtworDetail(banner));
                    } else if (banner.getLanguage().equalsIgnoreCase(altLanguage)) {
                        seriesAltLangDTOs.add(createArtworDetail(banner));
                    }
                } else if (banner.getBannerType2() == BannerType.BLANK) {
                    blankDTOs.add(createArtworDetail(banner));
                }
            }
        }
        
        if (!seasonBannerOnlySeries) {
            LOG.debug("Season {}-{}: Found {} season banners for language '{}'", id, season, seasonLangDTOs.size(), language);
            if (!language.equalsIgnoreCase(altLanguage)) {
                LOG.debug("Season {}-{}: Found {} season banners for alternate language '{}'", id, season, seasonAltLangDTOs.size(), altLanguage);
            }
            LOG.debug("Season {}-{}: Found {} season banners without language", id, season, seasonNoLangDTOs.size());
        }
        LOG.debug("Season {}-{}: Found {} series banners for language '{}'", id, season, seasonLangDTOs.size(), language);
        if (!language.equalsIgnoreCase(altLanguage)) {
            LOG.debug("Season {}-{}: Found {} series banners for alternate language '{}'", id, season, seasonAltLangDTOs.size(), altLanguage);
        }
        LOG.debug("Season {}-{}: Found {} series banners without language", id, season, seasonNoLangDTOs.size());
        LOG.debug("season {}-{}: Found {} blank banners", id, season, blankDTOs.size());

        final List<ArtworkDetailDTO> returnDTOs;
        if (configService.getBooleanProperty("thetvdb.season.banner.onlySeries", Boolean.FALSE) && !blankDTOs.isEmpty()) {
            LOG.info("Season {}-{}: Using blanks banners", id, season);
            returnDTOs = blankDTOs;
        } else if (!seasonLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: Using season banners with language '{}'", id, season, language);
            returnDTOs = seasonLangDTOs;
        } else if (!seasonAltLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No season banner found for language '{}', using season banners with language '{}'", id, season, language, altLanguage);
            returnDTOs = seasonAltLangDTOs;
        } else if (!seasonNoLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No season banner found for language '{}', using season banners with no language", id, season, language);
            returnDTOs = seasonNoLangDTOs;
        } else if (!seriesLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: Using series banners with language '{}'", id, season, language);
            returnDTOs = seriesLangDTOs;
        } else if (!seriesAltLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No series banner found for language '{}', using series banners with language '{}'", id, season, language, altLanguage);
            returnDTOs = seriesAltLangDTOs;
        } else if (!seriesNoLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No series banner found for language '{}', using series banners with no language", id, season, language);
            returnDTOs = seriesNoLangDTOs;
        } else if (!blankDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No banner found for language '{}', using blank banners", id, season, language);
            returnDTOs = blankDTOs;
        } else {
            com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApiWrapper.getSeries(id, language);
            if (tvdbSeries == null || StringUtils.isBlank(tvdbSeries.getBanner())) {
                returnDTOs = Collections.emptyList();
            } else {
                LOG.info("Season {}-{}: Using default series banner", id, season);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), tvdbSeries.getBanner(), CommonTools.getPartialHashCode(tvdbSeries.getBanner()));
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(Series series) {
        String id = tvdbScanner.getSeriesId(series);
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }
  
        LOG.debug("Scan banners for series {}", id);
      
        List<ArtworkDetailDTO> langDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> altLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> noLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> blankDTOs = new ArrayList<>(5);

        // get series artwork
        String language = localeService.getLocaleForConfig("thetvdb").getLanguage();
        String altLanguage = configService.getProperty("thetvdb.language.alternate", language);

        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        if (bannerList != null) {
            // find banners
            for (Banner banner : bannerList.getSeriesList()) {
                if (banner.getBannerType2() == BannerType.GRAPHICAL) {
                    if (StringUtils.isBlank(banner.getLanguage())) {
                        noLangDTOs.add(createArtworDetail(banner));
                    } else if (banner.getLanguage().equalsIgnoreCase(language)) {
                        langDTOs.add(createArtworDetail(banner));
                    } else if (banner.getLanguage().equalsIgnoreCase(altLanguage)) {
                        altLangDTOs.add(createArtworDetail(banner));
                    }
                } else if (banner.getBannerType2() == BannerType.BLANK) {
                    blankDTOs.add(createArtworDetail(banner));
                }
            }
        }
        
        LOG.debug("Series {}: Found {} banners for language '{}'", id, langDTOs.size(), language);
        if (!language.equalsIgnoreCase(altLanguage)) {
            LOG.debug("Series {}: Found {} banners for alternate language '{}'", id, altLangDTOs.size(), altLanguage);
        }
        LOG.debug("Series {}: Found {} banners without language", id, noLangDTOs.size());
        LOG.debug("Series {}: Found {} blank banners", id, blankDTOs.size());

        final List<ArtworkDetailDTO> returnDTOs;
        if (!langDTOs.isEmpty()) {
            LOG.info("Series {}: Using banners with language '{}'", id, language);
            returnDTOs = langDTOs;
        } else if (!altLangDTOs.isEmpty()) {
            LOG.info("Series {}: No banner found for language '{}', using banners with language '{}'", id, language, altLanguage);
            returnDTOs = altLangDTOs;
        } else if (!noLangDTOs.isEmpty()) {
            LOG.info("Series {}: No banner found for language '{}', using banners with no language", id, language);
            returnDTOs = noLangDTOs;
        } else if (!blankDTOs.isEmpty()) {
            LOG.info("Series {}: No banner found for language '{}', using blank banners", id, language);
            returnDTOs = blankDTOs;
        } else {
            com.omertron.thetvdbapi.model.Series tvdbSeries = tvdbApiWrapper.getSeries(id, language);
            if (tvdbSeries == null || StringUtils.isBlank(tvdbSeries.getBanner())) {
                returnDTOs = Collections.emptyList();
            } else {
                LOG.info("Series {}: Using default series banner", id);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), tvdbSeries.getBanner(), CommonTools.getPartialHashCode(tvdbSeries.getBanner()));
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    private ArtworkDetailDTO createArtworDetail(Banner banner) {
        String url = banner.getUrl();
        ArtworkDetailDTO dto = new ArtworkDetailDTO(getScannerName(), url, CommonTools.getPartialHashCode(url));

        // set language
        if (StringUtils.isNotBlank(banner.getLanguage())) {
            dto.setLanguageCode(banner.getLanguage());
        }

        // set rating
        if (banner.getRating() != null) {
            try {
                dto.setRating((int) (banner.getRating() * 10));
            } catch (Exception ignore) { //NOSONAR
                // ignore a possible number violation
            }
        }

        return dto;
    }

    @Override
    public List<ArtworkDetailDTO> getVideoImages(VideoData videoData) {
        if (videoData.isMovie()) {
            return Collections.emptyList();
        }
        
        String id = tvdbScanner.getSeriesId(videoData.getSeason().getSeries());
        if (StringUtils.isBlank(id)) {
            return Collections.emptyList();
        }
        
        final String language = localeService.getLocaleForConfig("thetvdb").getLanguage();
        Episode episode = tvdbApiWrapper.getEpisode(id, videoData.getSeason().getSeason(), videoData.getEpisode(), language);
        if (episode != null && StringUtils.isNotBlank(episode.getFilename())) {
            ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), episode.getFilename(), CommonTools.getPartialHashCode(episode.getFilename()));
            return Collections.singletonList(detailDTO);
        }
        
        return Collections.emptyList();
    }
}
