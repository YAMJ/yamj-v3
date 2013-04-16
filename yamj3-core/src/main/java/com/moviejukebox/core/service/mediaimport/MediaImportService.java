package com.moviejukebox.core.service.mediaimport;

import com.moviejukebox.core.database.model.type.ArtworkType;

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

                // create new poster artwork entry
                Artwork poster = new Artwork();
                poster.setArtworkType(ArtworkType.POSTER);
                poster.setVideoData(videoData);
                mediaDao.saveEntity(poster);

                // create new fanart artwork entry
                Artwork fanart = new Artwork();
                fanart.setArtworkType(ArtworkType.FANART);
                fanart.setVideoData(videoData);
                mediaDao.saveEntity(fanart);

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
                            
                            // create new poster artwork entry
                            Artwork poster = new Artwork();
                            poster.setArtworkType(ArtworkType.POSTER);
                            poster.setSeries(series);
                            mediaDao.saveEntity(poster);

                            // create new fanart artwork entry
                            Artwork fanart = new Artwork();
                            fanart.setArtworkType(ArtworkType.FANART);
                            mediaDao.saveEntity(fanart);

                            // create new banner artwork entry
                            Artwork banner = new Artwork();
                            banner.setArtworkType(ArtworkType.BANNER);
                            banner.setSeries(series);
                            mediaDao.saveEntity(banner);
                        }
                        
                        season = new Season();
                        season.setIdentifier(seasonIdentifier);
                        season.setSeason(dto.getSeason());
                        season.setTitle(dto.getTitle(), MEDIA_SOURCE);
                        season.setSeries(series);
                        season.setStatus(StatusType.NEW);
                        mediaDao.saveEntity(season);

                        // create new poster artwork entry
                        Artwork poster = new Artwork();
                        poster.setArtworkType(ArtworkType.POSTER);
                        poster.setSeason(season);
                        mediaDao.saveEntity(poster);

                        // create new fanart artwork entry
                        Artwork fanart = new Artwork();
                        fanart.setArtworkType(ArtworkType.FANART);
                        fanart.setSeason(season);
                        mediaDao.saveEntity(fanart);
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

                    // create new videoimage artwork entry
                    Artwork videoimage = new Artwork();
                    videoimage.setArtworkType(ArtworkType.VIDEOIMAGE);
                    videoimage.setVideoData(videoData);
                    mediaDao.saveEntity(videoimage);

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

    @Transactional(propagation = Propagation.REQUIRED)
    public void processingError(Long id) {
        if (id == null) return;
        
        StageFile stageFile = stagingDao.getStageFile(id);
        if (stageFile != null) {
            stageFile.setStatus(StatusType.ERROR);
            stagingDao.updateEntity(stageFile);
        }
    }
}
