package com.moviejukebox.core.database.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.ForeignKey;

@Entity
@Table(name = "video_file")
public class VideoFile extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 8411423609119475972L;
    
    @Column(name = "numberParts")
    private int numberParts;

    @Column(name = "firstPart")
    private int firstPart;

    @Column(name = "lastPart")
    private int lastPart;

    @Column(name = "fileLocation", length = 255)
    private String fileLocation;
    
    @Column(name = "filePlayerPath", length = 255)
    private String filePlayerPath;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "fileDate")
    private Date fileDate;
    
    @Column(name = "fileSize")
    private long fileSize;

    @Column(name = "container", length = 30)
    private String container;
    
    @Column(name = "resolution", length = 50)
    private String resolution;
    
    @Column(name = "aspect", length = 50)
    private String aspect;
    
    @Column(name = "videoSource", length = 50)
    private String videoSource;

    @Column(name = "videoOutput", length = 50)
    private String videoOutput;

    @Column(name = "audioChannels", length = 50)
    private String audioChannels;

    @Column(name = "fps")
    private float fps = 60;
    
    @Column(name = "subtitlesExchange", length = 10)
    private String subtitlesExchange;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEOFILE_MOVIE")
    @JoinColumn(name = "movieId", nullable = false)
    private Movie movie;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "videoFile")
    private Set<VideoFilePart> videoParts = new HashSet<VideoFilePart>(0);

    public int getNumberParts() {
        return numberParts;
    }

    public void setNumberParts(int numberParts) {
        this.numberParts = numberParts;
    }

    public int getFirstPart() {
        return firstPart;
    }

    public void setFirstPart(int firstPart) {
        this.firstPart = firstPart;
    }

    public int getLastPart() {
        return lastPart;
    }

    public void setLastPart(int lastPart) {
        this.lastPart = lastPart;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public String getFilePlayerPath() {
        return filePlayerPath;
    }

    public void setFilePlayerPath(String filePlayerPath) {
        this.filePlayerPath = filePlayerPath;
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

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAspect() {
        return aspect;
    }

    public void setAspect(String aspect) {
        this.aspect = aspect;
    }

    public String getVideoSource() {
        return videoSource;
    }

    public void setVideoSource(String videoSource) {
        this.videoSource = videoSource;
    }

    public String getVideoOutput() {
        return videoOutput;
    }

    public void setVideoOutput(String videoOutput) {
        this.videoOutput = videoOutput;
    }

    public String getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(String audioChannels) {
        this.audioChannels = audioChannels;
    }

    public float getFps() {
        return fps;
    }

    public void setFps(float fps) {
        this.fps = fps;
    }

    public String getSubtitlesExchange() {
        return subtitlesExchange;
    }

    public void setSubtitlesExchange(String subtitlesExchange) {
        this.subtitlesExchange = subtitlesExchange;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Set<VideoFilePart> getVideoParts() {
        return videoParts;
    }

    public void setVideoParts(Set<VideoFilePart> videoParts) {
        this.videoParts = videoParts;
    }
    
    public void addVideoPart(VideoFilePart videoPart) {
        videoPart.setVideoFile(this);
        this.videoParts.add(videoPart);
    }
}
