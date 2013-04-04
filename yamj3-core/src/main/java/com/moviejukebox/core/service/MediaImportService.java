package com.moviejukebox.core.service;

import com.moviejukebox.core.database.dao.MediaDao;
import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.MediaFile;
import com.moviejukebox.core.database.model.StageFile;
import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.database.model.type.VideoType;
import com.moviejukebox.core.scanner.FilenameDTO;
import com.moviejukebox.core.scanner.FilenameScanner;
import org.apache.commons.io.FilenameUtils;
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
        
        // VIDEO DATA
        String identifier = dto.buildVideoDataIdentifier();
        VideoData videoData = mediaDao.getVideoData(identifier);
        if (videoData == null) {
            // NEW video data
            videoData = new VideoData();
            videoData.setIdentifier(identifier);
            videoData.setSeason(dto.getSeason());
            videoData.setVideoType(dto.getSeason() > -1 ? VideoType.TVSHOW : VideoType.MOVIE);
            videoData.setMovieIds(dto.getIdMap());
            videoData.setTitle(dto.getTitle(), MEDIA_SOURCE);
            videoData.setTitleOriginal(dto.getTitle(), MEDIA_SOURCE);
            if (dto.getYear() > 0) {
                videoData.setPublicationYear(String.valueOf(dto.getYear()), MEDIA_SOURCE);
            }
            videoData.setStatus(StatusType.NEW);
            mediaDao.saveEntity(videoData);

            // TODO store episodes
            // TODO store sets
            
        } else {
            // TODO check what must be changed?
        }

        // MEDIA FILE
        String baseFileName = FilenameUtils.removeExtension(stageFile.getFileName());
        MediaFile mediaFile = mediaDao.getMediaFile(baseFileName);
        if (mediaFile == null) {
            // NEW media file
            // fill in scanned values
            mediaFile = new MediaFile();
            mediaFile.setBaseFileName(baseFileName);
            mediaFile.setFileDate(stageFile.getFileDate());
            mediaFile.setFileSize(stageFile.getFileSize());
            mediaFile.setContainer(dto.getContainer());
            mediaFile.setPart(dto.getPart());
            mediaFile.setFps(dto.getFps());
            mediaFile.setCodec(dto.getVideoCodec());
            mediaFile.setVideoSource(dto.getVideoSource());
            mediaFile.setStatus(StatusType.NEW);
            
            mediaFile.setVideoData(videoData);
            mediaFile.addStageFile(stageFile);
            stageFile.setMediaFile(mediaFile);
            mediaDao.saveEntity(mediaFile);
        } else {
            //  Possible reasons for already existing mediaFile:
            // - Other Library?
            // - Extension changed?
            // - Stored in another directory?
        }
        
        // TODO
        // - create associations to NFOs
        // - create local artwork entries
     
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
