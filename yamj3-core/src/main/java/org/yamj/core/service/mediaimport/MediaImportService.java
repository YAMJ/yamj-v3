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
package org.yamj.core.service.mediaimport;

import org.hibernate.Hibernate;

import org.yamj.core.database.model.type.StepType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.dao.MediaDao;
import org.yamj.core.database.dao.MetadataDao;
import org.yamj.core.database.dao.StagingDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.FileType;

/**
 * The media import service is a spring-managed service. This will be used by the MediaImportRunner only in order to access other
 * spring beans cause the MediaImportRunner itself is no spring-managed bean and dependency injection will fail on that runner.
 *
 */
@Service("mediaImportService")
public class MediaImportService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaImportService.class);
    private static final String MEDIA_SOURCE = "filename";
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private MetadataDao metadataDao;
    @Autowired
    private FilenameScanner filenameScanner;

    @Transactional(readOnly = true)
    public Long getNextStageFileId(final FileType fileType, final StatusType... statusTypes) {
        return this.stagingDao.getNextStageFileId(fileType, statusTypes);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processVideo(long id) {
        StageFile stageFile = stagingDao.getStageFile(id);
        if (stageFile.getMediaFile() == null) {
            LOG.info("Process new video {}-'{}'", stageFile.getId(), stageFile.getFileName());
            processNewVideo(stageFile);
        } else {
            // just update media file
            LOG.info("Process updated video {}-'{}'", stageFile.getId(), stageFile.getFileName());
            processUpdatedMediaFile(stageFile);
        }
    }

    private void processNewVideo(StageFile stageFile) {
        // scan filename for informations
        FilenameDTO dto = new FilenameDTO(stageFile);
        filenameScanner.scan(dto);
        LOG.debug("Scanned filename {}-'{}': title='{}', year={}",
                stageFile.getId(), stageFile.getFileName(), dto.getTitle(), dto.getYear());

        if (StringUtils.isBlank(dto.getTitle()) && (dto.getYear() > 0)) {
            if (dto.getYear() > 0) {
                LOG.warn("No valid title scanned from '{}', year will be used as title", stageFile.getFileName());
                dto.setTitle(String.valueOf(dto.getYear()));
                dto.setYear(-1);
            } else {
                LOG.error("No valid title and year could be scanned from filename '{}'", stageFile.getFileName());
                stageFile.setStatus(StatusType.ERROR);
                mediaDao.updateEntity(stageFile);
                return;
            }
        }

        // MEDIA FILE

        MediaFile mediaFile = mediaDao.getMediaFile(stageFile.getFileName());
        if (mediaFile != null) {
            LOG.warn("Media file for '{}' already present for new stage file", stageFile.getFileName());

            mediaFile.addStageFile(stageFile);
            stageFile.setMediaFile(mediaFile);
            mediaDao.updateEntity(mediaFile);

            // mark as duplicate and return
            stageFile.setStatus(StatusType.DUPLICATE);
            stagingDao.updateEntity(stageFile);

            return;
        }

        // NEW media file
        // fill in scanned values
        mediaFile = new MediaFile();
        mediaFile.setFileName(stageFile.getFileName());
        mediaFile.setFileDate(stageFile.getFileDate());
        mediaFile.setFileSize(stageFile.getFileSize());
        mediaFile.setContainer(dto.getContainer());
        mediaFile.setExtra(dto.isExtra());
        mediaFile.setPart(dto.getPart());
        mediaFile.setPartTitle(dto.getPartTitle());
        mediaFile.setMovieVersion(dto.getMovieVersion());
        mediaFile.setFps(dto.getFps());
        mediaFile.setCodec(dto.getVideoCodec());
        mediaFile.setVideoSource(dto.getVideoSource());
        mediaFile.setEpisodeCount(dto.getEpisodes().size());
        mediaFile.setStatus(StatusType.NEW);
        mediaFile.addStageFile(stageFile);
        stageFile.setMediaFile(mediaFile);

        LOG.debug("Store new media file: '{}'", mediaFile.getFileName());
        mediaDao.saveEntity(mediaFile);

        stageFile.setStatus(StatusType.DONE);
        stagingDao.updateEntity(stageFile);

        // METADATA OBJECTS

        if (dto.isMovie()) {
            // VIDEO DATA for movies

            String identifier = dto.buildIdentifier();
            VideoData videoData = metadataDao.getVideoData(identifier);
            if (videoData == null) {

                // NEW video data
                videoData = new VideoData();
                videoData.setIdentifier(identifier);
                videoData.setSourceDbIdMap(dto.getIdMap());
                videoData.setTitle(dto.getTitle(), MEDIA_SOURCE);
                videoData.setTitleOriginal(dto.getTitle(), MEDIA_SOURCE);
                videoData.setPublicationYear(dto.getYear(), MEDIA_SOURCE);
                videoData.setStatus(StatusType.NEW);
                videoData.setStep(StepType.ONLINE);
                mediaFile.addVideoData(videoData);
                videoData.addMediaFile(mediaFile);

                LOG.debug("Store new movie: '{}' - {}", videoData.getTitle(), videoData.getPublicationYear());
                metadataDao.saveEntity(videoData);

                // create new poster artwork entry
                Artwork poster = new Artwork();
                poster.setArtworkType(ArtworkType.POSTER);
                poster.setStatus(StatusType.NEW);
                poster.setVideoData(videoData);
                metadataDao.saveEntity(poster);

                // create new fanart artwork entry
                Artwork fanart = new Artwork();
                fanart.setArtworkType(ArtworkType.FANART);
                fanart.setStatus(StatusType.NEW);
                fanart.setVideoData(videoData);
                metadataDao.saveEntity(fanart);

            } else {
                mediaFile.addVideoData(videoData);
                videoData.addMediaFile(mediaFile);
                metadataDao.updateEntity(videoData);
            }
        } else {
            // VIDEO DATA for episodes
            for (Integer episode : dto.getEpisodes()) {
                String identifier = dto.buildEpisodeIdentifier(episode);
                VideoData videoData = metadataDao.getVideoData(identifier);
                if (videoData == null) {
                    // NEW video data

                    // getById or create season
                    String seasonIdentifier = dto.buildSeasonIdentifier();
                    Season season = metadataDao.getSeason(seasonIdentifier);
                    if (season == null) {

                        // getById or create series
                        String seriesIdentifier = dto.buildIdentifier();
                        Series series = metadataDao.getSeries(seriesIdentifier);
                        if (series == null) {
                            series = new Series();
                            series.setIdentifier(seriesIdentifier);
                            series.setTitle(dto.getTitle(), MEDIA_SOURCE);
                            series.setTitleOriginal(dto.getTitle(), MEDIA_SOURCE);
                            series.setSourceDbIdMap(dto.getIdMap());
                            series.setStatus(StatusType.NEW);
                            series.setStep(StepType.ONLINE);
                            LOG.debug("Store new series: '{}'", series.getTitle());
                            metadataDao.saveEntity(series);

                            // create new poster artwork entry
                            Artwork poster = new Artwork();
                            poster.setArtworkType(ArtworkType.POSTER);
                            poster.setStatus(StatusType.NEW);
                            poster.setSeries(series);
                            metadataDao.saveEntity(poster);

                            // create new fanart artwork entry
                            Artwork fanart = new Artwork();
                            fanart.setArtworkType(ArtworkType.FANART);
                            fanart.setStatus(StatusType.NEW);
                            fanart.setSeries(series);
                            metadataDao.saveEntity(fanart);

                            // create new banner artwork entry
                            Artwork banner = new Artwork();
                            banner.setArtworkType(ArtworkType.BANNER);
                            banner.setStatus(StatusType.NEW);
                            banner.setSeries(series);
                            metadataDao.saveEntity(banner);
                        }

                        season = new Season();
                        season.setIdentifier(seasonIdentifier);
                        season.setSeason(dto.getSeason());
                        season.setTitle(dto.getTitle(), MEDIA_SOURCE);
                        season.setTitleOriginal(dto.getTitle(), MEDIA_SOURCE);
                        season.setSeries(series);
                        season.setStatus(StatusType.NEW);
                        season.setStep(StepType.ONLINE);
                        
                        LOG.debug("Store new seaon: '{}' - Season {}", season.getTitle(), season.getSeason());
                        metadataDao.saveEntity(season);

                        // create new poster artwork entry
                        Artwork poster = new Artwork();
                        poster.setArtworkType(ArtworkType.POSTER);
                        poster.setStatus(StatusType.NEW);
                        poster.setSeason(season);
                        metadataDao.saveEntity(poster);

                        // create new fanart artwork entry
                        Artwork fanart = new Artwork();
                        fanart.setArtworkType(ArtworkType.FANART);
                        fanart.setStatus(StatusType.NEW);
                        fanart.setSeason(season);
                        metadataDao.saveEntity(fanart);

                        // create new banner artwork entry
                        Artwork banner = new Artwork();
                        banner.setArtworkType(ArtworkType.BANNER);
                        banner.setStatus(StatusType.NEW);
                        banner.setSeason(season);
                        metadataDao.saveEntity(banner);
                    }

                    videoData = new VideoData();
                    videoData.setIdentifier(identifier);
                    if (StringUtils.isNotBlank(dto.getEpisodeTitle())) {
                        videoData.setTitle(dto.getEpisodeTitle(), MEDIA_SOURCE);
                        videoData.setTitleOriginal(dto.getEpisodeTitle(), MEDIA_SOURCE);
                    } else {
                        videoData.setTitle(dto.getTitle(), MEDIA_SOURCE);
                        videoData.setTitleOriginal(dto.getTitle(), MEDIA_SOURCE);
                    }
                    videoData.setStatus(StatusType.NEW);
                    videoData.setStep(StepType.ONLINE);
                    videoData.setSeason(season);
                    videoData.setEpisode(episode);
                    mediaFile.addVideoData(videoData);
                    videoData.addMediaFile(mediaFile);

                    LOG.debug("Store new episode: '{}' - Season {} - Episode {}", season.getTitle(), season.getSeason(), videoData.getEpisode());
                    metadataDao.saveEntity(videoData);

                    // create new videoimage artwork entry
                    Artwork videoimage = new Artwork();
                    videoimage.setArtworkType(ArtworkType.VIDEOIMAGE);
                    videoimage.setStatus(StatusType.NEW);
                    videoimage.setVideoData(videoData);
                    metadataDao.saveEntity(videoimage);

                } else {
                    mediaFile.addVideoData(videoData);
                    videoData.addMediaFile(mediaFile);
                    metadataDao.updateEntity(videoData);
                }
            }
        }
    }

    private void processUpdatedMediaFile(StageFile stageFile) {
        MediaFile mediaFile = stageFile.getMediaFile();
        mediaFile.setFileDate(stageFile.getFileDate());
        mediaFile.setFileSize(stageFile.getFileSize());
        mediaFile.setStatus(StatusType.UPDATED);
        mediaDao.updateEntity(mediaFile);

        stageFile.setStatus(StatusType.DONE);
        stagingDao.updateEntity(stageFile);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processingError(Long id) {
        if (id == null) {
            return;
        }

        StageFile stageFile = stagingDao.getStageFile(id);
        if (stageFile != null) {
            stageFile.setStatus(StatusType.ERROR);
            stagingDao.updateEntity(stageFile);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processNfo(long id) {
        StageFile stageFile = stagingDao.getStageFile(id);
        if (stageFile.getMediaFile() == null) {
            LOG.info("Process new nfo {}-'{}'", stageFile.getId(), stageFile.getFileName());
            processNewNFO(stageFile);
        } else {
            // just update media file
            LOG.info("Process updated video {}-'{}'", stageFile.getId(), stageFile.getFileName());
            processUpdatedNFO(stageFile);
        }
    }

    private void processNewNFO(StageFile stageFile) {
        
        // case 1: search in same directory with same name
        MediaFile mediaFile = this.stagingDao.findMediaFile(FileType.VIDEO, stageFile.getBaseName(), stageFile.getStageDirectory());
        if (mediaFile != null) {
            // update meta-data for NFO scan
            this.updateNFOScan(mediaFile);
            
            // add stage file to media
            mediaFile.addStageFile(stageFile);
            stagingDao.updateEntity(mediaFile);
            
            // update stage file
            stageFile.setMediaFile(mediaFile);
            stageFile.setStatus(StatusType.DONE);
            stagingDao.updateEntity(stageFile);
            
            return;
        }
        
        // TODO more cases
        
        stageFile.setStatus(StatusType.INVALID);
        stagingDao.updateEntity(stageFile);
    }

    private void processUpdatedNFO(StageFile stageFile) {
        // update meta-data for NFO scan
        this.updateNFOScan(stageFile.getMediaFile());
        
        stageFile.setStatus(StatusType.DONE);
        stagingDao.updateEntity(stageFile);
    }

    private void updateNFOScan(MediaFile mediaFile) {
        for (VideoData videoData: mediaFile.getVideoDatas()) {
            if (videoData.isMovie()) {
                // mark video data for NFO scan
                videoData.setStatus(StatusType.UPDATED);
                videoData.setStep(StepType.NFO);
                stagingDao.updateEntity(videoData);
            } else {
                Series series = videoData.getSeason().getSeries();
                // mark series data for NFO scan
                series.setStatus(StatusType.UPDATED);
                series.setStep(StepType.NFO);
                stagingDao.updateEntity(series);
            }
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void processImage(long id) {
        StageFile stageFile = stagingDao.getStageFile(id);

        if (stageFile.getBaseName().equalsIgnoreCase("poster")
                || stageFile.getBaseName().equalsIgnoreCase("cover")
                || stageFile.getBaseName().equalsIgnoreCase("folder")) {
            // TODO apply poster to all video files in that directory
            LOG.trace("Generic poster found: {}", stageFile.getBaseName());
        } else if (StringUtils.endsWithIgnoreCase(stageFile.getBaseName(), ".poster")
                || StringUtils.endsWithIgnoreCase(stageFile.getBaseName(), "-poster")) {
            // TODO apply poster to single video
            LOG.trace("Poster found: {}", stageFile.getBaseName());
        } else if (stageFile.getBaseName().equalsIgnoreCase("fanart")
                || stageFile.getBaseName().equalsIgnoreCase("backdrop")
                || stageFile.getBaseName().equalsIgnoreCase("background")) {
            // TODO apply fanart to all video files in that directory
            LOG.trace("Generic fanart found: {}", stageFile.getBaseName());
        } else if (StringUtils.endsWithIgnoreCase(stageFile.getBaseName(), ".fanart")
                || StringUtils.endsWithIgnoreCase(stageFile.getBaseName(), "-fanart")) {
            // TODO apply fanart to single video
            LOG.trace("Fanart found: {}", stageFile.getBaseName());
        }
    }
}
