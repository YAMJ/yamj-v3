package com.moviejukebox.core.database.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "media_file")
public class MediaFile extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 8411423609119475972L;

    @NaturalId
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "file_date")
    private Date fileDate;
    
    @Column(name = "file_size")
    private long fileSize = -1;

    @Column(name = "part")
    private int part;

    @Column(name = "container", length = 30)
    private String container;
    
    @Column(name = "codec", length = 50)
    private String codec;

    @Column(name = "bitrate")
    private int bitrate = -1;

    @Column(name = "fps")
    private float fps = 60;

    @Column(name = "width")
    private int width = -1;

    @Column(name = "height")
    private int height = -1;

    @Column(name = "aspect", length = 30)
    private String aspect;

    @Column(name = "runtime")
    private long runtime;

    @Column(name = "video_source", length = 30)
    private String videoSource;

    @Column(name = "staged")
    private boolean staged;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_MEDIAFILE_SCANPATH")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "scanpath_id", nullable = false)
    private ScanPath scanPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_MEDIAFILE_VIDEODATA")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "data_id", nullable = false)
    private VideoData videoData;

    // GETTER and SETTER
    
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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getPart() {
        return part;
    }

    public void setPart(int part) {
        this.part = part;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public float getFps() {
        return fps;
    }

    public void setFps(float fps) {
        this.fps = fps;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getAspect() {
        return aspect;
    }

    public void setAspect(String aspect) {
        this.aspect = aspect;
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    public String getVideoSource() {
        return videoSource;
    }

    public void setVideoSource(String videoSource) {
        this.videoSource = videoSource;
    }

    public boolean isStaged() {
        return staged;
    }

    public void setStaged(boolean staged) {
        this.staged = staged;
    }

    public ScanPath getScanPath() {
        return scanPath;
    }

    public void setScanPath(ScanPath scanPath) {
        this.scanPath = scanPath;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.filePath == null?0:this.filePath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if ( this == other ) return true;
        if ( other == null ) return false;
        if ( !(other instanceof ScanPath) ) return false;
        MediaFile castOther = (MediaFile)other;
        return StringUtils.equals(this.filePath, castOther.filePath);
    }
}
