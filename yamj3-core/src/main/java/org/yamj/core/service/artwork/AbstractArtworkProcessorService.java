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

import java.awt.image.BufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.ArtworkProfile;
import org.yamj.core.database.model.type.ScalingType;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.scheduling.IQueueProcessService;
import org.yamj.core.service.file.FileStorageService;
import org.yamj.core.service.file.StorageType;
import org.yamj.core.tools.image.GraphicTools;

public abstract class AbstractArtworkProcessorService implements IQueueProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkGeneratedProcessorService.class);

    @Autowired
    protected ArtworkStorageService artworkStorageService;
    @Autowired
    protected FileStorageService fileStorageService;

    protected void createAndStoreImage(ArtworkLocated located, ArtworkProfile profile, String cacheFilename) throws Exception {
        final StorageType storageType = ArtworkTools.getStorageType(profile);
        
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