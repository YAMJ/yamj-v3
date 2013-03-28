package com.moviejukebox.common.dto;

import java.io.Serializable;

/**
 * Object for importing files into the core server.
 */
public class FileImportDTO implements Serializable {

    private static final long serialVersionUID = 2515787873612820679L;
    private String scanPath;
    private String filePath;
    private long fileSize;
    private long fileDate;

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
        return "FileImportDTO{" + "scanPath=" + scanPath + ", filePath=" + filePath + ", fileSize=" + fileSize + ", fileDate=" + fileDate + '}';
    }

}
