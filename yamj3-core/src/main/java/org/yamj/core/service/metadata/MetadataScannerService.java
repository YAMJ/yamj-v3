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

import org.yamj.core.config.ConfigServiceWrapper;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.service.metadata.nfo.NfoScannerService;
import org.yamj.core.service.metadata.online.OnlineScannerService;
import org.yamj.core.tools.ExceptionTools;
import org.yamj.core.tools.MetadataTools;

@Service("metadataScannerService")
public class MetadataScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataScannerService.class);

    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private NfoScannerService nfoScannerService;
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;

    /**
     * Scan a movie
     *
     * @param id
     */
    public void scanMovie(Long id) {
        VideoData videoData = this.metadataStorageService.getRequiredVideoData(id);

        // empty sort title; will be reset after scan
        videoData.setTitleSort(null);

        // NFO scanning
        this.nfoScannerService.scanMovie(videoData);

        // online scanning
        this.onlineScannerService.scanMovie(videoData);

        // reset sort title
        MetadataTools.setSortTitle(videoData, configServiceWrapper.getSortStripPrefixes());

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
    public void scanSeries(Long id) {
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


        // reset sort title
        List<String> prefixes = this.configServiceWrapper.getSortStripPrefixes();
        MetadataTools.setSortTitle(series, prefixes);
        for (Season season : series.getSeasons()) {
            MetadataTools.setSortTitle(season, prefixes);
            for (VideoData videoData : season.getVideoDatas()) {
                MetadataTools.setSortTitle(videoData, prefixes);
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
    public void scanPerson(Long id) {
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

    public boolean isFilmographyScanEnabled() {
        return this.onlineScannerService.isFilmographyScanEnabled();
    }

    /**
     * Scan the data site for information on the person
     *
     * @param id
     */
    public void scanFilmography(Long id) {
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

    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        if (queueElement.isMetadataType(MetaDataType.MOVIE)) {
            metadataStorageService.errorVideoData(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.SERIES)) {
            metadataStorageService.errorSeries(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.PERSON)) {
            metadataStorageService.errorPerson(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.FILMOGRAPHY)) {
            metadataStorageService.errorFilmography(queueElement.getId());
        }
    }
}
