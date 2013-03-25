package com.moviejukebox.core.database.model;

import com.moviejukebox.core.database.model.type.OverrideFlag;
import com.moviejukebox.core.database.model.type.VideoType;
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
import org.hibernate.annotations.*;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.Parameter;

@TypeDefs({
    @TypeDef(name = "videoType", 
        typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.VideoType")}),
    @TypeDef(name = "overrideFlag", 
        typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.OverrideFlag")})
})

@Entity
@Table(name = "video_data")
@SuppressWarnings("deprecation")
public class VideoData extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 5719107822219333629L;

    /**
     * This is the media file identifier.
     * This will be generated from a media file "<filetitle>_<fileyear>_<videotype>
     * This is needed in order to have the possibility to assoziate media files to
     * video metadata, i.e. if a new episode of a TV show has been scanned.
     */
    @NaturalId(mutable = false)
    @Column(name = "media_file_identifier", unique = true, length = 255)
    private String mediaFileIdentifier;

    @Type(type = "videoType")
    @Column(name = "video_type", nullable = false)
    private VideoType videoType;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "pulication_year", length = 10)
	private String publicationYear;

    @Column(name = "title_original", length = 255)
    private String titleOriginal;

    @Column(name = "title_index", length = 255)
    private String titleIndex;

    @Column(name = "release_date", length = 10)
    private String releaseDate;

    @Column(name = "top_rank")
    private int topRank = -1;

    @Lob
    @Column(name = "plot")
    private String plot;

    @Lob
    @Column(name = "outline")
    private String outline;

    @Lob
    @Column(name = "tagline")
    private String  tagline;

    @Lob
    @Column(name = "quote")
    private String  quote;

    @Column(name = "country", length = 100)
    private String country;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "moviedb_ids", joinColumns = @JoinColumn(name = "data_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length= 40)
    @Column(name = "moviedb_id", length = 200)
    private Map<String, String> movieIds = new HashMap<String, String>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "moviedb_ratings", joinColumns = @JoinColumn(name = "data_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length= 40)
    @Column(name = "rating", length = 30)
    private Map<String, Integer> movieRatings = new HashMap<String, Integer>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "data_override", joinColumns = @JoinColumn(name = "data_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length= 30)
    @MapKey(type = @Type(type = "overrideFlag"))    
    @Column(name = "source", length = 30)
    private Map<OverrideFlag, String> overrideFlags = new HashMap<OverrideFlag, String>(0);

    @ManyToMany
    @ForeignKey(name = "FK_MOVIEGENRES_DATA", inverseName = "FK_MOVIEGENRES_GENRE")
    @JoinTable(name= "movie_genres",
            joinColumns={@JoinColumn(name="data_id")},
            inverseJoinColumns={@JoinColumn(name="genre_id")})
    private Set<Genre> genres = new HashSet<Genre>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "videoData")
    private Set<MediaFile> mediaFiles = new HashSet<MediaFile>(0);
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "videoData")
    private Set<VideoSet> videoSets = new HashSet<VideoSet>(0);

    // GETTER and SETTER
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    public VideoType getVideoType() {
        return videoType;
    }

    public void setVideoType(VideoType videoType) {
        this.videoType = videoType;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public String getTitleIndex() {
        return titleIndex;
    }

    public void setTitleIndex(String titleIndex) {
        this.titleIndex = titleIndex;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
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

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
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

    public void setCountry(String country) {
        this.country = country;
    }

    public Map<String, String> getMovieIds() {
        return movieIds;
    }

    public void setMovieIds(Map<String, String> movieIds) {
        this.movieIds = movieIds;
    }

    public Map<String, Integer> getMovieRatings() {
        return movieRatings;
    }

    public void setMovieRatings(Map<String, Integer> movieRatings) {
        this.movieRatings = movieRatings;
    }

    public Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    public void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Set<MediaFile> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(Set<MediaFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public Set<VideoSet> getVideoSets() {
        return videoSets;
    }

    public void setBoxedSets(Set<VideoSet> videoSets) {
        this.videoSets = videoSets;
    }
}
