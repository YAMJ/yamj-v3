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
package org.yamj.core.service.metadata.nfo;

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.*;
import org.yamj.core.service.staging.StagingService;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.OverrideTools;

@Service("nfoScannerService")
public class NfoScannerService {

    public static final String SCANNER_ID = "nfo";
    private static final Logger LOG = LoggerFactory.getLogger(NfoScannerService.class);

    @Autowired
    private StagingService stagingService;
    @Autowired
    private InfoReader infoReader;
    
    public void scanMovie(VideoData videoData) {
        // remove override source for NFO
        videoData.removeOverrideSource(SCANNER_ID);
        
        // get the stage files
        List<StageFile> stageFiles = this.stagingService.getValidNFOFiles(videoData);
        if (CollectionUtils.isEmpty(stageFiles)) {
            videoData.setSkippendScansNfo(null);
            if (videoData.isWatchedNfo()) {
                // the date where the NFO change for watch was detected
                videoData.setWatchedNfo(false,  new Date(System.currentTimeMillis()));
            }
            return;
        }

        LOG.info("Scanning NFO data for movie '{}'", videoData.getIdentifier());
        
        // create an info DTO for movie
        InfoDTO infoDTO = new InfoDTO(false);
        infoDTO.setIds(videoData.getSourceDbIdMap());
       
        // scan the NFOs
        this.scanNFOs(stageFiles, infoDTO);
        
        if (infoDTO.isTvShow()) {
            LOG.warn("NFO's determined TV show for movie: {}", videoData.getIdentifier());
            return;
        }
        
        if (infoDTO.isChanged()) {
            // set video IDs
            for (Entry<String,String> entry : infoDTO.getIds().entrySet()) {
                videoData.setSourceDbId(entry.getKey(), entry.getValue());
            }
            
            
            // add skipped scans to modified sources if they are not skipped before NFO reading
            if (!videoData.isAllScansSkipped()) {
                for (String skippedSourceDb : infoDTO.getSkippedScans()) {
                    if (!"all".equalsIgnoreCase(skippedSourceDb) && !videoData.isSkippedScan(skippedSourceDb)) {
                        videoData.addModifiedSource(skippedSourceDb);
                    }
                }
            }
            // reset skipped scans
            videoData.setSkippendScansNfo(infoDTO.getSkippedScans());
            
            // set top 250
            videoData.setTopRank(infoDTO.getTop250());
            // set rating
            videoData.addRating(SCANNER_ID, infoDTO.getRating());
            
            // set watched by NFO
            if (infoDTO.isWatched() != videoData.isWatchedNfo()) {
                videoData.setWatchedNfo(infoDTO.isWatched(), infoDTO.getWatchedDate());
            }
            
            // set sort title
            videoData.setTitleSort(infoDTO.getTitleSort());
            
            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                videoData.setTitle(infoDTO.getTitle(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
                videoData.setTitleOriginal(infoDTO.getTitleOriginal(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
                videoData.setPublicationYear(infoDTO.getYear(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                videoData.setRelease(infoDTO.getReleaseDate(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                videoData.setPlot(infoDTO.getPlot(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                videoData.setOutline(infoDTO.getOutline(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteTagline(videoData, SCANNER_ID)) {
                videoData.setTagline(infoDTO.getTagline(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteQuote(videoData, SCANNER_ID)) {
                videoData.setQuote(infoDTO.getQuote(), SCANNER_ID);
            }
            
            if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
                videoData.setGenreNames(infoDTO.getGenres(), SCANNER_ID);
            }
            
            if (StringUtils.isNotBlank(infoDTO.getCompany())) {
                if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
                    Set<String> studioNames = Collections.singleton(infoDTO.getCompany());
                    videoData.setStudioNames(studioNames, SCANNER_ID);
                }
            }

            // add certifications
            videoData.setCertificationInfos(infoDTO.getCertificationInfos());

            // add boxed sets
            for (Entry<String, Integer> entry : infoDTO.getSetInfos().entrySet()) {
                videoData.addBoxedSetDTO(SCANNER_ID, entry.getKey(), entry.getValue());
            }

            // add credit DTOs for update in database
            videoData.addCreditDTOS(infoDTO.getCredits());
            
            // add poster URLs
            if (CollectionUtils.isNotEmpty(infoDTO.getPosterURLs())) {
                for (String posterURL : infoDTO.getPosterURLs()) {
                    videoData.addPosterDTO(SCANNER_ID, posterURL);
                }
            }

            // add fanart URLs
            if (CollectionUtils.isNotEmpty(infoDTO.getFanartURLs())) {
                for (String fanartURL : infoDTO.getFanartURLs()) {
                    videoData.addFanartDTO(SCANNER_ID, fanartURL);
                }
            }
        }
        
        LOG.debug("Scanned NFO data for movie '{}'", videoData.getIdentifier());
    }

    public void scanSeries(Series series) {
        // remove override source for NFO
        series.removeOverrideSource(SCANNER_ID);
        
        // get the stage files
        List<StageFile> stageFiles = this.stagingService.getValidNFOFiles(series);
        if (CollectionUtils.isEmpty(stageFiles)) {
            final Date actualDate = new Date(System.currentTimeMillis());
            series.setSkippendScansNfo(null);
            
            for (Season season : series.getSeasons()) {
                if (season.removeOverrideSource(SCANNER_ID)) {
                    season.setTvSeasonNotFound();
                }
                
                for (VideoData videoData : season.getVideoDatas()) {
                    if (videoData.removeOverrideSource(SCANNER_ID)) {
                        // reset status if NFO source has been removed
                        videoData.setTvEpisodeNotFound();
                    }
                    if (videoData.isWatchedNfo()) {
                        // the date where the NFO change for watch was detected
                        videoData.setWatchedNfo(false, actualDate);
                        
                        videoData.setTvEpisodeNotFound();
                    }
                }
            }

            return;
        }

        LOG.info("Scanning NFO data for series '{}'", series.getIdentifier());
        
        // create an info DTO for TV show
        InfoDTO infoDTO = new InfoDTO(true);
        infoDTO.setIds(series.getSourceDbIdMap());
       
        // scan the NFOs
        this.scanNFOs(stageFiles, infoDTO);

        if (!infoDTO.isTvShow()) {
            LOG.warn("NFO's determined movie for tv show: {}", series.getIdentifier());
            return;
        }

        if (infoDTO.isChanged()) {
            // set series IDs
            for (Entry<String,String> entry : infoDTO.getIds().entrySet()) {
                series.setSourceDbId(entry.getKey(), entry.getValue());
            }
            
            // add skipped scans to modified sources if they are not skipped before NFO reading
            if (!series.isAllScansSkipped()) {
                for (String skippedSourceDb : infoDTO.getSkippedScans()) {
                    if (!"all".equalsIgnoreCase(skippedSourceDb) && !series.isSkippedScan(skippedSourceDb)) {
                        series.addModifiedSource(skippedSourceDb);
                        for (Season season : series.getSeasons()) {
                            season.addModifiedSource(skippedSourceDb);
                            season.setStatus(StatusType.UPDATED);
                            for (VideoData videoData : season.getVideoDatas()) {
                                videoData.addModifiedSource(skippedSourceDb);
                                videoData.setStatus(StatusType.UPDATED);
                            }
                        }
                    }
                }
            }
            // reset skipped scans
            series.setSkippendScansNfo(infoDTO.getSkippedScans());
            
            // set sort title
            series.setTitleSort(infoDTO.getTitleSort());
            
            // set video values
            if (OverrideTools.checkOverwriteTitle(series, SCANNER_ID)) {
                series.setTitle(infoDTO.getTitle(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOriginalTitle(series, SCANNER_ID)) {
                series.setTitleOriginal(infoDTO.getTitleOriginal(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwritePlot(series, SCANNER_ID)) {
                series.setPlot(infoDTO.getPlot(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOutline(series, SCANNER_ID)) {
                series.setOutline(infoDTO.getOutline(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteGenres(series, SCANNER_ID)) {
                series.setGenreNames(infoDTO.getGenres(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteYear(series, SCANNER_ID)) {
                series.setStartYear(infoDTO.getYear(), SCANNER_ID);
            }

            // add certifications
            series.setCertificationInfos(infoDTO.getCertificationInfos());
            
            // add boxed sets
            for (Entry<String, Integer> entry : infoDTO.getSetInfos().entrySet()) {
                series.addBoxedSetDTO(SCANNER_ID, entry.getKey(), entry.getValue());
            }
            
            for (Season season : series.getSeasons()) {
                // remove override source for NFO
                season.removeOverrideSource(SCANNER_ID);
                // set sort title
                season.setTitleSort(infoDTO.getTitleSort());

                if (OverrideTools.checkOverwriteTitle(season, SCANNER_ID)) {
                    season.setTitle(infoDTO.getTitle(), SCANNER_ID);
                }
                if (OverrideTools.checkOverwriteOriginalTitle(season, SCANNER_ID)) {
                    season.setTitleOriginal(infoDTO.getTitleOriginal(), SCANNER_ID);
                }
                if (OverrideTools.checkOverwritePlot(season, SCANNER_ID)) {
                    season.setPlot(infoDTO.getPlot(), SCANNER_ID);
                }
                if (OverrideTools.checkOverwriteOutline(season, SCANNER_ID)) {
                    season.setOutline(infoDTO.getOutline(), SCANNER_ID);
                }

                if (OverrideTools.checkOverwriteYear(season, SCANNER_ID)) {
                    Date seasonYear = infoDTO.getSeasonYear(season.getSeason());
                    season.setPublicationYear(MetadataTools.extractYearAsInt(seasonYear), SCANNER_ID);
                }

                // mark season as done
                season.setTvSeasonDone();
                
                for (VideoData videoData : season.getVideoDatas()) {
                    
                    InfoEpisodeDTO episode = infoDTO.getEpisode(season.getSeason(), videoData.getEpisode());
                    if (episode == null) {
                        if (videoData.removeOverrideSource(SCANNER_ID)) {
                            // reset status if NFO source has been removed
                            videoData.setTvEpisodeNotFound();
                        }
                        
                        if (videoData.isWatchedNfo()) {
                            // the date where the NFO change for watch was detected
                            videoData.setWatchedNfo(false, infoDTO.getWatchedDate());
                            videoData.setTvEpisodeNotFound();
                        }
                        
                    } else {
                        // remove override source for NFO
                        videoData.removeOverrideSource(SCANNER_ID);
                        
                        if (episode.isWatched() != videoData.isWatchedNfo()) {
                            // set NFO watched flag
                            videoData.setWatchedNfo(episode.isWatched(), infoDTO.getWatchedDate());
                        }

                        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                            videoData.setTitle(episode.getTitle(), SCANNER_ID);
                        }
                        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                            videoData.setPlot(episode.getPlot(), SCANNER_ID);
                        }
                        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
                            videoData.setOutline(episode.getPlot(), SCANNER_ID);
                        }
                        if (OverrideTools.checkOverwriteReleaseDate(videoData, SCANNER_ID)) {
                            videoData.setRelease(episode.getFirstAired(), SCANNER_ID);
                        }
                        
                        // set cast and crew from NFO to all episodes
                        videoData.addCreditDTOS(infoDTO.getCredits());
                        
                        // mark episode as done
                        videoData.setTvEpisodeDone();
                    }
                }
            }
        }

        LOG.debug("Scanned NFO data for series '{}'", series.getIdentifier());
    }

    private InfoDTO scanNFOs(List<StageFile> stageFiles, InfoDTO infoDTO) {
        // parse the movie with each NFO
        for (StageFile stageFile : stageFiles) {
            try {
                LOG.debug("Scan NFO file {}-'{}'", stageFile.getId(), stageFile.getFileName());
                this.infoReader.readNfoFile(stageFile, infoDTO);
            } catch (Exception ex) {
                LOG.error("NFO scanning error", ex);
                
                try {
                    // mark stage file with error
                    stageFile.setStatus(StatusType.ERROR);
                    this.stagingService.updateStageFile(stageFile);
                } catch (Exception ignore) {
                    // error can be ignored cause will be done in next run
                }
            }
        }
        
        return infoDTO;
    }
}
