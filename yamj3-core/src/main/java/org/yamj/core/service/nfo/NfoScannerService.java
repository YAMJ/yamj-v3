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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.service.staging.StagingService;
import org.yamj.core.tools.OverrideTools;

@Service("nfoScannerService")
public class NfoScannerService {

    public static final String SCANNER_ID = "NFO";
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
        LOG.debug("Scanning nfo data for '{}'", videoData.getIdentifier());
        
        // get the stage files
        List<StageFile> stageFiles = this.stagingService.getValidNFOFiles(videoData);

        // create a new INFO object for movie
        InfoDTO infoDTO = new InfoDTO(false);

        // parse the movie with each NFO
        for (StageFile stageFile : stageFiles) {
            try {
                LOG.debug("Scan NFO file {}", stageFile.getFileName());
                this.infoReader.readNfoFile(stageFile, infoDTO);
            } catch (Exception ex) {
                LOG.error("NFO scanning error", ex);
                stageFile.setStatus(StatusType.ERROR);
                this.metadataStorageService.update(stageFile);
            }
        }

        if (infoDTO.isTvShow()) {
            LOG.warn("NFO's determined TV show for video {}; no changes");
        } else if (infoDTO.isChanged()) {

            // reset skip online scans
            videoData.setSkipOnlineScans(infoDTO.getSkipOnlineScans());
            // set top 250
            videoData.setTopRank(infoDTO.getTop250());

            if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
                videoData.setTitle(infoDTO.getTitle(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
                videoData.setTitleOriginal(infoDTO.getTitleOriginal(), SCANNER_ID);
            }
            if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
                videoData.setPublicationYear(infoDTO.getYear(), SCANNER_ID);
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
            // set credit DTOs for update in database
            videoData.setCreditDTOS(infoDTO.getCredits());
        }
        
        // mark video data as updated (online scan can be done)
        videoData.setStatus(StatusType.UPDATED);
        this.metadataStorageService.updateVideoData(videoData);
    }

    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        // TODO 
    }
}
