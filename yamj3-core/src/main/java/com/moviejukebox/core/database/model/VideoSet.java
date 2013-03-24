package com.moviejukebox.core.database.model;

import java.io.Serializable;
import javax.persistence.*;
import org.hibernate.annotations.ForeignKey;

@Entity
@Table(name = "video_set")
public class VideoSet extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3478878273175067619L;

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEOSET_VIDEODATA")
    @JoinColumn(name = "data_id", nullable = false)
    private VideoData videoData;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEOSET_BOXEDSET")
    @JoinColumn(name = "boxedset_id", nullable = false)
    private BoxedSet boxedSet;
    
    @JoinColumn(name = "ordering", nullable = false)
    private int ordering = -1;

    // GETTER and SETTER
    
    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public BoxedSet getBoxedSet() {
        return boxedSet;
    }

    public void setBoxedSet(BoxedSet boxedSet) {
        this.boxedSet = boxedSet;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }
}
