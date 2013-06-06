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
