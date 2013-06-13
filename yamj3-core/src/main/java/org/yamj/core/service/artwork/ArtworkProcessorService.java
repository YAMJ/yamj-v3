/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.artwork;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
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
import org.yamj.core.database.model.type.ImageFormat;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;
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
            // nothing to
            return;
        }
        
        ArtworkLocated located = artworkStorageService.getRequiredArtworkLocated(queueElement.getId());
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

        try {
            if (located.getStageFile() != null) {
                // TODO implement storage of artwork from stage file
                throw new RuntimeException("Artwork storage of stage file not implemented");
            } else {
                URL url = new URL(located.getUrl());
                fileStorageService.store(StorageType.ARTWORK, cacheFilename, url);
            }
        } catch (Exception error) {
            LOG.error("Failed to store artwork store artwork in file cache: {}", cacheFilename);
            LOG.warn("Storage error", error);
            return;
        }

        // set values in located artwork
        located.setCacheFilename(cacheFilename);
        located.setStatus(StatusType.DONE);
        artworkStorageService.updateArtworkLocated(located);
        
        // after that: try preProcessing of images
        List<ArtworkProfile> profiles = artworkStorageService.getPreProcessArtworkProfiles(located);
        for (ArtworkProfile profile : profiles) {
            try {
                // generate image for a profiles
                generateImage(located, profile);
            } catch (ImageReadException error) {
                LOG.error("Original image for {} is invalid", located);
                LOG.warn("Image generation error", error);
                
                // mark located artwork as invalid
                try {
                    located.setStatus(StatusType.INVALID);
                    artworkStorageService.updateArtworkLocated(located);
                } catch (Exception ex) {
                    // just log a warning
                    LOG.warn("Failed to mark artwork as invalid: " + located, ex);
                }
                
                // no further processing for that located image
                break;
            } catch (Exception error) {
                LOG.error("Failed to generate image for {} with profile {}", located, profile.getProfileName());
                LOG.warn("Image generation error", error);
            }
        }
    }

    private void generateImage(ArtworkLocated located, ArtworkProfile profile) throws Exception {
        LOG.debug("Generate image for {} with profile {}", located, profile.getProfileName());
        BufferedImage imageGraphic = GraphicTools.loadJPEGImage(this.fileStorageService.getFile(StorageType.ARTWORK, located.getCacheFilename()));
        
        // draw the image
        BufferedImage image = drawImage(imageGraphic, profile);

        // store image on stage system
        String cacheFilename = buildCacheFilename(located, profile);
        fileStorageService.storeArtwork(cacheFilename, image, profile.getImageFormat(), profile.getQuality());

        try {
            ArtworkGenerated generated = new ArtworkGenerated();
            generated.setArtworkLocated(located);
            generated.setArtworkProfile(profile);
            generated.setCacheFilename(cacheFilename);
            artworkStorageService.storeArtworkGenerated(generated);
        } catch (Exception error) {
            // delete generated file storage element also
            try {
                fileStorageService.delete(StorageType.ARTWORK, cacheFilename);
            } catch (Exception ignore) {}
            throw error;
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
        if (located.getArtwork().getVideoData() != null) {
            sb.append(located.getArtwork().getVideoData().getIdentifier());
            sb.append(".video.");
        } else if (located.getArtwork().getSeason() != null) {
            sb.append(located.getArtwork().getSeason().getIdentifier());
            sb.append(".season.");
        } else if (located.getArtwork().getSeries() != null) {
            sb.append(located.getArtwork().getSeries().getIdentifier());
            sb.append(".series.");
        } else {
            sb.append("unknown_");
            sb.append(located.getArtwork().getId());
            sb.append(".");
        }
        sb.append(located.getArtwork().getArtworkType().toString().toLowerCase());
        sb.append(".");
        sb.append(located.getId());
        sb.append(".");
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
    
    @SuppressWarnings("rawtypes")
    private boolean checkArtworkQuality(ArtworkLocated located) {
        if (StringUtils.isNotBlank(located.getUrl())) {
            Iterator readers = ImageIO.getImageReadersBySuffix("jpeg");
            ImageReader reader = (ImageReader) readers.next();
            int urlWidth;
            int urlHeight;
            try {
                URL url = new URL(located.getUrl());
                InputStream in = url.openStream();
                ImageInputStream iis = ImageIO.createImageInputStream(in);
                reader.setInput(iis, Boolean.TRUE);
                urlWidth = reader.getWidth(0);
                urlHeight = reader.getHeight(0);
            } catch (IOException error) {
                LOG.warn("Could not determine dimension of artwork: {}", located.getUrl());
                LOG.debug("Graphics error", error);
                return Boolean.FALSE;
            }

            // set values for later usage
            located.setWidth(urlWidth);
            located.setHeight(urlHeight);

            // TODO: check quality of artwork?
    
            
            /*
            float urlAspect = (float) urlWidth / (float) urlHeight;
            if (urlAspect > 1.0) {
                LOG.info("{} rejected: URL is wrong aspect (portrait/landscape)", located);
                return Boolean.FALSE;
            }

            // Adjust artwork width / height by the ValidateMatch figure
            int newArtworkWidth = artworkWidth * (artworkValidateMatch / 100);
            int newArtworkHeight = artworkHeight * (artworkValidateMatch / 100);
    
            if (urlWidth < newArtworkWidth) {
                logger.debug(LOG_MESSAGE + artworkImage + " rejected: URL width (" + urlWidth + ") is smaller than artwork width (" + newArtworkWidth + ")");
                return Boolean.FALSE;
            }
    
            if (urlHeight < newArtworkHeight) {
                logger.debug(LOG_MESSAGE + artworkImage + " rejected: URL height (" + urlHeight + ") is smaller than artwork height (" + newArtworkHeight + ")");
                return Boolean.FALSE;
            }
            */
        } else {
            // TODO: stage file needs no validation??
        }
        
        return Boolean.TRUE;
    }
}
