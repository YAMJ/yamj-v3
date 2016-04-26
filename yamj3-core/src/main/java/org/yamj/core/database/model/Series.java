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

import static org.yamj.plugin.api.Constants.ALL;

import java.util.*;
import java.util.Map.Entry;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.award.SeriesAward;
import org.yamj.core.database.model.dto.AwardDTO;
import org.yamj.core.database.model.dto.BoxedSetDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.OverrideFlag;

@NamedQueries({
    @NamedQuery(name = Series.QUERY_REQUIRED,
        query = "FROM Series ser JOIN FETCH ser.seasons sea JOIN FETCH sea.videoDatas vd WHERE ser.id = :id"
    ),
    @NamedQuery(name = Series.QUERY_REQUIRED_FOR_TRAILER,
        query = "FROM Series ser LEFT OUTER JOIN FETCH ser.trailers t LEFT OUTER JOIN FETCH t.stageFile s WHERE ser.id = :id"
    ),
    @NamedQuery(name = Series.UPDATE_RESCAN_ALL,
        query = "UPDATE Series SET status='UPDATED' WHERE status not in ('NEW','UPDATED')"
    ),
    @NamedQuery(name = Series.UPDATE_STATUS,
        query = "UPDATE Series SET status=:status WHERE id=:id"
    ),
    @NamedQuery(name = Series.UPDATE_STATUS_RECHECK,
        query = "UPDATE Series ser SET ser.status='UPDATED' WHERE ser.status not in ('NEW','UPDATED') "+
                "AND (ser.lastScanned is null or ser.lastScanned<=:compareDate)"
    ),
    @NamedQuery(name = Series.UPDATE_TRAILER_STATUS,
        query = "UPDATE Series SET trailerStatus=:status WHERE id=:id"
    )
})

@NamedNativeQueries({    
    @NamedNativeQuery(name = Series.QUERY_METADATA_QUEUE,
        query = "SELECT DISTINCT vd1.id,'MOVIE' as metatype,(case when vd1.update_timestamp is null then vd1.create_timestamp else vd1.update_timestamp end) as maxdate "+
                "FROM videodata vd1 WHERE vd1.status in ('NEW','UPDATED') and vd1.episode<0 UNION "+
                "SELECT DISTINCT ser.id,'SERIES' as mediatype,(case when ser.update_timestamp is null then ser.create_timestamp else ser.update_timestamp end) as maxdate "+
                "FROM series ser, season sea, videodata vd WHERE ser.id=sea.series_id and sea.id=vd.season_id and (ser.status in ('NEW','UPDATED') "+
                "or  (ser.status='DONE' and sea.status in ('NEW','UPDATED')) or  (ser.status='DONE' and vd.status in ('NEW','UPDATED'))) "
    )
})

@Entity
@Table(name = "series",
        uniqueConstraints = @UniqueConstraint(name = "UIX_SERIES_NATURALID", columnNames = {"identifier"}),
        indexes = {
            @Index(name = "IX_SERIES_TITLE", columnList = "title"),
            @Index(name = "IX_SERIES_STATUS", columnList = "status")}
)
@SuppressWarnings("unused")
public class Series extends AbstractMetadata {

    private static final long serialVersionUID = -5782361288021493423L;
    public static final String QUERY_REQUIRED = "series.required";
    public static final String QUERY_REQUIRED_FOR_TRAILER = "series.required.forTrailer";
    public static final String UPDATE_RESCAN_ALL = "series.rescanAll";
    public static final String UPDATE_STATUS = "series.updateStatus";
    public static final String UPDATE_STATUS_RECHECK = "series.updateStatus.forRecheck";
    public static final String UPDATE_TRAILER_STATUS = "series.updateTrailerStatus";
    public static final String QUERY_METADATA_QUEUE = "metadata.queue";
  
    @Column(name = "start_year")
    private int startYear = -1;

    @Column(name = "end_year")
    private int endYear = -1;

    @Column(name = "skip_scan_nfo", length = 255)
    private String skipScanNfo;

    @Column(name = "skip_scan_api", length = 255)
    private String skipScanApi;

    @Type(type = "statusType")
    @Column(name = "trailer_status", nullable = false, length = 30)
    private StatusType trailerStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "trailer_last_scanned")
    private Date trailerLastScanned;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "series_ids",
            joinColumns = @JoinColumn(name = "series_id"), foreignKey = @ForeignKey(name = "FK_SERIES_SOURCEIDS"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "series_ratings",
            joinColumns = @JoinColumn(name = "series_id"), foreignKey = @ForeignKey(name = "FK_SERIES_RATINGS"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", nullable = false)
    private Map<String, Integer> ratings = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "series_override",
            joinColumns = @JoinColumn(name = "series_id"), 
            foreignKey = @ForeignKey(name = "FK_SERIES_OVERRIDE"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<>(OverrideFlag.class);

    @ManyToMany
    @JoinTable(name = "series_genres",
            joinColumns = @JoinColumn(name = "series_id"),
            foreignKey = @ForeignKey(name = "FK_SERIESGENRES_SERIES"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"),
            inverseForeignKey = @ForeignKey(name = "FK_SERIESGENRES_GENRE"))
    private Set<Genre> genres = new HashSet<>(0);

    @ManyToMany
    @JoinTable(name = "series_studios",
            joinColumns = @JoinColumn(name = "series_id"),
            foreignKey = @ForeignKey(name = "FK_SERIESSTUDIOS_SERIES"),
            inverseJoinColumns = @JoinColumn(name = "studio_id"),
            inverseForeignKey = @ForeignKey(name = "FK_SERIESSTUDIOS_STUDIO"))
    private Set<Studio> studios = new HashSet<>(0);

    @ManyToMany
    @JoinTable(name = "series_countries",
            joinColumns = @JoinColumn(name = "series_id"),
            foreignKey = @ForeignKey(name = "FK_SERIESCOUNTRIES_SERIES"),
            inverseJoinColumns = @JoinColumn(name = "country_id"),
            inverseForeignKey = @ForeignKey(name = "FK_SERIESCOUNTRIES_COUNTRY"))
    private Set<Country> countries = new HashSet<>(0);

    @ManyToMany
    @JoinTable(name = "series_certifications",
            joinColumns = @JoinColumn(name = "series_id"), 
            foreignKey = @ForeignKey(name = "FK_SERIESCERTS_SERIES"),
            inverseJoinColumns = @JoinColumn(name = "cert_id"),
            inverseForeignKey = @ForeignKey(name = "FK_SERIESCERTS_CERTIFICATION"))
    private Set<Certification> certifications = new HashSet<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "series")
    private Set<Season> seasons = new HashSet<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "series")
    private List<Artwork> artworks = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "series")
    private List<BoxedSetOrder> boxedSets = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "seriesAwardPK.series")
    private List<SeriesAward> seriesAwards = new ArrayList<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "series")
    private List<Trailer> trailers = new ArrayList<>(0);

    @Transient
    private Collection<String> genreNames;

    @Transient
    private Collection<String> studioNames;

    @Transient
    private Collection<String> countryCodes;

    @Transient
    private final Map<String, String> certificationInfos = new HashMap<>(0);

    @Transient
    private final Collection<BoxedSetDTO> boxedSetDTOS = new HashSet<>(0);

    @Transient
    private final Collection<AwardDTO> awardDTOS = new HashSet<>(0);

    // CONSTRUCTORS
    
    public Series() {
        super();
    }

    public Series(String identifier) {
        super(identifier);
    }

    public Series(String identifier, Map<String, String> sourceDbIdMap) {
        super(identifier);
        this.sourceDbIdMap = sourceDbIdMap;
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
            setStartYear(startYear);
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    public void removeStartYear(String source) {
        if (hasOverrideSource(OverrideFlag.YEAR, source)) {
            String[] splitted = getIdentifier().split("_");
            int splitYear = Integer.parseInt(splitted[1]);
            setStartYear(splitYear > 0 ? splitYear : -1); 
            removeOverrideFlag(OverrideFlag.YEAR);
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
            setEndYear(endYear);
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    public void removeEndYear(String source) {
        if (hasOverrideSource(OverrideFlag.YEAR, source)) {
            setEndYear(-1);
            removeOverrideFlag(OverrideFlag.YEAR);
        }
    }
    
    @Override
    protected Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
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
            getRatings().put(sourceDb, rating);
        }
    }

    public void removeRating(String sourceDb) {
        if (StringUtils.isNotBlank(sourceDb)) {
            getRatings().remove(sourceDb);
        }
    }

    public int getRating(String sourceDb) {
        if (StringUtils.isNotBlank(sourceDb)) {
            return getRatings().get(sourceDb);
        }
        return -1;
    }

    @Override
    protected Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    private void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    @Override
    public boolean isAllScansSkipped() {
        return ALL.equalsIgnoreCase(getSkipScanNfo()) || ALL.equalsIgnoreCase(getSkipScanApi());
    }

    @Override
    public boolean isSkippedScan(String sourceDb) {
        if (getSkipScanNfo() == null && getSkipScanApi() == null) {
            return false;
        }
        
        return isAllScansSkipped() ||
               StringUtils.containsIgnoreCase(getSkipScanNfo(), sourceDb) ||
               StringUtils.containsIgnoreCase(getSkipScanApi(), sourceDb);
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
            if (artworkType == artwork.getArtworkType()) {
                return artwork;
            }
        }
        return null;
    }

    private void setArtworks(List<Artwork> artworks) {
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

    public List<SeriesAward> getSeriesAwards() {
        return seriesAwards;
    }

    public void setSeriesAwards(List<SeriesAward> seriesAwards) {
        this.seriesAwards = seriesAwards;
    }

    public List<Trailer> getTrailers() {
        return trailers;
    }

    public void setTrailers(List<Trailer> trailers) {
        this.trailers = trailers;
    }

    // TRANSIENT METHODS
    
    public Collection<String> getGenreNames() {
        return genreNames;
    }

    public void setGenreNames(Collection<String> genreNames, String source) {
        if (CollectionUtils.isNotEmpty(genreNames)) {
            this.genreNames = genreNames;
            setOverrideFlag(OverrideFlag.GENRES, source);
        }
    }

    public Collection<String> getStudioNames() {
        return studioNames;
    }

    public void setStudioNames(Collection<String> studioNames, String source) {
        if (CollectionUtils.isNotEmpty(studioNames)) {
            this.studioNames = studioNames;
            setOverrideFlag(OverrideFlag.STUDIOS, source);
        }
    }

    public Collection<String> getCountryCodes() {
        return countryCodes;
    }

    public void setCountryCodes(Collection<String> countryCodes, String source) {
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
                addCertificationInfo(entry.getKey(), entry.getValue());
            }
        }
    }

    public void addCertificationInfo(String countryCode, String certificate) {
        if (StringUtils.isNotBlank(countryCode) && StringUtils.isNotBlank(certificate)) {
            // check if country already present
            if (!getCertificationInfos().containsKey(countryCode)) {
                getCertificationInfos().put(countryCode, certificate);
            }
        }
    }

    public Collection<BoxedSetDTO> getBoxedSetDTOS() {
        return boxedSetDTOS;
    }

    public void addBoxedSetDTO(String source, String identifier, String name, Integer ordering, String sourceId) {
        if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(identifier)) {
            getBoxedSetDTOS().add(new BoxedSetDTO(source, identifier, name, ordering, sourceId));
        }
    }
    
    public Collection<AwardDTO> getAwardDTOS() {
        return awardDTOS;
    }

    public void addAwardDTO(String source, String event, String category, int year, boolean won, boolean nominated) {
        if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(event) && StringUtils.isNotBlank(category) && year > 0) {
            awardDTOS.add(new AwardDTO(source, event, category, year).setWon(won).setNominated(nominated));
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
        Series other = (Series) obj;
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
