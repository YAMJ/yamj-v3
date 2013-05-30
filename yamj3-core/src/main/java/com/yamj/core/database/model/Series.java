package com.yamj.core.database.model;

import org.hibernate.annotations.Index;

import com.yamj.core.database.model.type.OverrideFlag;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.CascadeType;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;
import org.hibernate.annotations.MapKey;

@SuppressWarnings("deprecation")
@javax.persistence.Entity
@javax.persistence.Table(name = "series")
@org.hibernate.annotations.Table(appliesTo = "series",
    indexes = {
        @Index(name = "series_title", columnNames = {"title"}),
        @Index(name = "series_status", columnNames = {"status"})
})
public class Series extends AbstractMetadata {

    private static final long serialVersionUID = -3336182194593898858L;

    @Column(name = "start_year")
    private int startYear = -1;
    
    @Column(name = "end_year")
    private int endYear = -1;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_ids", joinColumns = @JoinColumn(name = "series_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200)
    private Map<String, String> sourcedbIdMap = new HashMap<String, String>(0);
    
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_ratings", joinColumns = @JoinColumn(name = "series_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", length = 30)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);
    
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_override", joinColumns = @JoinColumn(name = "series_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKey(type = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30)
    private Map<OverrideFlag, String> overrideFlags = new HashMap<OverrideFlag, String>(0);
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "series")
    private Set<Season> seasons = new HashSet<Season>(0);

    // GETTER and SETTER
    
    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public Map<String, String> getMoviedbIdMap() {
        return sourcedbIdMap;
    }

    @Override
    public String getSourcedbId(String sourcedb) {
        return sourcedbIdMap.get(sourcedb);
    }

    public void setMoviedbIdMap(Map<String, String> sourcedbIdMap) {
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

    public void addRating(String source, Integer rating) {
        this.ratings.put(source, rating);
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

    public Set<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(Set<Season> seasons) {
        this.seasons = seasons;
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
        if (!(other instanceof Series)) {
            return false;
        }
        Series castOther = (Series) other;
        return StringUtils.equals(this.identifier, castOther.identifier);
    }
}
