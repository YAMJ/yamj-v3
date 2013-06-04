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
