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

import java.util.*;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.OverrideFlag;

@javax.persistence.Entity
@javax.persistence.Table(name = "season")
@org.hibernate.annotations.Table(appliesTo = "season",
    indexes = {
        @Index(name = "season_title", columnNames = {"title"}),
        @Index(name = "season_status", columnNames = {"status"})
})
public class Season extends AbstractMetadata {

    private static final long serialVersionUID = 7589022259013410259L;

    @Index(name = "season_season")
    @Column(name = "season", nullable = false)
    private int season;

    @Column(name = "first_aired", length = 10)
    private String firstAired;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "season_ids", joinColumns = @JoinColumn(name = "season_id"))
    @ForeignKey(name = "FK_SEASON_SOURCEIDS")
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200)
    private Map<String, String> sourcedbIdMap = new HashMap<String, String>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "season_ratings", joinColumns = @JoinColumn(name = "season_id"))
    @ForeignKey(name = "FK_SEASON_RATINGS")
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", length = 30)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "season_override", joinColumns = @JoinColumn(name = "season_id"))
    @ForeignKey(name = "FK_SEASON_OVERRIDE")
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @Column(name = "source", length = 30)
    private Map<OverrideFlag, String> overrideFlags = new HashMap<OverrideFlag, String>(0);

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_SEASON_SERIES")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "season")
    private Set<VideoData> videoDatas = new HashSet<VideoData>(0);

    // GETTER and SETTER

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public String getFirstAired() {
        return firstAired;
    }

    public void setFirstAired(String firstAired) {
        this.firstAired = firstAired;
    }

    public Map<String, String> getSourcedbIdMap() {
        return sourcedbIdMap;
    }

    @Override
    public String getSourcedbId(String sourcedb) {
        return sourcedbIdMap.get(sourcedb);
    }

    public void setSourcedbIdMap(Map<String, String> sourcedbIdMap) {
        this.sourcedbIdMap = sourcedbIdMap;
    }

    @Override
    public void setSourcedbId(String sourcedb, String id) {
        if (StringUtils.isNotBlank(id)) {
            sourcedbIdMap.put(sourcedb, id);
        }
    }

    public Map<String, Integer> getRatings() {
        return ratings;
    }

    public void setRatings(Map<String, Integer> ratings) {
        this.ratings = ratings;
    }

    public Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    public void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    @Override
    public void setOverrideFlag(OverrideFlag overrideFlag, String source) {
        this.overrideFlags.put(overrideFlag, source);
    }

    @Override
    public String getOverrideSource(OverrideFlag overrideFlag) {
        return overrideFlags.get(overrideFlag);
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public Set<VideoData> getVideoDatas() {
        return videoDatas;
    }

    public void setVideoDatas(Set<VideoData> videoDatas) {
        this.videoDatas = videoDatas;
    }

    // TV CHECKS

    public List<VideoData> getScannableTvEpisodes() {
        List<VideoData> episodes = new ArrayList<VideoData>();
        for (VideoData videoData : getVideoDatas()) {
            if (videoData.isScannableTvEpisode()) {
                episodes.add(videoData);
            }
        }
        return episodes;
    }

    public boolean isScannableTvSeason() {
        if (StatusType.DONE.equals(this.getStatus())) {
            return false;
        }
        return true;
    }

    public void setTvSeasonScanned() {
        this.setStatus(StatusType.PROCESSED);
    }

    public void setTvSeasonMissing() {
        this.setStatus(StatusType.MISSING);
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 17;
        int result = 1;
        result = prime * result + (this.identifier == null ? 0 : this.identifier.hashCode());
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
        if (!(other instanceof Season)) {
            return false;
        }
        Season castOther = (Season) other;
        return StringUtils.equals(this.identifier, castOther.identifier);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Season [ID=");
        sb.append(getId());
        sb.append(", identifier=");
        sb.append(getIdentifier());
        sb.append(", title=");
        sb.append(getTitle());
        sb.append(", title=");
        sb.append(getYear());
        sb.append("]");
        return sb.toString();
    }
}
