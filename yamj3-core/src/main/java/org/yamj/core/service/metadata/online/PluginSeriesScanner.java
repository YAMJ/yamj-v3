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
package org.yamj.core.service.metadata.online;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.metadata.Credit;
import org.yamj.plugin.api.metadata.Episode;
import org.yamj.plugin.api.metadata.SeriesScanner;

public class PluginSeriesScanner implements ISeriesScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginSeriesScanner.class);
    private final SeriesScanner seriesScanner;
    private final LocaleService localeService;
    
    public PluginSeriesScanner(SeriesScanner seriesScanner, LocaleService localeService) {
        this.seriesScanner = seriesScanner;
        this.localeService = localeService;
    }
    
    @Override
    public String getScannerName() {
        return seriesScanner.getScannerName();
    }
    
    @Override
    public String getSeriesId(Series series) {
        return getSeriesId(series, false);
    }

    private String getSeriesId(Series series, boolean throwTempError) {
        String seriesId = seriesScanner.getSeriesId(series.getTitle(), series.getTitleOriginal(), series.getStartYear(), series.getSourceDbIdMap(), throwTempError);
        series.setSourceDbId(getScannerName(), seriesId);
        return seriesId;
    }

    @Override
    public ScanResult scanSeries(Series series, boolean throwTempError) {
        String seriesId = getSeriesId(series, throwTempError);
        if (StringUtils.isBlank(seriesId)) {
            LOG.debug("{} id not available '{}'", getScannerName(), series.getIdentifier());
            return ScanResult.MISSING_ID;
        }
        
        final org.yamj.plugin.api.metadata.Series tvSeries = buildSeriesToScan(series); 
        final boolean scanned = seriesScanner.scanSeries(tvSeries, throwTempError);
        if (!scanned) {
            LOG.error("Can't find {} informations for series '{}'", getScannerName(), series.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        // set possible scanned series IDs only if not set before   
        for (Entry<String,String> entry : tvSeries.getIds().entrySet()) {
            if (getScannerName().equalsIgnoreCase(entry.getKey())) {
                series.setSourceDbId(entry.getKey(), entry.getValue());
            } else if (StringUtils.isBlank(series.getSourceDbId(entry.getKey()))) {
                series.setSourceDbId(entry.getKey(), entry.getValue());
            }
        }

        if (OverrideTools.checkOverwriteTitle(series, getScannerName())) {
            series.setTitle(tvSeries.getTitle(), getScannerName());
        }

        if (OverrideTools.checkOverwriteOriginalTitle(series, getScannerName())) {
            series.setTitleOriginal(tvSeries.getOriginalTitle(), getScannerName());
        }

        if (OverrideTools.checkOverwriteYear(series, getScannerName())) {
            series.setStartYear(tvSeries.getStartYear(), getScannerName());
            series.setEndYear(tvSeries.getEndYear(), getScannerName());
        }

        if (OverrideTools.checkOverwritePlot(series, getScannerName())) {
            series.setPlot(tvSeries.getPlot(), getScannerName());
        }

        if (OverrideTools.checkOverwriteOutline(series, getScannerName())) {
            series.setOutline(tvSeries.getOutline(), getScannerName());
        }

        series.addRating(getScannerName(), tvSeries.getRating());

        if (OverrideTools.checkOverwriteGenres(series, getScannerName())) {
            series.setGenreNames(tvSeries.getGenres(), getScannerName());
        }

        if (OverrideTools.checkOverwriteStudios(series, getScannerName())) {
            series.setStudioNames(tvSeries.getStudios(), getScannerName());
        }

        if (OverrideTools.checkOverwriteCountries(series, getScannerName())) {
            Set<String> countryCodes = new HashSet<>(tvSeries.getCountries().size());
            for (String country : tvSeries.getCountries()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(countryCode);
            }
            series.setCountryCodes(countryCodes, getScannerName());
        }

        scanSeasons(series, tvSeries);
        
        return ScanResult.OK;
    }

    private void scanSeasons(Series series, org.yamj.plugin.api.metadata.Series tvSeries) {
        for (Season season  : series.getSeasons()) {
           final org.yamj.plugin.api.metadata.Season tvSeason = tvSeries.getSeason(season.getSeason());
            
            if (!season.isTvSeasonDone(getScannerName())) {
                if (tvSeason == null || tvSeason.isNotValid()) {
                    // mark season as not found
                    season.removeOverrideSource(getScannerName());
                    season.removeSourceDbId(getScannerName());
                    season.setTvSeasonNotFound();
                } else {

                    // set possible scanned season IDs only if not set before   
                    for (Entry<String,String> entry : tvSeason.getIds().entrySet()) {
                        if (getScannerName().equalsIgnoreCase(entry.getKey())) {
                            season.setSourceDbId(entry.getKey(), entry.getValue());
                        } else if (StringUtils.isBlank(season.getSourceDbId(entry.getKey()))) {
                            season.setSourceDbId(entry.getKey(), entry.getValue());
                        }
                    }

                    if (OverrideTools.checkOverwriteTitle(season, getScannerName())) {
                        season.setTitle(tvSeason.getTitle(), getScannerName());
                    }
                    if (OverrideTools.checkOverwriteOriginalTitle(season,  getScannerName())) {
                        season.setTitleOriginal(tvSeason.getOriginalTitle(),  getScannerName());
                    }
                    if (OverrideTools.checkOverwriteYear(season,  getScannerName())) {
                        season.setPublicationYear(tvSeason.getYear(), getScannerName());
                    }
                    if (OverrideTools.checkOverwritePlot(season,  getScannerName())) {
                        season.setPlot(tvSeason.getPlot(),  getScannerName());
                    }
                    if (OverrideTools.checkOverwriteOutline(season,  getScannerName())) {
                        season.setOutline(tvSeason.getOutline(),  getScannerName());
                    }
        
                    season.addRating(getScannerName(), tvSeason.getRating());
        
                    // mark season as done
                    season.setTvSeasonDone();
                }
            }
            
            scanEpisodes(season, tvSeason);
        }
    }
    
    private void scanEpisodes(Season season, org.yamj.plugin.api.metadata.Season tvSeason) {
        for (VideoData videoData : season.getVideoDatas()) {
            
            if (videoData.isTvEpisodeDone(getScannerName())) {
                // nothing to do if already done
                continue;
            }

            Episode episode = (tvSeason == null ? null : tvSeason.getEpisode(videoData.getEpisode()));
            if (episode == null || episode.isNotValid()) {
                // mark episode as not found
                videoData.removeOverrideSource(getScannerName());
                videoData.removeSourceDbId(getScannerName());
                videoData.setTvEpisodeNotFound();
                continue;
            }
                
            // set possible scanned episode IDs only if not set before   
            for (Entry<String,String> entry : episode.getIds().entrySet()) {
                if (getScannerName().equalsIgnoreCase(entry.getKey())) {
                    videoData.setSourceDbId(entry.getKey(), entry.getValue());
                } else if (StringUtils.isBlank(videoData.getSourceDbId(entry.getKey()))) {
                    videoData.setSourceDbId(entry.getKey(), entry.getValue());
                }
            }

            if (OverrideTools.checkOverwriteTitle(videoData, getScannerName())) {
                videoData.setTitle(episode.getTitle(), getScannerName());
            }

            if (OverrideTools.checkOverwriteOriginalTitle(videoData, getScannerName())) {
                videoData.setTitle(episode.getOriginalTitle(), getScannerName());
            }

            if (OverrideTools.checkOverwritePlot(videoData, getScannerName())) {
                videoData.setPlot(episode.getPlot(), getScannerName());
            }

            if (OverrideTools.checkOverwriteOutline(videoData, getScannerName())) {
                videoData.setOutline(episode.getOutline(), getScannerName());
            }

            if (OverrideTools.checkOverwriteTagline(videoData, getScannerName())) {
                videoData.setTagline(episode.getTagline(), getScannerName());
            }

            if (OverrideTools.checkOverwriteQuote(videoData, getScannerName())) {
                videoData.setQuote(episode.getQuote(), getScannerName());
            }

            if (OverrideTools.checkOverwriteReleaseDate(videoData, getScannerName())) {
                String releaseCountryCode = localeService.findCountryCode(episode.getReleaseCountry());
                videoData.setRelease(releaseCountryCode, episode.getReleaseDate(), getScannerName());
            }

            videoData.addRating(getScannerName(), episode.getRating());

            for (Credit credit : episode.getCredits()) {
                videoData.addCreditDTO(new CreditDTO(getScannerName(), credit));
            }

            // mark episode as done
            videoData.setTvEpisodeDone();
        }
    }
    
    private org.yamj.plugin.api.metadata.Series buildSeriesToScan(Series series) { 
        final org.yamj.plugin.api.metadata.Series tvSeries =new org.yamj.plugin.api.metadata.Series().setIds(series.getSourceDbIdMap()); 

        for (Season season : series.getSeasons()) {
            // create season object
            org.yamj.plugin.api.metadata.Season tvSeason = new org.yamj.plugin.api.metadata.Season().setSeasonNumber(season.getSeason());
            tvSeason.setIds(season.getSourceDbIdMap());
            tvSeason.setScanNeeded(!season.isTvSeasonDone(getScannerName()));
            tvSeries.addSeason(tvSeason);
            
            for (VideoData videoData : season.getVideoDatas()) {
                if (videoData.isTvEpisodeDone(getScannerName())) {
                    // nothing to do if already done
                    continue;
                }
                
                Episode episode = new Episode().setEpisodeNumber(videoData.getEpisode());
                episode.setIds(videoData.getSourceDbIdMap());
                tvSeason.addEpisode(episode);
            }
        }
        
        return tvSeries;
    }

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNotBlank(dto.getId(getScannerName()))) {
            return true;
        }

        LOG.trace("Scanning NFO for {} ID", getScannerName());
        try {
            String id = seriesScanner.scanNFO(nfoContent);
            if (StringUtils.isNotBlank(id)) {
                LOG.debug("{} ID found in NFO: {}", getScannerName(), id);
                dto.addId(getScannerName(), id);
                return true;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No {} ID found in NFO", getScannerName());
        return false;
    }
}
