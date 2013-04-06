package com.moviejukebox.core.service;

import com.moviejukebox.core.scanner.file.FilenameDTO;
import com.moviejukebox.core.scanner.file.FilenameScanner;

import com.moviejukebox.core.database.dao.MediaDao;
import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.*;
import com.moviejukebox.core.database.model.type.StatusType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The media import service is a spring-managed service.
 * This will be used by the MediaImportRunner only in order
 * to access other spring beans cause the MediaImportRunner
 * itself is no spring-managed bean and dependency injection
 * will fail on that runner.
 *
 */
@Service("mediaImportService")
public class MediaImportService {

    private static final String MEDIA_SOURCE = "filename";
    
    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private FilenameScanner filenameScanner;

    @Transactional(propagation = Propagation.REQUIRED)
    public void processVideo(long id) {
        StageFile stageFile = stagingDao.getStageFile(id);
        if (StatusType.NEW.equals(stageFile.getStatus())) {
            processNewVideo(stageFile);
        } else {
            processUpdatedVideo(stageFile);
        }
    }
    
    private void processNewVideo(StageFile stageFile) {
        // scan filename for informations
        FilenameDTO dto = new FilenameDTO(stageFile);
        filenameScanner.scan(dto);

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
            mediaDao.saveEntity(mediaFile);
        } else {
            //  Possible reasons for already existing mediaFile:
            // - Other Library?
            // - Extension changed?
            // - Stored in another directory?
        }

        if (dto.isMovie()) {
            // VIDEO DATA for movies
            
            String identifier = dto.buildIdentifier();
            VideoData videoData = mediaDao.getVideoData(identifier);
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
                mediaDao.saveEntity(videoData);
            } else {
                mediaFile.addVideoData(videoData);
                videoData.addMediaFile(mediaFile);
                mediaDao.updateEntity(videoData);
            }
        } else {
            // VIDEO DATA for episodes
            for (Integer episode : dto.getEpisodes()) {
                String identifier = dto.buildEpisodeIdentifier(episode);
                VideoData videoData = mediaDao.getVideoData(identifier);
                if (videoData == null) {
                    // NEW video data

                    // get or create season
                    String seasonIdentifier = dto.buildSeasonIdentifier();
                    Season season = mediaDao.getSeason(seasonIdentifier);
                    if (season == null) {
                        
                        // get or create series
                        String seriesIdentifier = dto.buildIdentifier();
                        Series series = mediaDao.getSeries(seriesIdentifier);
                        if (series == null) {
                            series = new Series();
                            series.setIdentifier(seriesIdentifier);
                            series.setTitle(dto.getTitle(), MEDIA_SOURCE);
                            series.setMoviedbIdMap(dto.getIdMap());
                            series.setStatus(StatusType.NEW);
                            mediaDao.saveEntity(series);
                        }
                        
                        season = new Season();
                        season.setIdentifier(seasonIdentifier);
                        season.setSeason(dto.getSeason());
                        season.setTitle(dto.getTitle(), MEDIA_SOURCE);
                        season.setSeries(series);
                        season.setStatus(StatusType.NEW);
                        mediaDao.saveEntity(season);
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
                    mediaDao.saveEntity(videoData);
                } else {
                    mediaFile.addVideoData(videoData);
                    videoData.addMediaFile(mediaFile);
                    mediaDao.updateEntity(videoData);
                }
            }
        }
        
        // TODO
        // - create associations to NFOs
        // - create local artwork entries
        // - create set entries
     
        finish(stageFile);
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    private void processUpdatedVideo(StageFile stageFile) {
        MediaFile mediaFile = stageFile.getMediaFile();
        mediaFile.setFileDate(stageFile.getFileDate());
        mediaFile.setFileSize(stageFile.getFileSize());
        mediaFile.setStatus(StatusType.UPDATED);
        mediaDao.updateEntity(mediaFile);
        
        finish(stageFile);
    }
    
    private void finish(StageFile stageFile) {
        stageFile.setStatus(StatusType.DONE);
        stagingDao.updateEntity(stageFile);
    }

    public void processingError(Long id) {
        if (id == null) return;
        
        StageFile stageFile = stagingDao.getStageFile(id);
        if (stageFile != null) {
            stageFile.setStatus(StatusType.ERROR);
            stagingDao.updateEntity(stageFile);
        }
    }
}
