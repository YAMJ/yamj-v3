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
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.ArtworkGenerated;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.scheduling.IQueueProcessService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.FileTools;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.tools.image.GraphicTools;

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
        // get required located artwork
        ArtworkLocated located = artworkStorageService.getRequiredArtworkLocated(queueElement.getId());
        final StorageType storageType = located.getArtwork().getStorageType();
        LOG.debug("Process located artwork: {}", located);

        if (located.isNotCached()) {
            // just processed if cache file name not stored before
            // which means that no original image has been created
            
            // validate artwork
            boolean valid = checkArtworkQuality(located);
            if (!valid) {
                LOG.debug("Located artwork {} is not valid", located);
                located.setStatus(StatusType.INVALID);
                artworkStorageService.updateArtworkLocated(located);
                return;
            }
    
            if (SOURCE_UPLOAD.equals(located.getSource())) {
                LOG.debug("Located artwork {} needs an upload", located);
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
                LOG.warn("Storage error: {}", ex.getMessage());
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
                
                // reset cache values and mark located artwork and reset to updated
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
    
    private void generateImage(ArtworkLocated located, ArtworkProfile profile) throws Exception {
        final StorageType storageType = located.getArtwork().getStorageType();

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
        String cacheFilename = ArtworkTools.buildCacheFilename(located, profile);
        fileStorageService.storeImage(cacheFilename, storageType, image, profile.getImageType(), profile.getQuality());

        try {
            ArtworkGenerated generated = new ArtworkGenerated();
            generated.setArtworkLocated(located);
            generated.setArtworkProfile(profile);
            generated.setStatus(StatusType.DONE);
            generated.setCacheFilename(cacheFilename);
            String cacheDirectory = FileTools.createDirHash(cacheFilename);
            generated.setCacheDirectory(StringUtils.removeEnd(cacheDirectory, File.separator + cacheFilename));
            artworkStorageService.storeArtworkGenerated(generated);
        } catch (Exception ex) {
            // delete generated file storage element also
            LOG.trace("Failed to generate file storage for {}, error: {}", cacheFilename, ex.getMessage());
            fileStorageService.deleteFile(storageType, cacheFilename);
            throw ex;
        }
    }

    private static BufferedImage drawImage(BufferedImage imageGraphic, ArtworkProfile profile) {
        BufferedImage bi = imageGraphic;

        // TODO more graphic options
        
        int origWidth = imageGraphic.getWidth();
        int origHeight = imageGraphic.getHeight();
        float ratio = profile.getRatio();
        float rcqFactor = profile.getRounderCornerQuality();

        if (profile.isNormalize()) {
            if (origWidth < profile.getWidth() && origHeight < profile.getWidth()) {
            	// normalize image if below profile settings
                bi = GraphicTools.scaleToSizeNormalized((int) (origHeight * rcqFactor * ratio), (int) (origHeight * rcqFactor), bi);
            } else {
            	// normalize image
                bi = GraphicTools.scaleToSizeNormalized((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);
            }
        } else if (profile.isStretch()) {
        	// stretch image
            bi = GraphicTools.scaleToSizeStretch((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);
        } else if ((origWidth != profile.getWidth()) || (origHeight != profile.getHeight())) {
        	// scale image to given size
            bi = GraphicTools.scaleToSize((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);
        }

        // return image
        return bi;
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
}