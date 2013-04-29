package com.moviejukebox.core.database.model;

import com.moviejukebox.common.type.StatusType;
import javax.persistence.Column;
import org.hibernate.annotations.Type;

import com.moviejukebox.core.hibernate.usertypes.EnumStringUserType;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Parameter;

@TypeDef(name = "statusType",
        typeClass = EnumStringUserType.class,
        parameters = {
    @Parameter(name = "enumClassName", value = "com.moviejukebox.common.type.StatusType")})
@Entity
@Table(name = "mediafile")
public class MediaFile extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 8411423609119475972L;
    @NaturalId
    @Column(name = "fileName", nullable = false, length = 500)
    private String fileName;
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
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "mediafile_videodata",
            joinColumns = {
        @JoinColumn(name = "mediafile_id")},
            inverseJoinColumns = {
        @JoinColumn(name = "videodata_id")})
    private Set<VideoData> videoDatas = new HashSet<VideoData>(0);
    @OneToMany(cascade = CascadeType.DETACH, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "mediaFile")
    private Set<StageFile> stageFiles = new HashSet<StageFile>(0);

    // GETTER and SETTER
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public Set<VideoData> getVideoDatas() {
        return videoDatas;
    }

    public void setVideoDatas(Set<VideoData> videoDatas) {
        this.videoDatas = videoDatas;
    }

    public void addVideoData(VideoData videoData) {
        this.videoDatas.add(videoData);
    }

    public Set<StageFile> getStageFiles() {
        return stageFiles;
    }

    public void setStageFiles(Set<StageFile> stageFiles) {
        this.stageFiles = stageFiles;
    }

    public void addStageFile(StageFile stageFile) {
        this.stageFiles.add(stageFile);
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.fileName == null ? 0 : this.fileName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof MediaFile)) {
            return false;
        }
        MediaFile castOther = (MediaFile) other;
        return StringUtils.equals(this.fileName, castOther.fileName);
    }
}
