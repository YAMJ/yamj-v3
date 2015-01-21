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
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.IMetadata;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.artwork.ArtworkTools.HashCodeType;
import org.yamj.core.service.artwork.fanart.ITvShowFanartScanner;
import org.yamj.core.service.artwork.poster.ITvShowPosterScanner;
import org.yamj.core.service.artwork.tv.ITvShowBannerScanner;
import org.yamj.core.service.artwork.tv.ITvShowVideoImageScanner;
import org.yamj.core.service.metadata.online.TheTVDbApiWrapper;
import org.yamj.core.service.metadata.online.TheTVDbScanner;

import com.omertron.thetvdbapi.model.Banner;
import com.omertron.thetvdbapi.model.BannerType;
import com.omertron.thetvdbapi.model.Banners;
import com.omertron.thetvdbapi.model.Episode;

@Service("tvdbArtworkScanner")
public class TheTVDbArtworkScanner implements
        ITvShowPosterScanner, ITvShowFanartScanner, ITvShowBannerScanner,
        ITvShowVideoImageScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TheTVDbArtworkScanner.class);

    @Autowired
    private ConfigService configService;
    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private TheTVDbApiWrapper tvdbApiWrapper;

    @Override
    public String getScannerName() {
        return TheTVDbScanner.SCANNER_ID;
    }

    @PostConstruct
    public void init() throws Exception {
        LOG.info("Initialize TheTVDb artwork scanner");

        // register this scanner
        artworkScannerService.registerTvShowPosterScanner(this);
        artworkScannerService.registerTvShowFanartScanner(this);
        artworkScannerService.registerTvShowBannerScanner(this);
        artworkScannerService.registerTvShowVideoImageScanner(this);
    }

    @Override
    public String getId(String title, int year, int season) {
        return tvdbApiWrapper.getSeriesId(title, year);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String title, int year, int season) {
        String id = this.getId(title, year, season);
        return this.getPosters(id, season);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(String id, int season) {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        List<ArtworkDetailDTO> dtos;
        if (season < 0) {
            // series
            LOG.debug("Scan posters for series {}", id);
            dtos = this.getSeriesPosters(id);
        } else {
            // season
            LOG.debug("Scan posters for season {}-{}", id);
            dtos = this.getSeasonPosters(id, season);
        }
        return dtos;
    }

    private List<ArtworkDetailDTO> getSeriesPosters(String id) {
        List<ArtworkDetailDTO> langDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> altLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> noLangDTOs = new ArrayList<>(5);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        final String defaultLanguage = configService.getProperty("thetvdb.language", "en");
        final String altLanguage = configService.getProperty("thetvdb.language.alternate", "");

        // find posters
        for (Banner banner : bannerList.getPosterList()) {
            if (banner.getBannerType2() == BannerType.POSTER) {
                if (banner.getLanguage().equalsIgnoreCase(defaultLanguage)) {
                    langDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isNotBlank(altLanguage) && altLanguage.equalsIgnoreCase(banner.getLanguage())) {
                    altLangDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isBlank(banner.getLanguage())) {
                    noLangDTOs.add(createArtworDetail(banner));
                }
            }
        }

        LOG.debug("Series {}: Found {} posters for language '{}'", id, langDTOs.size(), defaultLanguage);
        if (StringUtils.isNotBlank(altLanguage)) {
            LOG.debug("Series {}: Found {} posters for alternate language '{}'", id, altLangDTOs.size(), altLanguage);
        }
        LOG.debug("Series {}: Found {} posters without language", id, noLangDTOs.size());

        List<ArtworkDetailDTO> returnDTOs = null;
        if (!langDTOs.isEmpty()) {
            LOG.info("Series {}: Using posters with language '{}'", id, defaultLanguage);
            returnDTOs = langDTOs;
        } else if (!altLangDTOs.isEmpty()) {
            LOG.info("Series {}: No poster found for language '{}', using posters with language '{}'", id, defaultLanguage, altLanguage);
            returnDTOs = altLangDTOs;
        } else if (!noLangDTOs.isEmpty()) {
            LOG.info("Series {}: No poster found for language '{}', using posters with no language", id, defaultLanguage);
            returnDTOs = noLangDTOs;
        } else {
            String url = tvdbApiWrapper.getSeries(id).getPoster();
            if (StringUtils.isNotBlank(url)) {
                LOG.info("Series {}: Using default series poster", id);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART);
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    private List<ArtworkDetailDTO> getSeasonPosters(String id, int season) {
        List<ArtworkDetailDTO> langDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> altLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> noLangDTOs = new ArrayList<>(5);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        final String defaultLanguage = configService.getProperty("thetvdb.language", "en");
        final String altLanguage = configService.getProperty("thetvdb.language.alternate", "");

        // find posters
        for (Banner banner : bannerList.getSeasonList()) {
            if ((banner.getSeason() == season) && (banner.getBannerType2() == BannerType.SEASON)) {
                if (banner.getLanguage().equalsIgnoreCase(defaultLanguage)) {
                    langDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isNotBlank(altLanguage) && banner.getLanguage().equalsIgnoreCase(altLanguage)) {
                    altLangDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isBlank(banner.getLanguage())) {
                    noLangDTOs.add(createArtworDetail(banner));
                }
            }
        }

        LOG.debug("Season {}-{}: Found {} posters for language '{}'", id, season, langDTOs.size(), defaultLanguage);
        if (StringUtils.isNotBlank(altLanguage)) {
            LOG.debug("Season {}-{}: Found {} posters for alternate language '{}'", id, season, altLangDTOs.size(), altLanguage);
        }
        LOG.debug("Season {}-{}: Found {} posters without language", id, season, noLangDTOs.size());

        List<ArtworkDetailDTO> returnDTOs = null;
        if (!langDTOs.isEmpty()) {
            LOG.info("Season {}-{}: Using posters with language '{}'", id, season, defaultLanguage);
            returnDTOs = langDTOs;
        } else if (!altLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No poster found for language '{}', using posters with language '{}'", id, season, defaultLanguage, altLanguage);
            returnDTOs = altLangDTOs;
        } else if (!noLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No poster found for language '{}', using posters with no language", id, season, defaultLanguage);
            returnDTOs = noLangDTOs;
        } else if (CollectionUtils.isNotEmpty(bannerList.getPosterList())) {
            LOG.info("Season {}-{}: No poster found by language, using first series poster found", id, season);
            returnDTOs = new ArrayList<>(1);
            Banner banner = bannerList.getPosterList().get(0);
            returnDTOs.add(createArtworDetail(banner));
        } else {
            String url = tvdbApiWrapper.getSeries(id).getPoster();
            if (StringUtils.isNotBlank(url)) {
                LOG.info("Season {}-{}: Using default series poster", id, season);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART);
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(String title, int year, int season) {
        String id = this.getId(title, year, season);
        return this.getFanarts(id, season);
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(String id, int season) {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        List<ArtworkDetailDTO> dtos;
        if (season < 0) {
            // series
            LOG.debug("Scan fanarts for series {}", id);
            dtos = this.getSeriesFanarts(id);
        } else {
            // season
            LOG.debug("Scan fanarts for season {}", id, season);
            dtos = this.getSeasonFanarts(id, season);
        }
        return dtos;
    }

    private List<ArtworkDetailDTO> getSeriesFanarts(String id) {
        List<ArtworkDetailDTO> hdDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> sdDTOs = new ArrayList<>(5);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);

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

        LOG.debug("Series {}: Found {} HD fanart", id, hdDTOs.size());
        LOG.debug("Series {}: Found {} SD fanart", id, sdDTOs.size());

        List<ArtworkDetailDTO> returnDTOs = null;
        if (!hdDTOs.isEmpty()) {
            LOG.info("Series {}: Using HD fanart", id);
            returnDTOs = hdDTOs;
        } else if (!sdDTOs.isEmpty()) {
            LOG.info("Series {}: No HD fanart found; using SD fanart", id);
            returnDTOs = sdDTOs;
        } else {
            String url = tvdbApiWrapper.getSeries(id).getFanart();
            if (StringUtils.isNotBlank(url)) {
                LOG.info("Series {}: Using default series fanart", id);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART);
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    private List<ArtworkDetailDTO> getSeasonFanarts(String id, int season) {
        // NOTE: no explicit season fanart in TheTVDb

        List<ArtworkDetailDTO> hdDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> sdDTOs = new ArrayList<>(5);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);

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

        LOG.debug("Season {}-{}: Found {} HD fanart", id, season, hdDTOs.size());
        LOG.debug("Season {}-{}: Found {} SD fanart", id, season, sdDTOs.size());

        List<ArtworkDetailDTO> returnDTOs = null;
        if (!hdDTOs.isEmpty()) {
            LOG.debug("Season {}-{}: Using HD fanart", id, season);
            returnDTOs = hdDTOs;
        } else if (!sdDTOs.isEmpty()) {
            LOG.debug("Season {}-{}: No HD fanart found; using SD fanart", id, season);
            returnDTOs = sdDTOs;
        } else {
            String url = tvdbApiWrapper.getSeries(id).getFanart();
            if (StringUtils.isNotBlank(url)) {
                LOG.debug("Season {}-{}: Using default series fanart", id, season);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART);
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    private ArtworkDetailDTO createArtworDetail(Banner banner) {
        String url = banner.getUrl();
        ArtworkDetailDTO dto = new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART);

        // set language
        if (StringUtils.isNotBlank(banner.getLanguage())) {
            dto.setLanguage(banner.getLanguage());
        }

        // set rating
        if (banner.getRating() != null) {
            try {
                dto.setRating((int) (banner.getRating() * 10));
            } catch (Exception ignore) {
                // ignore a possible number violation
            }
        }

        return dto;
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(String title, int year, int season) {
        String id = this.getId(title, year, season);
        return this.getBanners(id, season);
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(String id, int season) {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        List<ArtworkDetailDTO> dtos;
        if (season < 0) {
            // series
            LOG.debug("Scan banners for series {}", id);
            dtos = this.getSeriesBanners(id);
        } else {
            // season
            LOG.debug("Scan banners for season {}-{}", id, season);
            dtos = this.getSeasonBanners(id, season);
        }
        return dtos;
    }

    private List<ArtworkDetailDTO> getSeriesBanners(String id) {
        List<ArtworkDetailDTO> langDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> altLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> noLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> blankDTOs = new ArrayList<>(5);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        final String defaultLanguage = configService.getProperty("thetvdb.language", "en");
        final String altLanguage = configService.getProperty("thetvdb.language.alternate", "");

        // find banners
        for (Banner banner : bannerList.getSeriesList()) {
            if (banner.getBannerType2() == BannerType.GRAPHICAL) {
                if (banner.getLanguage().equalsIgnoreCase(defaultLanguage)) {
                    langDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isNotBlank(altLanguage) && altLanguage.equalsIgnoreCase(banner.getLanguage())) {
                    altLangDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isBlank(banner.getLanguage())) {
                    noLangDTOs.add(createArtworDetail(banner));
                }
            } else if (banner.getBannerType2() == BannerType.BLANK) {
                blankDTOs.add(createArtworDetail(banner));
            }
        }

        LOG.debug("Series {}: Found {} banners for language '{}'", id, langDTOs.size(), defaultLanguage);
        if (StringUtils.isNotBlank(altLanguage)) {
            LOG.debug("Series {}: Found {} banners for alternate language '{}'", id, altLangDTOs.size(), altLanguage);
        }
        LOG.debug("Series {}: Found {} banners without language", id, noLangDTOs.size());
        LOG.debug("Series {}: Found {} blank banners", id, blankDTOs.size());

        List<ArtworkDetailDTO> returnDTOs = null;
        if (!langDTOs.isEmpty()) {
            LOG.info("Series {}: Using banners with language '{}'", id, defaultLanguage);
            returnDTOs = langDTOs;
        } else if (!altLangDTOs.isEmpty()) {
            LOG.info("Series {}: No banner found for language '{}', using banners with language '{}'", id, defaultLanguage, altLanguage);
            returnDTOs = altLangDTOs;
        } else if (!noLangDTOs.isEmpty()) {
            LOG.info("Series {}: No banner found for language '{}', using banners with no language", id, defaultLanguage);
            returnDTOs = noLangDTOs;
        } else if (!blankDTOs.isEmpty()) {
            LOG.info("Series {}: No banner found for language '{}', using blank banners", id, defaultLanguage);
            returnDTOs = blankDTOs;
        } else {
            String url = tvdbApiWrapper.getSeries(id).getBanner();
            if (StringUtils.isNotBlank(url)) {
                LOG.info("Series {}: Using default series banner", id);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART);
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    private List<ArtworkDetailDTO> getSeasonBanners(String id, int season) {
        List<ArtworkDetailDTO> seasonLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seasonAltLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seasonNoLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seriesLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seriesAltLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> seriesNoLangDTOs = new ArrayList<>(5);
        List<ArtworkDetailDTO> blankDTOs = new ArrayList<>(5);

        // get series artwork
        final Banners bannerList = tvdbApiWrapper.getBanners(id);
        final String defaultLanguage = configService.getProperty("thetvdb.language", "en");
        final String altLanguage = configService.getProperty("thetvdb.language.alternate", "");
        final boolean seasonBannerOnlySeries = configService.getBooleanProperty("thetvdb.season.banner.onlySeries", Boolean.FALSE);

        // series banners
        if (!seasonBannerOnlySeries) {
            // season banners
            for (Banner banner : bannerList.getSeasonList()) {
                if (banner.getBannerType2() == BannerType.SEASONWIDE) {
                    if (banner.getLanguage().equalsIgnoreCase(defaultLanguage)) {
                        seasonLangDTOs.add(createArtworDetail(banner));
                    } else if (StringUtils.isNotBlank(altLanguage) && altLanguage.equalsIgnoreCase(banner.getLanguage())) {
                        seasonAltLangDTOs.add(createArtworDetail(banner));
                    } else if (StringUtils.isBlank(banner.getLanguage())) {
                        seasonNoLangDTOs.add(createArtworDetail(banner));
                    }
                }
            }
        }
        for (Banner banner : bannerList.getSeasonList()) {
            if (banner.getBannerType2() == BannerType.GRAPHICAL) {
                if (banner.getLanguage().equalsIgnoreCase(defaultLanguage)) {
                    seriesLangDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isNotBlank(altLanguage) && altLanguage.equalsIgnoreCase(banner.getLanguage())) {
                    seriesAltLangDTOs.add(createArtworDetail(banner));
                } else if (StringUtils.isBlank(banner.getLanguage())) {
                    seriesNoLangDTOs.add(createArtworDetail(banner));
                }
            } else if (banner.getBannerType2() == BannerType.BLANK) {
                blankDTOs.add(createArtworDetail(banner));
            }
        }

        if (!seasonBannerOnlySeries) {
            LOG.debug("Season {}-{}: Found {} season banners for language '{}'", id, season, seasonLangDTOs.size(), defaultLanguage);
            if (StringUtils.isNotBlank(altLanguage)) {
                LOG.debug("Season {}-{}: Found {} season banners for alternate language '{}'", id, season, seasonAltLangDTOs.size(), altLanguage);
            }
            LOG.debug("Season {}-{}: Found {} season banners without language", id, season, seasonNoLangDTOs.size());
        }
        LOG.debug("Season {}-{}: Found {} series banners for language '{}'", id, season, seasonLangDTOs.size(), defaultLanguage);
        if (StringUtils.isNotBlank(altLanguage)) {
            LOG.debug("Season {}-{}: Found {} series banners for alternate language '{}'", id, season, seasonAltLangDTOs.size(), altLanguage);
        }
        LOG.debug("Season {}-{}: Found {} series banners without language", id, season, seasonNoLangDTOs.size());
        LOG.debug("season {}-{}: Found {} blank banners", id, season, blankDTOs.size());

        List<ArtworkDetailDTO> returnDTOs = null;
        if (configService.getBooleanProperty("thetvdb.season.banner.onlySeries", Boolean.FALSE) && !blankDTOs.isEmpty()) {
            LOG.info("Season {}-{}: Using blanks banners", id, season);
            returnDTOs = blankDTOs;
        } else if (!seasonLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: Using season banners with language '{}'", id, season, defaultLanguage);
            returnDTOs = seasonLangDTOs;
        } else if (!seasonAltLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No season banner found for language '{}', using season banners with language '{}'", id, season, defaultLanguage, altLanguage);
            returnDTOs = seasonAltLangDTOs;
        } else if (!seasonNoLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No season banner found for language '{}', using season banners with no language", id, season, defaultLanguage);
            returnDTOs = seasonNoLangDTOs;
        } else if (!seriesLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: Using series banners with language '{}'", id, season, defaultLanguage);
            returnDTOs = seriesLangDTOs;
        } else if (!seriesAltLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No series banner found for language '{}', using series banners with language '{}'", id, season, defaultLanguage, altLanguage);
            returnDTOs = seriesAltLangDTOs;
        } else if (!seriesNoLangDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No series banner found for language '{}', using series banners with no language", id, season, defaultLanguage);
            returnDTOs = seriesNoLangDTOs;
        } else if (!blankDTOs.isEmpty()) {
            LOG.info("Season {}-{}: No banner found for language '{}', using blank banners", id, season, defaultLanguage);
            returnDTOs = blankDTOs;
        } else {
            String url = tvdbApiWrapper.getSeries(id).getBanner();
            if (StringUtils.isNotBlank(url)) {
                LOG.info("Season {}-{}: Using default series banner", id, season);
                ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), url, HashCodeType.PART);
                returnDTOs = Collections.singletonList(detailDTO);
            }
        }

        return returnDTOs;
    }

    @Override
    public String getId(String title, int year, int season, int episode) {
        // TheTVDb only knows series IDs
        return getId(title, year, season);
    }

    @Override
    public List<ArtworkDetailDTO> getVideoImages(String title, int year, int season, int episode) {
        String id = getId(title, year, season, episode);
        return getVideoImages(id, season, episode);
    }

    @Override
    public List<ArtworkDetailDTO> getVideoImages(String id, int season, int episodeNumber) {
        List<Episode> episodeList = tvdbApiWrapper.getSeasonEpisodes(id, season);
        if (CollectionUtils.isEmpty(episodeList)) {
            return null;
        }

        // NOTE: just one video image per episode
        for (Episode episode : episodeList) {
            if (episode.getEpisodeNumber() == episodeNumber) {
                if (StringUtils.isNotBlank(episode.getFilename())) {
                    ArtworkDetailDTO detailDTO = new ArtworkDetailDTO(getScannerName(), episode.getFilename(), HashCodeType.PART);
                    return Collections.singletonList(detailDTO);
                }
                // episode found but no image
                break;
            }
        }

        // no video image found
        return null;
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(IMetadata metadata) {
        String id = getId(metadata);
        return getPosters(id, metadata.getSeasonNumber());
    }

    @Override
    public List<ArtworkDetailDTO> getFanarts(IMetadata metadata) {
        String id = getId(metadata);
        return getFanarts(id, metadata.getSeasonNumber());
    }

    @Override
    public List<ArtworkDetailDTO> getBanners(IMetadata metadata) {
        String id = getId(metadata);
        return getBanners(id, metadata.getSeasonNumber());
    }

    @Override
    public List<ArtworkDetailDTO> getVideoImages(IMetadata metadata) {
        String id = getId(metadata);
        return getVideoImages(id, metadata.getSeasonNumber(), metadata.getEpisodeNumber());
    }

    @Override
    public String getId(IMetadata metadata) {
        // first get the series, cause TheTVDb has just one id
        // per series, not season based
        Series series;
        if (metadata instanceof Series) {
            series = (Series) metadata;
        } else if (metadata instanceof Season) {
            series = ((Season) metadata).getSeries();
        } else if (metadata instanceof VideoData) {
            series = ((VideoData) metadata).getSeason().getSeries();
        } else {
            // no valid object for scanning artwork from TheTVDb
            return null;
        }

        String id = series.getSourceDbId(TheTVDbScanner.SCANNER_ID);
        if (StringUtils.isBlank(id)) {
            id = tvdbApiWrapper.getSeriesId(series.getTitle(), series.getStartYear());
        }
        return id;
    }
}
