package com.moviejukebox.core.importer;

import com.moviejukebox.core.database.dao.FileStageDao;
import com.moviejukebox.core.database.dao.MediaDao;
import com.moviejukebox.core.database.model.FileStage;
import com.moviejukebox.core.database.model.MediaFile;
import com.moviejukebox.core.database.model.ScanPath;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The media import service is a spring-managed service.
 * This wil be used by the MediaImportRunner only in order
 * to access other spring beans cause the MediaImportRunner
 * itself is no spring-managed bean and dependency injection
 * will fail on that runner.
 *
 */
@Service("mediaImportService")
public class MediaImportService {

    @Autowired
    private FileStageDao fileStageDao;
    @Autowired
    private MediaDao mediaDao;

    public void deleteFileStage(FileStage fileStage) {
        // delete with ID cause entity is not bound to a session
        fileStageDao.deleteFileStage(fileStage.getId());
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized MediaFile initMediaFile(FileStage fileStage) {
        
        // process the scan path
        ScanPath scanPath = mediaDao.getScanPath(fileStage.getScanPath());
        if (scanPath == null) {
            // new scan path
            scanPath = new ScanPath();
            scanPath.setPlayerPath(fileStage.getScanPath());
            scanPath.setLastScanned(new Date(System.currentTimeMillis()));
            mediaDao.saveEntity(scanPath);
        } else {
            // existing scan path
            scanPath.setLastScanned(new Date(System.currentTimeMillis()));
            mediaDao.updateEntity(scanPath);
        }
        
        MediaFile mediaFile = mediaDao.getMediaFile(fileStage.getFilePath());
        if (mediaFile == null) {
            // new media file
            mediaFile = new MediaFile();
            mediaFile.setScanPath(scanPath);
            mediaFile.setFilePath(fileStage.getFilePath());
            mediaFile.setFileDate(fileStage.getFileDate());
            mediaFile.setFileSize(fileStage.getFileSize());
        } else {
            mediaFile.setFileDate(fileStage.getFileDate());
            mediaFile.setFileSize(fileStage.getFileSize());

            // if media file exists then the scan path's must match also
            if (!mediaFile.getScanPath().equals(scanPath)) {
                throw new RuntimeException("Mismatch in scan paths");
            }
        }
        
        return mediaFile;
    }
}
