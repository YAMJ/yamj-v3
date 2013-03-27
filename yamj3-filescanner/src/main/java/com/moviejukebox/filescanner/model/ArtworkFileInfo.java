package com.yamj.filescanner.server.model;

import com.yamj.filescanner.server.model.enumerations.ArtworkType;
import com.yamj.filescanner.server.model.enumerations.FileInfoType;
import java.awt.Dimension;

public class ArtworkFileInfo extends FileInformation {

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
