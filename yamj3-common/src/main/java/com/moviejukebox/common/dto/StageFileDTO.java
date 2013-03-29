package com.moviejukebox.common.dto;

import java.io.Serializable;

/**
 * Object for importing stage files into the core server.
 */
public class StageFileDTO implements Serializable {

    private static final long serialVersionUID = -2515870823273796114L;
    
    private String fileName;
    private long fileSize;
    private long fileDate;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileDate() {
        return fileDate;
    }

    public void setFileDate(long fileDate) {
        this.fileDate = fileDate;
    }

    @Override
    public String toString() {
        return "StageFileDTO{fileName='" + fileName + "', fileSize=" + fileSize + ", fileDate=" + fileDate + "}";
    }
}
