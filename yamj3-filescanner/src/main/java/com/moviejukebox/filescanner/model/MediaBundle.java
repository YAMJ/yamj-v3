package com.moviejukebox.filescanner.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MediaBundle {

    private String bundleName;
    private boolean updated = Boolean.FALSE;
    private Date lastUpdateDateTime = null;
    private List<MediaFileInfo> mediaFiles;
    private List<ArtworkFileInfo> artworkFiles;
    private List<NfoFileInfo> nfoFiles;

    public MediaBundle(String bundleName) {
        this.bundleName = bundleName;
        this.updated = Boolean.FALSE;
        this.lastUpdateDateTime = new Date();
        this.mediaFiles = Collections.emptyList();
        this.artworkFiles = Collections.emptyList();
        this.nfoFiles = Collections.emptyList();
    }

    public MediaBundle(String bundleName, boolean updated, Date lastUpdateDateTime) {
        this.bundleName = bundleName;
        this.updated = updated;
        this.lastUpdateDateTime = lastUpdateDateTime;
        this.mediaFiles = Collections.emptyList();
        this.artworkFiles = Collections.emptyList();
        this.nfoFiles = Collections.emptyList();
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public Date getLastUpdateDateTime() {
        return lastUpdateDateTime;
    }

    public void setLastUpdateDateTime(Date lastUpdateDateTime) {
        this.lastUpdateDateTime = lastUpdateDateTime;
    }

    public List<MediaFileInfo> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<MediaFileInfo> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public List<ArtworkFileInfo> getArtworkFiles() {
        return artworkFiles;
    }

    public void setArtworkFiles(List<ArtworkFileInfo> artworkFiles) {
        this.artworkFiles = artworkFiles;
    }

    public List<NfoFileInfo> getNfoFiles() {
        return nfoFiles;
    }

    public void setNfoFiles(List<NfoFileInfo> nfoFiles) {
        this.nfoFiles = nfoFiles;
    }
}
