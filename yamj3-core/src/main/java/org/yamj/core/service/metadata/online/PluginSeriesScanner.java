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
import org.yamj.core.service.various.IdentifierService;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.metadata.*;

public class PluginSeriesScanner implements ISeriesScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginSeriesScanner.class);
    private final SeriesScanner seriesScanner;
    private final LocaleService localeService;
    private final IdentifierService identifierService;

    public PluginSeriesScanner(SeriesScanner seriesScanner, LocaleService localeService, IdentifierService identifierService) {
        this.seriesScanner = seriesScanner;
        this.localeService = localeService;
        this.identifierService = identifierService;
    }
    
    public SeriesScanner getSeriesScanner() {
        return seriesScanner;
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
        String seriesId = seriesScanner.getSeriesId(series.getTitle(), series.getTitleOriginal(), series.getStartYear(), series.getIdMap(), throwTempError);
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
        
        final SeriesDTO seriesDTO = buildSeriesToScan(series); 
        final boolean scanned = seriesScanner.scanSeries(seriesDTO, throwTempError);
        if (!scanned) {
            LOG.error("Can't find {} informations for series '{}'", getScannerName(), series.getIdentifier());
            return ScanResult.NO_RESULT;
        }
        
        // set  IDs only if not set before   
        for (Entry<String,String> entry : seriesDTO.getIds().entrySet()) {
            if (getScannerName().equalsIgnoreCase(entry.getKey())) {
                series.setSourceDbId(entry.getKey(), entry.getValue());
            } else if (StringUtils.isBlank(series.getSourceDbId(entry.getKey()))) {
                series.setSourceDbId(entry.getKey(), entry.getValue());
            }
        }

        if (OverrideTools.checkOverwriteTitle(series, getScannerName())) {
            series.setTitle(seriesDTO.getTitle(), getScannerName());
        }

        if (OverrideTools.checkOverwriteOriginalTitle(series, getScannerName())) {
            series.setTitleOriginal(seriesDTO.getOriginalTitle(), getScannerName());
        }

        if (OverrideTools.checkOverwriteYear(series, getScannerName())) {
            series.setStartYear(seriesDTO.getStartYear(), getScannerName());
            series.setEndYear(seriesDTO.getEndYear(), getScannerName());
        }

        if (OverrideTools.checkOverwritePlot(series, getScannerName())) {
            series.setPlot(seriesDTO.getPlot(), getScannerName());
        }

        if (OverrideTools.checkOverwriteOutline(series, getScannerName())) {
            series.setOutline(seriesDTO.getOutline(), getScannerName());
        }

        if (OverrideTools.checkOverwriteGenres(series, getScannerName())) {
            series.setGenreNames(seriesDTO.getGenres(), getScannerName());
        }

        if (OverrideTools.checkOverwriteStudios(series, getScannerName())) {
            series.setStudioNames(seriesDTO.getStudios(), getScannerName());
        }

        if (seriesDTO.getCountries() != null && OverrideTools.checkOverwriteCountries(series, getScannerName())) {
            Set<String> countryCodes = new HashSet<>(seriesDTO.getCountries().size());
            for (String country : seriesDTO.getCountries()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(countryCode);
            }
            series.setCountryCodes(countryCodes, getScannerName());
        }

        for (Entry<String,String> certification : seriesDTO.getCertifications().entrySet()) {
            String countryCode = localeService.findCountryCode(certification.getKey());
            series.addCertificationInfo(countryCode, certification.getValue());
        }

        series.addRating(getScannerName(), seriesDTO.getRating());

        series.addAwardDTOS(seriesDTO.getAwards());
        
        scanSeasons(series, seriesDTO);
        
        return ScanResult.OK;
    }

    private void scanSeasons(Series series, SeriesDTO seriesDTO) {
        for (Season season  : series.getSeasons()) {
           final SeasonDTO seasonDTO = seriesDTO.getSeason(season.getSeason());
            
            if (!season.isTvSeasonDone(getScannerName())) {
                if (seasonDTO == null || seasonDTO.isNotValid()) {
                    // mark season as not found
                    season.removeOverrideSource(getScannerName());
                    season.removeSourceDbId(getScannerName());
                    season.setTvSeasonNotFound();
                } else {
                    
                    // set  IDs only if not set before   
                    for (Entry<String,String> entry : seasonDTO.getIds().entrySet()) {
                        if (getScannerName().equalsIgnoreCase(entry.getKey())) {
                            season.setSourceDbId(entry.getKey(), entry.getValue());
                        } else if (StringUtils.isBlank(season.getSourceDbId(entry.getKey()))) {
                            season.setSourceDbId(entry.getKey(), entry.getValue());
                        }
                    }

                    if (OverrideTools.checkOverwriteTitle(season, getScannerName())) {
                        season.setTitle(seasonDTO.getTitle(), getScannerName());
                    }
                    
                    if (OverrideTools.checkOverwriteOriginalTitle(season,  getScannerName())) {
                        season.setTitleOriginal(seasonDTO.getOriginalTitle(),  getScannerName());
                    }
                    
                    if (OverrideTools.checkOverwriteYear(season,  getScannerName())) {
                        season.setPublicationYear(seasonDTO.getYear(), getScannerName());
                    }
                    
                    if (OverrideTools.checkOverwritePlot(season,  getScannerName())) {
                        season.setPlot(seasonDTO.getPlot(),  getScannerName());
                    }
                    
                    if (OverrideTools.checkOverwriteOutline(season,  getScannerName())) {
                        season.setOutline(seasonDTO.getOutline(),  getScannerName());
                    }
        
                    season.addRating(getScannerName(), seasonDTO.getRating());
        
                    // mark season as done
                    season.setTvSeasonDone();
                }
            }
            
            scanEpisodes(season, seasonDTO);
        }
    }
    
    private void scanEpisodes(Season season, SeasonDTO seasonDTO) {
        for (VideoData videoData : season.getVideoDatas()) {
            
            if (videoData.isTvEpisodeDone(getScannerName())) {
                // nothing to do if already done
                continue;
            }

            EpisodeDTO episodeDTO = (seasonDTO == null ? null : seasonDTO.getEpisode(videoData.getEpisode()));
            if (episodeDTO == null || episodeDTO.isNotValid()) {
                // mark episode as not found
                videoData.removeOverrideSource(getScannerName());
                videoData.removeSourceDbId(getScannerName());
                videoData.setTvEpisodeNotFound();
                continue;
            }
                
            // set  IDs only if not set before   
            for (Entry<String,String> entry : episodeDTO.getIds().entrySet()) {
                if (getScannerName().equalsIgnoreCase(entry.getKey())) {
                    videoData.setSourceDbId(entry.getKey(), entry.getValue());
                } else if (StringUtils.isBlank(videoData.getSourceDbId(entry.getKey()))) {
                    videoData.setSourceDbId(entry.getKey(), entry.getValue());
                }
            }

            if (OverrideTools.checkOverwriteTitle(videoData, getScannerName())) {
                videoData.setTitle(episodeDTO.getTitle(), getScannerName());
            }

            if (OverrideTools.checkOverwriteOriginalTitle(videoData, getScannerName())) {
                videoData.setTitleOriginal(episodeDTO.getOriginalTitle(), getScannerName());
            }

            if (OverrideTools.checkOverwritePlot(videoData, getScannerName())) {
                videoData.setPlot(episodeDTO.getPlot(), getScannerName());
            }

            if (OverrideTools.checkOverwriteOutline(videoData, getScannerName())) {
                videoData.setOutline(episodeDTO.getOutline(), getScannerName());
            }

            if (OverrideTools.checkOverwriteTagline(videoData, getScannerName())) {
                videoData.setTagline(episodeDTO.getTagline(), getScannerName());
            }

            if (OverrideTools.checkOverwriteQuote(videoData, getScannerName())) {
                videoData.setQuote(episodeDTO.getQuote(), getScannerName());
            }

            if (OverrideTools.checkOverwriteReleaseDate(videoData, getScannerName())) {
                String releaseCountryCode = localeService.findCountryCode(episodeDTO.getReleaseCountry());
                videoData.setRelease(releaseCountryCode, episodeDTO.getReleaseDate(), getScannerName());
            }

            if (episodeDTO.getCredits() != null) {
                for (CreditDTO credit : episodeDTO.getCredits()) {
                    videoData.addCreditDTO(identifierService.createCredit(credit));
                }
            }
            
            videoData.addRating(getScannerName(), episodeDTO.getRating());

            // mark episode as done
            videoData.setTvEpisodeDone();
        }
    }
    
    private SeriesDTO buildSeriesToScan(Series series) { 
        final SeriesDTO seriesDTO = new SeriesDTO(series.getIdMap()).setTitle(series.getTitle());

        for (Season season : series.getSeasons()) {
            // create season object
            SeasonDTO seasonDTO = new SeasonDTO(season.getIdMap(), season.getSeason());
            seasonDTO.setScanNeeded(!season.isTvSeasonDone(getScannerName()));
            seriesDTO.addSeason(seasonDTO);
            seasonDTO.setSeries(seriesDTO);
            
            for (VideoData videoData : season.getVideoDatas()) {
                if (videoData.isTvEpisodeDone(getScannerName())) {
                    // nothing to do if already done
                    continue;
                }
                
                EpisodeDTO episodeDTO = new EpisodeDTO(videoData.getIdMap(), videoData.getEpisode());
                seasonDTO.addEpisode(episodeDTO);
                episodeDTO.setSeason(seasonDTO);
            }
        }
        
        return seriesDTO;
    }

    @Override
    public boolean scanNFO(String nfoContent, IdMap idMap) {
        try {
            return seriesScanner.scanNFO(nfoContent, idMap);
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
            return false;
        }
    }
}
