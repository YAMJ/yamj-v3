/*
 *      Copyright (c) 2004-2013 YAMJ Members
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.OverrideFlag;

@SuppressWarnings("unused")
@Entity
@Table(name = "videodata",
        uniqueConstraints
        = @UniqueConstraint(name = "UIX_VIDEODATA_NATURALID", columnNames = {"identifier"}))
@org.hibernate.annotations.Table(appliesTo = "videodata",
        indexes = {
            @Index(name = "IX_VIDEODATA_TITLE", columnNames = {"title"}),
            @Index(name = "IX_VIDEODATA_STATUS", columnNames = {"status"})
        })
public class VideoData extends AbstractMetadata implements IDataGenres, IDataCredits {

    private static final long serialVersionUID = 1L;
    @Column(name = "episode", nullable = false)
    private int episode = -1;
    @Index(name = "IX_VIDEODATA_PUBLICATIONYEAR")
    @Column(name = "publication_year", nullable = false)
    private int publicationYear = -1;
    @Column(name = "release_date", length = 10)
    private String releaseDate;
    @Column(name = "top_rank", nullable = false)
    private int topRank = -1;
    @Lob
    @Column(name = "tagline", length = 25000)
    private String tagline;
    @Lob
    @Column(name = "quote", length = 25000)
    private String quote;
    @Column(name = "country", length = 100)
    private String country;
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_ids", joinColumns
            = @JoinColumn(name = "videodata_id"))
    @ForeignKey(name = "FK_VIDEODATA_SOURCEIDS")
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<String, String>(0);
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_ratings", joinColumns
            = @JoinColumn(name = "videodata_id"))
    @ForeignKey(name = "FK_VIDEODATA_RATINGS")
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", length = 30, nullable = false)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_override", joinColumns
            = @JoinColumn(name = "videodata_id"))
    @ForeignKey(name = "FK_VIDEODATA_OVERRIDE")
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value
            = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<OverrideFlag, String>(OverrideFlag.class);
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
    @ForeignKey(name = "FK_REL_VIDEODATA_MEDIAFILE")
    private Set<MediaFile> mediaFiles = new HashSet<MediaFile>(0);
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderColumn(name = "ordering", nullable = false)
    @JoinColumn(name = "videodata_id", nullable = false, insertable = false, updatable = false)
    private List<CastCrew> credits = new ArrayList<CastCrew>(0);
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "videoData")
    private List<Artwork> artworks = new ArrayList<Artwork>(0);
    @Transient
    private final Set<CreditDTO> creditDTOS = new LinkedHashSet<CreditDTO>(0);
    @Transient
    private Set<String> genreNames = new LinkedHashSet<String>(0);

    // GETTER and SETTER
    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
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

    public void setReleaseDate(String releaseDate) {
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

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public void setTagline(String tagline, String source) {
        if (StringUtils.isNotBlank(tagline)) {
            this.tagline = tagline;
            setOverrideFlag(OverrideFlag.TAGLINE, source);
        }
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public void setQuote(String quote, String source) {
        if (StringUtils.isNotBlank(quote)) {
            this.quote = quote;
            setOverrideFlag(OverrideFlag.QUOTE, source);
        }
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setCountry(String country, String source) {
        if (StringUtils.isNotBlank(country)) {
            setCountry(country);
            setOverrideFlag(OverrideFlag.COUNTRY, source);
        }
    }

    public Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    @Override
    public String getSourceDbId(String sourceDb) {
        return sourceDbIdMap.get(sourceDb);
    }

    public void setSourceDbIdMap(Map<String, String> sourceDbIdMap) {
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

    public void setRatings(Map<String, Integer> ratings) {
        this.ratings = ratings;
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
        this.overrideFlags.put(overrideFlag, source);
    }

    @JsonIgnore // This is not needed for the API
    @Override
    public String getOverrideSource(OverrideFlag overrideFlag) {
        return overrideFlags.get(overrideFlag);
    }

    /**
     * Get the genres
     *
     * @return
     */
    @Override
    public Set<Genre> getGenres() {
        return genres;
    }

    /**
     * Set the genres
     *
     * @param genres
     */
    @Override
    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
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

    public void addCredit(CastCrew credit) {
        this.credits.add(credit);
    }

    public List<Artwork> getArtworks() {
        return artworks;
    }

    public void setArtworks(List<Artwork> artworks) {
        this.artworks = artworks;
    }

    // TRANSIENTS METHODS
    @JsonIgnore // This is not needed for the API
    @Override
    public Set<CreditDTO> getCreditDTOS() {
        return creditDTOS;
    }

    @Override
    public void addCreditDTO(CreditDTO creditDTO) {
        this.creditDTOS.add(creditDTO);
    }

    @Override
    public void addCreditDTOS(Set<CreditDTO> creditDTOS) {
        this.creditDTOS.addAll(creditDTOS);
    }

    /**
     * Get the string representation of the genres
     *
     * Usually populated from the source site
     *
     * @return
     */
    @Override
    public Set<String> getGenreNames() {
        return genreNames;
    }

    /**
     * Set the string representation of the genres
     *
     * Usually populated from the source site
     *
     * @param genreNames
     * @param source
     */
    @Override
    public void setGenreNames(Set<String> genreNames, String source) {
        if (CollectionUtils.isNotEmpty(genreNames)) {
            this.genreNames = genreNames;
            setOverrideFlag(OverrideFlag.GENRES, source);
        }
    }

    // TV CHECKS
    public boolean isScannableTvEpisode() {
        if (StatusType.DONE.equals(this.getStatus())) {
            return false;
        } else if (this.getEpisode() < 0) {
            return false;
        }
        return true;
    }

    public void setTvEpisodeScanned() {
        this.setStatus(StatusType.PROCESSED);
    }

    public void setTvEpisodeNotFound() {
        this.setStatus(StatusType.NOTFOUND);
    }

    @JsonIgnore // This is not needed for the API
    @Override
    public int getSeasonNumber() {
        if (isMovie()) {
            return -1;
        }
        return getSeason().getSeason();
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
        if (!(other instanceof VideoData)) {
            return false;
        }
        VideoData castOther = (VideoData) other;
        return StringUtils.equals(getIdentifier(), castOther.getIdentifier());
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
        sb.append(", title=");
        sb.append(getYear());
        sb.append("]");
        return sb.toString();
    }
}
