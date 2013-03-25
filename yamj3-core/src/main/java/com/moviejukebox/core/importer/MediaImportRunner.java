package com.moviejukebox.core.importer;

import com.moviejukebox.core.database.model.FileStage;
import com.moviejukebox.core.database.model.MediaFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaImportRunner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaImportRunner.class);

    private final FileStage fileStage;
    private final MediaImportService mediaImportService;
    
    public MediaImportRunner(FileStage fileStage, MediaImportService mediaImportService) {
        this.fileStage = fileStage;
        this.mediaImportService = mediaImportService;
    }

    @Override
    public void run() {
        try {
            // initiate the media file
            MediaFile mediaFile = mediaImportService.initMediaFile(fileStage);
            if (mediaFile == null) {
                // nothing to do right now cause file has not changed
            } else if (mediaFile.isNewlyCreated()) {
                LOGGER.debug("New media file: " + fileStage);
                processNewVideoMedia(mediaFile);
            } else {
                LOGGER.debug("Update media file: " + fileStage);
                processUpdateVideoMedia(mediaFile);
            }
            
            // file stage can be deleted cause everything was OK
            mediaImportService.deleteFileStage(fileStage);
        } catch (Exception error) {
            LOGGER.error("Error during processing of file stage: " + fileStage, error);
            // TODO mark file stage as errorness
        }
    }
    
    private void processNewVideoMedia(MediaFile mediaFile) {
        
    }

    private void processUpdateVideoMedia(MediaFile mediaFile) {
        
    }
}
