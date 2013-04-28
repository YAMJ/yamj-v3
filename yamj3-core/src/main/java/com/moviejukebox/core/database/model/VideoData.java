package com.moviejukebox.core.database.model;

import org.hibernate.annotations.Index;

import com.moviejukebox.core.database.model.dto.CreditDTO;
import com.moviejukebox.core.database.model.type.OverrideFlag;
import com.moviejukebox.common.type.StatusType;
import com.moviejukebox.core.hibernate.usertypes.EnumStringUserType;
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.collections.CollectionUtils;
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
        @Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.StatusType")})
})
@Entity
@Table(name = "videodata")
@SuppressWarnings({"unused", "deprecation"})
public class VideoData extends AbstractAuditable implements
        IMoviedbIdentifiable, Serializable {

    private static final long serialVersionUID = 5719107822219333629L;
    /**
     * This is the video data identifier. This will be generated from a scanned file name by
     * "<filetitle>_<fileyear>_<season>_<episode>
     * This is needed in order to have the possibility to associate media files to video meta data, i.e. if a new episode of a TV
     * show has been scanned.
     */
    @NaturalId
    @Column(name = "identifier", unique = true, length = 200)
    private String identifier;
    @Index(name = "videodata_title")
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    @Column(name = "episode", nullable = false)
    private int episode = -1;
    @Index(name = "videodata_publication_year")
    @Column(name = "publication_year")
    private int publicationYear = -1;
    @Column(name = "title_original", length = 255)
    private String titleOriginal;
    @Column(name = "release_date", length = 10)
    private String releaseDate;
    @Column(name = "top_rank")
    private int topRank = -1;
    @Lob
    @Column(name = "plot", length = 50000)
    private String plot;
    @Lob
    @Column(name = "outline", length = 50000)
    private String outline;
    @Lob
    @Column(name = "tagline", length = 25000)
    private String tagline;
    @Lob
    @Column(name = "quote", length = 25000)
    private String quote;
    @Column(name = "country", length = 100)
    private String country;
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_ids", joinColumns =
            @JoinColumn(name = "videodata_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length = 40)
    @Column(name = "moviedb_id", length = 200)
    private Map<String, String> moviedbIdMap = new HashMap<String, String>(0);
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_ratings", joinColumns =
            @JoinColumn(name = "videodata_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length = 40)
    @Column(name = "rating", length = 30)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_override", joinColumns =
            @JoinColumn(name = "videodata_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKey(type =
            @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30)
    private Map<OverrideFlag, String> overrideFlags = new HashMap<OverrideFlag, String>(0);
    @ManyToMany
    @ForeignKey(name = "FK_DATAGENRES_VIDEODATA", inverseName = "FK_DATAGENRES_GENRE")
    @JoinTable(name = "videodata_genres",
            joinColumns = {
        @JoinColumn(name = "data_id")},
            inverseJoinColumns = {
        @JoinColumn(name = "genre_id")})
    private Set<Genre> genres = new HashSet<Genre>(0);
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEODATA_SEASON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "season_id")
    private Season season;
    @ManyToMany(mappedBy = "videoDatas")
    private Set<MediaFile> mediaFiles = new HashSet<MediaFile>(0);
    @OneToMany(cascade = CascadeType.DETACH, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderColumn(name = "order_data", nullable = false)
    @JoinColumn(name = "data_id", nullable = false, insertable = false, updatable = false)
    private List<CastCrew> credits = new ArrayList<CastCrew>(0);
    @Transient
    private List<CreditDTO> creditDTOS = new ArrayList<CreditDTO>(0);

    // GETTER and SETTER
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    public void setTitle(String title, String source) {
        if (StringUtils.isNotBlank(title)) {
            setTitle(title);
            setOverrideFlag(OverrideFlag.TITLE, source);
        }
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    private void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal, String source) {
        if (StringUtils.isNotBlank(titleOriginal)) {
            setTitleOriginal(titleOriginal);
            setOverrideFlag(OverrideFlag.ORIGINALTITLE, source);
        }
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    private void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public void setPublicationYear(int publicationYear, String source) {
        if (publicationYear > 0) {
            setPublicationYear(publicationYear);
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    private void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setReleaseDate(String releaseDate, String source) {
        if (StringUtils.isNotBlank(releaseDate)) {
            this.releaseDate = releaseDate;
            setOverrideFlag(OverrideFlag.RELEASEDATE, source);
        }
    }

    public int getTopRank() {
        return topRank;
    }

    public void setTopRank(int topRank) {
        this.topRank = topRank;
    }

    public String getPlot() {
        return plot;
    }

    private void setPlot(String plot) {
        this.plot = plot;
    }

    public void setPlot(String plot, String source) {
        if (StringUtils.isNotBlank(plot)) {
            setPlot(plot);
            setOverrideFlag(OverrideFlag.PLOT, source);
        }
    }

    public String getOutline() {
        return outline;
    }

    private void setOutline(String outline) {
        this.outline = outline;
    }

    public void setOutline(String outline, String source) {
        if (StringUtils.isNotBlank(outline)) {
            setOutline(outline);
            setOverrideFlag(OverrideFlag.OUTLINE, source);
        }
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public String getCountry() {
        return country;
    }

    private void setCountry(String country) {
        this.country = country;
    }

    public void setCountry(String country, String source) {
        if (StringUtils.isNotBlank(country)) {
            setCountry(country);
            setOverrideFlag(OverrideFlag.COUNTRY, source);
        }
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

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public void setGenres(Collection<String> genres, String source) {
        if (CollectionUtils.isNotEmpty(genres)) {
            this.genres.clear();
            for (String genre : genres) {
                this.genres.add(new Genre(genre));
            }
        }
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Set<MediaFile> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(Set<MediaFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public void addMediaFile(MediaFile mediaFile) {
        this.mediaFiles.add(mediaFile);
    }

    public List<CastCrew> getCredits() {
        return credits;
    }

    public void setCredits(List<CastCrew> credits) {
        this.credits = credits;
    }

    public void addCredit(CastCrew castCrew) {
        this.credits.add(castCrew);
    }

    public List<CreditDTO> getCreditDTOS() {
        return creditDTOS;
    }

    public void addCreditDTO(CreditDTO creditDTO) {
        this.creditDTOS.add(creditDTO);
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
        if (!(other instanceof VideoData)) {
            return false;
        }
        VideoData castOther = (VideoData) other;
        return StringUtils.equals(this.identifier, castOther.identifier);
    }
}
