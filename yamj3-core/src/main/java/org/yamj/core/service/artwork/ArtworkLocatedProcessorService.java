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

import static org.yamj.core.service.artwork.ArtworkTools.SOURCE_UPLOAD;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sanselan.ImageReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.ArtworkGenerated;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.tools.image.GraphicTools;

@Service("artworkLocatedProcessorService")
@DependsOn("artworkInitialization")
public class ArtworkLocatedProcessorService extends AbstractArtworkProcessorService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkLocatedProcessorService.class);
    
    @Override
    public void processQueueElement(QueueDTO queueElement) {
        // get required located artwork
        ArtworkLocated located = artworkStorageService.getRequiredArtworkLocated(queueElement.getId());
        final StorageType storageType = ArtworkTools.getStorageType(located);
        LOG.debug("Process located artwork: {}", located);

        if (located.isNotCached()) {
            // just processed if cache file name not stored before
            // which means that no original image has been created
            
            // validate artwork
            boolean valid = checkArtworkQuality(located);
            if (!valid) {
                LOG.warn("Located artwork {} is not valid", located);
                located.setStatus(StatusType.INVALID);
                artworkStorageService.updateArtworkLocated(located);
                return;
            }
    
            if (SOURCE_UPLOAD.equals(located.getSource())) {
                LOG.info("Located artwork {} needs an upload", located);
                located.setStatus(StatusType.INVALID);
                artworkStorageService.updateArtworkLocated(located);
                return;
            }

            // store original in file cache
            String cacheFilename = ArtworkTools.buildCacheFilename(located);
            LOG.trace("Cache artwork with file name: {}", cacheFilename);
    
            boolean stored;
            try {
                if (located.getStageFile() != null) {
                    if (StringUtils.startsWith(located.getSource(), "attachment")) {
                        // file contains attached artwork
                        stored = this.storedAttachedArwork(storageType, located, cacheFilename);
                    } else {
                        // file is an artwork
                        stored = fileStorageService.store(storageType, cacheFilename, located.getStageFile());
                    }
                } else {
                    stored = fileStorageService.store(storageType, cacheFilename, new URL(located.getUrl()));
                }
            } catch (IOException ex) {
                LOG.error("Storage error: {}", ex.getMessage());
                LOG.trace("Storage error", ex);
                
                return;
            }

            if (!stored) {
                LOG.error("Failed to store artwork in file cache: {}", cacheFilename);
                // mark located artwork with error
                located.setStatus(StatusType.ERROR);
                artworkStorageService.updateArtworkLocated(located);
                return;
            }

            // set values in located artwork
            String cacheDirectory = FileTools.createDirHash(cacheFilename);
            located.setCacheDirectory(StringUtils.removeEnd(cacheDirectory, File.separator + cacheFilename));
            located.setCacheFilename(cacheFilename);
        }
        
        // located has been done
        located.setStatus(StatusType.DONE);

        // after that: try preProcessing of images
        List<ArtworkProfile> profiles = artworkStorageService.getPreProcessArtworkProfiles(located);
        for (ArtworkProfile profile : profiles) {
            try {
                // generate image for a profiles
                generateImage(located, profile);
            } catch (IOException ex)  {
                LOG.warn("Original image is not found: {}/{}", located.getCacheDirectory(), located.getCacheFilename());
                LOG.trace("Image generation error", ex);
                
                // reset cache values and mark located artwork for update
                located.setCacheDirectory(null);
                located.setCacheFilename(null);
                located.setStatus(StatusType.UPDATED);

                // no further processing for that located image
                break;
            } catch (OutOfMemoryError ex) {
                LOG.error("Failed to load/transform image due to memory constraints: {}", located);
                LOG.trace("Out of memory", ex);

                // mark located artwork as error
                located.setStatus(StatusType.ERROR);

                // no further processing for that located image
                break;
             } catch (ImageReadException ex) {
                LOG.warn("Original image is invalid: {}", located);
                LOG.trace("Invalid image error", ex);

                // mark located artwork as invalid
                located.setStatus(StatusType.INVALID);

                // no further processing for that located image
                break;
            } catch (Exception ex) {
                LOG.error("Failed to generate image for {} with profile {}", located, profile.getProfileName());
                LOG.warn("Image generation error", ex);
            }
        }

        // update located artwork in database
        artworkStorageService.updateArtworkLocated(located);
    }

    @Override
    public void processErrorOccurred(QueueDTO queueElement, Exception error) {
        LOG.error("Failed processing of located artwork "+queueElement.getId(), error);
       
        artworkStorageService.errorArtworkLocated(queueElement.getId());
    }

    private boolean storedAttachedArwork(StorageType storageType, ArtworkLocated located, String cacheFilename) {
        // file contains attached artwork
        int attachmentId = 1;
        try {
            attachmentId = Integer.parseInt(located.getSource().split("#")[1]);
        } catch (Exception ex) { //NOSONAR
           LOG.warn("Failed to determine attachment id from source {}", located.getSource());
           return false;
        }
        
        return fileStorageService.store(storageType, cacheFilename, located.getStageFile(), attachmentId);
    }
    
    private ArtworkGenerated generateImage(ArtworkLocated located, ArtworkProfile profile) throws Exception {
        // build cache filename
        final String cacheFilename = ArtworkTools.buildCacheFilename(located, profile);
        
        // create and store image
        createAndStoreImage(located, profile, cacheFilename);
        
        try {
            ArtworkGenerated generated = new ArtworkGenerated();
            generated.setArtworkLocated(located);
            generated.setArtworkProfile(profile);
            generated.setStatus(StatusType.DONE);
            generated.setCacheFilename(cacheFilename);
            String cacheDirectory = FileTools.createDirHash(cacheFilename);
            generated.setCacheDirectory(StringUtils.removeEnd(cacheDirectory, File.separator + cacheFilename));
            artworkStorageService.storeArtworkGenerated(generated);
            return generated;
        } catch (Exception ex) {
            // delete generated file storage element also
            LOG.trace("Failed to generate file storage for {}, error: {}", cacheFilename, ex.getMessage());
            final StorageType storageType = ArtworkTools.getStorageType(profile);
            fileStorageService.deleteFile(storageType, cacheFilename);
            throw ex;
        }
    }

    private static boolean checkArtworkQuality(ArtworkLocated located) {
        if (StringUtils.isNotBlank(located.getUrl())) {

            if (located.getWidth() <= 0 || located.getHeight() <= 0) {
                // retrieve dimension
                try {
                    // get dimension
                    Dimension dimension = GraphicTools.getDimension(located.getUrl());
                    if (dimension.getHeight() <= 0 || dimension.getWidth() <= 0) {
                        LOG.warn("No valid image dimension determined: {}", located);
                        return Boolean.FALSE;
                    }

                    // set values for later usage
                    located.setWidth((int) dimension.getWidth());
                    located.setHeight((int) dimension.getHeight());
                } catch (IOException ex) {
                    LOG.warn("Could not determine image dimension cause invalid image: {}", located);
                    LOG.trace("Invalid image error", ex);
                    return Boolean.FALSE;
                }
            }

            // TODO check quality of artwork?
        } else {
            // TODO stage file needs no image validation??
            LOG.trace("Located URL was blank for {}", located);
        }

        return Boolean.TRUE;
    }
    
    public ImageDTO getImage(Long id, ArtworkType artworkType, String profileName) throws Exception {
        ImageDTO result = new ImageDTO();

        ArtworkGenerated generated = this.artworkStorageService.getArtworkGenerated(id, artworkType, profileName);
        if (generated != null) {
            final StorageType storageType = ArtworkTools.getStorageType(artworkType);
            final String filename = FilenameUtils.concat(generated.getCacheDirectory(), generated.getCacheFilename());
            result.setResource(this.fileStorageService.getStorageName(storageType, filename));
            result.setHttpStatus(HttpStatus.OK);
            result.setMediaType(MediaType.IMAGE_JPEG);
            return result;
        }

        // if no generated image found then create one
        ArtworkLocated located;
        try {
            located = this.artworkStorageService.getRequiredArtworkLocated(id);
            if (located.isNotCached()) {
                // located image must be cached
                return null;
            }
        } catch (IncorrectResultSizeDataAccessException ex) {
            // no located image found
            return null;
        }
        
        ArtworkProfile profile = this.artworkStorageService.getArtworkProfile(profileName, artworkType);
        if (profile == null || located.getArtwork().getArtworkType() != profile.getArtworkType()) {
            // profile must be present and artwork types must match
            return null;
        }
        
        // create the image and the database entry
        generated = this.generateImage(located, profile);
        
        // return the image
        final StorageType storageType = ArtworkTools.getStorageType(artworkType);
        final String filename = FilenameUtils.concat(generated.getCacheDirectory(), generated.getCacheFilename());
        result.setResource(this.fileStorageService.getStorageName(storageType, filename));
        result.setHttpStatus(HttpStatus.CREATED);
        result.setMediaType(MediaType.IMAGE_JPEG);
        return result;
    }
}