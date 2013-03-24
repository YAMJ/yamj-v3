package com.moviejukebox.core.database.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.moviejukebox.core.database.model.type.FormatType;
import com.moviejukebox.core.database.model.type.MovieType;
import com.moviejukebox.core.database.model.type.OverrideFlag;
import com.moviejukebox.core.hibernate.usertypes.EnumStringUserType;

@TypeDefs({
    @TypeDef(name = "movieType", 
        typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.MovieType")}),
    @TypeDef(name = "formatType", 
        typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.FormatType")}),
    @TypeDef(name = "overrideFlag", 
        typeClass = EnumStringUserType.class,
        parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.OverrideFlag")})
})

@Entity
@Table(name = "movie")
@SuppressWarnings("deprecation")
public class Movie extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 5719107822219333629L;

    @NaturalId(mutable = true)
	@Column(name = "title", nullable = false, length = 255)
	private String title;

    @NaturalId(mutable = true)
	@Column(name = "year", length = 10)
	private String year;

    @NaturalId(mutable = true)
    @Type(type = "movieType")
    @Column(name = "movieType", nullable = false)
    private MovieType movieType;

    @NaturalId(mutable = true)
    @Type(type = "formatType")
    @Column(name = "formatType", nullable = false)
    private FormatType formatType;

    @Column(name = "title_original", length = 255)
    private String titleOriginal;

    @Column(name = "title_sort", length = 255)
    private String titleSort;

    @Column(name = "mjb_revision", nullable = false, length = 10)
    private String mjbRevision;

    // Safe name for generated files
    @Column(name = "base_name", nullable = false, length = 255)
    private String baseName;

    // Base name for finding posters, NFO, banners, etc.
    @Column(name = "base_filename", nullable = false, length = 255)
    private String baseFilename;

    @Column(name = "release_date", length = 10)
    private String releaseDate;

    @Column(name = "top250")
    private int top250 = -1;

    @Lob
    @Column(name = "plot")
    private String plot;

    @Lob
    @Column(name = "outline")
    private String outline;

    @Lob
    @Column(name = "quote")
    private String  quote;

    @Lob
    @Column(name = "tagline")
    private String  tagline;
 
    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "company", length = 200)
    private String company;
    
    @Column(name = "runtime")
    private int runtime;

    @Column(name = "language", length = 255)
    private String language;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "movie_ids", joinColumns = @JoinColumn(name = "movieId"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length= 40)
    @Column(name = "id", length = 40)
    private Map<String, String> movieIds = new HashMap<String, String>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "movie_ratings", joinColumns = @JoinColumn(name = "movieId"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "source", length= 40)
    @Column(name = "rating", length = 40)
    private Map<String, Integer> movieRatings = new HashMap<String, Integer>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "movie_override", joinColumns = @JoinColumn(name = "movieId"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length= 40)
    @MapKey(type = @Type(type = "overrideFlag"))    
    @Column(name = "source", length = 40)
    private Map<OverrideFlag, String> overrideFlags = new HashMap<OverrideFlag, String>(0);

    @ManyToMany
    @ForeignKey(name = "FK_MOVIEGENRES_MOVIE", inverseName = "FK_MOVIEGENRES_GENRE")
    @JoinTable(name= "movie_genres",
            joinColumns={@JoinColumn(name="movieId")},
            inverseJoinColumns={@JoinColumn(name="genreId")})
    private Set<Genre> genres = new HashSet<Genre>(0);

    @ManyToOne
    @ForeignKey(name = "FK_MOVIE_CERTIFICATION")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "certificationId", nullable = false)
    private Certification certification;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "movie")
    private Set<VideoFile> videoFiles = new HashSet<VideoFile>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "movie")
    private Set<BoxedSet> boxedSets = new HashSet<BoxedSet>(0);

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public MovieType getMovieType() {
        return movieType;
    }

    public void setMovieType(MovieType movieType) {
        this.movieType = movieType;
    }

    public FormatType getFormatType() {
        return formatType;
    }

    public void setFormatType(FormatType formatType) {
        this.formatType = formatType;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public String getTitleSort() {
        return titleSort;
    }

    public void setTitleSort(String titleSort) {
        this.titleSort = titleSort;
    }

    public String getMjbRevision() {
        return mjbRevision;
    }

    public void setMjbRevision(String mjbRevision) {
        this.mjbRevision = mjbRevision;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getBaseFilename() {
        return baseFilename;
    }

    public void setBaseFilename(String baseFilename) {
        this.baseFilename = baseFilename;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getTop250() {
        return top250;
    }

    public void setTop250(int top250) {
        this.top250 = top250;
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

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, String> getMovieIds() {
        return movieIds;
    }

    public void setMovieIds(Map<String, String> movieIds) {
        this.movieIds = movieIds;
    }

    public void addMovieId(String moviedb, String id) {
        this.movieIds.put(moviedb, id);
    }
    
    public Map<String, Integer> getMovieRatings() {
        return movieRatings;
    }

    public void setMovieRatings(Map<String, Integer> movieRatings) {
        this.movieRatings = movieRatings;
    }

    public void addMovieRating(String source, Integer rating) {
        this.movieRatings.put(source, rating);
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

    public void addGenre(String name) {
        Genre genre = new Genre();
        genre.setName(name);
        this.genres.add(genre);
    }
    
    public Certification getCertification() {
        return certification;
    }

    public void setCertification(Certification certification) {
        this.certification = certification;
    }

    public void setCertification(String name) {
        Certification certification = new Certification();
        certification.setName(name);
        this.certification = certification;
    }
    
    public Set<VideoFile> getVideoFiles() {
        return videoFiles;
    }

    public void setVideoFiles(Set<VideoFile> videoFiles) {
        this.videoFiles = videoFiles;
    }
    
    public void addVideoFile(VideoFile videoFile) {
        videoFile.setMovie(this);
        this.videoFiles.add(videoFile);
    }

    public Set<BoxedSet> getBoxedSets() {
        return boxedSets;
    }

    public void setBoxedSets(Set<BoxedSet> boxedSets) {
        this.boxedSets = boxedSets;
    }

    public void addBoxedSet(String name) {
        addBoxedSet(name, -1);
    }
    
    public void addBoxedSet(String name, int order) {
        BoxedSet boxedSet = new BoxedSet();
        boxedSet.setSetOrder(order);
        boxedSet.setSetDescriptor(name);
        boxedSet.setMovie(this);
        this.boxedSets.add(boxedSet);
    }
}
