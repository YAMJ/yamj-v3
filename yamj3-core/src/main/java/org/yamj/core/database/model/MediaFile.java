/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.model;

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
import org.yamj.common.type.StatusType;

@Entity
@Table(name = "mediafile",
    uniqueConstraints= @UniqueConstraint(name="UIX_MEDIAFILE_NATURALID", columnNames={"file_name"})
)
@SuppressWarnings("unused")
public class MediaFile extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 8411423609119475972L;
    
    @NaturalId
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "file_date")
    private Date fileDate;
    
    @Column(name = "file_size", nullable = false)
    private long fileSize = -1;

    @Column(name = "extra", nullable = false)
    private boolean extra = false;

    @Column(name = "part", nullable = false)
    private int part = -1;

    @Column(name = "part_title", length = 200)
    private String partTitle;

    @Column(name = "movie_version", length = 200)
    private String movieVersion;

    @Column(name = "container", length = 30)
    private String container;
    
    @Column(name = "codec", length = 50)
    private String codec;

    @Column(name = "codec_format", length = 50)
    private String codecFormat;

    @Column(name = "codec_profile", length = 50)
    private String codecProfile;

    @Column(name = "bitrate", nullable = false)
    private int bitrate = -1;

    @Column(name = "overall_bitrate", nullable = false)
    private int overallBitrate = -1;

    @Column(name = "fps", nullable = false)
    private float fps = -1;
    
    @Column(name = "width", nullable = false)
    private int width = -1;
    
    @Column(name = "height", nullable = false)
    private int height = -1;
    
    @Column(name = "aspect_ratio", length = 30)
    private String aspectRatio;
    
    @Column(name = "runtime", nullable = false)
    private int runtime;
    
    @Column(name = "video_source", length = 30)
    private String videoSource;

    @Column(name = "episode_count", nullable = false)
    private int episodeCount = 0;
    
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "mediafile_videodata",
        joinColumns = {@JoinColumn(name = "mediafile_id")}, inverseJoinColumns = {@JoinColumn(name = "videodata_id")})
    @ForeignKey(name = "FK_REL_MEDIAFILE_VIDEODATA")
    private Set<VideoData> videoDatas = new HashSet<VideoData>(0);
    
    @OneToMany(cascade = CascadeType.DETACH, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "mediaFile")
    private Set<StageFile> stageFiles = new HashSet<StageFile>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true,  mappedBy = "mediaFile")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<AudioCodec> audioCodecs = new HashSet<AudioCodec>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true,  mappedBy = "mediaFile")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Subtitle> subtitles = new HashSet<Subtitle>(0);

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

    public boolean isExtra() {
        return extra;
    }

    public void setExtra(boolean extra) {
        this.extra = extra;
    }

    public int getPart() {
        return part;
    }

    public void setPart(int part) {
        this.part = part;
    }

    public String getPartTitle() {
        return partTitle;
    }

    public void setPartTitle(String partTitle) {
        this.partTitle = partTitle;
    }

    public String getMovieVersion() {
        return movieVersion;
    }

    public void setMovieVersion(String movieVersion) {
        this.movieVersion = movieVersion;
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
    
    public String getCodecFormat() {
        return codecFormat;
    }

    public void setCodecFormat(String codecFormat) {
        this.codecFormat = codecFormat;
    }

    public String getCodecProfile() {
        return codecProfile;
    }

    public void setCodecProfile(String codecProfile) {
        this.codecProfile = codecProfile;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getOverallBitrate() {
        return overallBitrate;
    }

    public void setOverallBitrate(int overallBitrate) {
        this.overallBitrate = overallBitrate;
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

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public String getVideoSource() {
        return videoSource;
    }

    public void setVideoSource(String videoSource) {
        this.videoSource = videoSource;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
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

    private void setVideoDatas(Set<VideoData> videoDatas) {
        this.videoDatas = videoDatas;
    }

    public void addVideoData(VideoData videoData) {
        this.videoDatas.add(videoData);
    }

    public Set<StageFile> getStageFiles() {
        return stageFiles;
    }

    private void setStageFiles(Set<StageFile> stageFiles) {
        this.stageFiles = stageFiles;
    }

    public void addStageFile(StageFile stageFile) {
        this.stageFiles.add(stageFile);
    }

    public Set<AudioCodec> getAudioCodecs() {
        return audioCodecs;
    }

    private void setAudioCodecs(Set<AudioCodec> audioCodecs) {
        this.audioCodecs = audioCodecs;
    }

    public AudioCodec getAudioCodec(int counter) {
        for (AudioCodec audioCodec : this.audioCodecs) {
            if (audioCodec.getCounter() == counter) {
                return audioCodec;
            }
        }
        return null;
    }

    public Set<Subtitle> getSubtitles() {
        return subtitles;
    }

    private void setSubtitles(Set<Subtitle> subtitles) {
        this.subtitles = subtitles;
    }

    public Subtitle getSubtitle(int counter) {
        for (Subtitle subtitle : this.subtitles) {
            if (subtitle.getStageFile() == null && subtitle.getCounter() == counter) {
                return subtitle;
            }
        }
        return null;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (this.fileName == null ? 0 : this.fileName.hashCode());
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
