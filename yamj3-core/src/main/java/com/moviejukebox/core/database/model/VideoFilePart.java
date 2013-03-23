package com.moviejukebox.core.database.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.moviejukebox.core.database.model.type.OverrideFlag;
import com.moviejukebox.core.hibernate.usertypes.EnumStringUserType;

@TypeDef(name = "overrideFlag", 
typeClass = EnumStringUserType.class,
parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.OverrideFlag")})

@Entity
@Table(name = "video_file_parts")
@SuppressWarnings("deprecation")
public class VideoFilePart extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -9127548516718306527L;

    @Column(name = "season")
    private int season;
    
    @Column(name = "part")
    private int part;

    @Column(name = "title", length = 255)
    private String title;

    @Lob
    @Column(name = "plot")
    private String plot;

    @Column(name = "watched")
    private boolean watched;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "watchedDate")
    private Date watchedDate;

    @Column(name = "afterSeason", length = 255)
    private String afterSeason;

    @Column(name = "beforeSeason", length = 255)
    private String beforeSeason;

    @Column(name = "beforeEpisode", length = 255)
    private String beforeEpisode;

    @Column(name = "firstAired", length = 255)
    private String firstAired;
    
    @Column(name = "rating")
    private int rating = -1;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEOPART_VIDEOIMAGE")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videoimageId")
    private Artwork videoimage;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEOPART_VIDEOFILE")
    @JoinColumn(name = "videoFileId", nullable = false)
    private VideoFile videoFile;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "video_file_parts_override", joinColumns = @JoinColumn(name = "videoPartId"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length= 40)
    @MapKey(type = @Type(type = "overrideFlag"))    
    @Column(name = "source", length = 40)
    private Map<OverrideFlag, String> overrideFlags = new HashMap<OverrideFlag, String>(0);

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getPart() {
        return part;
    }

    public void setPart(int part) {
        this.part = part;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public Date getWatchedDate() {
        return watchedDate;
    }

    public void setWatchedDate(Date watchedDate) {
        this.watchedDate = watchedDate;
    }

    public String getAfterSeason() {
        return afterSeason;
    }

    public void setAfterSeason(String afterSeason) {
        this.afterSeason = afterSeason;
    }

    public String getBeforeSeason() {
        return beforeSeason;
    }

    public void setBeforeSeason(String beforeSeason) {
        this.beforeSeason = beforeSeason;
    }

    public String getBeforeEpisode() {
        return beforeEpisode;
    }

    public void setBeforeEpisode(String beforeEpisode) {
        this.beforeEpisode = beforeEpisode;
    }

    public String getFirstAired() {
        return firstAired;
    }

    public void setFirstAired(String firstAired) {
        this.firstAired = firstAired;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Artwork getVideoimage() {
        return videoimage;
    }

    public void setVideoimage(Artwork videoimage) {
        this.videoimage = videoimage;
    }

    public VideoFile getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(VideoFile videoFile) {
        this.videoFile = videoFile;
    }

    public Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    public void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }
}
