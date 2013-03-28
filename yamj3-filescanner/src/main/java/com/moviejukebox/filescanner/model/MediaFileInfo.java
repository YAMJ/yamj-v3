package com.moviejukebox.filescanner.model;

import com.moviejukebox.filescanner.model.enumerations.FileInfoType;

public class MediaFileInfo extends FileInformation {

    private static final long serialVersionUID = 3225616721099784226L;
    
    private String fileType;
    private String mediainfo;
    private double fileSize = 0;

    public MediaFileInfo() {
        super(FileInfoType.MEDIA);
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getMediainfo() {
        return mediainfo;
    }

    public void setMediainfo(String mediainfo) {
        this.mediainfo = mediainfo;
    }

    public double getFileSize() {
        return fileSize;
    }

    public void setFileSize(double fileSize) {
        this.fileSize = fileSize;
    }

}
