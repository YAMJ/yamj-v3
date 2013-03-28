package com.moviejukebox.filescanner.model;

import com.moviejukebox.filescanner.model.enumerations.FileInfoType;
import com.moviejukebox.filescanner.model.enumerations.NfoLevel;

public class NfoFileInfo extends FileInformation {

    private static final long serialVersionUID = 5196534707268208505L;
    
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
