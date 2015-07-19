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

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import java.io.File;
import java.net.URL;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.Trailer;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.TrailerStorageService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.file.StorageType;

@Service("trailerProcessorService")
public class TrailerProcessorService {

    private static final Logger LOG = LoggerFactory.getLogger(TrailerProcessorService.class);
    @Autowired
    private TrailerStorageService trailerStorageService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ConfigService configService;
    
    private File tempDir;
    
    @PostConstruct
    public void init() {
        // determine temporary directory
        tempDir = new File(System.getProperty("java.io.tmpdir"));
    }
    
    public void processTrailer(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to do
            return;
        }
        
        Trailer trailer = trailerStorageService.getRequiredTrailer(queueElement.getId());
        LOG.debug("Process trailer: {}", trailer);

        if (trailer.getStageFile() != null) {
            // local scanned trailers will not be processed further on
            trailer.setStatus(StatusType.DONE);
            trailerStorageService.updateTrailer(trailer);
            return;
        }

        File tempFile;
        try {
            URL web = new URL(trailer.getUrl());
            VGetParser user = VGet.parser(web);
            VideoInfo info = user.info(web);
            VGet v = new VGet(info, tempDir);
            v.download(user);
            tempFile = v.getTarget();
        } catch (Exception e) {
            LOG.error("Download failed for trailer {}-'{}'", trailer.getId(), trailer.getTitle());
            LOG.trace("Trailer download error", e);
            trailer.setStatus(StatusType.ERROR);
            trailerStorageService.updateTrailer(trailer);
            return;
        }

        // download the trailer
        String cacheFilename = buildCacheFilename(trailer, tempFile);

        boolean stored = fileStorageService.store(StorageType.TRAILER, cacheFilename, tempFile, true);
        if (!stored) {
            LOG.error("Failed to store trailer in file cache: {}", cacheFilename);
            // mark located artwork with error
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

    private static String buildCacheFilename(Trailer trailer, File tempFile) {
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
        sb.append(trailer.getSourceHash());
        sb.append(".");
        
        // 3. extension
        sb.append(FilenameUtils.getExtension(tempFile.getName()));
        
        return sb.toString();
    }

    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        trailerStorageService.errorTrailer(queueElement.getId());
    }
}
