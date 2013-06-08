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
package org.yamj.core.service.mediaimport;

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
        if (StatusType.NEW.equals(stageFile.getStatus())) {
            LOG.info("Process new video {}-'{}'", stageFile.getId(), stageFile.getFileName());
            processNewVideo(stageFile);
        } else {
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
        
        // MEDIA FILE
        
        MediaFile mediaFile = mediaDao.getMediaFile(stageFile.getFileName());
        if (mediaFile == null) {
            // NEW media file
            // fill in scanned values
            mediaFile = new MediaFile();
            mediaFile.setFileName(stageFile.getFileName());
            mediaFile.setFileDate(stageFile.getFileDate());
            mediaFile.setFileSize(stageFile.getFileSize());
            mediaFile.setContainer(dto.getContainer());
            mediaFile.setPart(dto.getPart());
            mediaFile.setFps(dto.getFps());
            mediaFile.setCodec(dto.getVideoCodec());
            mediaFile.setVideoSource(dto.getVideoSource());
            mediaFile.setStatus(StatusType.NEW);
            mediaFile.addStageFile(stageFile);
            stageFile.setMediaFile(mediaFile);

            LOG.debug("Store new media file: '{}'", mediaFile.getFileName());
            mediaDao.saveEntity(mediaFile);
        } else {
            LOG.warn("Media file for '{}' already present for new stage file", stageFile.getFileName());
            //  Possible reasons for already existing mediaFile:
            // - Other Library?
            // - Stored in another directory?
        }

        // METADATA OBJECTS
        
        if (dto.isMovie()) {
            // VIDEO DATA for movies

            String identifier = dto.buildIdentifier();
            VideoData videoData = metadataDao.getVideoData(identifier);
            if (videoData == null) {

                // NEW video data
                videoData = new VideoData();
                videoData.setIdentifier(identifier);
                videoData.setMoviedbIdMap(dto.getIdMap());
                videoData.setTitle(dto.getTitle(), MEDIA_SOURCE);
                videoData.setTitleOriginal(dto.getTitle(), MEDIA_SOURCE);
                videoData.setPublicationYear(dto.getYear(), MEDIA_SOURCE);
                videoData.setStatus(StatusType.NEW);
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
                            series.setMoviedbIdMap(dto.getIdMap());
                            series.setStatus(StatusType.NEW);

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
                        season.setSeries(series);
                        season.setStatus(StatusType.NEW);

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

        // TODO
        // - create associations to NFOs
        // - create local artwork entries
        // - create set entries

        stageFile.setStatus(StatusType.DONE);
        stagingDao.updateEntity(stageFile);
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
}
