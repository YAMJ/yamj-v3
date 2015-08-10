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

import java.util.*;
import java.util.Map.Entry;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.Table;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.award.MovieAward;
import org.yamj.core.database.model.dto.AwardDTO;
import org.yamj.core.database.model.dto.BoxedSetDTO;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.OverrideFlag;

@Entity
@Table(name = "videodata",
        uniqueConstraints = @UniqueConstraint(name = "UIX_VIDEODATA_NATURALID", columnNames = {"identifier"}),
        indexes = {
            @Index(name = "IX_VIDEODATA_TITLE", columnList = "title"),
            @Index(name = "IX_VIDEODATA_STATUS", columnList = "status"),
            @Index(name = "IX_VIDEODATA_PUBLICATIONYEAR", columnList = "publication_year")}
)
@SuppressWarnings("unused")
public class VideoData extends AbstractMetadata {

    private static final long serialVersionUID = 885531396557944590L;

    @Column(name = "episode", nullable = false)
    private int episode = -1;

    @Column(name = "publication_year", nullable = false)
    private int publicationYear = -1;

    @Column(name = "release_country_code", length = 4)
    private String releaseCountryCode;

    @Temporal(value = TemporalType.DATE)
    @Column(name = "release_date")
    private Date releaseDate;

    @Column(name = "top_rank", nullable = false)
    private int topRank = -1;

    @Lob
    @Column(name = "tagline", length = 25000)
    private String tagline;

    @Lob
    @Column(name = "quote", length = 25000)
    private String quote;

    @Column(name = "watched_nfo", nullable = false)
    private boolean watchedNfo = false;

    @Column(name = "watched_file", nullable = false)
    private boolean watchedFile = false;

    @Column(name = "watched_api", nullable = false)
    private boolean watchedApi = false;

    @Column(name = "skip_scan_nfo", length = 255)
    private String skipScanNfo;

    @Column(name = "skip_scan_api", length = 255)
    private String skipScanApi;

    @Type(type = "statusType")
    @Column(name = "trailer_status", nullable = false, length = 30)
    private StatusType trailerStatus;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "trailer_last_scanned")
    private Date trailerLastScanned;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_ids",
            joinColumns = @JoinColumn(name = "videodata_id", foreignKey = @ForeignKey(name = "FK_VIDEODATA_SOURCEIDS")))
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_ratings",
            joinColumns = @JoinColumn(name = "videodata_id"), foreignKey = @ForeignKey(name = "FK_VIDEODATA_RATINGS"))
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", nullable = false)
    private Map<String, Integer> ratings = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_override",
            joinColumns = @JoinColumn(name = "videodata_id", foreignKey = @ForeignKey(name = "FK_VIDEODATA_OVERRIDE")))
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<>(OverrideFlag.class);

    @ManyToMany
    @JoinTable(name = "videodata_genres",
            joinColumns = {@JoinColumn(name = "data_id", foreignKey = @ForeignKey(name = "FK_DATAGENRES_VIDEODATA"))},
            inverseJoinColumns = {@JoinColumn(name = "genre_id", foreignKey = @ForeignKey(name = "FK_DATAGENRES_GENRE"))})
    private Set<Genre> genres = new HashSet<>(0);

    @ManyToMany
    @JoinTable(name = "videodata_studios",
            joinColumns = {@JoinColumn(name = "data_id", foreignKey = @ForeignKey(name = "FK_DATASTUDIOS_VIDEODATA"))},
            inverseJoinColumns = {@JoinColumn(name = "studio_id", foreignKey = @ForeignKey(name = "FK_DATASTUDIOS_STUDIO"))})
    private Set<Studio> studios = new HashSet<>(0);

    @ManyToMany
    @JoinTable(name = "videodata_countries",
            joinColumns = {@JoinColumn(name = "data_id", foreignKey = @ForeignKey(name = "FK_DATACOUNTRIES_VIDEODATA"))},
            inverseJoinColumns = {@JoinColumn(name = "country_id", foreignKey = @ForeignKey(name = "FK_DATACOUNTRIES_COUNTRY"))})
    private Set<Country> countries = new HashSet<>(0);

    @ManyToMany
    @JoinTable(name = "videodata_certifications",
            joinColumns = {@JoinColumn(name = "data_id", foreignKey = @ForeignKey(name = "FK_DATACERTS_VIDEODATA"))},
            inverseJoinColumns = {@JoinColumn(name = "cert_id", foreignKey = @ForeignKey(name = "FK_DATACERTS_CERTIFICATION"))})
   
    private Set<Certification> certifications = new HashSet<>(0);

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_VIDEODATA_SEASON"), name = "season_id")
    private Season season;

    @ManyToMany(mappedBy = "videoDatas")
    private Set<MediaFile> mediaFiles = new HashSet<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "castCrewPK.videoData")
    private List<CastCrew> credits = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "videoData")
    private List<Artwork> artworks = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "videoData")
    private List<BoxedSetOrder> boxedSets = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "nfoRelationPK.videoData")
    private List<NfoRelation> nfoRelations = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "movieAwardPK.videoData")
    private List<MovieAward> movieAwards = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "videoData")
    private List<Trailer> trailers = new ArrayList<>(0);

    @Transient
    private Set<String> genreNames;

    @Transient
    private Set<String> studioNames;

    @Transient
    private Set<String> countryCodes;

    @Transient
    private final Map<String, String> certificationInfos = new HashMap<>(0);

    @Transient
    private final Set<BoxedSetDTO> boxedSetDTOS = new HashSet<>(0);

    @Transient
    private final Set<CreditDTO> creditDTOS = new LinkedHashSet<>(0);

    @Transient
    private final Map<String, String> posterURLS = new HashMap<>(0);

    @Transient
    private final Map<String, String> fanartURLS = new HashMap<>(0);

    @Transient
    private final Set<AwardDTO> awardDTOS = new HashSet<>(0);

    // CONSTRUCTORS
    
    public VideoData() {
        super();
    }

    public VideoData(String identifier) {
        super(identifier);
    }

    // GETTER and SETTER

    @Override
    public void removeTitle(String source) {
        if (hasOverrideSource(OverrideFlag.TITLE, source)) {
            if (isMovie()) { // just for movies
                String[] splitted = getIdentifier().split("_");
                setTitle(splitted[0]);
            }
            removeOverrideFlag(OverrideFlag.TITLE);
        }
    }

    @Override
    public void removeTitleOriginal(String source) {
        if (hasOverrideSource(OverrideFlag.ORIGINALTITLE, source)) {
            if (isMovie()) { // just for movies
                String[] splitted = getIdentifier().split("_");
                setTitleOriginal(splitted[0]);
            }
            removeOverrideFlag(OverrideFlag.ORIGINALTITLE);
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
            this.publicationYear = publicationYear;
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    public void removePublicationYear(String source) {
        if (hasOverrideSource(OverrideFlag.YEAR, source)) {
            if (isMovie()) { // just for movies
                String[] splitted = this.getIdentifier().split("_");
                int splitYear = Integer.parseInt(splitted[1]);
                this.publicationYear = (splitYear > 0 ? splitYear : -1); 
            }
            removeOverrideFlag(OverrideFlag.YEAR);
        }
    }

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public String getReleaseCountryCode() {
        return releaseCountryCode;
    }

    private void setReleaseCountryCode(String releaseCountryCode) {
        this.releaseCountryCode = releaseCountryCode;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    private void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setRelease(Date releaseDate, String source) {
        setRelease(null, releaseDate, source);
    }

    public void setRelease(String releaseCountryCode, Date releaseDate, String source) {
        if (releaseDate != null) {
            this.releaseCountryCode = releaseCountryCode;
            this.releaseDate = releaseDate;
            setOverrideFlag(OverrideFlag.RELEASEDATE, source);
        }
    }

    public void removeRelease(String source) {
        if (hasOverrideSource(OverrideFlag.RELEASEDATE, source)) {
            this.releaseCountryCode = null;
            this.releaseDate = null;
            removeOverrideFlag(OverrideFlag.RELEASEDATE);
        }
    }

    public int getTopRank() {
        return topRank;
    }

    public void setTopRank(int topRank) {
        if (topRank > 0) {
            this.topRank = topRank;
        }
    }

    public void removeTopRank() {
        this.topRank = -1;
    }
    
    public String getTagline() {
        return tagline;
    }

    private void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public void setTagline(String tagline, String source) {
        if (StringUtils.isNotBlank(tagline)) {
            this.tagline = tagline.trim();
            setOverrideFlag(OverrideFlag.TAGLINE, source);
        }
    }

    public void removeTagline(String source) {
        if (hasOverrideSource(OverrideFlag.TAGLINE, source)) {
            this.tagline = null;
            removeOverrideFlag(OverrideFlag.TAGLINE);
        }
    }

    public String getQuote() {
        return quote;
    }

    private void setQuote(String quote) {
        this.quote = quote;
    }

    public void setQuote(String quote, String source) {
        if (StringUtils.isNotBlank(quote)) {
            this.quote = quote.trim();
            setOverrideFlag(OverrideFlag.QUOTE, source);
        }
    }

    public void removeQuote(String source) {
        if (hasOverrideSource(OverrideFlag.QUOTE, source)) {
            this.quote = null;
            removeOverrideFlag(OverrideFlag.QUOTE);
        }
    }

    @Override
    public Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    public void setSourceDbIdMap(Map<String, String> sourceDbIdMap) {
        this.sourceDbIdMap = sourceDbIdMap;
    }

    private boolean isWatchedNfo() {
        return watchedNfo;
    }

    public void setWatchedNfo(boolean watchedNfo) {
        this.watchedNfo = watchedNfo;
    }

    public boolean isWatchedFile() {
        return watchedFile;
    }

    public void setWatchedFile(boolean watchedFile) {
        this.watchedFile = watchedFile;
    }

    public boolean isWatchedApi() {
        return watchedApi;
    }

    public void setWatchedApi(boolean watchedApi) {
        this.watchedApi = watchedApi;
    }

    private String getSkipScanNfo() {
        return skipScanNfo;
    }

    private void setSkipScanNfo(String skipScanNfo) {
        this.skipScanNfo = skipScanNfo;
    }

    @Override
    protected String getSkipScanApi() {
        return skipScanApi;
    }

    @Override
    protected void setSkipScanApi(String skipScanApi) {
        this.skipScanApi = skipScanApi;
    }
    
    private Map<String, Integer> getRatings() {
        return ratings;
    }

    private void setRatings(Map<String, Integer> ratings) {
        this.ratings = ratings;
    }

    public void addRating(String sourceDb, int rating) {
        if (StringUtils.isNotBlank(sourceDb) && (rating >= 0)) {
            this.ratings.put(sourceDb, rating);
        }
    }

    public void removeRating(String sourceDb) {
        if (StringUtils.isNotBlank(sourceDb)) {
            this.ratings.remove(sourceDb);
        }
    }
   
    @Override
    protected Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    private void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    public boolean isAllScansSkipped() {
        if ("all".equalsIgnoreCase(getSkipScanNfo())) return true;
        if ("all".equalsIgnoreCase(getSkipScanApi())) return true;
        return false;
    }

    @Override
    public boolean isSkippedScan(String sourceDb) {
        if (isMovie()) {
            // skip movie
            if (getSkipScanNfo() == null && getSkipScanApi() == null) return false;
            if ("all".equalsIgnoreCase(getSkipScanNfo())) return true;
            if ("all".equalsIgnoreCase(getSkipScanApi())) return true;
            if (StringUtils.containsIgnoreCase(getSkipScanNfo(), sourceDb)) return true;
            if (StringUtils.containsIgnoreCase(getSkipScanApi(), sourceDb)) return true;
            return false;
        }
        // skip episode
        return getSeason().isSkippedScan(sourceDb);
    }
    
    public void setSkippendScansNfo(Set<String> skippedScans) {
        if (CollectionUtils.isEmpty(skippedScans)) {
            setSkipScanNfo(null);
        } else {
            setSkipScanNfo(StringUtils.join(skippedScans, ';'));
        }
    }

    public StatusType getTrailerStatus() {
        return trailerStatus;
    }

    public void setTrailerStatus(StatusType trailerStatus) {
        this.trailerStatus = trailerStatus;
    }

    public Date getTrailerLastScanned() {
        return trailerLastScanned;
    }

    public void setTrailerLastScanned(Date trailerLastScanned) {
        this.trailerLastScanned = trailerLastScanned;
    }
    
    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Set<Studio> getStudios() {
        return studios;
    }

    public void setStudios(Set<Studio> studios) {
        this.studios = studios;
    }

    public Set<Country> getCountries() {
        return countries;
    }

    public void setCountries(Set<Country> countries) {
        this.countries = countries;
    }

    public Set<Certification> getCertifications() {
        return certifications;
    }

    public void setCertifications(Set<Certification> certifications) {
        this.certifications = certifications;
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

    private void setMediaFiles(Set<MediaFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public void addMediaFile(MediaFile mediaFile) {
        this.mediaFiles.add(mediaFile);
    }

    public List<CastCrew> getCredits() {
        return credits;
    }

    private void setCredits(List<CastCrew> credits) {
        this.credits = credits;
    }

    public List<Artwork> getArtworks() {
        return artworks;
    }

    public Artwork getArtwork(ArtworkType artworkType) {
        for (Artwork artwork : getArtworks()) {
            if (artworkType.equals(artwork.getArtworkType())) {
                return artwork;
            }
        }
        return null;
    }

    public void setArtworks(List<Artwork> artworks) {
        this.artworks = artworks;
    }

    public List<BoxedSetOrder> getBoxedSets() {
        return boxedSets;
    }

    private void setBoxedSets(List<BoxedSetOrder> boxedSets) {
        this.boxedSets = boxedSets;
    }

    public void addBoxedSet(BoxedSetOrder boxedSet) {
        this.boxedSets.add(boxedSet);
    }

    public List<NfoRelation> getNfoRelations() {
        return nfoRelations;
    }

    public void setNfoRelations(List<NfoRelation> nfoRelations) {
        this.nfoRelations = nfoRelations;
    }

    public void addNfoRelation(NfoRelation nfoRelation) {
        this.nfoRelations.add(nfoRelation);
    }

    public List<MovieAward> getMovieAwards() {
        return movieAwards;
    }

    public void setMovieAwards(List<MovieAward> movieAwards) {
        this.movieAwards = movieAwards;
    }

    public List<Trailer> getTrailers() {
        return trailers;
    }

    public void setTrailers(List<Trailer> trailers) {
        this.trailers = trailers;
    }
    
    // TRANSIENT METHODS
   
    public boolean isWatched() {
        return (this.watchedNfo || this.watchedFile || this.watchedApi);
    }

    public Set<CreditDTO> getCreditDTOS() {
        return creditDTOS;
    }

    public void addCreditDTO(CreditDTO creditDTO) {
        this.creditDTOS.add(creditDTO);
    }

    public void addCreditDTOS(Set<CreditDTO> creditDTOS) {
        this.creditDTOS.addAll(creditDTOS);
    }

    public Set<String> getGenreNames() {
        return genreNames;
    }

    public void setGenreNames(Set<String> genreNames, String source) {
        if (CollectionUtils.isNotEmpty(genreNames)) {
            this.genreNames = genreNames;
            setOverrideFlag(OverrideFlag.GENRES, source);
        }
    }

    public Set<String> getStudioNames() {
        return studioNames;
    }

    public void setStudioNames(Set<String> studioNames, String source) {
        if (CollectionUtils.isNotEmpty(studioNames)) {
            this.studioNames = studioNames;
            setOverrideFlag(OverrideFlag.STUDIOS, source);
        }
    }

    public Set<String> getCountryCodes() {
        return countryCodes;
    }

    public void setCountryCodes(Set<String> countryCodes, String source) {
        if (CollectionUtils.isNotEmpty(countryCodes)) {
            this.countryCodes = countryCodes;
            setOverrideFlag(OverrideFlag.COUNTRIES, source);
        }
    }

    public Map<String, String> getCertificationInfos() {
        return certificationInfos;
    }

    public void setCertificationInfos(Map<String, String> certificationInfos) {
        if (MapUtils.isNotEmpty(certificationInfos)) {
            for (Entry<String, String> entry : certificationInfos.entrySet()) {
                this.addCertificationInfo(entry.getKey(), entry.getValue());
            }
        }
    }

    public void addCertificationInfo(String countryCode, String certificate) {
        if (StringUtils.isNotBlank(countryCode) && StringUtils.isNotBlank(certificate)) {
            // check if country code already present
            for (String storedCode : this.certificationInfos.keySet()) {
                if (countryCode.equals(storedCode)) {
                    // certificate for country already present
                    return;
                }
            }
            this.certificationInfos.put(countryCode, certificate);
        }
    }

    public Set<BoxedSetDTO> getBoxedSetDTOS() {
        return boxedSetDTOS;
    }

    public void addBoxedSetDTO(String source, String name) {
        this.addBoxedSetDTO(source, name, null, null);
    }

    public void addBoxedSetDTO(String source, String name, Integer ordering) {
        this.addBoxedSetDTO(source, name, ordering, null);
    }

    public void addBoxedSetDTO(String source, String name, Integer ordering, String sourceId) {
        if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(name)) {
            this.boxedSetDTOS.add(new BoxedSetDTO(source, name, ordering, sourceId));
        }
    }

    public Map<String, String> getPosterURLS() {
        return posterURLS;
    }

    public void addPosterURL(String posterURL, String source) {
        if (StringUtils.isNotBlank(posterURL) && StringUtils.isNotBlank(source)) {
            this.posterURLS.put(posterURL.trim(), source);
        }
    }

    public Map<String, String> getFanartURLS() {
        return fanartURLS;
    }

    public void addFanartURL(String fanartURL, String source) {
        if (StringUtils.isNotBlank(fanartURL) && StringUtils.isNotBlank(source)) {
            this.fanartURLS.put(fanartURL.trim(), source);
        }
    }

    public Set<AwardDTO> getAwardDTOS() {
        return awardDTOS;
    }

    public void addAwards(Collection<AwardDTO> awards, String source) {
        if (CollectionUtils.isEmpty(awards)) {
            return;
        }

        for (AwardDTO award : awards) {
            if (StringUtils.isBlank(award.getEvent()) || StringUtils.isBlank(award.getCategory()) || award.getYear() <= 0) {
                // event, category and year must be given
                continue;
            }
            award.setSource(source);
            this.awardDTOS.add(award);
        }
    }

    public void addAward(String event, String category, int year, String source) {
        if (StringUtils.isNotBlank(event) && StringUtils.isNotBlank(category) && year > 0 && StringUtils.isNotBlank(source)) {
            this.awardDTOS.add(new AwardDTO(event, category, year, source).setWon(true));
        }
    }

    // TV CHECKS
    
    public boolean isTvEpisodeDone(String sourceDb) {
        if (StringUtils.isBlank(this.getSourceDbId(sourceDb))) {
            // not done if episode ID not set
            return false;
        }
        return (StatusType.DONE.equals(this.getStatus()));
    }

    public void setTvEpisodeDone() {
        setLastScanned(new Date(System.currentTimeMillis()));
        this.setStatus(StatusType.TEMP_DONE);
    }

    public void setTvEpisodeNotFound() {
        setLastScanned(new Date(System.currentTimeMillis()));
        if (StatusType.DONE.equals(this.getStatus())) {
            // do not reset done
            return;
        } else if (StatusType.TEMP_DONE.equals(this.getStatus())) {
            // do not reset temporary done
            return;
        }
        this.setStatus(StatusType.NOTFOUND);
    }

    @Override
    public int getEpisodeNumber() {
        return episode;
    }

    @Override
    public boolean isMovie() {
        return (episode < 0);
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getIdentifier())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof VideoData)) {
            return false;
        }
        final VideoData other = (VideoData) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getIdentifier(), other.getIdentifier())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VideoData [ID=");
        sb.append(getId());
        sb.append(", identifier=");
        sb.append(getIdentifier());
        sb.append(", title=");
        sb.append(getTitle());
        sb.append(", year=");
        sb.append(getPublicationYear());
        sb.append("]");
        return sb.toString();
    }
}
