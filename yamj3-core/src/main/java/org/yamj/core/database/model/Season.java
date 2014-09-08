/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import javax.persistence.Column;
import org.hibernate.annotations.Index;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import java.util.*;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.OverrideFlag;

@Entity
@Table(name = "season",
        uniqueConstraints =
        @UniqueConstraint(name = "UIX_SEASON_NATURALID", columnNames = {"identifier"}))
@org.hibernate.annotations.Table(appliesTo = "season",
        indexes = {
    @Index(name = "IX_SEASON_TITLE", columnNames = {"title"}),
    @Index(name = "IX_SEASON_STATUS", columnNames = {"status"})
})
@SuppressWarnings("unused")
public class Season extends AbstractMetadata {

    private static final long serialVersionUID = 1858640563119637343L;

    @Index(name = "IX_SEASON_SEASON")
    @Column(name = "season", nullable = false)
    private int season;

    @Index(name = "IX_SEASON_PUBLICATIONYEAR")
    @Column(name = "publication_year", nullable = false)
    private int publicationYear = -1;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "season_ids", joinColumns = @JoinColumn(name = "season_id"))
    @ForeignKey(name = "FK_SEASON_SOURCEIDS")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<String, String>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "season_ratings", joinColumns = @JoinColumn(name = "season_id"))
    @ForeignKey(name = "FK_SEASON_RATINGS")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", nullable = false)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "season_override", joinColumns = @JoinColumn(name = "season_id"))
    @ForeignKey(name = "FK_SEASON_OVERRIDE")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<OverrideFlag, String>(OverrideFlag.class);

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_SEASON_SERIES")
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "season")
    private Set<VideoData> videoDatas = new HashSet<VideoData>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "season")
    private List<Artwork> artworks = new ArrayList<Artwork>(0);

    // GETTER and SETTER
    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    private void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public void setPublicationYear(int publicationYear, String source) {
        if (publicationYear >= 0) {
            this.publicationYear = publicationYear;
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    @Override
    public String getSkipOnlineScans() {
        return null;
    }

    private Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    @Override
    public String getSourceDbId(String sourceDb) {
        return sourceDbIdMap.get(sourceDb);
    }

    private void setSourceDbIdMap(Map<String, String> sourceDbIdMap) {
        this.sourceDbIdMap = sourceDbIdMap;
    }

    @Override
    public void setSourceDbId(String sourceDb, String id) {
        if (StringUtils.isNotBlank(id)) {
            sourceDbIdMap.put(sourceDb, id);
        }
    }

    public Map<String, Integer> getRatings() {
        return ratings;
    }

    private void setRatings(Map<String, Integer> ratings) {
        this.ratings = ratings;
    }

    public void addRating(String sourceDb, int rating) {
        if (StringUtils.isNotBlank(sourceDb) && (rating >= 0)) {
            this.ratings.put(sourceDb, Integer.valueOf(rating));
        }
    }

    @JsonIgnore // This is not needed for the API
    public Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    public void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    @Override
    public void setOverrideFlag(OverrideFlag overrideFlag, String source) {
        this.overrideFlags.put(overrideFlag, source.toLowerCase());
    }

    @JsonIgnore // This is not needed for the API
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

    public List<Artwork> getArtworks() {
        return artworks;
    }

    public void setArtworks(List<Artwork> artworks) {
        this.artworks = artworks;
    }

    // TV CHECKS
    @JsonIgnore
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
        this.setStatus(StatusType.WAIT);
    }

    public void setTvSeasonNotFound() {
        this.setStatus(StatusType.NOTFOUND);
    }

    @Override
    public int getSeasonNumber() {
        return season;
    }

    @Override
    public Set<Genre> getGenres() {
        // no genres in season
        return Collections.emptySet();
    }

    @Override
    public Set<Studio> getStudios() {
        // no studios in season
        return Collections.emptySet();
    }

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (getIdentifier() == null ? 0 : getIdentifier().hashCode());
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
        return StringUtils.equals(getIdentifier(), castOther.getIdentifier());
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
