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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.ArtworkGenerated;
import org.yamj.core.database.model.dto.QueueDTO;

@Service("artworkGeneratedProcessorService")
@DependsOn("artworkInitialization")
public class ArtworkGeneratedProcessorService extends AbstractArtworkProcessorService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkGeneratedProcessorService.class);
    
    @Override
    public void processQueueElement(QueueDTO queueElement) {
        // get required generated artwork
        ArtworkGenerated generated = artworkStorageService.getRequiredArtworkGenerated(queueElement.getId());
        LOG.debug("Process generated artwork: {}", generated);

        try {
            // generate image
            createAndStoreImage(generated.getArtworkLocated(), generated.getArtworkProfile(), generated.getCacheFilename());

            // mark generated image as done
            generated.setStatus(StatusType.DONE);
        } catch (Exception ex) {
            LOG.error("Failed to generate image for {}", generated);
            LOG.warn("Image generation error", ex);

            // mark generated image as error
            generated.setStatus(StatusType.ERROR);
        }
        
        this.artworkStorageService.updateArtworkGenerated(generated);
    }

    @Override
    public void processErrorOccurred(QueueDTO queueElement, Exception error) {
        // nothing to to cause just the generated artwork may not exist
    }
}