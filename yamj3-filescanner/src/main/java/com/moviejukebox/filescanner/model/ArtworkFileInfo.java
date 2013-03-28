package com.moviejukebox.filescanner.model;

import com.moviejukebox.filescanner.model.enumerations.ArtworkType;
import com.moviejukebox.filescanner.model.enumerations.FileInfoType;
import java.awt.Dimension;

public class ArtworkFileInfo extends FileInformation {

    private static final long serialVersionUID = 6935956906838369872L;
    
    private ArtworkType artworkType;
    private Dimension artworkSize;

    public ArtworkFileInfo() {
        super(FileInfoType.ARTWORK);
    }

    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public Dimension getArtworkSize() {
        return artworkSize;
    }

    public void setArtworkSize(Dimension artworkSize) {
        this.artworkSize = artworkSize;
    }

    public void setArtworSize(int width, int height) {
        this.artworkSize = new Dimension(width, height);
    }
}
