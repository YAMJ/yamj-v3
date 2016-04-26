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
package org.yamj.core.service.trailer;

import java.io.File;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Trailer;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.TrailerStorageService;
import org.yamj.core.scheduling.IQueueProcessService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.service.trailer.online.YouTubeDownloadParser;
import org.yamj.plugin.api.model.type.ContainerType;
import org.yamj.plugin.api.trailer.TrailerDownloadDTO;

@Service("trailerProcessorService")
public class TrailerProcessorService implements IQueueProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(TrailerProcessorService.class);
    
    @Autowired
    private TrailerStorageService trailerStorageService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private YouTubeDownloadParser youTubeDownloadParser;
    
    @Override
    public void processQueueElement(QueueDTO queueElement) {
        // get required trailer
        Trailer trailer = trailerStorageService.getRequiredTrailer(queueElement.getId());
        LOG.debug("Process trailer: {}", trailer);

        if (trailer.getStageFile() != null) {
            // local scanned trailers will not be processed further on
            trailer.setStatus(StatusType.DONE);
            trailerStorageService.updateTrailer(trailer);
            return;
        }
        if (trailer.isCached()) {
            // no trailer download if already cached
            trailer.setStatus(StatusType.DONE);
            trailerStorageService.updateTrailer(trailer);
            return;
        }

        TrailerDownloadDTO dto = null;
        if ("youtube".equalsIgnoreCase(trailer.getSource())) {
            // NOTE: for YouTube the hash code is the video ID
            dto = youTubeDownloadParser.extract(trailer.getHashCode());
        } else {
            try {
                // defaults to MP4 and URL
                dto = new TrailerDownloadDTO(trailer.getContainer(), new URL(trailer.getUrl()));
            } catch (Exception e) {
                LOG.warn("Malformed URL: {}", trailer.getUrl());
            }
        }
        
        if (dto == null) {
            // no trailer found
            trailer.setStatus(StatusType.NOTFOUND);
            trailerStorageService.updateTrailer(trailer);
            return;
        }

        // download the trailer
        String cacheFilename = buildCacheFilename(trailer, dto.getContainer());

        boolean stored = false;
        try {
            stored = fileStorageService.store(StorageType.TRAILER, cacheFilename, dto.getUrl());
            if (!stored) {
                LOG.error("Failed to store trailer in file cache: {}", cacheFilename);
            }
        } catch (Exception e) {
            LOG.error("Failed to download trailer: " + dto.getUrl(), e);
        }
        
        if (!stored) {
            // mark trailer as errorness
            trailer.setStatus(StatusType.ERROR);
            trailerStorageService.updateTrailer(trailer);
            return;
        }

        // set values in located artwork
        String cacheDirectory = FileTools.createDirHash(cacheFilename);
        trailer.setCacheDirectory(StringUtils.removeEnd(cacheDirectory, File.separator + cacheFilename));
        trailer.setCacheFilename(cacheFilename);
        trailer.setStatus(StatusType.DONE);
        trailerStorageService.updateTrailer(trailer);
    }

    private static String buildCacheFilename(Trailer trailer, ContainerType container) {
        StringBuilder sb = new StringBuilder();
        
        // 1. video name
        if (trailer.getVideoData() != null) {
            sb.append(trailer.getVideoData().getIdentifier());
            sb.append(".movie.");
        } else if (trailer.getSeries() != null) {
            sb.append(trailer.getSeries().getIdentifier());
            sb.append(".series.");
        } else {
            // should never happen
            sb.append("unknown_");
            sb.append(trailer.getId());
            sb.append(".");
        }
        
        // 2. source
        sb.append(trailer.getSource());
        sb.append(".");
        sb.append(trailer.getHashCode());
        sb.append(".");
        
        // 3. extension
        switch (container) {
        case GP3:
            sb.append("3gp");
            break;
        default:
            sb.append(container.name().toLowerCase());
            break;
        }
        
        return sb.toString();
    }

    @Override
    public void processErrorOccurred(QueueDTO queueElement, Exception error) {
        LOG.error("Failed processing of trailer "+queueElement.getId(), error);
        
        trailerStorageService.errorTrailer(queueElement.getId());
    }
}
