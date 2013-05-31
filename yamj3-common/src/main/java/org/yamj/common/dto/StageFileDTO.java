package org.yamj.common.dto;

import java.io.File;
import java.io.Serializable;

/**
 * Object for importing stage files into the core server.
 */
public class StageFileDTO implements Serializable {

    private static final long serialVersionUID = -2515870823273796114L;
    private String fileName;
    private long fileSize;
    private long fileDate;

    public StageFileDTO() {
    }

    public StageFileDTO(File stageFile) {
        if (stageFile.isFile()) {
            this.fileName = stageFile.getName();
            this.fileSize = stageFile.length();
            this.fileDate = stageFile.lastModified();
        }
    }

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
