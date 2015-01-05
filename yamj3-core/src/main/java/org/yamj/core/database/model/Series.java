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
import javax.persistence.Table;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.OverrideFlag;

@Entity
@Table(name = "series",
        uniqueConstraints
        = @UniqueConstraint(name = "UIX_SERIES_NATURALID", columnNames = {"identifier"}))
@org.hibernate.annotations.Table(appliesTo = "series",
        indexes = {
            @Index(name = "IX_SERIES_TITLE", columnNames = {"title"}),
            @Index(name = "IX_SERIES_STATUS", columnNames = {"status"})
        })
@SuppressWarnings("unused")
public class Series extends AbstractMetadata {

    private static final long serialVersionUID = -5782361288021493423L;

    @Column(name = "start_year")
    private int startYear = -1;

    @Column(name = "end_year")
    private int endYear = -1;

    @Column(name = "skip_online_scans", length = 255)
    private String skipOnlineScans;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_ids", joinColumns = @JoinColumn(name = "series_id"))
    @ForeignKey(name = "FK_SERIES_SOURCEIDS")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<String, String>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_ratings", joinColumns = @JoinColumn(name = "series_id"))
    @ForeignKey(name = "FK_SERIES_RATINGS")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", nullable = false)
    private Map<String, Integer> ratings = new HashMap<String, Integer>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "series_override", joinColumns = @JoinColumn(name = "series_id"))
    @ForeignKey(name = "FK_SERIES_OVERRIDE")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<OverrideFlag, String>(OverrideFlag.class);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "series")
    private Set<Season> seasons = new HashSet<Season>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "series")
    private List<Artwork> artworks = new ArrayList<Artwork>(0);

    @ManyToMany
    @ForeignKey(name = "FK_SERIESGENRES_SERIES", inverseName = "FK_SERIESGENRES_GENRE")
    @JoinTable(name = "series_genres",
            joinColumns = {
                @JoinColumn(name = "series_id")},
            inverseJoinColumns = {
                @JoinColumn(name = "genre_id")})
    private Set<Genre> genres = new HashSet<Genre>(0);

    @ManyToMany
    @ForeignKey(name = "FK_SERIESCERTS_SERIES", inverseName = "FK_SERIESCERTS_CERTIFICATION")
    @JoinTable(name = "series_certifications",
            joinColumns = @JoinColumn(name = "series_id"),
            inverseJoinColumns = @JoinColumn(name = "cert_id"))
    private Set<Certification> certifications = new HashSet<Certification>(0);

    @ManyToMany
    @ForeignKey(name = "FK_SERIESSTUDIOS_SERIES", inverseName = "FK_SERIESSTUDIOS_STUDIO")
    @JoinTable(name = "series_studios",
            joinColumns = @JoinColumn(name = "series_id"),
            inverseJoinColumns = @JoinColumn(name = "studio_id"))
    private Set<Studio> studios = new HashSet<Studio>(0);

    @Transient
    private Set<String> genreNames;

    @Transient
    private Set<String> studioNames;

    @Transient
    private Map<String, String> certificationInfos = new HashMap<String, String>(0);

    @Transient
    private Map<String, String> posterURLS = new HashMap<String, String>(0);

    @Transient
    private Map<String, String> fanartURLS = new HashMap<String, String>(0);

    // CONSTRUCTORS
    public Series() {
        super();
    }

    public Series(String identifier) {
        super();
        setIdentifier(identifier);
    }

    // GETTER and SETTER
    public int getStartYear() {
        return startYear;
    }

    private void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public void setStartYear(int startYear, String source) {
        if (startYear > 0) {
            this.startYear = startYear;
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    public int getEndYear() {
        return endYear;
    }

    private void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public void setEndYear(int endYear, String source) {
        if (endYear > 0) {
            this.endYear = endYear;
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    @Override
    public String getSkipOnlineScans() {
        return skipOnlineScans;
    }

    public void setSkipOnlineScans(String skipOnlineScans) {
        this.skipOnlineScans = skipOnlineScans;
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
            sourceDbIdMap.put(sourceDb, id.trim());
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

    public Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    private void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    @Override
    public void setOverrideFlag(OverrideFlag overrideFlag, String source) {
        this.overrideFlags.put(overrideFlag, source.toLowerCase());
    }

    @Override
    public String getOverrideSource(OverrideFlag overrideFlag) {
        return overrideFlags.get(overrideFlag);
    }

    @Override
    public boolean removeOverrideSource(final String source) {
        boolean removed = false;
        for (Iterator<Entry<OverrideFlag, String>> it = this.overrideFlags.entrySet().iterator(); it.hasNext();) {
            Entry<OverrideFlag, String> e = it.next();
            if (StringUtils.endsWithIgnoreCase(e.getValue(), source)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    public Set<Season> getSeasons() {
        return seasons;
    }

    private void setSeasons(Set<Season> seasons) {
        this.seasons = seasons;
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

    private void setArtworks(List<Artwork> artworks) {
        this.artworks = artworks;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Set<Certification> getCertifications() {
        return certifications;
    }

    public void setCertifications(Set<Certification> certifications) {
        this.certifications = certifications;
    }

    public Set<Studio> getStudios() {
        return studios;
    }

    public void setStudios(Set<Studio> studios) {
        this.studios = studios;
    }

    // TRANSIENTS METHODS
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

    public void addCertificationInfo(String country, String certificate) {
        if (StringUtils.isNotBlank(country) && StringUtils.isNotBlank(certificate)) {
            // check if country already present
            for (String stored : this.certificationInfos.keySet()) {
                if (country.equalsIgnoreCase(stored)) {
                    // certificate for country already present
                    return;
                }
            }
            this.certificationInfos.put(country, certificate);
        }
    }

    public Map<String, String> getPosterURLS() {
        return posterURLS;
    }

    public void addPosterURL(String posterURL, String source) {
        if (StringUtils.isNotBlank(posterURL)) {
            this.posterURLS.put(posterURL, source);
        }
    }

    public Map<String, String> getFanartURLS() {
        return fanartURLS;
    }

    public void addFanartURL(String fanartURL, String source) {
        if (StringUtils.isNotBlank(fanartURL)) {
            this.fanartURLS.put(fanartURL, source);
        }
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
        if (!(obj instanceof Series)) {
            return false;
        }
        final Series other = (Series) obj;
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
        sb.append("Series [ID=");
        sb.append(getId());
        sb.append(", identifier=");
        sb.append(getIdentifier());
        sb.append(", title=");
        sb.append(getTitle());
        sb.append(", startYear=");
        sb.append(getStartYear());
        sb.append(", endYear=");
        sb.append(getEndYear());
        sb.append("]");
        return sb.toString();
    }
}
