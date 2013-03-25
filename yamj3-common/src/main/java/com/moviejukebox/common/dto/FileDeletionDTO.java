package com.moviejukebox.common.dto;

import java.io.Serializable;

/**
 * Object for deleting files from the core server.
 */
public class FileDeletionDTO implements Serializable {

    private static final long serialVersionUID = 949098336790324913L;
    
    private String scanPath;
    private String filePath;

    public String getScanPath() {
        return scanPath;
    }

    public void setScanPath(String scanPath) {
        this.scanPath = scanPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
