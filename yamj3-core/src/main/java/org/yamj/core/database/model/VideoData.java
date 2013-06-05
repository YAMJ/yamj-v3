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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.OverrideFlag;

@SuppressWarnings("unused")
@javax.persistence.Entity
@javax.persistence.Table(name = "videodata")
@org.hibernate.annotations.Table(appliesTo = "videodata",
    indexes = {
        @Index(name = "videodata_title", columnNames = {"title"}),
        @Index(name = "videodata_status", columnNames = {"status"})
    })
public class VideoData extends AbstractMetadata {

    private static final long serialVersionUID = 5719107822219333629L;

    @Column(name = "episode", nullable = false)
    private int episode = -1;

    @Index(name = "videodata_publication_year")
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
    @JoinTable(name = "videodata_ids", joinColumns = @JoinColumn(name = "videodata_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200)
    private Map<String, String> sourcedbIdMap = new HashMap<String, String>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_ratings", joinColumns = @JoinColumn(name = "videodata_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", length = 30)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "videodata_override", joinColumns =@JoinColumn(name = "videodata_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @Column(name = "source", length = 30)
    private Map<OverrideFlag, String> overrideFlags = new HashMap<OverrideFlag, String>(0);

    @ManyToMany
    @ForeignKey(name = "FK_DATAGENRES_VIDEODATA", inverseName = "FK_DATAGENRES_GENRE")
    @JoinTable(name = "videodata_genres",
        joinColumns = {@JoinColumn(name = "data_id")},
        inverseJoinColumns = {@JoinColumn(name = "genre_id")})
    private Set<Genre> genres = new HashSet<Genre>(0);

    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_VIDEODATA_SEASON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "season_id")
    private Season season;

    @ManyToMany(mappedBy = "videoDatas")
    private Set<MediaFile> mediaFiles = new HashSet<MediaFile>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderColumn(name = "ordering", nullable = false)
    @JoinColumn(name = "videodata_id", nullable = false, insertable = false, updatable = false)
    private List<CastCrew> credits = new ArrayList<CastCrew>(0);

    @Transient
    private List<CreditDTO> creditDTOS = new ArrayList<CreditDTO>(0);

    // GETTER and SETTER

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

    public String getTagline() {
        return tagline;
    }

    private void setTagline(String tagline) {
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

    private void setQuote(String quote) {
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

    private void setCountry(String country) {
        this.country = country;
    }

    public void setCountry(String country, String source) {
        if (StringUtils.isNotBlank(country)) {
            setCountry(country);
            setOverrideFlag(OverrideFlag.COUNTRY, source);
        }
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

    public void addCredit(CastCrew credit) {
        this.credits.add(credit);
    }

    public List<CreditDTO> getCreditDTOS() {
        return creditDTOS;
    }

    public void addCreditDTO(CreditDTO creditDTO) {
        this.creditDTOS.add(creditDTO);
    }

    public void addCredditDTOS(List<CreditDTO> creditDTOS) {
        this.creditDTOS.addAll(creditDTOS);
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

    public void setTvEpisodeMissing() {
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
        if (!(other instanceof VideoData)) {
            return false;
        }
        VideoData castOther = (VideoData) other;
        return StringUtils.equals(this.identifier, castOther.identifier);
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
