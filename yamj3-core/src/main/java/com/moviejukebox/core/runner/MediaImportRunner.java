package com.moviejukebox.core.runner;

import com.moviejukebox.core.database.model.FileStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaImportRunner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaImportRunner.class);

    private final FileStage fileStage;
    
    public MediaImportRunner(FileStage fileStage) {
        this.fileStage = fileStage;
    }
    
    @Override
    public void run() {
        LOGGER.debug("Processing " + fileStage);
    }
}
