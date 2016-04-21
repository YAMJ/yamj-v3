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
package org.yamj.core.service.metadata;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.scheduling.IQueueProcessService;
import org.yamj.core.service.metadata.nfo.NfoScannerService;
import org.yamj.core.service.metadata.online.OnlineScannerService;
import org.yamj.core.tools.ExceptionTools;
import org.yamj.core.tools.YamjTools;

@Service("metadataScannerService")
public class MetadataScannerService implements IQueueProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataScannerService.class);

    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private NfoScannerService nfoScannerService;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ExtraScannerService extraScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;

    @Override
    public void processQueueElement(QueueDTO queueElement) {
        if (queueElement.getId() == null) {
            // nothing to do
        } else if (queueElement.isMetadataType(MetaDataType.MOVIE)) {
            scanMovie(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.SERIES)) {
            scanSeries(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.PERSON)) {
            scanPerson(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.FILMOGRAPHY)) {
            scanFilmography(queueElement.getId());
        }
    }
    
    /**
     * Scan a movie
     *
     * @param id
     */
    private void scanMovie(Long id) {
        VideoData videoData = this.metadataStorageService.getRequiredVideoData(id);

        // empty sort title; will be reset after scan
        videoData.setTitleSort(null);

        // NFO scanning
        this.nfoScannerService.scanMovie(videoData);

        // online scanning
        this.onlineScannerService.scanMovie(videoData);

        // extra scanning
        this.extraScannerService.scanMovie(videoData);

        // reset sort title
        YamjTools.setSortTitle(videoData, configServiceWrapper.getSortStripPrefixes());

        try {
            // store associated entities
            metadataStorageService.storeAssociatedEntities(videoData);

            // update meta data in one transaction
            metadataStorageService.updateScannedMetaData(videoData);

            LOG.debug("Updated movie in database: {}-'{}'", id, videoData.getTitle());
        } catch (Exception error) {
            // NOTE: status will not be changed
            if (ExceptionTools.isLockingError(error)) {
                LOG.warn("Locking error while storing movie {}-'{}'", id, videoData.getTitle());
            } else {
                LOG.error("Failed storing movie {}-'{}'", id, videoData.getTitle());
                LOG.error("Storage error", error);
            }
        }
    }

    /**
     * Scan a TV Series
     *
     * @param id
     */
    private void scanSeries(Long id) {
        Series series = this.metadataStorageService.getRequiredSeries(id);

        // empty sort title; will be reset after scan
        series.setTitleSort(null);
        for (Season season : series.getSeasons()) {
            season.setTitleSort(null);
            for (VideoData videoData : season.getVideoDatas()) {
                videoData.setTitleSort(null);
            }
        }

        // NFO scanning
        this.nfoScannerService.scanSeries(series);

        // online scanning
        this.onlineScannerService.scanSeries(series);

        // extra scanning
        this.extraScannerService.scanSeries(series);

        // reset sort title
        List<String> prefixes = this.configServiceWrapper.getSortStripPrefixes();
        YamjTools.setSortTitle(series, prefixes);
        for (Season season : series.getSeasons()) {
            YamjTools.setSortTitle(season, prefixes);
            for (VideoData videoData : season.getVideoDatas()) {
                YamjTools.setSortTitle(videoData, prefixes);
            }
        }

        try {
            // store associated entities
            metadataStorageService.storeAssociatedEntities(series);

            // update meta data in one transaction
            metadataStorageService.updateScannedMetaData(series);

            LOG.debug("Updated series in database: {}-'{}'", id, series.getTitle());
        } catch (Exception error) {
            // NOTE: status will not be changed
            if (ExceptionTools.isLockingError(error)) {
                LOG.warn("Locking error while storing series {}-'{}'", id, series.getTitle());
            } else {
                LOG.error("Failed storing series {}-'{}'", id, series.getTitle());
                LOG.error("Storage error", error);
            }
        }
    }

    /**
     * Scan the data site for information on the person
     *
     * @param id
     */
    private void scanPerson(Long id) {
        Person person = metadataStorageService.getRequiredPerson(id);

        // online scanning (only)
        this.onlineScannerService.scanPerson(person);

        try {
            // update person in one transaction
            metadataStorageService.updateScannedPerson(person);

            LOG.debug("Updated person in database: {}-'{}'", id, person.getName());
        } catch (Exception error) {
            // NOTE: status will not be changed
            if (ExceptionTools.isLockingError(error)) {
                LOG.warn("Locking error while storing person {}-'{}'", id, person.getName());
            } else {
                LOG.error("Failed storing person {}-'{}'", id, person.getName());
                LOG.error("Storage error", error);
            }
        }
    }

    /**
     * Scan the data site for information on the person
     *
     * @param id
     */
    private void scanFilmography(Long id) {
        Person person = metadataStorageService.getRequiredPerson(id);

        // online scanning (only)
        this.onlineScannerService.scanFilmography(person);

        try {
            // update person in one transaction
            metadataStorageService.updateScannedPersonFilmography(person);

            LOG.debug("Updated person filmography in database: {}-'{}'", id, person.getName());
        } catch (Exception error) {
            // NOTE: status will not be changed
            if (ExceptionTools.isLockingError(error)) {
                LOG.warn("Locking error while storing person filmography {}-'{}'", id, person.getName());
            } else {
                LOG.error("Failed storing person filmography {}-'{}'", id, person.getName());
                LOG.error("Storage error", error);
            }
        }
    }

    @Override
    public void processErrorOccurred(QueueDTO queueElement, Exception error) {
        if (queueElement.getId() == null) {
            // nothing to do
        } else if (queueElement.isMetadataType(MetaDataType.MOVIE)) {
            LOG.error("Failed scan for movie "+queueElement.getId(), error);
            metadataStorageService.errorVideoData(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.SERIES)) {
            LOG.error("Failed scan for series "+queueElement.getId(), error);
            metadataStorageService.errorSeries(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.PERSON)) {
            LOG.error("Failed scan for person "+queueElement.getId(), error);
            metadataStorageService.errorPerson(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.FILMOGRAPHY)) {
            LOG.error("Failed scan for filmography of person "+queueElement.getId(), error);
            metadataStorageService.errorFilmography(queueElement.getId());
        }
    }
}
