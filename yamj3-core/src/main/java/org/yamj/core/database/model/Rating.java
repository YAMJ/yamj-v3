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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Used to store the various ratings for a video
 *
 * TODO: Use for all ratings in other objects
 *
 * @author stuart.boston
 */
@Entity
@Table(name = "rating",
        uniqueConstraints = {
    @UniqueConstraint(columnNames = {"video_id", "source_id", "rating"})})
public class Rating extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 1L;
    @Column(name = "video_id", nullable = false)
    private int videoId;
    @Column(name = "source_id", nullable = false, length = 50)
    private String sourceId;
    @Column(name = "rating", nullable = false)
    private int rating;

    public Rating() {
    }

    public Rating(int videoId, String sourceId, int rating) {
        this.videoId = videoId;
        this.sourceId = sourceId;
        this.rating = rating;
    }

    public int getVideoId() {
        return videoId;
    }

    public void setVideoId(int videoId) {
        this.videoId = videoId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
