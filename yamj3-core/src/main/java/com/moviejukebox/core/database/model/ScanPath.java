package com.moviejukebox.core.database.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "scan_path")
public class ScanPath extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -5113519542293276527L;
    
    @NaturalId(mutable = true)
    @Column(name = "player_path", nullable = false, length = 255)
    private String playerPath;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "last_scanned", nullable = false)
    private Date lastScanned;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "scanPath")
    private Set<MediaFile> mediaFiles = new HashSet<MediaFile>(0);

    // SETTER and GETTER
    
    public String getPlayerPath() {
        return playerPath;
    }

    public void setPlayerPath(String playerPath) {
        this.playerPath = playerPath;
    }

    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    public Set<MediaFile> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(Set<MediaFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.playerPath == null?0:this.playerPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if ( this == other ) return true;
        if ( other == null ) return false;
        if ( !(other instanceof ScanPath) ) return false;
        ScanPath castOther = (ScanPath)other;
        return StringUtils.equals(this.playerPath, castOther.playerPath);
    }
}
