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
package org.yamj.core.service.artwork;

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
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.ArtworkGenerated;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.ImageFormat;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.service.file.tools.FileTools;
import org.yamj.core.tools.image.GraphicTools;

@Service("artworkProcessorService")
public class ArtworkProcessorService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkProcessorService.class);
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private FileStorageService fileStorageService;

    public void processArtwork(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to do
            return;
        }

        ArtworkLocated located = artworkStorageService.getRequiredArtworkLocated(queueElement.getId());
        StorageType storageType;
        if (located.getArtwork().getArtworkType() == ArtworkType.PHOTO) {
            storageType = StorageType.PHOTO;
        } else {
            storageType = StorageType.ARTWORK;
        }
        LOG.debug("Process located artwork: {}", located);

        // validate artwork
        boolean valid = checkArtworkQuality(located);
        if (!valid) {
            LOG.debug("Located artwork {} is not valid", located);
            located.setStatus(StatusType.INVALID);
            artworkStorageService.updateArtworkLocated(located);
            return;
        }

        // store original in file cache
        String cacheFilename = buildCacheFilename(located);
        LOG.debug("Cache artwork with file name: {}", cacheFilename);

        boolean stored;
        try {
            if (located.getStageFile() != null) {
                stored = fileStorageService.store(storageType, cacheFilename, located.getStageFile());
            } else {
                stored = fileStorageService.store(storageType, cacheFilename, new URL(located.getUrl()));
            }
        } catch (IOException error) {
            LOG.warn("Storage error: {}", error.getMessage(), error);
            return;
        }

        if (!stored) {
            LOG.error("Failed to store artwork store artwork in file cache: {}", cacheFilename);
            // mark located artwork with error
            located.setStatus(StatusType.ERROR);
            artworkStorageService.updateArtworkLocated(located);
            return;
        }

        // set values in located artwork
        String cacheDirectory = FileTools.createDirHash(cacheFilename);
        located.setCacheDirectory(StringUtils.removeEnd(cacheDirectory, File.separator + cacheFilename));
        located.setCacheFilename(cacheFilename);
        located.setStatus(StatusType.DONE);

        // after that: try preProcessing of images
        List<ArtworkProfile> profiles = artworkStorageService.getPreProcessArtworkProfiles(located);
        for (ArtworkProfile profile : profiles) {
            try {
                // generate image for a profiles
                generateImage(located, profile);
            } catch (ImageReadException error) {
                LOG.warn("Original image is invalid: {}", located);
                LOG.trace("Invalid image error", error);

                // mark located artwork as invalid
                located.setStatus(StatusType.INVALID);

                // no further processing for that located image
                break;
            } catch (Exception error) {
                LOG.error("Failed to generate image for {} with profile {}", located, profile.getProfileName());
                LOG.warn("Image generation error", error);
            }
        }

        // update located artwork in database
        artworkStorageService.updateArtworkLocated(located);
    }

    private void generateImage(ArtworkLocated located, ArtworkProfile profile) throws Exception {
        StorageType storageType;
        if (located.getArtwork().getArtworkType() == ArtworkType.PHOTO) {
            storageType = StorageType.PHOTO;
        } else {
            storageType = StorageType.ARTWORK;
        }

        LOG.debug("Generate image for {} with profile {}", located, profile.getProfileName());
        BufferedImage imageGraphic = null;
        try {
            imageGraphic = GraphicTools.loadJPEGImage(this.fileStorageService.getFile(storageType, located.getCacheFilename()));
        } catch (OutOfMemoryError ex) {
            LOG.error("Failed to load/transform image '{}' due to memory constraints", located.getCacheFilename());
        }

        if (imageGraphic == null) {
            LOG.error("Error processing image '{}', not processed further.", located.getCacheFilename());
            // Mark the image as unprocessable
            located.setStatus(StatusType.ERROR);
            artworkStorageService.updateArtworkLocated(located);
            // quit
            return;
        }

        // set dimension of original image if not done before
        if (located.getWidth() <= 0 || located.getHeight() <= 0) {
            located.setWidth(imageGraphic.getWidth());
            located.setHeight(imageGraphic.getHeight());
        }

        // draw the image
        BufferedImage image = drawImage(imageGraphic, profile);

        // store image on stage system
        String cacheFilename = buildCacheFilename(located, profile);
        fileStorageService.storeImage(cacheFilename, storageType, image, profile.getImageFormat(), profile.getQuality());

        try {
            ArtworkGenerated generated = new ArtworkGenerated();
            generated.setArtworkLocated(located);
            generated.setArtworkProfile(profile);
            generated.setCacheFilename(cacheFilename);
            String cacheDirectory = FileTools.createDirHash(cacheFilename);
            generated.setCacheDirectory(StringUtils.removeEnd(cacheDirectory, File.separator + cacheFilename));
            artworkStorageService.storeArtworkGenerated(generated);
        } catch (Exception ex) {
            // delete generated file storage element also
            LOG.trace("Failed to generate file storage for {}, error: {}", cacheFilename, ex.getMessage());
            try {
                fileStorageService.delete(storageType, cacheFilename);
            } catch (IOException ex2) {
                LOG.trace("Unable to delete file after exception: {}", ex2.getMessage(), ex);
            }
            throw ex;
        }
    }

    private BufferedImage drawImage(BufferedImage imageGraphic, ArtworkProfile profile) {
        BufferedImage bi = imageGraphic;

        int origWidth = imageGraphic.getWidth();
        int origHeight = imageGraphic.getHeight();
        float ratio = profile.getRatio();
        float rcqFactor = profile.getRounderCornerQuality();

        boolean skipResize = false;
        if (origWidth < profile.getWidth() && origHeight < profile.getWidth()) {
            //Perhaps better: if (origWidth == imageWidth && origHeight == imageHeight && !addHDLogo && !addLanguage) {
            skipResize = true;
        }

        // fit image to size
        if (profile.isImageNormalize()) {
            if (skipResize) {
                bi = GraphicTools.scaleToSizeNormalized((int) (origHeight * profile.getRounderCornerQuality() * ratio), (int) (origHeight * rcqFactor), bi);
            } else {
                bi = GraphicTools.scaleToSizeNormalized((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);
            }
        } else if (profile.isImageStretch()) {
            bi = GraphicTools.scaleToSizeStretch((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);

        } else if (!skipResize) {
            bi = GraphicTools.scaleToSize((int) (profile.getWidth() * rcqFactor), (int) (profile.getHeight() * rcqFactor), bi);
        }

        // return image
        return bi;
    }

    private String buildCacheFilename(ArtworkLocated located) {
        return buildCacheFilename(located, null);
    }

    private String buildCacheFilename(ArtworkLocated located, ArtworkProfile artworkProfile) {
        StringBuilder sb = new StringBuilder();
        
        // 1. video name
        if (located.getArtwork().getVideoData() != null) {
            sb.append(located.getArtwork().getVideoData().getIdentifier());
            if (located.getArtwork().getVideoData().isMovie()) {
                sb.append(".movie.");
            } else {
                sb.append(".episode.");
            }
        } else if (located.getArtwork().getSeason() != null) {
            sb.append(located.getArtwork().getSeason().getIdentifier());
            sb.append(".season.");
        } else if (located.getArtwork().getSeries() != null) {
            sb.append(located.getArtwork().getSeries().getIdentifier());
            sb.append(".series.");
        } else if (located.getArtwork().getPerson() != null) {
        	sb.append(FileTools.makeSafeFilename(located.getArtwork().getPerson().getName()));
            sb.append(".person.");
        } else {
            sb.append("unknown_");
            sb.append(located.getArtwork().getId());
            sb.append(".");
        }
        
        // 2. artwork type
        sb.append(located.getArtwork().getArtworkType().toString().toLowerCase());
        sb.append(".");
        
        // 3. hash code
        if (StringUtils.isBlank(located.getHashCode())) {
            sb.append(located.getId());
        } else {
            sb.append(located.getHashCode());
        }
        sb.append(".");

        // 4. profile and suffix
        if (artworkProfile == null) {
            // it's the original image
            sb.append("original");
            // TODO determine suffix from URL or stage file name
            sb.append(".jpg");
        } else {
            // it's a generated image
            sb.append(artworkProfile.getProfileName().toLowerCase());
            if (ImageFormat.PNG == artworkProfile.getImageFormat()) {
                sb.append(".png");
            } else {
                sb.append(".jpg");
            }
        }
        
        return sb.toString();
    }

    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        artworkStorageService.errorArtworkLocated(queueElement.getId());
    }

    private boolean checkArtworkQuality(ArtworkLocated located) {
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

            // TODO: check quality of artwork?
        } else {
            // TODO: stage file needs no validation??
            LOG.trace("Located URL was blank for {}", located.toString());
        }

        return Boolean.TRUE;
    }
}
