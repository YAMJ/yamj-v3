package com.yamj.filescanner.server.model;

import com.yamj.filescanner.server.model.enumerations.FileInfoType;

public class MediaFileInfo extends FileInformation {

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
