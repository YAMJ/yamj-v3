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

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
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
import org.yamj.core.database.model.type.StepType;

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
            
            // process new video
            processNewVideo(stageFile);

            // attach NFO files
            attachNfoFilesToVideo(stageFile);

            // TODO attach images
        } else {
            LOG.info("Process updated video {}-'{}'", stageFile.getId(), stageFile.getFileName());
            
            // just update media file
            MediaFile mediaFile = stageFile.getMediaFile();
            mediaFile.setFileDate(stageFile.getFileDate());
            mediaFile.setFileSize(stageFile.getFileSize());
            mediaFile.setStatus(StatusType.UPDATED);
            mediaDao.updateEntity(mediaFile);
        }

        // mark stage file as done
        stageFile.setStatus(StatusType.DONE);
        stagingDao.updateEntity(stageFile);
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
            stageFile.setStatus(StatusType.DUPLICATE);
            mediaDao.updateEntity(mediaFile);
            return;
        }
        
        // new media file
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
                videoData.setStep(StepType.NFO);
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
                            series.setStep(StepType.NFO);
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
                        season.setStep(StepType.NFO);
                        
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
                    videoData.setStep(StepType.NFO);
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
    
    
    private void attachNfoFilesToVideo(StageFile stageFile) {
        if (stageFile.getMediaFile() == null) {
            return;
        }
        Set<VideoData> videoDatas = stageFile.getMediaFile().getVideoDatas();
        if (CollectionUtils.isEmpty(videoDatas)) {
            return;
        }

        Map<StageFile,Integer> nfoFiles = new HashMap<StageFile,Integer>();
        
        // case 1: NFO file has same base name in same directory
        StageFile foundNfoFile = this.stagingDao.findStageFile(FileType.NFO, stageFile.getBaseName(), stageFile.getStageDirectory());
        if (foundNfoFile != null) {
            nfoFiles.put(foundNfoFile, Integer.valueOf(1));
        }

        // TODO more cases

        if (MapUtils.isEmpty(nfoFiles)) {
            // no NFO files found
            return;
        }

        for (Entry<StageFile,Integer> entry : nfoFiles.entrySet()) {
            StageFile nfoFile = entry.getKey();
            int priority = entry.getValue().intValue();
            
            for (VideoData videoData : videoDatas) {
                LOG.debug("Found NFO {}-'{}' for video data '{}'", nfoFile.getId(), nfoFile.getFileName(), videoData.getIdentifier());

                NfoRelation nfoRelation = new NfoRelation();
                nfoRelation.setStageFile(nfoFile);
                nfoRelation.setVideoData(videoData);
                nfoRelation.setPriority(priority);

                if (!videoData.getNfoRelations().contains(nfoRelation)) {
                    videoData.addNfoRelation(nfoRelation);
                    nfoFile.addNfoRelation(nfoRelation);
                    this.mediaDao.saveEntity(nfoRelation);
                    
                    LOG.debug("Stored new NFO relation: stageFile={}, videoData={}",
                                    nfoRelation.getStageFile().getId(),
                                    nfoRelation.getVideoData().getId());
                }
            }
        }
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
        if (StatusType.NEW.equals(stageFile.getStatus())) {
            LOG.info("Process new nfo {}-'{}'", stageFile.getId(), stageFile.getFileName());
            
            // process new NFO
            processNewNFO(stageFile);
        } else {
            LOG.info("Process updated nfo {}-'{}'", stageFile.getId(), stageFile.getFileName());
        }

        // mark stage file as done
        stageFile.setStatus(StatusType.DONE);
        stagingDao.updateEntity(stageFile);
        
        // update meta-data for NFO scan
        for (NfoRelation nfoRelation : stageFile.getNfoRelations()) {
            VideoData videoData = nfoRelation.getVideoData();
            if (videoData.isMovie()) {
                videoData.setStatus(StatusType.UPDATED);
                videoData.setStep(StepType.NFO);
                stagingDao.updateEntity(videoData);
                
                LOG.debug("Marked movie {}-'{}' for NFO scan", videoData.getId(), videoData.getTitle());
            } else {
                Series series = videoData.getSeason().getSeries();
                series.setStatus(StatusType.UPDATED);
                series.setStep(StepType.NFO);
                stagingDao.updateEntity(series);
                
                LOG.debug("Marked series {}-'{}' for NFO scan", series.getId(), series.getTitle());
            }
        }
    }

    private void processNewNFO(StageFile stageFile) {
        Map<VideoData,Integer> videoFiles = new HashMap<VideoData,Integer>();

        // case 1: Video file has same base name in same directory
        List<VideoData> videoDatas = this.stagingDao.findVideoDatasForNFO(stageFile.getBaseName(), stageFile.getStageDirectory());
        for (VideoData videoData : videoDatas) {
            videoFiles.put(videoData, Integer.valueOf(1));
        }
        
        // TODO more cases

        if (MapUtils.isEmpty(videoFiles)) {
            // no NFO files found
            return;
        }

        for (Entry<VideoData,Integer> entry : videoFiles.entrySet()) {
            VideoData videoData = entry.getKey();
            LOG.debug("Found video data {}-'{}' for nfo file '{}'", videoData.getId(), videoData.getIdentifier(), stageFile.getFileName());

            NfoRelation nfoRelation = new NfoRelation();
            nfoRelation.setStageFile(stageFile);
            nfoRelation.setVideoData(videoData);
            nfoRelation.setPriority(entry.getValue().intValue());

            if (!stageFile.getNfoRelations().contains(nfoRelation)) {
                stageFile.addNfoRelation(nfoRelation);
                videoData.addNfoRelation(nfoRelation);
                this.mediaDao.saveEntity(nfoRelation);

                LOG.debug("Stored new NFO relation: stageFile={}, videoData={}",
                                nfoRelation.getStageFile().getId(),
                                nfoRelation.getVideoData().getId());
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
