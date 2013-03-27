package com.yamj.filescanner.server.model;

import com.yamj.filescanner.server.model.enumerations.FileInfoType;
import com.yamj.filescanner.server.model.enumerations.NfoLevel;

public class NfoFileInfo extends FileInformation {

    private NfoLevel nfoLevel;
    private String contents;

    public NfoFileInfo() {
        super(FileInfoType.NFO);
    }

    public NfoLevel getNfoLevel() {
        return nfoLevel;
    }

    public void setNfoLevel(NfoLevel nfoLevel) {
        this.nfoLevel = nfoLevel;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }
}
