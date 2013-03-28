package com.moviejukebox.filescanner.model;

import com.moviejukebox.filescanner.model.enumerations.FileInfoType;
import java.io.Serializable;
import java.util.Date;

public abstract class FileInformation implements Serializable {

    private static final long serialVersionUID = -8702575085136475579L;
    
    private FileInfoType type;
    private String filename;
    private String filePath;
    private Date fileDate;
    private boolean updated;

    public FileInformation(FileInfoType type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Date getFileDate() {
        return fileDate;
    }

    public void setFileDate(Date fileDate) {
        this.fileDate = fileDate;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public FileInfoType getType() {
        return type;
    }

    public void setType(FileInfoType type) {
        this.type = type;
    }

}
