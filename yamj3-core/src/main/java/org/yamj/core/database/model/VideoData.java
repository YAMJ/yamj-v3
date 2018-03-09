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
import org.joda.time.DateTime;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.award.MovieAward;
import org.yamj.core.database.model.dto.AwardDTO;
import org.yamj.core.database.model.dto.BoxedSetDTO;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.OverrideFlag;
import org.yamj.plugin.api.artwork.ArtworkDTO;
import org.yamj.plugin.api.model.type.ArtworkType;

@NamedQueries({
    @NamedQuery(name = VideoData.QUERY_REQUIRED_FOR_TRAILER,
        query = "FROM VideoData vd LEFT OUTER JOIN FETCH vd.trailers t LEFT OUTER JOIN FETCH t.stageFile s WHERE vd.id = :id"
    ),
    @NamedQuery(name = VideoData.QUERY_FIND_VIDEOS_FOR_NFO_BY_DIRECTORY,
        query = "SELECT distinct vd FROM VideoData vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf WHERE sf.fileType in ('VIDEO','BLURAY','HDDVD','DVD') "+
                "AND mf.extra=:extra AND sf.stageDirectory=:stageDirectory AND sf.status != 'DELETED'"
    ),
    @NamedQuery(name = VideoData.QUERY_FIND_VIDEOS_FOR_NFO_BY_NAME_AND_DIRECTORY,
        query = "SELECT distinct vd FROM VideoData vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf WHERE sf.fileType in ('VIDEO','BLURAY','HDDVD','DVD') "+
                "AND mf.extra=:extra AND lower(sf.baseName)=:baseName AND sf.stageDirectory=:stageDirectory AND sf.status != 'DELETED'"
    ),
    @NamedQuery(name = VideoData.QUERY_FIND_VIDEOS_FOR_NFO_BY_NAME_AND_LIBRARY,
        query = "SELECT distinct vd FROM VideoData vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf JOIN sf.stageDirectory sd "+
                "WHERE sf.fileType in ('VIDEO','BLURAY','HDDVD','DVD') AND mf.extra=:extra AND lower(sf.baseName)=:baseName AND sd.library=:library AND sf.status != 'DELETED'"
    ),
    @NamedQuery(name = VideoData.QUERY_FIND_VIDEOS_FOR_NFO_BY_DIRECTORIES,
        query = "SELECT distinct vd FROM VideoData vd JOIN vd.mediaFiles mf JOIN mf.stageFiles sf "+
                "WHERE sf.fileType in ('VIDEO','BLURAY','HDDVD','DVD') AND mf.extra=:extra AND sf.stageDirectory in (:stageDirectories) AND sf.status != 'DELETED'"
    ),
    @NamedQuery(name = VideoData.QUERY_FIND_VIDEOS_FOR_PERSON,
        query = "SELECT distinct vd FROM VideoData vd JOIN vd.credits credit WHERE credit.castCrewPK.person.id=:id"
    ),
    @NamedQuery(name = VideoData.QUERY_IDS_RECHECK_MOVIE,
        query = "SELECT vd.id FROM VideoData vd WHERE vd.status not in ('NEW','UPDATED') AND (vd.lastScanned is null or vd.lastScanned<=:compareDate) AND vd.episode<0 ORDER BY vd.lastScanned"
    ),
    @NamedQuery(name = VideoData.QUERY_IDS_RECHECK_EPISODE,
        query = "SELECT vd.id FROM VideoData vd WHERE vd.status not in ('NEW','UPDATED') AND (vd.lastScanned is null or vd.lastScanned<=:compareDate) AND vd.episode>=0 ORDER BY vd.lastScanned"
    ),
    @NamedQuery(name = VideoData.UPDATE_RESCAN_ALL,
        query = "UPDATE VideoData SET status='UPDATED' WHERE status not in ('NEW','UPDATED')"
    ),
    @NamedQuery(name = VideoData.UPDATE_STATUS,
        query = "UPDATE VideoData SET status=:status WHERE id=:id"
    ),
    @NamedQuery(name = VideoData.UPDATE_STATUS_RECHECK,
        query = "UPDATE VideoData vd SET vd.status='UPDATED' WHERE vd.id in (:idList)"
    ),
    @NamedQuery(name = VideoData.UPDATE_TRAILER_STATUS,
        query = "UPDATE VideoData SET trailerStatus=:status WHERE id=:id"
    )
})

@NamedNativeQueries({    
    @NamedNativeQuery(name = VideoData.QUERY_EPISODES_OF_MEDIAFILE,
        query = "SELECT vd.id from mediafile_videodata mv1,videodata vd,mediafile mf "+
                "WHERE exists (select 1 from mediafile_videodata mv2 where mv2.videodata_id=:id and mv1.mediafile_id=mv2.mediafile_id) "+
                "AND mf.id=mv1.mediafile_id AND mv1.videodata_id=vd.id AND mf.extra=0 order by vd.episode"
    ),
    @NamedNativeQuery(name = VideoData.QUERY_TRAKTTV_MOVIES,
        query = "SELECT concat(vid.sourcedb,'#',vid.sourcedb_id), vd.id FROM videodata_ids vid JOIN videodata vd on vd.id=videodata_id and vd.episode<0 "
    ),
    @NamedNativeQuery(name = VideoData.QUERY_TRAKTTV_EPISODES,
        query = "SELECT concat(sid.sourcedb,'#',sid.sourcedb_id,'#',sea.season,'#',vd.episode), vd.id "+
                "FROM series_ids sid JOIN series ser on ser.id=sid.series_id "+
                "JOIN season sea on ser.id=sea.series_id JOIN videodata vd on vd.season_id=sea.id "
    ),
    @NamedNativeQuery(name = VideoData.QUERY_TRAKTTV_WATCHED_MOVIES,
        query = "SELECT vd.id, vid.sourcedb, vid.sourcedb_id, vd.watched, vd.watched_date, vd.identifier, vd.watched_trakttv, vd.watched_trakttv_last_date "+
                "FROM videodata_ids vid JOIN videodata vd on vd.id=videodata_id and vd.episode<0 "+
                "WHERE vd.watched=1 AND (vd.watched_trakttv_last_date is null OR (vd.watched_date>=:checkDate AND vd.watched_date > vd.watched_trakttv_last_date))"
    ),
    @NamedNativeQuery(name = VideoData.QUERY_TRAKTTV_WATCHED_EPISODES,
        query = "SELECT vd.id, sid.sourcedb, sid.sourcedb_id, vd.watched, vd.watched_date, sea.season, vd.episode, vd.identifier, vd.watched_trakttv, vd.watched_trakttv_last_date "+
                "FROM series_ids sid JOIN season sea on sea.series_id=sid.series_id JOIN videodata vd on vd.season_id=sea.id "+
                "WHERE vd.watched=1 AND (vd.watched_trakttv_last_date is null OR (vd.watched_date>=:checkDate AND vd.watched_date > vd.watched_trakttv_last_date))"
    ),
    @NamedNativeQuery(name = VideoData.QUERY_TRAKTTV_COLLECTED_MOVIES,
        query = "SELECT vd.id, vid.sourcedb, vid.sourcedb_id, min(sf.file_date) as collect_date, vd.identifier, vd.title, vd.title_original, vd.publication_year "+
                "FROM videodata vd JOIN videodata_ids vid on vd.id=vid.videodata_id JOIN mediafile_videodata mv on mv.videodata_id=vd.id "+
                "JOIN mediafile mf on mf.id=mv.mediafile_id and mf.extra=0 JOIN stage_file sf on sf.mediafile_id=mf.id and sf.status!='DELETED' "+
                "and sf.file_type in ('VIDEO','BLURAY','HDDVD','DVD') "+
                "WHERE vd.episode<0 and vd.status='DONE' "+
                "AND ((vd.create_timestamp>=:checkDate or (vd.update_timestamp is not null and vd.update_timestamp>=:checkDate)) OR "+
                "     not exists (select 1 from videodata_ids vid2 where vd.id=vid2.videodata_id and vid2.sourcedb='trakttv')) "+
                "GROUP BY vd.id, vid.sourcedb, vid.sourcedb_id, vd.identifier, vd.title, vd.title_original, vd.publication_year"
    ),
    @NamedNativeQuery(name = VideoData.QUERY_TRAKTTV_COLLECTED_EPISODES,
        query = "SELECT vd.id, sid.sourcedb, sid.sourcedb_id, min(sf.file_date) as collect_date, vd.identifier, sea.season, vd.episode, ser.title, ser.title_original, ser.start_year "+
                "FROM series ser JOIN series_ids sid on ser.id=sid.series_id JOIN season sea on ser.id=sea.series_id "+
                "JOIN videodata vd on sea.id=vd.season_id and vd.status='DONE' JOIN mediafile_videodata mv on mv.videodata_id=vd.id "+
                "JOIN mediafile mf on mf.id=mv.mediafile_id and mf.extra=0 JOIN stage_file sf on sf.mediafile_id=mf.id and sf.status!='DELETED' "+
                "and sf.file_type in ('VIDEO','BLURAY','HDDVD','DVD') "+
                "WHERE ((vd.create_timestamp>=:checkDate or (vd.update_timestamp is not null and vd.update_timestamp>=:checkDate)) OR "+
                "       not exists (select 1 from series_ids sid2 where ser.id=sid2.series_id and sid2.sourcedb='trakttv')) "+
                "GROUP BY vd.id, sid.sourcedb, sid.sourcedb_id, sea.season, vd.episode, vd.identifier, ser.title, ser.title_original, ser.start_year"
    ),
    @NamedNativeQuery(name = "metadata.rating.movie", resultSetMapping="metadata.rating",
        query = "SELECT r1.rating, r1.sourcedb AS source, 2 AS sorting "+
                "FROM videodata_ratings r1 WHERE r1.videodata_id=:id UNION "+
                "SELECT round(grouped.average) AS rating, 'combined' AS source, 1 AS sorting FROM "+
                "  (SELECT avg(r2.rating) as average FROM videodata_ratings r2 WHERE r2.videodata_id=:id) AS grouped "+
                "WHERE grouped.average is not null ORDER BY sorting, source"
    ),
    @NamedNativeQuery(name = "metadata.externalid.movie", resultSetMapping="metadata.externalid",
        query = "SELECT ids.videodata_id AS id, ids.sourcedb_id AS externalId, ids.sourcedb,"+
                "concat(coalesce(vd.skip_scan_api,''),';',coalesce(vd.skip_scan_nfo,'')) like concat('%',ids.sourcedb,'%') as skipped "+
                "FROM videodata vd, videodata_ids ids WHERE vd.id=:id AND ids.videodata_id=vd.id"
    )    
})

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
    public static final String QUERY_REQUIRED_FOR_TRAILER = "videoData.required.forTrailer";
    public static final String QUERY_FIND_VIDEOS_FOR_NFO_BY_DIRECTORY = "videoData.findVideoDatasForNFO.byDirectory";
    public static final String QUERY_FIND_VIDEOS_FOR_NFO_BY_NAME_AND_DIRECTORY = "videoData.findVideoDatasForNFO.byNameAndDirectory";
    public static final String QUERY_FIND_VIDEOS_FOR_NFO_BY_NAME_AND_LIBRARY = "videoData.findVideoDatasForNFO.byNameAndLibrary";
    public static final String QUERY_FIND_VIDEOS_FOR_NFO_BY_DIRECTORIES = "videoData.findVideoDatasForNFO.byDirectories";
    public static final String QUERY_FIND_VIDEOS_FOR_PERSON = "videoData.findVideoDatas.forPerson";
    public static final String QUERY_IDS_RECHECK_MOVIE = "videoData.ids.forMovieRecheck";
    public static final String QUERY_IDS_RECHECK_EPISODE = "videoData.ids.forEpisodeRecheck";
    public static final String UPDATE_RESCAN_ALL = "videoData.rescanAll";
    public static final String UPDATE_STATUS = "videoData.updateStatus";
    public static final String UPDATE_STATUS_RECHECK ="videoData.updateStatus.forRecheck";
    public static final String UPDATE_TRAILER_STATUS = "videoData.updateTrailerStatus";
    public static final String QUERY_EPISODES_OF_MEDIAFILE = "videoData.episodesOfMediaFile";
    public static final String QUERY_TRAKTTV_MOVIES = "videoData.trakttv.movies";
    public static final String QUERY_TRAKTTV_EPISODES = "videoData.trakttv.episodes";
    public static final String QUERY_TRAKTTV_WATCHED_MOVIES = "videoData.trakttv.watchedMovies";
    public static final String QUERY_TRAKTTV_WATCHED_EPISODES = "videoData.trakttv.watchedEpisodes";
    public static final String QUERY_TRAKTTV_COLLECTED_MOVIES = "videoData.trakttv.collectedMovies";
    public static final String QUERY_TRAKTTV_COLLECTED_EPISODES = "videoData.trakttv.collectedEpisodes";
    
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

    @Column(name = "tagline", length = 2000)
    private String tagline;

    @Column(name = "quote", length = 2000)
    private String quote;

    @Column(name = "watched_nfo", nullable = false)
    private boolean watchedNfo = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "watched_nfo_last_date")
    private Date watchedNfoLastDate;

    @Column(name = "watched_api", nullable = false)
    private boolean watchedApi = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "watched_api_last_date")
    private Date watchedApiLastDate;

    @Column(name = "watched_trakttv", nullable = false)
    private boolean watchedTraktTv = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "watched_trakttv_last_date")
    private Date watchedTraktTvLastDate;
    
    @Column(name = "watched", nullable = false)
    private boolean watched = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "watched_date")
    private Date watchedDate;

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
    @CollectionTable(name = "videodata_ids",
            joinColumns = @JoinColumn(name = "videodata_id"), foreignKey = @ForeignKey(name = "FK_VIDEODATA_SOURCEIDS"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "videodata_ratings",
            joinColumns = @JoinColumn(name = "videodata_id"), foreignKey = @ForeignKey(name = "FK_VIDEODATA_RATINGS"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", nullable = false)
    private Map<String, Integer> ratings = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "videodata_override",
            joinColumns = @JoinColumn(name = "videodata_id"), foreignKey = @ForeignKey(name = "FK_VIDEODATA_OVERRIDE"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<>(OverrideFlag.class);

    @ManyToMany
    @JoinTable(name = "videodata_genres",
            joinColumns = @JoinColumn(name = "data_id"),
            foreignKey = @ForeignKey(name = "FK_DATAGENRES_VIDEODATA"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"),
            inverseForeignKey = @ForeignKey(name = "FK_DATAGENRES_GENRE"))
    private Set<Genre> genres = new HashSet<>(0);

    @ManyToMany
    @JoinTable(name = "videodata_studios",
            joinColumns = @JoinColumn(name = "data_id"),
            foreignKey = @ForeignKey(name = "FK_DATASTUDIOS_VIDEODATA"),
            inverseJoinColumns = @JoinColumn(name = "studio_id"),
            inverseForeignKey = @ForeignKey(name = "FK_DATASTUDIOS_STUDIO"))
    private Set<Studio> studios = new HashSet<>(0);
	
	// add videodata_libraries
	@ManyToMany
    @JoinTable(name = "videodata_libraries",
            joinColumns = @JoinColumn(name = "data_id"),
            foreignKey = @ForeignKey(name = "FK_DATALIBRARIES_VIDEODATA"),
            inverseJoinColumns = @JoinColumn(name = "library_id"),
            inverseForeignKey = @ForeignKey(name = "FK_DATALIBRARIES_LIBRARY"))
    private Set<Library> libraries = new HashSet<>(0);
	// end videodata_libraries

    @ManyToMany
    @JoinTable(name = "videodata_countries",
            joinColumns = @JoinColumn(name = "data_id"),
            foreignKey = @ForeignKey(name = "FK_DATACOUNTRIES_VIDEODATA"),
            inverseJoinColumns = @JoinColumn(name = "country_id"),
            inverseForeignKey = @ForeignKey(name = "FK_DATACOUNTRIES_COUNTRY"))
    private Set<Country> countries = new HashSet<>(0);

    @ManyToMany
    @JoinTable(name = "videodata_certifications",
            joinColumns = @JoinColumn(name = "data_id"),
            foreignKey = @ForeignKey(name = "FK_DATACERTS_VIDEODATA"),
            inverseJoinColumns = @JoinColumn(name = "cert_id"),
            inverseForeignKey = @ForeignKey(name = "FK_DATACERTS_CERTIFICATION"))
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
    private Collection<String> genreNames;

    @Transient
    private Collection<String> studioNames;
	
	@Transient
    private Collection<String> libraryNames;

    @Transient
    private Collection<String> countryCodes;

    @Transient
    private final Map<String, String> certificationInfos = new HashMap<>(0);

    @Transient
    private final Collection<BoxedSetDTO> boxedSetDTOS = new HashSet<>(0);

    @Transient
    private final Collection<CreditDTO> creditDTOS = new LinkedHashSet<>(0);

    @Transient
    private final Collection<ArtworkDTO> posterDTOS = new HashSet<>(0);

    @Transient
    private final Collection<ArtworkDTO> fanartDTOS = new HashSet<>(0);

    @Transient
    private final Collection<AwardDTO> awardDTOS = new HashSet<>(0);

    // CONSTRUCTORS
    
    public VideoData() {
        super();
    }

    public VideoData(String identifier) {
        super(identifier);
    }

    public VideoData(String identifier, Map<String, String> sourceDbIdMap) {
        super(identifier);
        this.sourceDbIdMap = sourceDbIdMap;
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
            setPublicationYear(publicationYear);
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    public void removePublicationYear(String source) {
        if (hasOverrideSource(OverrideFlag.YEAR, source)) {
            if (isMovie()) { // just for movies
                String[] splitted = getIdentifier().split("_");
                int splitYear = Integer.parseInt(splitted[1]);
                setPublicationYear(splitYear > 0 ? splitYear : -1); 
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
            setReleaseCountryCode(releaseCountryCode);
            setReleaseDate(releaseDate);
            setOverrideFlag(OverrideFlag.RELEASEDATE, source);
        }
    }

    public void removeRelease(String source) {
        if (hasOverrideSource(OverrideFlag.RELEASEDATE, source)) {
            setReleaseCountryCode(null);
            setReleaseDate(null);
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
    
    public String getTagline() {
        return tagline;
    }

    private void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public void setTagline(String tagline, String source) {
        if (StringUtils.isNotBlank(tagline)) {
            setTagline(StringUtils.abbreviate(tagline.trim(), 2000));
            setOverrideFlag(OverrideFlag.TAGLINE, source);
        }
    }

    public void removeTagline(String source) {
        if (hasOverrideSource(OverrideFlag.TAGLINE, source)) {
            setTagline(null);
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
            setQuote(StringUtils.abbreviate(quote.trim(), 2000));
            setOverrideFlag(OverrideFlag.QUOTE, source);
        }
    }

    public void removeQuote(String source) {
        if (hasOverrideSource(OverrideFlag.QUOTE, source)) {
            setQuote(null);
            removeOverrideFlag(OverrideFlag.QUOTE);
        }
    }

    @Override
    protected Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    public boolean isWatchedNfo() {
        return watchedNfo;
    }

    private void setWatchedNfo(boolean watchedNfo) {
        this.watchedNfo = watchedNfo;
    }

    public Date getWatchedNfoLastDate() {
        return watchedNfoLastDate;
    }

    private void setWatchedNfoLastDate(Date watchedNfoLastDate) {
        this.watchedNfoLastDate = watchedNfoLastDate;
    }

    public void setWatchedNfo(final boolean watchedNfo, final Date watchedNfoLastDate) {
        if (watchedNfoLastDate != null) {
            final Date dateWithoutMS = new DateTime(watchedNfoLastDate.getTime()).withMillisOfSecond(0).toDate();

            setWatchedNfo(watchedNfo);
            setWatchedNfoLastDate(dateWithoutMS);
    
            if (getWatchedDate() == null || getWatchedDate().before(dateWithoutMS)) {
                setWatched(watchedNfo);
                setWatchedDate(dateWithoutMS);
            }
        }
    }
    
    public boolean isWatchedApi() {
        return watchedApi;
    }

    private void setWatchedApi(boolean watchedApi) {
        this.watchedApi = watchedApi;
    }

    public Date getWatchedApiLastDate() {
        return watchedApiLastDate;
    }

    private void setWatchedApiLastDate(Date watchedApiLastDate) {
        this.watchedApiLastDate = watchedApiLastDate;
    }

    public void setWatchedApi(final boolean watchedApi, final Date watchedApiLastDate) {
        if (watchedApiLastDate != null) {
            final Date dateWithoutMS = new DateTime(watchedApiLastDate.getTime()).withMillisOfSecond(0).toDate();
            
            setWatchedApi(watchedApi);
            setWatchedApiLastDate(dateWithoutMS);
    
            if (getWatchedDate() == null || getWatchedDate().before(dateWithoutMS)) {
                setWatched(watchedApi);
                setWatchedDate(dateWithoutMS);
            }
        }
    }
    
    public boolean isWatchedTraktTv() {
        return watchedTraktTv;
    }

    private void setWatchedTraktTv(boolean watchedTraktTv) {
        this.watchedTraktTv = watchedTraktTv;
    }

    public Date getWatchedTraktTvLastDate() {
        return watchedTraktTvLastDate;
    }

    private void setWatchedTraktTvLastDate(Date watchedTraktTvLastDate) {
        this.watchedTraktTvLastDate = watchedTraktTvLastDate;
    }

    public void setWatchedTraktTv(boolean watchedTraktTv, Date watchedTraktTvLastDate) {
        if (watchedTraktTvLastDate != null) {
            final Date dateWithoutMS = new DateTime(watchedTraktTvLastDate.getTime()).withMillisOfSecond(0).toDate();
            
            setWatchedTraktTv(watchedTraktTv);
            setWatchedTraktTvLastDate(dateWithoutMS);
    
            if (getWatchedDate() == null || getWatchedDate().before(dateWithoutMS)) {
                setWatched(watchedTraktTv);
                setWatchedDate(dateWithoutMS);
            }
        }
    }

    public boolean isWatched() {
        return watched;
    }

    private void setWatched(boolean watched) {
        this.watched = watched;
    }

    public Date getWatchedDate() {
        return watchedDate;
    }

    private void setWatchedDate(Date watchedDate) {
        this.watchedDate = watchedDate;
    }

    public void setWatched(final boolean watched, final Date watchedDate) {
        if (watchedDate != null) {
            setWatched(watched);
            setWatchedDate(new DateTime(watchedDate.getTime()).withMillisOfSecond(0).toDate());
        }
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
        if (StringUtils.isNotBlank(sourceDb) && (rating > 0)) {
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
        if (isMovie()) {
            // skip movie
            if (getSkipScanNfo() == null && getSkipScanApi() == null) {
                return false;
            }
            
            return isAllScansSkipped() ||
                   StringUtils.containsIgnoreCase(getSkipScanNfo(), sourceDb) ||
                   StringUtils.containsIgnoreCase(getSkipScanApi(), sourceDb);
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
        getMediaFiles().add(mediaFile);
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
        getBoxedSets().add(boxedSet);
    }

    public List<NfoRelation> getNfoRelations() {
        return nfoRelations;
    }

    public void setNfoRelations(List<NfoRelation> nfoRelations) {
        this.nfoRelations = nfoRelations;
    }

    public void addNfoRelation(NfoRelation nfoRelation) {
        getNfoRelations().add(nfoRelation);
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
   
    public Collection<CreditDTO> getCreditDTOS() {
        return creditDTOS;
    }

    public void addCreditDTO(CreditDTO creditDTO) {
        if (creditDTO != null) {
            getCreditDTOS().add(creditDTO);
        }
    }

    public void addCreditDTOS(Collection<CreditDTO> creditDTOS) {
        if (creditDTOS != null) {
            getCreditDTOS().addAll(creditDTOS);
        }
    }

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
    
    public Collection<String> getLibraryNames() {
    	return libraryNames;
    }
    
	public Collection<String> setLibrary(String LibraryNames) {
        return libraryNames;
	}
	
    public void setLibraryNames(Collection<String> libraryNames, String source) {
        if (CollectionUtils.isNotEmpty(libraryNames)) {
            this.libraryNames = libraryNames;
            setOverrideFlag(OverrideFlag.LIBRARIES, source);
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
        if (StringUtils.isNotBlank(countryCode) && StringUtils.isNotBlank(certificate) && !getCertificationInfos().containsKey(countryCode)) {
            getCertificationInfos().put(countryCode, certificate);
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

    public Collection<ArtworkDTO> getPosterDTOS() {
        return posterDTOS;
    }

    public void addPosterDTO(String source, String url) {
         if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(url)) {
             getPosterDTOS().add(new ArtworkDTO(source, url));
        }
    }

    public Collection<ArtworkDTO> getFanartDTOS() {
        return this.fanartDTOS;
    }

    public void addFanartDTO(String source, String url) {
        if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(url)) {
            getFanartDTOS().add(new ArtworkDTO(source, url));
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
    
    // TV CHECKS
    
    public boolean isTvEpisodeDone(String sourceDb) {
        if (StringUtils.isBlank(getSourceDbId(sourceDb))) {
            // not done if episode ID not set
            return false;
        }
        return StatusType.DONE.equals(getStatus());
    }

    public void setTvEpisodeDone() {
        setStatus(StatusType.TEMP_DONE);
    }

    public void setTvEpisodeNotFound() {
        if (StatusType.DONE.equals(getStatus())) {
            // reset to temporary done state
            setStatus(StatusType.TEMP_DONE);
        } else if (!StatusType.TEMP_DONE.equals(getStatus())) {
            // do not reset temporary done
            setStatus(StatusType.NOTFOUND);
        }
    }
    
    public void setTvEpisodeFinished() {
        if (StatusType.TEMP_DONE.equals(getStatus())) {
            setStatus(StatusType.DONE);
        }        
    }

    public boolean isMovie() {
        return getEpisode() < 0;
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
        VideoData other = (VideoData) obj;
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
