/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.model;

import java.io.Serializable;
import javax.persistence.*;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "boxed_set_order",
    uniqueConstraints = {@UniqueConstraint(name = "UIX_BOXEDSET_VIDEODATA", columnNames = {"boxedset_id","videodata_id"}),
                         @UniqueConstraint(name = "UIX_BOXEDSET_SERIES", columnNames = {"boxedset_id", "series_id"}),
                         @UniqueConstraint(name = "UIX_BOXEDSET_ORDER_NATURALID", columnNames = {"boxedset_id", "series_id", "videodata_id"})}
)
public class BoxedSetOrder extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3478878273175067619L;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boxedset_id", nullable = false, foreignKey = @ForeignKey(name = "FK_BOXEDSETORDER_BOXEDSET"))
    private BoxedSet boxedSet;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "videodata_id", nullable = true, foreignKey = @ForeignKey(name = "FK_BOXEDSETORDER_VIDEODATA"))
    private VideoData videoData;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = true, foreignKey = @ForeignKey(name = "FK_BOXEDSETORDER_SERIES"))
    private Series series;

    @JoinColumn(name = "ordering", nullable = false)
    private int ordering = -1;

    // GETTER and SETTER

    public BoxedSet getBoxedSet() {
        return boxedSet;
    }

    public void setBoxedSet(BoxedSet boxedSet) {
        this.boxedSet = boxedSet;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }
}
