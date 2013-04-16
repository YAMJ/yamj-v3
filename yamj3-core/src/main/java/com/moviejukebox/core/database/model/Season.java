package com.moviejukebox.core.database.model;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;

import com.moviejukebox.core.database.model.type.OverrideFlag;
import com.moviejukebox.core.database.model.type.StatusType;
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
        parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.OverrideFlag")}),
    @TypeDef(name = "statusType",
        typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.StatusType")})
})

@Entity
@Table(name = "season")
@SuppressWarnings("deprecation")
public class Season extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 7589022259013410259L;

    /**
     * This is the season identifier.
     * This will be generated from a scanned file name by "<filetitle>_<fileyear>_<season>"
     * This is needed in order to have the possibility to associate video data to
     * seasons, i.e. if a new episode of a TV show has been scanned.
     */
    @NaturalId
    @Column(name = "identifier", unique = true, length = 200)
    private String identifier;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

    @Column(name = "title_original", length = 255)
    private String titleOriginal;

	@Column(name = "season", nullable=false)
	private int season;
	
	@Column(name = "start_year", length = 10)
	private String startYear;

    @Column(name = "end_year", length = 10)
    private String endYear;

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
    @JoinTable(name = "season_ids", joinColumns = @JoinColumn(name = "season_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length= 40)
    @Column(name = "moviedb_id", length = 200)
    private Map<String, String> moviedbIdMap = new HashMap<String, String>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "season_ratings", joinColumns = @JoinColumn(name = "season_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length= 40)
    @Column(name = "rating", length = 30)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "season_override", joinColumns = @JoinColumn(name = "season_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length= 30)
    @MapKey(type = @Type(type = "overrideFlag"))    
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

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public String getStartYear() {
        return startYear;
    }

    public void setStartYear(String startYear) {
        this.startYear = startYear;
    }

    public String getEndYear() {
        return endYear;
    }

    public void setEndYear(String endYear) {
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

    public void setMoviedbIdMap(Map<String, String> moviedbIdMap) {
        this.moviedbIdMap = moviedbIdMap;
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

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.identifier == null?0:this.identifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if ( this == other ) return true;
        if ( other == null ) return false;
        if ( !(other instanceof Season) ) return false;
        Season castOther = (Season)other;
        return StringUtils.equals(this.identifier, castOther.identifier);
    }}
