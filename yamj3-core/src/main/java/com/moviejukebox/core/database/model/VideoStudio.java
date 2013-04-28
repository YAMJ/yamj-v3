package com.moviejukebox.core.database.model;

import java.io.Serializable;
import javax.persistence.*;
import org.hibernate.annotations.ForeignKey;

@Entity
@Table(name = "video_studio")
public class VideoStudio extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3478878273175067619L;
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEOSTUDIO_VIDEODATA")
    @JoinColumn(name = "data_id", nullable = false)
    private VideoData videoData;
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEOSTUDIO_STUDIO")
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    // GETTER and SETTER
    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public Studio getStudio() {
        return studio;
    }

    public void setStudio(Studio studio) {
        this.studio = studio;
    }
}
