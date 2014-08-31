/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.core.service.nfo;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.service.staging.StagingService;
import org.yamj.core.tools.OverrideTools;

@Service("nfoScannerService")
public class NfoScannerService {

    public static final String SCANNER_ID = "nfo";
    private static final Logger LOG = LoggerFactory.getLogger(NfoScannerService.class);

    @Autowired
    private StagingService stagingService;
    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private InfoReader infoReader;
    
    public void scanMovieNfo(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }
        
        VideoData videoData = metadataStorageService.getRequiredVideoData(queueElement.getId());
        LOG.info("Scanning NFO data for movie '{}'", videoData.getIdentifier());
        
        // get the stage files ...
        List<StageFile> stageFiles = this.stagingService.getValidNFOFiles(videoData);
        // ... and scan all NFOs
        InfoDTO infoDTO = this.scanNFOs(stageFiles, false);

        if (infoDTO.isTvShow()) {
            LOG.warn("NFO's determined TV show for movie: {}", videoData.getIdentifier());
        } else if (infoDTO.isChanged()) {

            // set video IDs
            for (Entry<String,String> entry : infoDTO.getIds().entrySet()) {
                videoData.setSourceDbId(entry.getKey(), entry.getValue());
            }
            // reset skip online scans
            videoData.setSkipOnlineScans(infoDTO.getSkipOnlineScans());
            // set top 250
            videoData.setTopRank(infoDTO.getTop250());
            // set rating
            videoData.addRating(SCANNER_ID, infoDTO.getRating());
            
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
                videoData.setReleaseDate(infoDTO.getReleaseDate(), SCANNER_ID);
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
            if (CollectionUtils.isNotEmpty(infoDTO.getGenres())) {
                if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
                    videoData.setGenreNames(infoDTO.getGenres(), SCANNER_ID);
                }
            }
            if (StringUtils.isNotBlank(infoDTO.getCompany())) {
                if (OverrideTools.checkOverwriteStudios(videoData, SCANNER_ID)) {
                    Set<String> studioNames = Collections.singleton(infoDTO.getCompany());
                    videoData.setStudioNames(studioNames, SCANNER_ID);
                }
            }

            // set credit DTOs for update in database
            videoData.setCreditDTOS(infoDTO.getCredits());
        }

        // store associated entities
        this.metadataStorageService.storeAssociatedEntities(videoData);

        // update video data
        this.metadataStorageService.updateMetaData(videoData);
    }

    public void scanSerieseNfo(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }
        
        Series series = metadataStorageService.getRequiredSeries(queueElement.getId());
        LOG.info("Scanning NFO data for series '{}'", series.getIdentifier());
        
        // get the stage files ...
        List<StageFile> stageFiles = this.stagingService.getValidNFOFiles(series);
        // ... and scan all NFOs
        InfoDTO infoDTO = this.scanNFOs(stageFiles, true);

        if (!infoDTO.isTvShow()) {
            LOG.warn("NFO's determined movie for tv show: {}", series.getIdentifier());
        } else if (infoDTO.isChanged()) {

            // set series IDs
            for (Entry<String,String> entry : infoDTO.getIds().entrySet()) {
                series.setSourceDbId(entry.getKey(), entry.getValue());
            }
            // reset skip online scans
            series.setSkipOnlineScans(infoDTO.getSkipOnlineScans());
            
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

            for (Season season : series.getSeasons()) {
                for (VideoData videoData : season.getVideoDatas()) {
                    InfoEpisodeDTO episode = infoDTO.getEpisode(season.getSeason(), videoData.getEpisode());
                    if (episode == null) {
                        // mark episode as not found
                        videoData.setTvEpisodeNotFound();
                    } else {
                        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                            videoData.setTitle(episode.getTitle(), SCANNER_ID);
                        }
                        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
                            videoData.setPlot(episode.getPlot(), SCANNER_ID);
                        }
                        
                        // set actors from NFO to all episodes
                        videoData.addCreditDTOS(infoDTO.getCredits());
                        
                        // mark episode as scanned
                        videoData.setTvEpisodeScanned();
                    }
                }
                
                // TODO set publication year from lowest firstAired
                //      of all episodes of that season
            }
        }

        // store associated entities
        this.metadataStorageService.storeAssociatedEntities(series);

        // update Series
        this.metadataStorageService.updateMetaData(series);
    }

    private InfoDTO scanNFOs(List<StageFile> stageFiles, boolean tvShow) {
        // create a new INFO object
        InfoDTO infoDTO = new InfoDTO(tvShow);

        // parse the movie with each NFO
        for (StageFile stageFile : stageFiles) {
            try {
                LOG.debug("Scan NFO file '{}'", stageFile.getFileName());
                this.infoReader.readNfoFile(stageFile, infoDTO);
            } catch (Exception ex) {
                LOG.error("NFO scanning error", ex);
                stageFile.setStatus(StatusType.ERROR);
                this.metadataStorageService.update(stageFile);
            }
        }
        
        return infoDTO;
    }
    
    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        // just set next stage
        if (queueElement.isMetadataType(MetaDataType.MOVIE)) {
            VideoData videoData = metadataStorageService.getRequiredVideoData(queueElement.getId());
            this.metadataStorageService.setNextStep(videoData);
        } else if (queueElement.isMetadataType(MetaDataType.SERIES)) {
            Series series = metadataStorageService.getRequiredSeries(queueElement.getId());
            this.metadataStorageService.setNextStep(series);
        }
    }
}
