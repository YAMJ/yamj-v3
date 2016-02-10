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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.ArtworkGenerated;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.scheduling.IQueueProcessService;
import org.yamj.core.service.file.StorageType;

@Service("artworkGeneratedProcessorService")
@DependsOn("artworkInitialization")
public class ArtworkGeneratedProcessorService implements IQueueProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkGeneratedProcessorService.class);
    
    @Autowired
    private ArtworkStorageService artworkStorageService;

    @Override
    public void processQueueElement(QueueDTO queueElement) {
        // get required generated artwork
        ArtworkGenerated generated = artworkStorageService.getRequiredArtworkGenerated(queueElement.getId());
        final StorageType storageType = generated.getArtworkLocated().getArtwork().getStorageType();
        LOG.info("Process generated artwork: {}", generated);

        // TODO
    }

    @Override
    public void processErrorOccurred(QueueDTO queueElement, Exception error) {
        LOG.error("Failed processing of generated artwork "+queueElement.getId(), error);
        
        // TODO
    }
}