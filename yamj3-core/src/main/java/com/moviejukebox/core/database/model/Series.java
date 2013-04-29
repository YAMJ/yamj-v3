package com.moviejukebox.core.database.model;

import javax.persistence.Column;

import com.moviejukebox.core.database.model.type.OverrideFlag;
import com.moviejukebox.common.type.StatusType;
import com.moviejukebox.core.hibernate.usertypes.EnumStringUserType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.Parameter;

@TypeDefs({
    @TypeDef(name = "overrideFlag",
            typeClass = EnumStringUserType.class,
            parameters = {
        @Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.OverrideFlag")}),
    @TypeDef(name = "statusType",
            typeClass = EnumStringUserType.class,
            parameters = {
        @Parameter(name = "enumClassName", value = "com.moviejukebox.common.type.StatusType")})
})
@Entity
@Table(name = "series")
@SuppressWarnings("deprecation")
public class Series extends AbstractAuditable implements
        IMoviedbIdentifiable, Serializable {

    private static final long serialVersionUID = -3336182194593898858L;
    /**
     * This is the series identifier. This will be generated from a scanned file name by "<filetitle>_<fileyear>" This is needed in
     * order to have the possibility to associate season to series.
     */
    @NaturalId
    @Column(name = "identifier", unique = true, length = 200)
    private String identifier;
    @Index(name = "series_title")
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    @Column(name = "title_original", length = 255)
    private String titleOriginal;
    @Column(name = "start_year")
    private int startYear = -1;
    @Column(name = "end_year")
    private int endYear = -1;
    @Lob
    @Column(name = "plot", length = 50000)
    private String plot;
    @Lob
    @Column(name = "outline", length = 50000)
    private String outline;
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_ids", joinColumns =
            @JoinColumn(name = "series_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length = 40)
    @Column(name = "moviedb_id", length = 200)
    private Map<String, String> moviedbIdMap = new HashMap<String, String>(0);
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_ratings", joinColumns =
            @JoinColumn(name = "series_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length = 40)
    @Column(name = "rating", length = 30)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_override", joinColumns =
            @JoinColumn(name = "series_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKey(type =
            @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30)
    private Map<OverrideFlag, String> overrideFlags = new HashMap<OverrideFlag, String>(0);
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "series")
    private Set<Season> seasons = new HashSet<Season>(0);

    // GETTER and SETTER
    public String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    public void setTitle(String title, String source) {
        if (!StringUtils.isBlank(title)) {
            setTitle(title);
            setOverrideFlag(OverrideFlag.TITLE, source);
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

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

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public Map<String, String> getMoviedbIdMap() {
        return moviedbIdMap;
    }

    @Override
    public String getMoviedbId(String moviedb) {
        return moviedbIdMap.get(moviedb);
    }

    public void setMoviedbIdMap(Map<String, String> moviedbIdMap) {
        this.moviedbIdMap = moviedbIdMap;
    }

    @Override
    public void setMoviedbId(String moviedb, String id) {
        if (StringUtils.isNotBlank(id)) {
            moviedbIdMap.put(moviedb, id);
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

    public void setOverrideFlag(OverrideFlag overrideFlag, String source) {
        this.overrideFlags.put(overrideFlag, source);
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
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.identifier == null ? 0 : this.identifier.hashCode());
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
