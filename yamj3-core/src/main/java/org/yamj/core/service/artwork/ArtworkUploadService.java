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
package org.yamj.core.service.artwork;

import static org.yamj.core.service.artwork.ArtworkStorageTools.SOURCE_UPLOAD;

import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.mediaimport.FilenameScanner;
import org.yamj.plugin.api.model.type.ImageType;

@Service("artworkUploadService")
public class ArtworkUploadService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkUploadService.class);
    
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private FilenameScanner filenameScanner;
    
    public ApiStatus uploadArtwork(ArtworkType artworkType, MetaDataType metaDataType, Long id, MultipartFile image) {
        String filename = image.getOriginalFilename();
        if (StringUtils.isBlank(filename)) {
            filename = image.getName();
        }
        
        final String extension = FilenameUtils.getExtension(filename);
        if (filenameScanner.determineFileType(extension) != FileType.IMAGE) {
            return ApiStatus.unsupportedMediaType("Uploaded file '" + filename + "' is no valid image");
        }
        
        // find matching artwork
        Artwork artwork = this.artworkStorageService.getArtwork(artworkType, metaDataType, id);
        if (artwork == null) {
            return ApiStatus.notFound("No matching artwork found");
        }
        
        // get or create located artwork
        final String hashCode = Integer.toString(Math.abs(filename.hashCode()));
        ArtworkLocated located = this.artworkStorageService.getArtworkLocated(artwork, SOURCE_UPLOAD, hashCode);
        
        if (located == null) {
            located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource(SOURCE_UPLOAD);
            located.setHashCode(hashCode);
            located.setPriority(5);
            located.setImageType(ImageType.fromString(extension));
            located.setStatus(StatusType.NEW);
        } else {
            located.setArtwork(artwork);
            located.setPriority(5);
            located.setImageType(ImageType.fromString(extension));
            located.setStatus(StatusType.UPDATED);
        }

        final String cacheFilename = ArtworkStorageTools.buildCacheFilename(located);
        LOG.trace("Cache uploaded image with file name: {}", cacheFilename);

        // save file to cache
        try {
            fileStorageService.store(ArtworkStorageTools.getStorageType(artwork), cacheFilename, image.getBytes());
        } catch (Exception e) {
            LOG.warn("Failed to store uploaded file: " + cacheFilename, e);
            return ApiStatus.internalError("Failed to store uploaded file into cache");
        }
        
        // store located artwork
        String cacheDirectory = FileTools.createDirHash(cacheFilename);
        located.setCacheFilename(cacheFilename);
        located.setCacheDirectory(StringUtils.removeEnd(cacheDirectory, File.separator + cacheFilename));
        this.artworkStorageService.storeArtworkLocated(located);
        
        return ApiStatus.ok("Cached image as '" + cacheFilename + "'");
    }
}
