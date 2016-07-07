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

import static org.yamj.core.ServiceConstants.IMAGE_GENERATION_ERROR;
import static org.yamj.core.ServiceConstants.STORAGE_ERROR;
import static org.yamj.core.service.artwork.ArtworkStorageTools.SOURCE_UPLOAD;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.sanselan.ImageReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.ArtworkGenerated;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ScalingType;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.scheduling.IQueueProcessService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.tools.image.GraphicTools;
import org.yamj.plugin.api.model.type.ArtworkType;

@Service("artworkProcessorService")
@DependsOn("artworkInitialization")
public class ArtworkProcessorService implements IQueueProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkProcessorService.class);
    
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public void processQueueElement(QueueDTO queueElement) {
        if (queueElement.getId() == null) {
            // nothing to do
        } else if (queueElement.getLocatedArtwork()) {
            processLocatedArtwork(queueElement.getId());
        } else {
            processGeneratedArtwork(queueElement.getId());
        }
    }
    
    private void processLocatedArtwork(final Long id) {
        // get required located artwork
        ArtworkLocated located = artworkStorageService.getRequiredArtworkLocated(id);
        final StorageType storageType = ArtworkStorageTools.getStorageType(located);
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
            String cacheFilename = ArtworkStorageTools.buildCacheFilename(located);
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
                LOG.error("{}: {}", STORAGE_ERROR, ex.getMessage());
                LOG.trace(STORAGE_ERROR, ex);
                
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
                LOG.trace(IMAGE_GENERATION_ERROR, ex);
                
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
                LOG.warn(IMAGE_GENERATION_ERROR, ex);
            }
        }

        // update located artwork in database
        artworkStorageService.updateArtworkLocated(located);
    }

    private void processGeneratedArtwork(final Long id) {
        // get required generated artwork
        ArtworkGenerated generated = artworkStorageService.getRequiredArtworkGenerated(id);
        LOG.debug("Process generated artwork: {}", generated);

        try {
            // generate image
            createAndStoreImage(generated.getArtworkLocated(), generated.getArtworkProfile(), generated.getCacheFilename());

            // mark generated image as done
            generated.setStatus(StatusType.DONE);
        } catch (Exception ex) {
            LOG.error("Failed to generate image for {}", generated);
            LOG.warn(IMAGE_GENERATION_ERROR, ex);

            // mark generated image as error
            generated.setStatus(StatusType.ERROR);
        }
        
        this.artworkStorageService.updateArtworkGenerated(generated);
    }
    
    @Override
    public void processErrorOccurred(QueueDTO queueElement, Exception error) {
        LOG.error("Failed processing of "+(queueElement.getLocatedArtwork()?"located":"generated")+" artwork "+queueElement.getId(), error);
        if (queueElement.getLocatedArtwork()) {
            artworkStorageService.errorArtworkLocated(queueElement.getId());
        } else {
            artworkStorageService.errorArtworkGenerated(queueElement.getId());
        }
       
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
    
    private ArtworkGenerated generateImage(ArtworkLocated located, ArtworkProfile profile) throws IOException, ImageReadException { //NOSONAR
        // build cache filename
        final String cacheFilename = ArtworkStorageTools.buildCacheFilename(located, profile);
        
        // create and store image
        createAndStoreImage(located, profile, cacheFilename);
        
        try {
            final String cacheDir = StringUtils.removeEnd(FileTools.createDirHash(cacheFilename), File.separator + cacheFilename);
            return artworkStorageService.storeArtworkGenerated(located, profile, cacheDir, cacheFilename);
        } catch (Exception ex) {
            // delete generated file storage element also
            LOG.trace("Failed to generate file storage for {}, error: {}", cacheFilename, ex.getMessage());
            final StorageType storageType = ArtworkStorageTools.getStorageType(profile);
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
                        return false;
                    }

                    // set values for later usage
                    located.setWidth((int) dimension.getWidth());
                    located.setHeight((int) dimension.getHeight());
                } catch (IOException ex) {
                    LOG.warn("Could not determine image dimension cause invalid image: {}", located);
                    LOG.trace("Invalid image error", ex);
                    return false;
                }
            }

            // TODO check quality of artwork?
        } else {
            // TODO stage file needs no image validation??
            LOG.trace("Located URL was blank for {}", located);
        }

        return true;
    }
    
    public ImageDTO getImage(Long id, String profileName) throws IOException, ImageReadException { //NOSONAR
        ImageDTO result = new ImageDTO();

        ArtworkGenerated generated = this.artworkStorageService.getArtworkGenerated(id, profileName);
        if (generated != null) {
            final StorageType storageType = ArtworkStorageTools.getStorageType(generated.getArtworkProfile().getArtworkType());
            result.setResource(this.fileStorageService.getStorageName(storageType, generated.getFullCacheFilename()));
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
        } catch (IncorrectResultSizeDataAccessException ex) { //NOSONAR
            // no located image found
            return null;
        }
        
        final MetaDataType metaDataType = ArtworkStorageTools.getMetaDataType(located);
        final ArtworkType artworkType = located.getArtwork().getArtworkType();
        ArtworkProfile profile = this.artworkStorageService.getArtworkProfile(profileName, metaDataType, artworkType);
        if (profile == null) {
            // profile must be present
            return null;
        }
        
        // create the image and the database entry
        generated = this.generateImage(located, profile);
        
        // return the image
        final StorageType storageType = ArtworkStorageTools.getStorageType(located);
        result.setResource(this.fileStorageService.getStorageName(storageType, generated.getFullCacheFilename()));
        result.setMediaType(MediaType.IMAGE_JPEG);
        return result;
    }

    private void createAndStoreImage(ArtworkLocated located, ArtworkProfile profile, String cacheFilename) throws IOException, ImageReadException { 
        final StorageType storageType = ArtworkStorageTools.getStorageType(profile);
        
        LOG.trace("Generate image for {} with profile {}", located, profile.getProfileName());
        BufferedImage imageGraphic = GraphicTools.loadJPEGImage(this.fileStorageService.getFile(storageType, located.getCacheFilename()));
    
        // set dimension of original image if not done before
        if (located.getWidth() <= 0 || located.getHeight() <= 0) {
            located.setWidth(imageGraphic.getWidth());
            located.setHeight(imageGraphic.getHeight());
        }
    
        // draw the image
        BufferedImage image = drawImage(imageGraphic, profile);
    
        // store image on stage system
        fileStorageService.storeImage(cacheFilename, storageType, image, profile.getImageType(), profile.getQuality());
    }
    
    private static BufferedImage drawImage(BufferedImage imageGraphic, ArtworkProfile profile) {
        BufferedImage bi = imageGraphic;

        // TODO more graphic options
        
        int origWidth = imageGraphic.getWidth();
        int origHeight = imageGraphic.getHeight();
        float ratio = profile.getRatio();
        float rcqFactor = profile.getRounderCornerQuality();

        if (ScalingType.NORMALIZE == profile.getScalingType()) {
            if (origWidth < profile.getWidth() && origHeight < profile.getWidth()) {
                // normalize image if below profile settings
                bi = GraphicTools.scaleToSizeNormalized((int) (origHeight * rcqFactor * ratio), (int) (origHeight * rcqFactor), bi);
            } else {
                // normalize image
                bi = GraphicTools.scaleToSizeNormalized((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);
            }
        } else if (ScalingType.STRETCH == profile.getScalingType()) {
            // stretch image
            bi = GraphicTools.scaleToSizeStretch((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);
        } else if ((origWidth != profile.getWidth()) || (origHeight != profile.getHeight())) {
            // scale image to given size
            bi = GraphicTools.scaleToSize((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);
        }

        // return image
        return bi;
    }
}