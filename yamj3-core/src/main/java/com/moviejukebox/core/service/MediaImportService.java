package com.moviejukebox.core.service;

import com.moviejukebox.core.database.dao.StagingDao;
import com.moviejukebox.core.database.model.StageFile;
import com.moviejukebox.core.database.model.type.StatusType;
import com.moviejukebox.core.scanner.FilenameDTO;
import com.moviejukebox.core.scanner.FilenameScanner;
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

    @Autowired
    private StagingDao stagingDao;
    @Autowired
    private FilenameScanner filenameScanner;

    @Transactional(propagation = Propagation.REQUIRED)
    public void processNewVideo(StageFile stageFile) {
        // scan filename for informations
        FilenameDTO dto = new FilenameDTO(stageFile);
        filenameScanner.scan(dto);
        
        System.err.println(dto);
        
        // TODO
        // - create VideoData
        // - create Mediafile
        // - create associations to NFOs
        // - create local artwork entries
     
        finish(stageFile);
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void processUpdatedVideo(StageFile stageFile) {
        
        // TODO update mediainfo only
        
        finish(stageFile);
    }
    
    private void finish(StageFile stageFile) {
        stageFile.setStatus(StatusType.DONE);
        stagingDao.updateEntity(stageFile);
    }

    public void processingError(StageFile stageFile) {
        if (stageFile != null) {
            stageFile.setStatus(StatusType.ERROR);
            stagingDao.updateEntity(stageFile);
        }
    }
}
