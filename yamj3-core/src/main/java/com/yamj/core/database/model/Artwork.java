package com.yamj.core.database.model;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.yamj.core.database.model.type.ArtworkType;
import com.yamj.core.hibernate.usertypes.EnumStringUserType;

@TypeDef(name = "artworkType",
        typeClass = EnumStringUserType.class,
        parameters = {
    @Parameter(name = "enumClassName", value = "com.yamj.core.database.model.type.ArtworkType")})
@Entity
@Table(name = "artwork")
public class Artwork extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -981494909436217076L;
    @Type(type = "artworkType")
    @Column(name = "artwork_type", nullable = false)
    private ArtworkType artworkType;
    @Column(name = "url")
    private String url;
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_STAGEFILE")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "stagefile_id")
    private StageFile stageFile;
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_VIDEODATA")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "videodata_id")
    private VideoData videoData;
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_SEASON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "season_id")
    private Season season;
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_ARTWORK_SERIES")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id")
    private Series series;

    // GETTER and SETTER
    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public StageFile getStageFile() {
        return stageFile;
    }

    public void setStageFile(StageFile stageFile) {
        this.stageFile = stageFile;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }
}
