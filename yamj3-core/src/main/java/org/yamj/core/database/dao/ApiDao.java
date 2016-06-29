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
package org.yamj.core.database.dao;

import static org.yamj.core.CachingNames.*;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.builder.*;
import org.yamj.core.api.model.dto.*;
import org.yamj.core.api.options.*;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.database.model.type.ResolutionType;
import org.yamj.core.hibernate.HibernateDao;
import org.yamj.plugin.api.model.type.ArtworkType;
import org.yamj.plugin.api.model.type.ParticipationType;

@Repository("apiDao")
public class ApiDao extends HibernateDao {

    private static final Logger LOG = LoggerFactory.getLogger(ApiDao.class);

    // LITERALS
    private static final String ID = "id";
    private static final String YEAR = "year";
    private static final String TITLE = "title";
    private static final String EPISODE = "episode";
    private static final String SEASON = "season";
    private static final String SEASON_ID = "seasonId";
    private static final String SERIES_ID = "seriesId";
    private static final String VIDEO_YEAR = "videoYear";
    private static final String RELEASE_DATE = "releaseDate";
    private static final String ORIGINAL_TITLE = "originalTitle";
    private static final String SORT_TITLE = "sortTitle";
    private static final String CACHE_FILENAME = "cacheFilename";
    private static final String CACHE_DIR = "cacheDir";
    private static final String WATCHED = "watched";
    private static final String VIDEO_TYPE = "videoType";               
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String JOB = "job";
    private static final String EXTRA = "extra";
    private static final String CREATION = "creation";
    private static final String LASTSCAN = "lastscan";
    private static final String SOURCE = "source";
    private static final String MIN_WIDTH = "minWidth";
    private static final String MAX_WIDTH = "maxWidth";
    private static final String COMBINED = "combined";
    private static final String NEWEST_DATE = "newestDate";
    private static final String ARTWORK_TYPE = "artworkType";
    private static final String ARTWORK_ID = "artworkId";
    private static final String LOCATED_ID = "locatedId";
    private static final String GENERATED_ID = "generatedId";
    private static final String COUNTRY_CODE = "countryCode";
    private static final String MULTIPLE = "Multiple";
    private static final String CREATE_TIMESTAMP = "createTimestamp";
    private static final String UPDATE_TIMESTAMP = "updateTimestamp";
    
    // SQL
    private static final String SQL_UNION = " UNION ";
    private static final String SQL_UNION_ALL = " UNION ALL ";
    private static final String SQL_AS_VIDEO_TYPE = "' AS videoType";
    private static final String SQL_WHERE_1_EQ_1 = " WHERE 1=1";
    private static final String SQL_COMMA_SPACE_QUOTE = ", '";
    private static final String SQL_ARTWORK_TYPE_IN_ARTWORKLIST = " AND a.artwork_type IN (:artworklist) ";
    private static final String SQL_LEFT_JOIN_ARTWORK_GENERATED = " LEFT JOIN artwork_generated ag ON al.id=ag.located_id ";
    private static final String SQL_LEFT_JOIN_ARTWORK_LOCATED = " LEFT JOIN artwork_located al ON a.id=al.artwork_id and al.status not in ('INVALID','NOTFOUND','ERROR','IGNORE','DELETED') ";
    private static final String SQL_IGNORE_STATUS_SET = " NOT IN ('DELETED','INVALID','DUPLICATE') ";

    /**
     * Generate the query and load the results into the wrapper
     *
     * @param wrapper
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<ApiVideoDTO> getVideoList(ApiWrapperList<ApiVideoDTO> wrapper, OptionsIndexVideo options) {
        IndexParams params = new IndexParams(options);

        SqlScalars sqlScalars = new SqlScalars(generateSqlForVideoList(params));
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(VIDEO_TYPE, StringType.INSTANCE);                      
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(SORT_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(VIDEO_YEAR, IntegerType.INSTANCE);
        sqlScalars.addScalar(RELEASE_DATE, DateType.INSTANCE);
        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON, LongType.INSTANCE);
        sqlScalars.addScalar(EPISODE, LongType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);

        if (params.includeNewest() || params.excludeNewest()) {
            sqlScalars.addScalar("newest", TimestampType.INSTANCE);
        }

        // add Scalars for additional data item columns
        DataItemTools.addDataItemScalars(sqlScalars, params.getDataItems());
        // add additional parameters
        params.addScalarParameters(sqlScalars);

        DataItemTools.addDataItemScalars(sqlScalars, params.getDataItems());

        List<ApiVideoDTO> queryResults = executeQueryWithTransform(ApiVideoDTO.class, sqlScalars, wrapper);
        if (queryResults.isEmpty()) {
            LOG.debug("No results found to process for video list");
            return queryResults;
        }

        if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
            LOG.trace("Adding artwork to index videos");

            // build the meta data maps for faster retrieval
            Map<MetaDataType, List<Long>> metaDataIds = new EnumMap<>(MetaDataType.class);
            for (MetaDataType mdt : MetaDataType.values()) {
                metaDataIds.put(mdt, new ArrayList());
            }
            Map<String, ApiVideoDTO> metaDataResults = new HashMap<>();
            for (ApiVideoDTO video : queryResults) {
                // add the item to the map for further processing
                metaDataResults.put(KeyMaker.makeKey(video), video);
                // add the ID to the list
                metaDataIds.get(video.getVideoType()).add(video.getId());
            }
            // remove any blank entries
            for (MetaDataType mdt : MetaDataType.values()) {
                if (CollectionUtils.isEmpty(metaDataIds.get(mdt))) {
                    metaDataIds.remove(mdt);
                }
            }

            addArtworks(metaDataIds, metaDataResults, options);
        }
        
        return queryResults;
    }

    /**
     * Generate the SQL for the video list
     *
     * Note: In this method MetaDataType.UNKNOWN will return all types
     *
     * @param params
     * @return
     */
    private static String generateSqlForVideoList(IndexParams params) {
        Set<MetaDataType> mdt = params.getMetaDataTypes();
        LOG.debug("Getting video list for types: {}", mdt);
        if (CollectionUtils.isNotEmpty(params.getDataItems())) {
            LOG.debug("Additional data items requested: {}", params.getDataItems());
        }

        StringBuilder sbSQL = new StringBuilder();

        // add the movie entries
        if (mdt.contains(MetaDataType.MOVIE)) {
            sbSQL.append(generateSqlForVideo(true, params));
        }

        // add the TV series entries
        if (mdt.contains(MetaDataType.SERIES)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION_ALL);
            }
            sbSQL.append(generateSqlForSeries(params));
        }

        // add the TV season entries
        if (mdt.contains(MetaDataType.SEASON)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION_ALL);
            }
            sbSQL.append(generateSqlForSeason(params));
        }

        // add the TV episode entries
        if (mdt.contains(MetaDataType.EPISODE)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION_ALL);
            }
            sbSQL.append(generateSqlForVideo(false, params));
        }

        // Add the sort string, this will be empty if there is no sort required
        sbSQL.append(params.getSortString());

        LOG.trace("SqlForVideoList: {}", sbSQL);
        return sbSQL.toString();
    }

    /**
     * Create the SQL fragment for the selection of movies
     */
    private static String generateSqlForVideo(boolean isMovie, IndexParams params) {
        StringBuilder sbSQL = new StringBuilder();

        sbSQL.append("SELECT vd.id");
        if (isMovie) {
            sbSQL.append(SQL_COMMA_SPACE_QUOTE).append(MetaDataType.MOVIE).append(SQL_AS_VIDEO_TYPE);
        } else {
            sbSQL.append(SQL_COMMA_SPACE_QUOTE).append(MetaDataType.EPISODE).append(SQL_AS_VIDEO_TYPE);
        }
        sbSQL.append(", vd.title, vd.title_original AS originalTitle, vd.title_sort AS sortTitle");
        sbSQL.append(", vd.publication_year AS videoYear, vd.release_date as releaseDate");
        sbSQL.append(", null AS seriesId, vd.season_id AS seasonId, null AS season, vd.episode AS episode ");
        sbSQL.append(", vd.watched AS watched, vd.create_timestamp as createTimestamp ");
        
        sbSQL.append(DataItemTools.addSqlDataItems(params.getDataItems(), "vd"));

        if (params.includeNewest() || params.excludeNewest()) {
            String source = params.getNewestSource();
            if (CREATION.equalsIgnoreCase(source)) {
                sbSQL.append(", vd.create_timestamp AS newest");
            } else if (LASTSCAN.equalsIgnoreCase(source)) {
                sbSQL.append(", vd.last_scanned AS newest");
            } else {
                params.addParameter(EXTRA, Boolean.FALSE);

                sbSQL.append(", (SELECT MAX(sf.file_date) FROM stage_file sf ");
                sbSQL.append("JOIN mediafile mf ON mf.id=sf.mediafile_id ");
                sbSQL.append("JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
                sbSQL.append("WHERE mv.videodata_id=vd.id ");
                sbSQL.append("AND sf.file_type='");
                sbSQL.append(FileType.VIDEO.name());
                sbSQL.append("' AND sf.status");
                sbSQL.append(SQL_IGNORE_STATUS_SET);
                sbSQL.append("AND mf.extra=:extra) AS newest");
            }
        }

        sbSQL.append(" FROM videodata vd WHERE vd.episode");
        sbSQL.append(isMovie ? "<0" : ">-1");

        if (params.getId() > 0L) {
            sbSQL.append(" AND vd.id=").append(params.getId());
        }

        if (params.includeYear()) {
            sbSQL.append(" AND vd.publication_year=").append(params.getYear());
        } else if (params.excludeYear()) {
            sbSQL.append(" AND vd.publication_year!=").append(params.getYear());
        }
        if (params.getYearStart() > 0) {
            sbSQL.append(" AND vd.publication_year>=").append(params.getYearStart());
        }
        if (params.getYearEnd() > 0) {
            sbSQL.append(" AND vd.publication_year<=").append(params.getYearEnd());
        }

        if (params.getWatched() != null) {
            sbSQL.append(" AND vd.watched=");
            sbSQL.append(params.getWatched().booleanValue() ? "1" : "0");
        }

        // check genre
        if (params.includeGenre() || params.excludeGenre()) {
            final String genre = params.getGenreName();

            addExistsOrNot(params.includeGenre(), sbSQL);
            if (isMovie) {
                sbSQL.append("SELECT 1 FROM videodata_genres vg, genre g ");
                sbSQL.append("WHERE vd.id=vg.data_id AND vg.genre_id=g.id ");
            } else {
                sbSQL.append("SELECT 1 FROM series_genres sg, genre g, season sea WHERE vd.season_id=sea.id ");
                sbSQL.append("AND sg.series_id=sea.series_id AND sg.genre_id=g.id ");

            }
            sbSQL.append("AND (lower(g.name)='").append(genre).append("'");
            sbSQL.append(" or (g.target_api is not null and lower(g.target_api)='").append(genre).append("')");
            sbSQL.append(" or (g.target_xml is not null and lower(g.target_xml)='").append(genre).append("')))");
        }

        // check studio
        if (params.includeStudio() || params.excludeStudio()) {
            final String studio = params.getStudioName();

            addExistsOrNot(params.includeStudio(), sbSQL);
            if (StringUtils.isNumeric(studio)) {
                if (isMovie) {
                    sbSQL.append("SELECT 1 FROM videodata_studios vs WHERE vd.id=vs.data_id AND vs.studio_id=");
                } else {
                    sbSQL.append("SELECT 1 FROM series_studios ss, season sea WHERE vd.season_id=sea.id ");
                    sbSQL.append("AND ss.series_id=sea.series_id AND ss.studio_id=");
                }
                sbSQL.append(studio).append(")");
            } else {
                if (isMovie) {
                    sbSQL.append("SELECT 1 FROM videodata_studios vs, studio stu WHERE vd.id=vs.data_id AND vs.studio_id=stu.id ");
                } else {
                    sbSQL.append("SELECT 1 FROM series_studios ss, studio stu, season sea WHERE vd.season_id=sea.id ");
                    sbSQL.append("AND ss.series_id=sea.series_id AND ss.studio_id=stu.id ");
                }
                sbSQL.append("AND lower(stu.name)='").append(studio).append("')");
            }
        }

        // check country
        if (params.includeCountry() || params.excludeCountry()) {

            addExistsOrNot(params.includeCountry(), sbSQL);
            if (isMovie) {
                sbSQL.append("SELECT 1 FROM videodata_countries vc, country c WHERE vd.id=vc.data_id AND vc.country_id=c.id ");
            } else {
                sbSQL.append("SELECT 1 FROM series_countries sc, country c, season sea WHERE vd.season_id=sea.id ");
                sbSQL.append("AND sc.series_id=sea.series_id AND sc.country_id=c.id ");

            }
            sbSQL.append("AND lower(c.country_code)='").append(params.getCountryCode()).append("')");
        }

        // check studio
        if (params.includeCertification() || params.excludeCertification()) {
            int certId = params.getCertificationId();
            if (certId > 0) {

                addExistsOrNot(params.includeCertification(), sbSQL);
                if (isMovie) {
                    sbSQL.append("SELECT 1 FROM videodata_certifications vc WHERE vd.id=vc.data_id AND vc.cert_id=");
                } else {
                    sbSQL.append("SELECT 1 FROM series_certifications sc, season sea WHERE vd.season_id=sea.id ");
                    sbSQL.append("AND sc.series_id=sea.series_id AND sc.cert_id=");
                }
                sbSQL.append(certId).append(")");
            }
        }

        // check award
        if (params.includeAward() || params.excludeAward()) {
            final String awardName = params.getAwardName();

            addExistsOrNot(params.includeAward(), sbSQL);
            if (StringUtils.isNumeric(awardName)) {
                if (isMovie) {
                    sbSQL.append("SELECT 1 FROM videodata_awards va WHERE vd.id=va.videodata_id AND va.award_id=");
                } else {
                    sbSQL.append("SELECT 1 FROM series_awards sa, season sea WHERE vd.season_id=sea.id ");
                    sbSQL.append("AND sa.series_id=sea.series_id AND sa.award_id=");
                }
                sbSQL.append(awardName).append(")");
            } else {
                if (isMovie) {
                    sbSQL.append("SELECT 1 FROM videodata_awards va, award a WHERE vd.id=va.videodata_id AND va.award_id=a.id ");
                } else {
                    sbSQL.append("SELECT 1 FROM series_awards sa, award a, season sea WHERE vd.season_id=sea.id ");
                    sbSQL.append("AND sa.series_id=sea.series_id AND sa.award_id=a.id ");
                }
                sbSQL.append("AND lower(a.event)='").append(awardName).append("')");
            }
        }

        // check video source
        if (params.includeVideoSource() || params.excludeVideoSource()) {
            params.addParameter(EXTRA, Boolean.FALSE);
            params.addParameter("videoSource", params.getVideoSource().toLowerCase());

            addExistsOrNot(params.includeVideoSource(), sbSQL);
            sbSQL.append("SELECT 1 FROM mediafile mf ");
            sbSQL.append("JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id WHERE mv.videodata_id=vd.id ");
            sbSQL.append("AND mf.extra=:extra AND lower(mf.video_source)=:videoSource)");
        }

        // check resolution
        if (params.includeResolution() || params.excludeResolution()) {
            final ResolutionType resType = params.getResolution();
            params.addParameter(EXTRA, Boolean.FALSE);
            params.addParameter(MIN_WIDTH, resType.getMinWidth());
            params.addParameter(MAX_WIDTH, resType.getMaxWidth());

            addExistsOrNot(params.includeResolution(), sbSQL);
            sbSQL.append("SELECT 1 FROM mediafile mf JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
            sbSQL.append("WHERE mv.videodata_id=vd.id AND mf.extra=:extra ");
            sbSQL.append("AND mf.width>=:minWidth AND mf.width<=:maxWidth)");
        }
        
        // check rating
        if (params.includeRating() || params.excludeRating()) {
            String source = params.getRatingSource();
            if (source != null) {
                final int rating = params.getRating();

                addExistsOrNot(params.includeRating(), sbSQL);
                if (COMBINED.equalsIgnoreCase(source)) {
                    sbSQL.append("SELECT avg(vr.rating/10) as test, vr.videodata_id FROM videodata_ratings vr ");
                    sbSQL.append("WHERE vr.videodata_id = vd.id GROUP BY vr.videodata_id HAVING round(test)=").append(rating);
                } else {
                    sbSQL.append("SELECT 1 FROM videodata_ratings vr WHERE vr.videodata_id = vd.id ");
                    sbSQL.append("AND vr.sourcedb='").append(source).append("' ");
                    sbSQL.append("AND round(vr.rating/10)=").append(rating);
                }
                sbSQL.append(")");
            }
        }

        // check newest
        if (params.includeNewest() || params.excludeNewest()) {
            String source = params.getNewestSource();
            if (source != null) {
                Date newestDate = params.getNewestDate();
                params.addParameter(NEWEST_DATE, newestDate);

                if (CREATION.equalsIgnoreCase(source)) {
                    if (params.includeNewest()) {
                        sbSQL.append(" AND vd.create_timestamp >= :newestDate");
                    } else {
                        sbSQL.append(" AND vd.create_timestamp < :newestDate");
                    }
                } else if (LASTSCAN.equalsIgnoreCase(source)) {
                    if (params.includeNewest()) {
                        sbSQL.append(" AND (vd.last_scanned is null or vd.last_scanned >= :newestDate)");
                    } else {
                        sbSQL.append(" AND vd.last_scanned is not null AND vd.last_scanned < :newestDate");
                    }
                } else {
                    params.addParameter(EXTRA, Boolean.FALSE);
                    
                    addExistsOrNot(params.includeNewest(), sbSQL);
                    sbSQL.append("SELECT 1 FROM stage_file sf JOIN mediafile mf ON mf.id=sf.mediafile_id ");
                    sbSQL.append("JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
                    sbSQL.append("WHERE mv.videodata_id=vd.id AND sf.file_type='");
                    sbSQL.append(FileType.VIDEO.toString());
                    sbSQL.append("' AND sf.status != '");
                    sbSQL.append(StatusType.DUPLICATE.toString());
                    sbSQL.append("' AND mf.extra=:extra AND sf.file_date >= :newestDate)");
                }
            }
        }

        // check boxed set
        if (params.includeBoxedSet() || params.excludeBoxedSet()) {
            final int boxSetId = params.getBoxSetId();
            if (boxSetId > 0) {
                
                addExistsOrNot(params.includeBoxedSet(), sbSQL);
                sbSQL.append("SELECT 1 FROM boxed_set_order bo ");
                if (isMovie) {
                    sbSQL.append("WHERE bo.videodata_id=vd.id ");
                } else {
                    sbSQL.append("JOIN season sea ON sea.series_id=bo.series_id WHERE vd.season_id=sea.id ");
                }
                sbSQL.append("AND bo.boxedset_id=");
                sbSQL.append(boxSetId);
                sbSQL.append(")");
            }
        }

        // add the search string, this will be empty if there is no search required
        sbSQL.append(params.getSearchString(false));

        return sbSQL.toString();
    }

    /**
     * Create the SQL fragment for the selection of series
     */
    private static String generateSqlForSeries(IndexParams params) {
        StringBuilder sbSQL = new StringBuilder();

        sbSQL.append("SELECT ser.id");
        sbSQL.append(SQL_COMMA_SPACE_QUOTE).append(MetaDataType.SERIES).append(SQL_AS_VIDEO_TYPE);
        sbSQL.append(", ser.title, ser.title_original AS originalTitle, ser.title_sort AS sortTitle");
        sbSQL.append(", ser.start_year AS videoYear, null as releaseDate");
        sbSQL.append(", ser.id AS seriesId, null AS seasonId, null AS season, -1 AS episode");
        sbSQL.append(", (SELECT min(vid.watched) from videodata vid,season sea where vid.season_id=sea.id and sea.series_id=ser.id) as watched");
        sbSQL.append(", ser.create_timestamp as createTimestamp ");
        sbSQL.append(DataItemTools.addSqlDataItems(params.getDataItems(), "ser"));

        if (params.includeNewest() || params.excludeNewest()) {
            String source = params.getNewestSource();
            if (CREATION.equalsIgnoreCase(source)) {
                sbSQL.append(", ser.create_timestamp AS newest");
            } else if (LASTSCAN.equalsIgnoreCase(source)) {
                sbSQL.append(", ser.last_scanned AS newest");
            } else {
                params.addParameter(EXTRA, Boolean.FALSE);

                sbSQL.append(", (SELECT MAX(sf.file_date) FROM stage_file sf ");
                sbSQL.append("JOIN mediafile mf ON mf.id=sf.mediafile_id JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
                sbSQL.append("JOIN videodata vd ON mv.videodata_id=vd.id JOIN season sea ON sea.id=vd.season_id ");
                sbSQL.append("WHERE sea.series_id=ser.id AND sf.file_type='");
                sbSQL.append(FileType.VIDEO.name());
                sbSQL.append("' AND sf.status");
                sbSQL.append(SQL_IGNORE_STATUS_SET);
                sbSQL.append("AND mf.extra=:extra) as newest ");
            }
        }

        sbSQL.append(" FROM series ser");

        sbSQL.append(SQL_WHERE_1_EQ_1); // To make it easier to add the optional include and excludes

        if (params.getId() > 0L) {
            sbSQL.append(" AND ser.id=").append(params.getId());
        }

        if (params.includeYear()) {
            sbSQL.append(" AND ser.start_year=").append(params.getYear());
        } else if (params.excludeYear()) {
            sbSQL.append(" AND ser.start_year!=").append(params.getYear());
        }
        if (params.getYearStart() > 0) {
            sbSQL.append(" AND ser.start_year>=").append(params.getYearStart());
        }
        if (params.getYearEnd() > 0) {
            sbSQL.append(" AND ser.start_year<=").append(params.getYearEnd());
        }

        if (params.getWatched() != null) {
            sbSQL.append(" AND exists");
            sbSQL.append(" (SELECT 1 FROM videodata v,season sea WHERE v.watched=" );
            sbSQL.append(params.getWatched().booleanValue() ? "1" : "0");
            sbSQL.append(" AND v.season_id=sea.id and sea.series_id=ser.id)");
        }

        // check genre
        if (params.includeGenre() || params.excludeGenre()) {
            final String genre = params.getGenreName();

            addExistsOrNot(params.includeGenre(), sbSQL);
            sbSQL.append("SELECT 1 FROM series_genres sg, genre g WHERE ser.id=sg.series_id ");
            sbSQL.append("AND sg.genre_id=g.id ");
            sbSQL.append("AND (lower(g.name)='").append(genre).append("'");
            sbSQL.append(" or (g.target_api is not null and lower(g.target_api)='").append(genre).append("')");
            sbSQL.append(" or (g.target_xml is not null and lower(g.target_xml)='").append(genre).append("')))");
        }

        // check studio
        if (params.includeStudio() || params.excludeStudio()) {
            final String studio = params.getStudioName();

            addExistsOrNot(params.includeStudio(), sbSQL);
            if (StringUtils.isNumeric(studio)) {
                sbSQL.append("SELECT 1 FROM series_studios ss WHERE ss.series_id=ser.id ");
                sbSQL.append("AND ss.studio_id=").append(studio).append(")");
            } else {
                sbSQL.append("SELECT 1 FROM series_studios ss, studio stu WHERE ser.id=ss.series_id ");
                sbSQL.append("AND ss.studio_id=stu.id AND lower(stu.name)='").append(studio).append("')");
            }
        }

        // check country
        if (params.includeCountry() || params.excludeCountry()) {

            addExistsOrNot(params.includeCountry(), sbSQL);
            sbSQL.append("SELECT 1 FROM series_countries sc, country c ");
            sbSQL.append("WHERE ser.id=sc.series_id ");
            sbSQL.append("AND sc.country_id=c.id ");
            sbSQL.append("AND lower(c.country_code)='").append(params.getCountryCode()).append("')");
        }

        // check award
        if (params.includeAward() || params.excludeAward()) {
            final String awardName = params.getAwardName();

            addExistsOrNot(params.includeAward(), sbSQL);
            if (StringUtils.isNumeric(awardName)) {
                sbSQL.append("SELECT 1 FROM series_awards sa WHERE sa.series_id=ser.id ");
                sbSQL.append("AND sa.award_id=").append(awardName).append(")");
            } else {
                sbSQL.append("SELECT 1 FROM series_awards sa, award a WHERE sa.series_id=ser.id ");
                sbSQL.append("AND sa.award_id=a.id AND lower(a.event)='").append(awardName).append("')");
            }
        }

        // check certification
        if (params.includeCertification() || params.excludeCertification()) {
            final int certId = params.getCertificationId();
            if (certId > 0) {
                
                addExistsOrNot(params.includeCertification(), sbSQL);
                sbSQL.append("SELECT 1 FROM series_certifications sc WHERE ser.id=sc.series_id ");
                sbSQL.append("AND sc.cert_id=").append(certId).append(")");
            }
        }

        // check video source
        if (params.includeVideoSource() || params.excludeVideoSource()) {
            params.addParameter(EXTRA, Boolean.FALSE);
            params.addParameter("videoSource", params.getVideoSource().toLowerCase());

            addExistsOrNot(params.includeVideoSource(), sbSQL);
            sbSQL.append("SELECT 1 FROM mediafile mf JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
            sbSQL.append("JOIN videodata vd ON mv.videodata_id=vd.id JOIN season sea ON sea.id=vd.season_id ");
            sbSQL.append("WHERE sea.series_id=ser.id AND mf.extra=:extra AND lower(mf.video_source)=:videoSource)");
        }

        // check resolution
        if (params.includeResolution() || params.excludeResolution()) {
            final ResolutionType resType = params.getResolution();
            params.addParameter(EXTRA, Boolean.FALSE);
            params.addParameter(MIN_WIDTH, resType.getMinWidth());
            params.addParameter(MAX_WIDTH, resType.getMaxWidth());

            addExistsOrNot(params.includeResolution(), sbSQL);
            sbSQL.append("SELECT 1 FROM mediafile mf JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
            sbSQL.append("JOIN videodata vd ON mv.videodata_id=vd.id JOIN season sea ON sea.id=vd.season_id ");
            sbSQL.append("WHERE sea.series_id=ser.id AND mf.extra=:extra AND mf.width>=:minWidth  AND mf.width<=:maxWidth)");
        }
        
        // check rating
        if (params.includeRating() || params.excludeRating()) {
            String source = params.getRatingSource();
            if (source != null) {
                final int rating = params.getRating();

                addExistsOrNot(params.includeRating(), sbSQL);
                if (COMBINED.equalsIgnoreCase(source)) {
                    sbSQL.append("SELECT avg(sr.rating/10) as test, sr.series_id FROM series_ratings sr ");
                    sbSQL.append("WHERE sr.series_id = ser.id GROUP BY sr.series_id ");
                    sbSQL.append("HAVING round(test)=").append(rating);
                } else {
                    sbSQL.append("SELECT 1 FROM series_ratings sr WHERE sr.series_id = ser.id ");
                    sbSQL.append("AND sr.sourcedb='").append(source).append("' ");
                    sbSQL.append("AND round(sr.rating/10)=").append(rating);
                }
                sbSQL.append(")");
            }
        }

        // check newest
        if (params.includeNewest() || params.excludeNewest()) {
            String source = params.getNewestSource();
            if (source != null) {
                Date newestDate = params.getNewestDate();
                params.addParameter(NEWEST_DATE, newestDate);

                if (CREATION.equalsIgnoreCase(source)) {
                    if (params.includeNewest()) {
                        sbSQL.append(" AND ser.create_timestamp >= :newestDate");
                    } else {
                        sbSQL.append(" AND ser.create_timestamp < :newestDate");
                    }
                } else if (LASTSCAN.equalsIgnoreCase(source)) {
                    if (params.includeNewest()) {
                        sbSQL.append(" AND (ser.last_scanned is null or ser.last_scanned >= :newestDate)");
                    } else {
                        sbSQL.append(" AND ser.last_scanned is not null AND ser.last_scanned < :newestDate");
                    }
                } else {
                    params.addParameter(EXTRA, Boolean.FALSE);
                    
                    addExistsOrNot(params.includeNewest(), sbSQL);
                    sbSQL.append("SELECT 1 FROM stage_file sf JOIN mediafile mf ON mf.id=sf.mediafile_id ");
                    sbSQL.append("JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id JOIN videodata vd ON mv.videodata_id=vd.id ");
                    sbSQL.append("JOIN season sea ON sea.id=vd.season_id WHERE sea.series_id=ser.id ");
                    sbSQL.append("AND sf.file_type='");
                    sbSQL.append(FileType.VIDEO.name());
                    sbSQL.append("' AND sf.status");
                    sbSQL.append(SQL_IGNORE_STATUS_SET);
                    sbSQL.append("AND mf.extra=:extra ");
                    sbSQL.append("AND sf.file_date >= :newestDate)");
                }
            }
        }

        // check boxed set
        if (params.includeBoxedSet() || params.excludeBoxedSet()) {
            int boxSetId = params.getBoxSetId();
            if (boxSetId > 0) {
                addExistsOrNot(params.includeBoxedSet(), sbSQL);
                sbSQL.append("SELECT 1 FROM boxed_set_order bo WHERE bo.series_id=ser.id ");
                sbSQL.append("AND bo.boxedset_id=").append(boxSetId).append(")");
            }
        }

        // add the search string, this will be empty if there is no search required
        sbSQL.append(params.getSearchString(false));

        return sbSQL.toString();
    }

    /**
     * Create the SQL fragment for the selection of seasons
     *
     * @param options
     * @param includes
     * @param excludes
     * @return
     */
    private static String generateSqlForSeason(IndexParams params) {
        StringBuilder sbSQL = new StringBuilder();

        sbSQL.append("SELECT sea.id");
        sbSQL.append(SQL_COMMA_SPACE_QUOTE).append(MetaDataType.SEASON).append(SQL_AS_VIDEO_TYPE);
        sbSQL.append(", sea.title, sea.title_original AS originalTitle, sea.title_sort AS sortTitle");
        sbSQL.append(", sea.publication_year as videoYear, null as releaseDate");
        sbSQL.append(", sea.series_id AS seriesId, sea.id AS seasonId, sea.season AS season, -1 AS episode");
        sbSQL.append(", (SELECT min(vid.watched) from videodata vid where vid.season_id=sea.id) as watched");
        sbSQL.append(", sea.create_timestamp as createTimestamp ");
        sbSQL.append(DataItemTools.addSqlDataItems(params.getDataItems(), "sea"));

        if (params.includeNewest() || params.excludeNewest()) {
            String source = params.getNewestSource();
            if (CREATION.equalsIgnoreCase(source)) {
                sbSQL.append(", sea.create_timestamp AS newest");
            } else if (LASTSCAN.equalsIgnoreCase(source)) {
                sbSQL.append(", sea.last_scanned AS newest");
            } else {
                params.addParameter(EXTRA, Boolean.FALSE);

                sbSQL.append(", (SELECT MAX(sf.file_date) FROM stage_file sf JOIN mediafile mf ON mf.id=sf.mediafile_id ");
                sbSQL.append("JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id JOIN videodata vd ON mv.videodata_id=vd.id ");
                sbSQL.append("WHERE vd.season_id=sea.id AND sf.file_type='");
                sbSQL.append(FileType.VIDEO.name());
                sbSQL.append("' AND sf.status");
                sbSQL.append(SQL_IGNORE_STATUS_SET);
                sbSQL.append("AND mf.extra=:extra) AS newest");
            }
        }

        sbSQL.append(" FROM season sea");

        sbSQL.append(SQL_WHERE_1_EQ_1); // To make it easier to add the optional include and excludes
        if (params.getId().longValue() > 0L) {
            sbSQL.append(" AND sea.id=").append(params.getId());
        }

        if (params.includeYear()) {
            sbSQL.append(" AND sea.publication_year=").append(params.getYear());
        } else if (params.excludeYear()) {
            sbSQL.append(" AND sea.publication_year!=").append(params.getYear());
        }
        if (params.getYearStart() > 0) {
            sbSQL.append(" AND sea.publication_year>=").append(params.getYearStart());
        }
        if (params.getYearEnd() > 0) {
            sbSQL.append(" AND sea.publication_year<=").append(params.getYearEnd());
        }
        
        // check watched
        if (params.getWatched() != null) {
            sbSQL.append(" AND exists (SELECT 1 FROM videodata v WHERE v.watched=");
            sbSQL.append(params.getWatched().booleanValue() ? "1" : "0");
            sbSQL.append(" AND v.season_id=sea.id)");
        }

        // check genre
        if (params.includeGenre() || params.excludeGenre()) {
            final String genre = params.getGenreName();

            addExistsOrNot(params.includeGenre(), sbSQL);
            sbSQL.append("SELECT 1 FROM series_genres sg, genre g WHERE sea.series_id=sg.series_id AND sg.genre_id=g.id ");
            sbSQL.append("AND (lower(g.name)='").append(genre).append("'");
            sbSQL.append(" or (g.target_api is not null and lower(g.target_api)='").append(genre).append("')");
            sbSQL.append(" or (g.target_xml is not null and lower(g.target_xml)='").append(genre).append("')))");
        }

        // check studio
        if (params.includeStudio() || params.excludeStudio()) {
            final String studio = params.getStudioName();

            addExistsOrNot(params.includeStudio(), sbSQL);
            if (StringUtils.isNumeric(studio)) {
                sbSQL.append("SELECT 1 FROM series_studios ss WHERE sea.series_id=ss.series_id ");
                sbSQL.append("AND ss.studio_id=").append(studio).append(")");
            } else {
                sbSQL.append("SELECT 1 FROM series_studios ss, studio stu WHERE sea.series_id=ss.series_id ");
                sbSQL.append("AND ss.studio_id=stu.id AND lower(stu.name)='").append(studio).append("')");
            }
        }

        // check country
        if (params.includeCountry() || params.excludeCountry()) {

            addExistsOrNot(params.includeStudio(), sbSQL);
            sbSQL.append("SELECT 1 FROM series_countries sc, country c WHERE sea.series_id=sc.series_id ");
            sbSQL.append("AND sc.country_id=c.id AND lower(c.country_code)='").append(params.getCountryCode()).append("')");
        }

        // check award
        if (params.includeAward() || params.excludeAward()) {
            final String awardName = params.getAwardName();

            addExistsOrNot(params.includeAward(), sbSQL);
            if (StringUtils.isNumeric(awardName)) {
                sbSQL.append("SELECT 1 FROM series_awards sa WHERE sa.series_id=sea.series_id ");
                sbSQL.append("AND sa.award_id=").append(awardName).append(")");
            } else {
                sbSQL.append("SELECT 1 FROM series_awards sa, award a WHERE sa.series_id=sea.series_id ");
                sbSQL.append("AND sa.award_id=a.id AND lower(a.event)='").append(awardName).append("')");
            }
        }

        // check certification
        if (params.includeCertification() || params.excludeCertification()) {
            int certId = params.getCertificationId();
            if (certId > 0) {
                addExistsOrNot(params.includeCertification(), sbSQL);
                sbSQL.append("SELECT 1 FROM series_certifications sc WHERE sea.series_id=sc.series_id ");
                sbSQL.append("AND sc.cert_id=").append(certId).append(")");
            }
        }

        // check video source
        if (params.includeVideoSource() || params.excludeVideoSource()) {
            params.addParameter(EXTRA, Boolean.FALSE);
            params.addParameter("videoSource", params.getVideoSource().toLowerCase());

            addExistsOrNot(params.includeVideoSource(), sbSQL);
            sbSQL.append("SELECT 1 FROM mediafile mf JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
            sbSQL.append("JOIN videodata vd ON mv.videodata_id=vd.id WHERE vd.season_id=sea.id ");
            sbSQL.append("AND mf.extra=:extra AND lower(mf.video_source)=:videoSource)");
        }

        // check resolution
        if (params.includeResolution() || params.excludeResolution()) {
            final ResolutionType resType = params.getResolution();
            params.addParameter(EXTRA, Boolean.FALSE);
            params.addParameter(MIN_WIDTH, resType.getMinWidth());
            params.addParameter(MAX_WIDTH, resType.getMaxWidth());

            addExistsOrNot(params.includeResolution(), sbSQL);
            sbSQL.append("SELECT 1 FROM mediafile mf JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
            sbSQL.append("JOIN videodata vd ON mv.videodata_id=vd.id WHERE vd.season_id=sea.id ");
            sbSQL.append("AND mf.extra=:extra AND mf.width>=:minWidth AND mf.width<=:maxWidth)");
        }
        
        // check rating
        if (params.includeRating() || params.excludeRating()) {
            String source = params.getRatingSource();
            if (source != null) {
                final int rating = params.getRating();

                addExistsOrNot(params.includeRating(), sbSQL);
                if (COMBINED.equalsIgnoreCase(source)) {
                    sbSQL.append("SELECT avg(sr.rating/10) as test, sr.series_id ");
                    sbSQL.append("FROM series_ratings sr WHERE sr.series_id = sea.series_id ");
                    sbSQL.append("GROUP BY sr.series_id HAVING round(test)=").append(rating);
                } else {
                    sbSQL.append("SELECT 1 FROM series_ratings sr WHERE sr.series_id = sea.series_id ");
                    sbSQL.append("AND sr.sourcedb='").append(source).append("' ");
                    sbSQL.append("AND round(sr.rating/10)=").append(rating);
                }
                sbSQL.append(")");
            }
        }

        // check newest
        if (params.includeNewest() || params.excludeNewest()) {
            String source = params.getNewestSource();
            if (source != null) {
                Date newestDate = params.getNewestDate();
                params.addParameter(NEWEST_DATE, newestDate);

                if (CREATION.equalsIgnoreCase(source)) {
                    if (params.includeNewest()) {
                        sbSQL.append(" AND sea.create_timestamp >= :newestDate");
                    } else {
                        sbSQL.append(" AND sea.create_timestamp < :newestDate");
                    }
                } else if (LASTSCAN.equalsIgnoreCase(source)) {
                    if (params.includeNewest()) {
                        sbSQL.append(" AND (sea.last_scanned is null or sea.last_scanned >= :newestDate)");
                    } else {
                        sbSQL.append(" AND sea.last_scanned is not null AND sea.last_scanned < :newestDate");
                    }
                } else {
                    params.addParameter(EXTRA, Boolean.FALSE);

                    addExistsOrNot(params.includeNewest(), sbSQL);
                    sbSQL.append("JOIN mediafile mf ON mf.id=sf.mediafile_id JOIN mediafile_videodata mv ON mv.mediafile_id=mf.id ");
                    sbSQL.append("JOIN videodata vd ON mv.videodata_id=vd.id WHERE vd.season_id=sea.id ");
                    sbSQL.append("AND sf.file_type='");
                    sbSQL.append(FileType.VIDEO.toString());
                    sbSQL.append("' AND sf.status != '");
                    sbSQL.append(StatusType.DUPLICATE.toString());
                    sbSQL.append("' AND mf.extra=:extra ");
                    sbSQL.append("AND sf.file_date >= :newestDate)");
                }
            }
        }

        // check boxed set
        if (params.includeBoxedSet() || params.excludeBoxedSet()) {
            final int boxSetId = params.getBoxSetId();
            if (boxSetId > 0) {
                addExistsOrNot(params.includeBoxedSet(), sbSQL);
                sbSQL.append("SELECT 1 FROM boxed_set_order bo WHERE bo.series_id=sea.series_id ");
                sbSQL.append("AND bo.boxedset_id=").append(boxSetId).append(")");
            }
        }

        // add the search string, this will be empty if there is no search required
        sbSQL.append(params.getSearchString(false));

        return sbSQL.toString();
    }

    /**
     * Search the list of IDs for artwork and add to the artworkList.
     *
     * @param metaDataIds
     * @param artworkList
     * @param options
     */
    private void addArtworks(Map<MetaDataType, List<Long>> metaDataIds, Map<String, ApiVideoDTO> artworkList, OptionsIndexVideo options) {
        Set<String> artworkRequired = options.getArtworkTypes();
        LOG.debug("Artwork required: {}", artworkRequired);

        SqlScalars sqlScalars = new SqlScalars();
        boolean hasMovie = CollectionUtils.isNotEmpty(metaDataIds.get(MetaDataType.MOVIE));
        boolean hasSeries = CollectionUtils.isNotEmpty(metaDataIds.get(MetaDataType.SERIES));
        boolean hasSeason = CollectionUtils.isNotEmpty(metaDataIds.get(MetaDataType.SEASON));
        boolean hasEpisode = CollectionUtils.isNotEmpty(metaDataIds.get(MetaDataType.EPISODE));

        if (hasMovie) {
            sqlScalars.addToSql("SELECT 'MOVIE' as source, v.id, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkType, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
            sqlScalars.addToSql("FROM videodata v, artwork a");
            sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
            sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
            sqlScalars.addToSql("WHERE v.id=a.videodata_id");
            sqlScalars.addToSql("AND v.episode<0");
            sqlScalars.addToSql("AND v.id IN (:movielist)");
            sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
        }

        if (hasSeries) {
            if (hasMovie) {
                sqlScalars.addToSql(SQL_UNION);
            }

            sqlScalars.addToSql("SELECT 'SERIES' as source, s.id, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkType, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
            sqlScalars.addToSql("FROM series s, artwork a");
            sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
            sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
            sqlScalars.addToSql("WHERE s.id=a.series_id");
            sqlScalars.addToSql("AND s.id IN (:serieslist)");
            sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
        }

        if (hasSeason) {
            if (hasMovie || hasSeries) {
                sqlScalars.addToSql(SQL_UNION);
            }

            sqlScalars.addToSql("SELECT 'SEASON' as source, s.id, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkType, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
            sqlScalars.addToSql("FROM season s, artwork a");
            sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
            sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
            sqlScalars.addToSql("WHERE s.id=a.season_id");
            sqlScalars.addToSql("AND s.id IN (:seasonlist)");
            sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
        }

        if (hasEpisode) {
            if (hasMovie || hasSeries || hasSeason) {
                sqlScalars.addToSql(SQL_UNION);
            }

            sqlScalars.addToSql("SELECT 'EPISODE' as source, v.id, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkType, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
            sqlScalars.addToSql("FROM videodata v, artwork a");
            sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
            sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
            sqlScalars.addToSql("WHERE v.id=a.videodata_id");
            sqlScalars.addToSql("AND v.episode>-1");
            sqlScalars.addToSql("AND v.id IN (:episodelist)");
            sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
        }

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(SOURCE, StringType.INSTANCE);
        sqlScalars.addScalar(ARTWORK_ID, LongType.INSTANCE);
        sqlScalars.addScalar(LOCATED_ID, LongType.INSTANCE);
        sqlScalars.addScalar(GENERATED_ID, LongType.INSTANCE);
        sqlScalars.addScalar(ARTWORK_TYPE, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_DIR, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_FILENAME, StringType.INSTANCE);

        if (hasMovie) {
            sqlScalars.addParameter("movielist", metaDataIds.get(MetaDataType.MOVIE));
        }

        if (hasSeries) {
            sqlScalars.addParameter("serieslist", metaDataIds.get(MetaDataType.SERIES));
        }

        if (hasSeason) {
            sqlScalars.addParameter("seasonlist", metaDataIds.get(MetaDataType.SEASON));
        }

        if (hasEpisode) {
            sqlScalars.addParameter("episodelist", metaDataIds.get(MetaDataType.EPISODE));
        }

        sqlScalars.addParameter("artworklist", artworkRequired);

        List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars);

        LOG.trace("Found {} artworks", results.size());
        for (ApiArtworkDTO ia : results) {
            final String key = KeyMaker.makeKey(ia);
            LOG.trace("  {} = {}", key, ia.toString());
            artworkList.get(key).addArtwork(ia);
        }
    }

    /**
     * Get a list of the people
     *
     * @param wrapper
     */
    public List<ApiPersonDTO> getPersonList(ApiWrapperList<ApiPersonDTO> wrapper) {
        OptionsId options = (OptionsId) wrapper.getOptions();
        SqlScalars sqlScalars = generateSqlForPerson(options);
        List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
        
        if (!results.isEmpty() && options.hasDataItem(DataItem.ARTWORK)) {
            LOG.trace("Adding photos");
            // Get the artwork associated with the IDs in the results
            Set<String> artworkRequired = Collections.singleton(ArtworkType.PHOTO.toString());
            Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForMetadata(MetaDataType.PERSON, generateIdList(results), artworkRequired, options.getArtworksortdir());
            for (ApiPersonDTO p : results) {
                if (artworkList.containsKey(p.getId())) {
                    p.setArtwork(artworkList.get(p.getId()));
                }
            }
        }
        
        return results;
    }

    /**
     * Get a single person using the ID in the wrapper options.
     *
     * @param wrapper
     */
    public ApiPersonDTO getPerson(ApiWrapperSingle<ApiPersonDTO> wrapper) {
        OptionsId options = (OptionsId) wrapper.getOptions();
        SqlScalars sqlScalars = generateSqlForPerson(options);
        List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
        
        ApiPersonDTO person = null;
        if (CollectionUtils.isNotEmpty(results)) {
            person = results.get(0);
            
            if (options.hasDataItem(DataItem.ARTWORK)) {
                LOG.info("Adding photo for '{}'", person.getName());
                // Add the artwork
                Set<String> artworkRequired = Collections.singleton(ArtworkType.PHOTO.toString());
                Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForMetadata(MetaDataType.PERSON, person.getId(), artworkRequired, options.getArtworksortdir());
                if (artworkList.containsKey(options.getId())) {
                    LOG.info("Found {} artworks", artworkList.get(options.getId()).size());
                    person.setArtwork(artworkList.get(options.getId()));
                } else {
                    LOG.info("No artwork found for person ID {}", options.getId());
                }
            }

            if (options.hasDataItem(DataItem.EXTERNALID)) {
                LOG.trace("Adding external IDs for ID {}", options.getId());
                person.setExternalIds(getExternalIdsForMetadata(MetaDataType.PERSON, options.getId()));
            }

            if (options.hasDataItem(DataItem.FILMOGRAPHY_INSIDE)) {
                LOG.info("Adding filmograpghy inside for '{}'", person.getName());
                person.setFilmography(getPersonFilmographyInside(person.getId(), options));
            } else if (options.hasDataItem(DataItem.FILMOGRAPHY_SCANNED)) {
                LOG.info("Adding filmograpghy scanned for '{}'", person.getName());
                person.setFilmography(getPersonFilmographyScanned(person.getId(), options));
            }
        }
        return person;
    }

    private List<ApiFilmographyDTO> getPersonFilmographyInside(Long id, OptionsId options) {
        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT DISTINCT '");
        sbSQL.append(ParticipationType.MOVIE.name());
        sbSQL.append("' as type, c1.job as job, c1.role as role, c1.voice_role as voiceRole, ");
        sbSQL.append("v1.title as title, v1.title_original as originalTitle, v1.publication_year as year, -1 as yearEnd,");
        sbSQL.append("v1.release_date as releaseDate, v1.release_country_code as releaseCountryCode,");
        sbSQL.append("v1.id as videoDataId, null as seriesId ");

        if (options.hasDataItem(DataItem.PLOT)) {
            sbSQL.append(", v1.plot as description ");
        } else {
            sbSQL.append(", null as description ");
        }

        sbSQL.append("FROM cast_crew c1, videodata v1 ");
        sbSQL.append("WHERE c1.person_id = :id and v1.id=c1.videodata_id and v1.episode<0 ");
        sbSQL.append(SQL_UNION);
        sbSQL.append("SELECT DISTINCT '");
        sbSQL.append(ParticipationType.SERIES.name());
        sbSQL.append("' as type, c2.job as job, c2.role as role, c2.voice_role as voiceRole, ");
        sbSQL.append("ser.title as title, ser.title_original as originalTitle, ser.start_year as year, ser.end_year as yearEnd,");
        sbSQL.append("null as releaseDate, null as releaseCountryCode,");
        sbSQL.append("null as videoDataId, ser.id as seriesId ");

        if (options.hasDataItem(DataItem.PLOT)) {
            sbSQL.append(", ser.plot as description ");
        } else {
            sbSQL.append(", null as description ");
        }

        sbSQL.append("FROM cast_crew c2, videodata v2, season sea, series ser ");
        sbSQL.append("WHERE c2.person_id = :id and v2.id=c2.videodata_id and v2.episode>=0 ");
        sbSQL.append("and v2.season_id=sea.id and sea.series_id=ser.id ");

        // sorting
        final String sortDir = "DESC".equalsIgnoreCase(options.getSortdir()) ? "DESC" : "ASC";

        sbSQL.append("ORDER BY ");
        if (TITLE.equalsIgnoreCase(options.getSortby())) {
            sbSQL.append("title ");
            sbSQL.append(sortDir);
            sbSQL.append(", ");
        } else if (TYPE.equalsIgnoreCase(options.getSortby())) {
            sbSQL.append("type ");
            sbSQL.append(sortDir);
            sbSQL.append(", ");
        } else if (JOB.equalsIgnoreCase(options.getSortby())) {
            sbSQL.append("job ");
            sbSQL.append(sortDir);
            sbSQL.append(", ");
        }
        sbSQL.append("year ");
        sbSQL.append(sortDir);
        sbSQL.append(", releaseDate ");
        sbSQL.append(sortDir);

        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        LOG.info("Filmography inside SQL: {}", sqlScalars.getSql());

        return retrieveFilmography(id, sqlScalars);
    }

    private List<ApiFilmographyDTO> getPersonFilmographyScanned(Long id, OptionsId options) {
        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT DISTINCT p.participation_type as type, p.job as job, p.role as role, p.voice_role as voiceRole, ");
        sbSQL.append("p.title as title, p.title_original as originalTitle, p.year as year,p.year_end as yearEnd,");
        sbSQL.append("p.release_date as releaseDate, p.release_country_code as releaseCountryCode,");
        sbSQL.append("movie.id as videoDataId, serids.series_id as seriesId ");

        if (options.hasDataItem(DataItem.PLOT)) {
            sbSQL.append(", p.description as description ");
        } else {
            sbSQL.append(", null as description ");
        }

        sbSQL.append("FROM participation p ");
        sbSQL.append(" LEFT OUTER JOIN (SELECT DISTINCT v1.id, p1.id as participation_id ");
        sbSQL.append("  FROM participation p1 ");
        sbSQL.append("  JOIN cast_crew c1 ON c1.person_id=p1.person_id ");
        sbSQL.append("  JOIN videodata v1 ON c1.videodata_id=v1.id and v1.episode<0 ");
        sbSQL.append("  LEFT OUTER JOIN videodata_ids ids on v1.id=ids.videodata_id ");
        sbSQL.append("  WHERE p1.person_id=:id and p1.participation_type='MOVIE' ");
        sbSQL.append("  and ((v1.publication_year=p1.year and p1.title_original is not null and upper(v1.title_original)=upper(p1.title_original)) ");
        sbSQL.append("       or (ids.sourcedb=p1.sourcedb and ids.sourcedb_id=p1.sourcedb_id))) movie ");
        sbSQL.append(" ON p.id=movie.participation_id ");
        sbSQL.append("LEFT OUTER JOIN series_ids serids ON serids.sourcedb=p.sourcedb and serids.sourcedb_id=p.sourcedb_id ");
        sbSQL.append("WHERE p.person_id = :id ");

        // sorting
        final String sortDir = "DESC".equalsIgnoreCase(options.getSortdir()) ? "DESC" : "ASC";

        sbSQL.append("ORDER BY ");
        if (TITLE.equalsIgnoreCase(options.getSortby())) {
            sbSQL.append("p.title ");
            sbSQL.append(sortDir);
            sbSQL.append(", ");
        } else if (TYPE.equalsIgnoreCase(options.getSortby())) {
            sbSQL.append("p.participation_type ");
            sbSQL.append(sortDir);
            sbSQL.append(", ");
        } else if (JOB.equalsIgnoreCase(options.getSortby())) {
            sbSQL.append("p.job ");
            sbSQL.append(sortDir);
            sbSQL.append(", ");
        }
        sbSQL.append("p.year ");
        sbSQL.append(sortDir);
        sbSQL.append(", p.release_date ");
        sbSQL.append(sortDir);

        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        LOG.info("Filmography scanned SQL: {}", sqlScalars.getSql());

        return retrieveFilmography(id, sqlScalars);
    }

    public List<ApiFilmographyDTO> retrieveFilmography(Long id, SqlScalars sqlScalars) {
        sqlScalars.addScalar(TYPE, StringType.INSTANCE);
        sqlScalars.addScalar(JOB, StringType.INSTANCE);
        sqlScalars.addScalar("role", StringType.INSTANCE);
        sqlScalars.addScalar("voiceRole", BooleanType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(YEAR, IntegerType.INSTANCE);
        sqlScalars.addScalar("yearEnd", IntegerType.INSTANCE);
        sqlScalars.addScalar("releaseDate", DateType.INSTANCE);
        sqlScalars.addScalar("releaseCountryCode", StringType.INSTANCE);
        sqlScalars.addScalar("description", StringType.INSTANCE);
        sqlScalars.addScalar("videoDataId", LongType.INSTANCE);
        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);

        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiFilmographyDTO.class, sqlScalars);
    }

    public  List<ApiPersonDTO> getPersonListByVideoType(MetaDataType metaDataType, ApiWrapperList<ApiPersonDTO> wrapper) {
        OptionsId options = (OptionsId) wrapper.getOptions();
        LOG.info("Getting person list for {} with ID {}", metaDataType, options.getId());

        SqlScalars sqlScalars = generateSqlForVideoPerson(metaDataType, options);
        List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
        LOG.info("Found {} results for {} with ID {}", results.size(), metaDataType, options.getId());

        if (!results.isEmpty() && options.hasDataItem(DataItem.ARTWORK)) {
            LOG.info("Looking for person artwork for {} with ID {}", metaDataType, options.getId());

            Set<String> artworkRequired = Collections.singleton(ArtworkType.PHOTO.toString());
            Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForMetadata(MetaDataType.PERSON, generateIdList(results), artworkRequired, options.getArtworksortdir());
            for (ApiPersonDTO person : results) {
                if (artworkList.containsKey(person.getId())) {
                    person.setArtwork(artworkList.get(person.getId()));
                }
            }
        } else {
            LOG.info("No artwork found/requested for {} with ID {}", metaDataType, options.getId());
        }

        return results;
    }

    /**
     * Generates a list of people in a video
     *
     * @param metaDataType
     * @param options
     * @return
     */
    private static SqlScalars generateSqlForVideoPerson(MetaDataType metaDataType, OptionsId options) {
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT DISTINCT p.id,");
        if (options.hasDataItem(DataItem.BIOGRAPHY)) {
            sqlScalars.addToSql("p.biography,");
            sqlScalars.addScalar("biography", StringType.INSTANCE);
        }
        sqlScalars.addToSql("p.name,");
        sqlScalars.addToSql("p.first_name as firstName,");
        sqlScalars.addToSql("p.last_name as lastName,");
        sqlScalars.addToSql("p.birth_day AS birthDay,");
        sqlScalars.addToSql("p.birth_place AS birthPlace,");
        sqlScalars.addToSql("p.birth_name AS birthName,");
        sqlScalars.addToSql("p.death_day AS deathDay,");
        sqlScalars.addToSql("p.death_place AS deathPlace,");
        sqlScalars.addToSql("c.job as job,");
        sqlScalars.addToSql("c.role as role,");
        sqlScalars.addToSql("c.voice_role as voiceRole ");
        sqlScalars.addToSql("FROM person p ");

        if (metaDataType == MetaDataType.SERIES) {
            sqlScalars.addToSql("JOIN cast_crew c ON c.person_id=p.id");
            sqlScalars.addToSql("JOIN videodata vd ON vd.id=c.videodata_id");
            sqlScalars.addToSql("JOIN season sea ON sea.id=vd.season_id and sea.series_id=:id");
        } else if (metaDataType == MetaDataType.SEASON) {
            sqlScalars.addToSql("JOIN cast_crew c ON c.person_id=p.id");
            sqlScalars.addToSql("JOIN videodata vd ON vd.id=c.videodata_id and vd.season_id=:id");
        } else {
            // defaults to movie/episode
            sqlScalars.addToSql("JOIN cast_crew c ON c.person_id=p.id and c.videodata_id=:id");
        }

        sqlScalars.addToSql("WHERE p.id=c.person_id");
        sqlScalars.addToSql("AND p.status"+SQL_IGNORE_STATUS_SET);
        
        if (MapUtils.isNotEmpty(options.splitJobs())) {
            sqlScalars.addToSql("AND c.job IN (:jobs)");
            sqlScalars.addParameter("jobs", options.getJobTypes());
        }

        // Add the search string
        sqlScalars.addToSql(options.getSearchString(false));
        // This will default to blank if there's no  required
        sqlScalars.addToSql(options.getSortString());

        // Add the ID
        sqlScalars.addParameter(ID, options.getId());

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(NAME, StringType.INSTANCE);
        sqlScalars.addScalar("firstName", StringType.INSTANCE);
        sqlScalars.addScalar("lastName", StringType.INSTANCE);
        sqlScalars.addScalar("birthDay", DateType.INSTANCE);
        sqlScalars.addScalar("birthPlace", StringType.INSTANCE);
        sqlScalars.addScalar("birthName", StringType.INSTANCE);
        sqlScalars.addScalar("deathDay", DateType.INSTANCE);
        sqlScalars.addScalar("deathPlace", StringType.INSTANCE);
        sqlScalars.addScalar(JOB, StringType.INSTANCE);
        sqlScalars.addScalar("role", StringType.INSTANCE);
        sqlScalars.addScalar("voiceRole", BooleanType.INSTANCE);

        LOG.debug("SQL ForVideoPerson: {}", sqlScalars.getSql());
        return sqlScalars;
    }

    /**
     * Generate the SQL for the information about a person
     *
     * @param options
     * @return
     */
    private static SqlScalars generateSqlForPerson(OptionsId options) {
        SqlScalars sqlScalars = new SqlScalars();
        // Make sure to set the alias for the files for the Transformation into the class
        sqlScalars.addToSql("SELECT DISTINCT p.id,p.name,");
        sqlScalars.addToSql("p.first_name AS firstName,");
        sqlScalars.addToSql("p.last_name AS lastName,");
        sqlScalars.addToSql("p.birth_day AS birthDay,");
        sqlScalars.addToSql("p.birth_place AS birthPlace,");
        sqlScalars.addToSql("p.birth_name AS birthName,");
        sqlScalars.addToSql("p.death_day AS deathDay,");
        sqlScalars.addToSql("p.death_place AS deathPlace");
        sqlScalars.addToSql(DataItemTools.addSqlDataItems(options.splitDataItems(), "p").toString());
        sqlScalars.addToSql("FROM person p");

        if (options.getId() > 0L) {
            sqlScalars.addToSql("WHERE p.status" + SQL_IGNORE_STATUS_SET);
            sqlScalars.addToSql("AND id=:id");
            sqlScalars.addParameter(ID, options.getId());
        } else {
            if (MapUtils.isNotEmpty(options.splitJobs())) {
                sqlScalars.addToSql(", cast_crew c");
            }
            sqlScalars.addToSql("WHERE p.status" + SQL_IGNORE_STATUS_SET);

            if (MapUtils.isNotEmpty(options.splitJobs())) {
                sqlScalars.addToSql("AND p.id=c.person_id");
                sqlScalars.addToSql("AND c.job IN (:jobs)");
                sqlScalars.addParameter("jobs", options.getJobTypes());
            }

            // Add the search string
            sqlScalars.addToSql(options.getSearchString(false));
            // This will default to blank if there's no sort required
            sqlScalars.addToSql(options.getSortString());
        }

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(NAME, StringType.INSTANCE);
        sqlScalars.addScalar("firstName", StringType.INSTANCE);
        sqlScalars.addScalar("lastName", StringType.INSTANCE);
        sqlScalars.addScalar("birthDay", DateType.INSTANCE);
        sqlScalars.addScalar("birthPlace", StringType.INSTANCE);
        sqlScalars.addScalar("birthName", StringType.INSTANCE);
        sqlScalars.addScalar("deathDay", DateType.INSTANCE);
        sqlScalars.addScalar("deathPlace", StringType.INSTANCE);

        // add Scalars for additional data item columns
        DataItemTools.addDataItemScalars(sqlScalars, options.splitDataItems());

        return sqlScalars;
    }

    //<editor-fold defaultstate="collapsed" desc="Artwork Methods">
    public ApiArtworkDTO getArtworkById(Long id) {
        SqlScalars sqlScalars = getSqlArtwork(new OptionsIndexArtwork(id));

        List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public List<ApiArtworkDTO> getArtworkList(ApiWrapperList<ApiArtworkDTO> wrapper) {
        SqlScalars sqlScalars = getSqlArtwork((OptionsIndexArtwork) wrapper.getOptions());
        return executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, wrapper);
    }

    private static SqlScalars getSqlArtwork(OptionsIndexArtwork options) {
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT a.id AS artworkId,");
        sqlScalars.addToSql("al.id AS locatedId,");
        sqlScalars.addToSql("ag.id AS generatedId,");
        sqlScalars.addToSql("a.season_id AS seasonId,");
        sqlScalars.addToSql("a.series_id AS seriesId,");
        sqlScalars.addToSql("a.videodata_id AS videodataId,");
        sqlScalars.addToSql("a.artwork_type AS artworkType,");
        sqlScalars.addToSql("ag.cache_filename AS cacheFilename,");
        sqlScalars.addToSql("ag.cache_dir AS cacheDir");
        sqlScalars.addToSql("FROM artwork a");
        sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
        sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
        sqlScalars.addToSql(SQL_WHERE_1_EQ_1);
        
        if (options != null) {
            if (options.getId() > 0L) {
                sqlScalars.addToSql("AND a.id=:id");
                sqlScalars.addParameter(ID, options.getId());
            }

            if (CollectionUtils.isNotEmpty(options.getArtwork())) {
                sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
                sqlScalars.addParameter("artworklist", options.getArtwork());
            }

            if (CollectionUtils.isNotEmpty(options.getVideo())) {
                StringBuilder sb = new StringBuilder("AND (");
                boolean first = true;
                for (String type : options.getVideo()) {
                    MetaDataType mdt = MetaDataType.fromString(type);
                    if (first) {
                        first = false;
                    } else {
                        sb.append(" OR");
                    }
                    if (mdt == MetaDataType.MOVIE) {
                        sb.append(" videodata_id IS NOT NULL");
                    } else if (mdt == MetaDataType.SERIES) {
                        sb.append(" series_id IS NOT NULL");
                    } else if (mdt == MetaDataType.SEASON) {
                        sb.append(" season_id IS NOT NULL");
                    } else if (mdt == MetaDataType.PERSON) {
                        sb.append(" person_id IS NOT NULL");
                    } else if (mdt == MetaDataType.BOXSET) {
                        sb.append(" boxedset_id IS NOT NULL");
                    }
                }
                sb.append(")");
                sqlScalars.addToSql(sb.toString());
            }
        }

        // Add the scalars
        sqlScalars.addScalar(ARTWORK_ID, LongType.INSTANCE);
        sqlScalars.addScalar(LOCATED_ID, LongType.INSTANCE);
        sqlScalars.addScalar(GENERATED_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar("videodataId", LongType.INSTANCE);
        sqlScalars.addScalar(ARTWORK_TYPE, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_DIR, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_FILENAME, StringType.INSTANCE);

        return sqlScalars;
    }
    //</editor-fold>

    public List<ApiEpisodeDTO> getEpisodeList(ApiWrapperList<ApiEpisodeDTO> wrapper, OptionsEpisode options) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT ser.id AS seriesId, sea.id AS seasonId, sea.season, vid.episode, ");
        sqlScalars.addToSql("vid.id, vid.title, vid.title_original as originalTitle, vid.release_date as firstAired, vid.watched as watched, ");
        if (options.hasDataItem(DataItem.PLOT)) {
            sqlScalars.addToSql("vid.plot, ");
            sqlScalars.addScalar("plot", StringType.INSTANCE);
        }
        if (options.hasDataItem(DataItem.OUTLINE)) {
            sqlScalars.addToSql("vid.outline, ");
            sqlScalars.addScalar("outline", StringType.INSTANCE);
        }
        sqlScalars.addToSql("ag.cache_filename AS cacheFilename, ag.cache_dir AS cacheDir ");
        sqlScalars.addToSql("FROM season sea, series ser, videodata vid, artwork a ");
        sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
        sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
        sqlScalars.addToSql("WHERE sea.series_id=ser.id AND vid.season_id=sea.id AND a.videodata_id=vid.id ");

        if (options.getSeriesid() > 0L) {
            sqlScalars.addToSql("AND ser.id=:seriesid");
            sqlScalars.addParameter("seriesid", options.getSeriesid());
            if (options.getSeason() > 0L) {
                sqlScalars.addToSql("AND sea.season=:season");
                sqlScalars.addParameter(SEASON, options.getSeason());
            }
        }

        if (options.getSeasonid() > 0L) {
            sqlScalars.addToSql("AND sea.id=:seasonid");
            sqlScalars.addParameter("seasonid", options.getSeasonid());
        }

        if (options.getWatched() != null) {
            sqlScalars.addToSql("AND vid.watched=:watched");
            sqlScalars.addParameter("watched", options.getWatched().booleanValue());
        }

        sqlScalars.addToSql("ORDER BY seriesId, season, episode");
        LOG.debug("getEpisodeList SQL: {}", sqlScalars.getSql());

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON, LongType.INSTANCE);
        sqlScalars.addScalar(EPISODE, LongType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_FILENAME, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_DIR, StringType.INSTANCE);
        sqlScalars.addScalar("firstAired", DateType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);

        return executeQueryWithTransform(ApiEpisodeDTO.class, sqlScalars, wrapper);
    }

    public ApiVideoDTO getSingleVideo(ApiWrapperSingle<ApiVideoDTO> wrapper, OptionsIndexVideo options) {
        IndexParams params = new IndexParams(options);
        MetaDataType type = MetaDataType.fromString(options.getType());

        String sql;
        if (type == MetaDataType.MOVIE) {
            sql = generateSqlForVideo(true, params);
        } else if (type == MetaDataType.SERIES) {
            sql = generateSqlForSeries(params);
        } else if (type == MetaDataType.SEASON) {
            sql = generateSqlForSeason(params);
        } else {
            throw new UnsupportedOperationException("Unable to process type '" + type + "' (Original: '" + options.getType() + "')");
        }
        LOG.trace("SQL for {}-{}: {}", type, params.getId(), sql);

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(VIDEO_TYPE, StringType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(SORT_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(VIDEO_YEAR, IntegerType.INSTANCE);
        sqlScalars.addScalar(RELEASE_DATE, DateType.INSTANCE);
        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON, LongType.INSTANCE);
        sqlScalars.addScalar(EPISODE, LongType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);
        
        // add Scalars for additional data item columns
        DataItemTools.addDataItemScalars(sqlScalars, params.getDataItems());
        // add additional parameters
        params.addScalarParameters(sqlScalars);

        List<ApiVideoDTO> queryResults = executeQueryWithTransform(ApiVideoDTO.class, sqlScalars, wrapper);
        LOG.trace("Found {} results for ID {}", queryResults.size(), params.getId());
        
        if (queryResults.isEmpty()) {
            // nothing found
            return null;
        }
        
        // get single video
        ApiVideoDTO video = queryResults.get(0);

        if (options.hasDataItem(DataItem.ARTWORK)) {
            LOG.trace("Adding artwork for ID {}", options.getId());
            Map<Long, List<ApiArtworkDTO>> artworkList;
            if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
                artworkList = getArtworkForMetadata(type, options.getId(), options.getArtworkTypes(), options.getArtworksortdir());
            } else {
                artworkList = getArtworkForMetadata(type, options.getId(), options.getArtworksortdir());
            }

            if (artworkList.containsKey(options.getId())) {
                video.setArtwork(artworkList.get(options.getId()));
            }
        }
        
        return video;
    }

    @Cacheable(value=API_VIDEOSOURCE, key="{#type, #id}")
    public String getVideoSourceForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT mf.video_source FROM mediafile_videodata mv ");
        sql.append("JOIN mediafile mf on mf.id=mv.mediafile_id and mf.extra=0 and mf.video_source is not null ");

        if (type == MetaDataType.SERIES) {
            sql.append("JOIN videodata vd on vd.id=mv.videodata_id JOIN season sea on sea.id=vd.season_id ");
            sql.append("WHERE sea.series_id=:id");
        } else if (type == MetaDataType.SEASON) {
            sql.append("JOIN videodata vd on vd.id=mv.videodata_id WHERE vd.season_id=:id");
        } else {
            sql.append("WHERE mv.videodata_id=:id");
        }
        
        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addParameter(ID, id);
        sqlScalars.addScalar("video_source", StringType.INSTANCE);
        List<String> videoSources = executeQueryWithTransform(String.class, sqlScalars);
        
        if (videoSources.size() == 1) {
            return videoSources.get(0);
        } else if (videoSources.size() > 1) {
            return MULTIPLE;
        }
        return null;
    }
        
    /**
     * Get a list of the files associated with a metadata object.
     *
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    public List<ApiFileDTO> getFilesForMetadata(MetaDataType type, Long id) {
        // Build the SQL statement
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT mf.id as id, mf.extra as extra, mf.part as part, mf.part_title as partTitle, mf.movie_version as version, ");
        sql.append("mf.container as container, mf.codec as codec, mf.codec_format as codecFormat, mf.codec_profile as codecProfile, ");
        sql.append("mf.bitrate as bitrate, mf.overall_bitrate as overallBitrate, mf.fps as fps, ");
        sql.append("mf.width as width, mf.height as height, mf.aspect_ratio as aspectRatio, mf.runtime as runtime, mf.video_source as videoSource, ");
        sql.append("sf.id as fileId, sf.full_path as fileName, sf.file_date as fileDate, sf.file_size as fileSize, ");

        if (type == MetaDataType.MOVIE) {
            sql.append("null as season, null as episode ");
            sql.append("FROM mediafile_videodata mv, mediafile mf, stage_file sf ");
            sql.append("WHERE mv.videodata_id=:id ");
        } else if (type == MetaDataType.SERIES) {
            sql.append("sea.season, vd.episode ");
            sql.append("FROM mediafile_videodata mv, mediafile mf, stage_file sf, season sea, videodata vd ");
            sql.append("WHERE sea.series_id=:id and vd.season_id=sea.id and mv.videodata_id=vd.id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("sea.season, vd.episode ");
            sql.append("FROM mediafile_videodata mv, mediafile mf, stage_file sf, season sea, videodata vd ");
            sql.append("WHERE sea.id=:id and vd.season_id=sea.id and mv.videodata_id=vd.id ");
        } else if (type == MetaDataType.EPISODE) {
            sql.append("sea.season, vd.episode ");
            sql.append("FROM mediafile_videodata mv, mediafile mf, stage_file sf, season sea, videodata vd ");
            sql.append("WHERE vd.id=:id and vd.season_id=sea.id and mv.videodata_id=vd.id ");
        }

        sql.append("and mv.mediafile_id=mf.id and sf.mediafile_id=mf.id ");
        sql.append("and sf.file_type='");
        sql.append(FileType.VIDEO.name());
        sql.append("' and sf.status");
        sql.append(SQL_IGNORE_STATUS_SET);

        if (type == MetaDataType.SERIES || type == MetaDataType.SEASON) {
            sql.append("ORDER BY sea.season ASC, vd.episode ASC");
        }

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(EXTRA, BooleanType.INSTANCE);
        sqlScalars.addScalar("part", IntegerType.INSTANCE);
        sqlScalars.addScalar("partTitle", StringType.INSTANCE);
        sqlScalars.addScalar("version", StringType.INSTANCE);
        sqlScalars.addScalar("container", StringType.INSTANCE);
        sqlScalars.addScalar("codec", StringType.INSTANCE);
        sqlScalars.addScalar("codecFormat", StringType.INSTANCE);
        sqlScalars.addScalar("codecProfile", StringType.INSTANCE);
        sqlScalars.addScalar("bitrate", IntegerType.INSTANCE);
        sqlScalars.addScalar("overallBitrate", IntegerType.INSTANCE);
        sqlScalars.addScalar("fps", FloatType.INSTANCE);
        sqlScalars.addScalar("width", IntegerType.INSTANCE);
        sqlScalars.addScalar("height", IntegerType.INSTANCE);
        sqlScalars.addScalar("aspectRatio", StringType.INSTANCE);
        sqlScalars.addScalar("runtime", IntegerType.INSTANCE);
        sqlScalars.addScalar("videoSource", StringType.INSTANCE);
        sqlScalars.addScalar("fileId", LongType.INSTANCE);
        sqlScalars.addScalar("fileName", StringType.INSTANCE);
        sqlScalars.addScalar("fileDate", TimestampType.INSTANCE);
        sqlScalars.addScalar("fileSize", LongType.INSTANCE);
        sqlScalars.addScalar(SEASON, LongType.INSTANCE);
        sqlScalars.addScalar(EPISODE, LongType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        List<ApiFileDTO> results = executeQueryWithTransform(ApiFileDTO.class, sqlScalars);
        for (ApiFileDTO file : results) {
            file.setAudioCodecs(this.getAudioCodecs(file.getId()));
            file.setSubtitles(this.getSubtitles(file.getId()));
        }
        return results;
    }

    /**
     * Get a list of the trailers for a metadata object.
     *
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    @Cacheable(value=API_TRAILERS, key="{#type, #id}")
    public List<ApiTrailerDTO> getTrailersForMetadata(MetaDataType type, Long id) {
        // build the SQL statement
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.id, t.title, t.url, t.source, t.hash_code as hashCode, t.cache_dir as cacheDir, t.cache_filename as cacheFilename FROM trailer t ");

        if (type == MetaDataType.SERIES) {
            sql.append("WHERE t.series_id=:id ");
        } else {
            sql.append("WHERE t.videodata_id=:id ");
        }
        sql.append("and t.status");
        sql.append(SQL_IGNORE_STATUS_SET);
        sql.append("order by t.id ");
        
        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar("url", StringType.INSTANCE);
        sqlScalars.addScalar(SOURCE, StringType.INSTANCE);
        sqlScalars.addScalar("hashCode", StringType.INSTANCE);
        sqlScalars.addScalar("cacheDir", StringType.INSTANCE);
        sqlScalars.addScalar("cacheFilename", StringType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiTrailerDTO.class, sqlScalars);
    }

    private List<ApiAudioCodecDTO> getAudioCodecs(long mediaFileId) {
        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT ac.codec, ac.codec_format as codecFormat, ac.bitrate, ac.channels, ac.language_code as languageCode ");
        sbSQL.append("FROM audio_codec ac WHERE ac.mediafile_id=:id ORDER BY ac.counter ASC");

        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        sqlScalars.addScalar("codec", StringType.INSTANCE);
        sqlScalars.addScalar("codecFormat", StringType.INSTANCE);
        sqlScalars.addScalar("bitrate", IntegerType.INSTANCE);
        sqlScalars.addScalar("channels", IntegerType.INSTANCE);
        sqlScalars.addScalar("languageCode", StringType.INSTANCE);
        sqlScalars.addParameter(ID, mediaFileId);

        return executeQueryWithTransform(ApiAudioCodecDTO.class, sqlScalars);
    }

    private List<ApiSubtitleDTO> getSubtitles(long mediaFileId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT st.counter, st.format, st.language_code as languageCode, st.default_flag AS defaultFlag,");
        sql.append("st.forced_flag AS forcedFlag, sf.full_path as filePath ");
        sql.append("FROM subtitle st ");
        sql.append("LEFT OUTER JOIN stage_file sf ON sf.id=st.stagefile_id AND sf.status" + SQL_IGNORE_STATUS_SET);
        sql.append("WHERE st.mediafile_id=:id ");
        sql.append("ORDER BY sf.full_path DESC, st.counter ASC");

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar("format", StringType.INSTANCE);
        sqlScalars.addScalar("languageCode", StringType.INSTANCE);
        sqlScalars.addScalar("defaultFlag", BooleanType.INSTANCE);
        sqlScalars.addScalar("forcedFlag", BooleanType.INSTANCE);
        sqlScalars.addScalar("filePath", StringType.INSTANCE);
        sqlScalars.addParameter(ID, mediaFileId);

        return executeQueryWithTransform(ApiSubtitleDTO.class, sqlScalars);
    }

    /**
     * Get a list of the genres for a metadata object.
     *
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    @Cacheable(value=API_GENRES, key="{#type, #id}")
    public List<ApiGenreDTO> getGenresForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ");
        sql.append("CASE ");
        sql.append(" WHEN target_api is not null THEN target_api ");
        sql.append(" WHEN target_xml is not null THEN target_xml ");
        sql.append(" ELSE name ");
        sql.append("END as name ");
        if (type == MetaDataType.SERIES) {
            sql.append("FROM series_genres sg, genre g ");
            sql.append("WHERE sg.series_id=:id AND sg.genre_id=g.id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("FROM season sea, series_genres sg, genre g ");
            sql.append("WHERE sea.id=:id ");
            sql.append("AND sg.series_id=sea.series_id ");
            sql.append("AND sg.genre_id=g.id ");
        } else {
            // defaults to movie
            sql.append("FROM videodata_genres vg, genre g ");
            sql.append("WHERE vg.data_id=:id AND vg.genre_id=g.id ");
        }
        sql.append("ORDER BY name");

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(NAME, StringType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiGenreDTO.class, sqlScalars);
    }

    /**
     * Get a list of the studios for a metadata object.
     *
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    @Cacheable(value=API_STUDIOS, key="{#type, #id}")
    public List<Studio> getStudiosForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT s.id, s.name FROM studio s ");
        if (type == MetaDataType.SERIES) {
            sql.append("JOIN series_studios ss ON s.id=ss.studio_id and ss.series_id=:id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("JOIN season sea ON sea.id=:id JOIN series_studios ss ON s.id=ss.studio_id and ss.series_id=sea.series_id ");
        } else {
            // defaults to movie
            sql.append("JOIN videodata_studios vs ON s.id=vs.studio_id and vs.data_id=:id ");
        }
        sql.append("ORDER BY name");

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(NAME, StringType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(Studio.class, sqlScalars);
    }
    
    /**
     * Get a list of the genres for a metadata object.
     *
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    @Cacheable(value=API_COUNTRIES, key="{#type, #id}")
    public List<ApiCountryDTO> getCountriesForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder("SELECT c.id, c.country_code as countryCode ");
        if (type == MetaDataType.SERIES) {
            sql.append("FROM series_countries sc, country c ");
            sql.append("WHERE sc.series_id=:id AND sc.country_id=c.id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("FROM season sea, series_countries sc, country c ");
            sql.append("WHERE sea.id=:id AND sc.series_id=sea.series_id AND sc.country_id=c.id ");
        } else {
            // defaults to movie
            sql.append("FROM videodata_countries vc, country c ");
            sql.append("WHERE vc.data_id=:id AND vc.country_id=c.id ");
        }

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(COUNTRY_CODE, StringType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiCountryDTO.class, sqlScalars);
    }

    /**
     * Get a list of the certifications for a metadata object.
     *
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    @Cacheable(value=API_CERTIFICATIONS, key="{#type, #id}")
    public List<ApiCertificationDTO> getCertificationsForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT c.id, c.country_code as countryCode, c.certificate FROM certification c ");
        if (type == MetaDataType.SERIES) {
            sql.append("JOIN series_certifications sc ON c.id=sc.cert_id and sc.series_id=:id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("JOIN season sea ON sea.id = :id ");
            sql.append("JOIN series_certifications sc ON c.id=sc.cert_id and sc.series_id=sea.series_id ");
        } else {
            // defaults to movie
            sql.append("JOIN videodata_certifications vc ON c.id=vc.cert_id and vc.data_id=:id ");
        }
        sql.append("ORDER BY country_code, certificate");

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(COUNTRY_CODE, StringType.INSTANCE);
        sqlScalars.addScalar("certificate", StringType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiCertificationDTO.class, sqlScalars);
    }

    /**
     * Get a list of the ratings for a metadata object.
     *
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    @Cacheable(value=API_RATINGS, key="{#type, #id}")
    public List<ApiRatingDTO> getRatingsForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder("SELECT r1.rating, r1.sourcedb AS source, 2 AS sorting ");
        if (type == MetaDataType.SERIES) {
            sql.append("FROM series_ratings r1 WHERE r1.series_id=:id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("FROM series_ratings r1, season sea WHERE sea.id=:id AND sea.series_id=r1.series_id ");
        } else {
            // defaults to movie
            sql.append("FROM videodata_ratings r1 WHERE r1.videodata_id=:id ");
        }
        // combined rating
        sql.append(SQL_UNION);
        sql.append("SELECT round(grouped.average) AS rating, 'combined' AS source, 1 AS sorting FROM ");
        sql.append("(SELECT avg(r2.rating) as average ");
        if (type == MetaDataType.SERIES) {
            sql.append("FROM series_ratings r2 WHERE r2.series_id=:id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("FROM series_ratings r2, season sea WHERE sea.id=:id AND sea.series_id=r2.series_id ");
        } else {
            // defaults to movie
            sql.append("FROM videodata_ratings r2 WHERE r2.videodata_id=:id ");
        }
        sql.append(") AS grouped ");
        sql.append("WHERE grouped.average is not null ORDER BY sorting, source");

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(SOURCE, StringType.INSTANCE);
        sqlScalars.addScalar("rating", IntegerType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiRatingDTO.class, sqlScalars);
    }

    /**
     * Get a list of the awards for a metadata object.
     *
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    @Cacheable(value=API_AWARDS, key="{#type, #id}")
    public List<ApiAwardDTO> getAwardsForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT a.event, a.category, a.sourcedb as source, c.year, c.won, c.nominated ");
        if (type == MetaDataType.SERIES) {
            sql.append("FROM series_awards c JOIN award a ON c.award_id=a.id WHERE c.series_id=:id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("FROM series_awards c JOIN season sea ON c.series_id=sea.series_id JOIN award a ON c.award_id=a.id WHERE sea.id=:id ");
        } else {
            sql.append("FROM videodata_awards c JOIN award a ON c.award_id=a.id WHERE c.videodata_id=:id ");
        }
        sql.append("ORDER BY year, event");

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar("event", StringType.INSTANCE);
        sqlScalars.addScalar("category", StringType.INSTANCE);
        sqlScalars.addScalar(SOURCE, StringType.INSTANCE);
        sqlScalars.addScalar("year", IntegerType.INSTANCE);
        sqlScalars.addScalar("won", BooleanType.INSTANCE);
        sqlScalars.addScalar("nominated", BooleanType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiAwardDTO.class, sqlScalars);
    }

    /**
     * Get list of external IDs for a metadata object.
     * 
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    @Cacheable(value=API_EXTERNAL_IDS, key="{#type, #id}")
    public List<ApiExternalIdDTO> getExternalIdsForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder();

        if (type == MetaDataType.SERIES) {
            sql.append("SELECT ids.series_id AS id, ids.sourcedb_id AS externalId, ids.sourcedb AS sourcedb,");
            sql.append("concat(coalesce(ser.skip_scan_api,''),';',coalesce(ser.skip_scan_nfo,'')) like concat('%',ids.sourcedb,'%') as skipped ");
            sql.append("FROM series ser, series_ids ids WHERE ser.id=:id AND ids.series_id=ser.id");
        } else if (type == MetaDataType.SEASON) {
            sql.append("SELECT ids.season_id AS id, ids.sourcedb_id AS externalId, ids.sourcedb AS sourcedb, 0 as skipped ");
            sql.append("FROM season_ids ids WHERE ids.season_id=:id");
        } else if (type == MetaDataType.PERSON) {
            sql.append("SELECT ids.person_id AS id, ids.sourcedb_id AS externalId, ids.sourcedb AS sourcedb,");
            sql.append("coalesce(p.skip_scan_api,'') like concat('%',ids.sourcedb,'%') as skipped");
            sql.append("FROM person p, person_ids ids WHERE p.id=:id AND ids.person_id=p.id AND p.status"+SQL_IGNORE_STATUS_SET);
        } else {
            sql.append("SELECT ids.videodata_id AS id, ids.sourcedb_id AS externalId, ids.sourcedb AS sourcedb,");
            sql.append("concat(coalesce(vd.skip_scan_api,''),';',coalesce(vd.skip_scan_nfo,'')) like concat('%',ids.sourcedb,'%') as skipped ");
            sql.append("FROM videodata vd, videodata_ids ids WHERE vd.id=:id AND ids.videodata_id=vd.id");
        }

        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar("externalId", StringType.INSTANCE);
        sqlScalars.addScalar("sourcedb", StringType.INSTANCE);
        sqlScalars.addScalar("skipped", BooleanType.INSTANCE);
        sqlScalars.addParameter(ID, id);
        
        return executeQueryWithTransform(ApiExternalIdDTO.class, sqlScalars);
    }

    /**
     * Get a list of the cast for a metadata object.
     * 
     * @param type the metadata type
     * @param id the id of the metadata object
     * @return
     */
    public List<ApiPersonDTO> getCastForMetadata(MetaDataType type, Long id, List<DataItem> dataItems, Set<String> jobs) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT DISTINCT p.id,");
        if (dataItems.contains(DataItem.BIOGRAPHY)) {
            sqlScalars.addToSql("p.biography,");
            sqlScalars.addScalar("biography", StringType.INSTANCE);
        }
        sqlScalars.addToSql("p.name,p.first_name AS firstName,p.last_name AS lastName,");
        sqlScalars.addToSql("p.birth_day AS birthDay,p.birth_place AS birthPlace,p.birth_name AS birthName,");
        sqlScalars.addToSql("p.death_day AS deathDay,p.death_place AS deathPlace,");
        sqlScalars.addToSql("c.role as role,c.voice_role as voiceRole,c.job as job ");
        sqlScalars.addToSql("FROM person p ");

        if (type == MetaDataType.SERIES) {
            sqlScalars.addToSql("JOIN cast_crew c ON p.id=c.person_id");
            sqlScalars.addToSql("JOIN season sea ON sea.series_id=:id");
            sqlScalars.addToSql("JOIN videodata vd ON vd.id=c.videodata_id and vd.season_id=sea.id");
        } else if (type == MetaDataType.SEASON) {
            sqlScalars.addToSql("JOIN cast_crew c ON p.id=c.person_id ");
            sqlScalars.addToSql("JOIN videodata vd ON vd.id=c.videodata_id and vd.season_id=:id");
        } else {
            // defaults to movie/episode
            sqlScalars.addToSql("JOIN cast_crew c ON p.id=c.person_id and c.videodata_id=:id");
        }

        sqlScalars.addToSql("WHERE p.status" + SQL_IGNORE_STATUS_SET);
        
        if (jobs != null) {
            sqlScalars.addToSql("AND c.job in (:jobs)");
            sqlScalars.addParameter("jobs", jobs);
        }
        
        sqlScalars.addToSql("ORDER BY c.ordering");

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(NAME, StringType.INSTANCE);
        sqlScalars.addScalar("firstName", StringType.INSTANCE);
        sqlScalars.addScalar("lastName", StringType.INSTANCE);
        sqlScalars.addScalar("birthDay", DateType.INSTANCE);
        sqlScalars.addScalar("birthPlace", StringType.INSTANCE);
        sqlScalars.addScalar("birthName", StringType.INSTANCE);
        sqlScalars.addScalar("deathDay", DateType.INSTANCE);
        sqlScalars.addScalar("deathPlace", StringType.INSTANCE);
        sqlScalars.addScalar("role", StringType.INSTANCE);
        sqlScalars.addScalar("voiceRole", BooleanType.INSTANCE);
        sqlScalars.addScalar(JOB, StringType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiPersonDTO.class, sqlScalars);
    }

    /**
     * Get a list of all artwork available for a video ID
     *
     * @param type
     * @param id
     * @return
     */
    private Map<Long, List<ApiArtworkDTO>> getArtworkForMetadata(MetaDataType type, Long id, String artworkSortDir) {
        Set<String> artworkRequired = new HashSet<>();
        for (ArtworkType at : ArtworkType.values()) {
            artworkRequired.add(at.toString());
        }
        // remove the unknown type
        artworkRequired.remove(ArtworkType.UNKNOWN.toString());

        return getArtworkForMetadata(type, id, artworkRequired, artworkSortDir);
    }

    /**
     * Get a select list of artwork available for a video ID
     *
     * @param type
     * @param id
     * @param artworkRequired
     * @param artworkSortDir
     * @return
     */
    private Map<Long, List<ApiArtworkDTO>> getArtworkForMetadata(MetaDataType type, Object id, Set<String> artworkRequired, String artworkSortDir) {
        LOG.trace("Artwork required for {} ID {} is {}", type, id, artworkRequired);

        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT '").append(type.toString()).append("' AS source,");
        sbSQL.append(" v.id AS id, a.id AS artworkId, al.id AS locatedId, ag.id AS generatedId,");
        sbSQL.append(" a.artwork_type AS artworkType, ag.cache_dir AS cacheDir, ag.cache_filename AS cacheFilename ");
        if (type == MetaDataType.MOVIE) {
            sbSQL.append("FROM videodata v ");
        } else if (type == MetaDataType.SERIES) {
            sbSQL.append("FROM series v ");
        } else if (type == MetaDataType.SEASON) {
            sbSQL.append("FROM season v ");
        } else if (type == MetaDataType.PERSON) {
            sbSQL.append("FROM person v");
        } else if (type == MetaDataType.BOXSET) {
            sbSQL.append("FROM boxed_set v");
        }
        sbSQL.append(", artwork a");    // artwork must be last for the LEFT JOIN
        sbSQL.append(SQL_LEFT_JOIN_ARTWORK_LOCATED);
        sbSQL.append(SQL_LEFT_JOIN_ARTWORK_GENERATED);
        if (type == MetaDataType.MOVIE) {
            sbSQL.append(" WHERE v.id=a.videodata_id");
            sbSQL.append(" AND v.episode<0");
        } else if (type == MetaDataType.SERIES) {
            sbSQL.append(" WHERE v.id=a.series_id");
        } else if (type == MetaDataType.SEASON) {
            sbSQL.append(" WHERE v.id=a.season_id");
        } else if (type == MetaDataType.PERSON) {
            sbSQL.append(" WHERE v.id=a.person_id");
        } else if (type == MetaDataType.BOXSET) {
            sbSQL.append(" WHERE v.id=a.boxedset_id");
        }
        sbSQL.append(" AND al.id is not null");
        sbSQL.append(" AND v.id IN (:id)");
        sbSQL.append(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
        if ("DESC".equalsIgnoreCase(artworkSortDir)) {
            sbSQL.append(" ORDER BY al.create_timestamp DESC");
        } else {
            sbSQL.append(" ORDER BY al.create_timestamp ASC");
        }
        
        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        LOG.trace("Artwork SQL: {}", sqlScalars.getSql());

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(SOURCE, StringType.INSTANCE);
        sqlScalars.addScalar(ARTWORK_ID, LongType.INSTANCE);
        sqlScalars.addScalar(LOCATED_ID, LongType.INSTANCE);
        sqlScalars.addScalar(GENERATED_ID, LongType.INSTANCE);
        sqlScalars.addScalar(ARTWORK_TYPE, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_DIR, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_FILENAME, StringType.INSTANCE);

        sqlScalars.addParameter(ID, id);
        sqlScalars.addParameter("artworklist", artworkRequired);

        List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars);
        return generateIdMapList(results);
    }

    public List<ApiSeriesInfoDTO> getSeriesInfo(ApiWrapperList<ApiSeriesInfoDTO> wrapper, OptionsIdArtwork options) {
        final Long id = options.getId();

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT s.id AS seriesId, s.title, s.title_original AS originalTitle, s.start_year AS year,");
        if (options.hasDataItem(DataItem.PLOT)) {
            sqlScalars.addToSql("s.plot,");
            sqlScalars.addScalar("plot", StringType.INSTANCE);
        }
        if (options.hasDataItem(DataItem.OUTLINE)) {
            sqlScalars.addToSql("s.outline,");
            sqlScalars.addScalar("outline", StringType.INSTANCE);
        }
        sqlScalars.addToSql("(SELECT min(vid.watched) from videodata vid,season sea where vid.season_id=sea.id and sea.series_id=s.id) as watched");
        sqlScalars.addToSql("FROM series s WHERE id=:id ORDER BY id");

        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(YEAR, IntegerType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        List<ApiSeriesInfoDTO> seriesResults = executeQueryWithTransform(ApiSeriesInfoDTO.class, sqlScalars, wrapper);
        LOG.debug("Found {} series for ID {}", seriesResults.size(), id);

        for (ApiSeriesInfoDTO series : seriesResults) {

            if (options.hasDataItem(DataItem.ARTWORK)) {
                Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForMetadata(MetaDataType.SERIES, id, options.getArtworkTypes(), options.getArtworksortdir());
                if (artworkList == null || !artworkList.containsKey(id) || CollectionUtils.isEmpty(artworkList.get(id))) {
                    LOG.debug("No artwork found for seriesId {}", id);
                } else {
                    for (ApiArtworkDTO artwork : artworkList.get(id)) {
                        series.addArtwork(artwork);
                    }
                }
            }
            series.setSeasonList(getSeasonInfo(options));
        }
        
        return seriesResults;
    }

    private List<ApiSeasonInfoDTO> getSeasonInfo(OptionsIdArtwork options) {
        LOG.debug("Getting season information for series ID {}", options.getId());
        
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT s.series_id AS seriesId, s.id AS seasonId, s.season, s.title, s.title_original AS originalTitle,");
        if (options.hasDataItem(DataItem.PLOT)) {
            sqlScalars.addToSql("s.plot,");
            sqlScalars.addScalar("plot", StringType.INSTANCE);
        }
        if (options.hasDataItem(DataItem.OUTLINE)) {
            sqlScalars.addToSql("s.outline,");
            sqlScalars.addScalar("outline", StringType.INSTANCE);
        }
        sqlScalars.addToSql("(SELECT min(vid.watched) from videodata vid where vid.season_id=s.id) as watched ");
        sqlScalars.addToSql("FROM season s WHERE series_id=:id ORDER BY series_id, season");
        sqlScalars.addParameter(ID, options.getId());

        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON, IntegerType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);

        List<ApiSeasonInfoDTO> seasonResults = executeQueryWithTransform(ApiSeasonInfoDTO.class, sqlScalars);
        LOG.debug("Found {} seasons for series ID {}", seasonResults.size(), options.getId());

        if (options.hasDataItem(DataItem.ARTWORK)) {
            for (ApiSeasonInfoDTO season : seasonResults) {
                Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForMetadata(MetaDataType.SEASON, season.getSeasonId(), options.getArtworkTypes(), options.getArtworksortdir());
                if (artworkList == null || !artworkList.containsKey(season.getSeasonId()) || CollectionUtils.isEmpty(artworkList.get(season.getSeasonId()))) {
                    LOG.debug("No artwork found for series ID {} and season {}", options.getId(), season.getSeason());
                } else {
                    for (ApiArtworkDTO artwork : artworkList.get(season.getSeasonId())) {
                        season.addArtwork(artwork);
                    }
                }
            }
        }

        return seasonResults;
    }

    //<editor-fold defaultstate="collapsed" desc="Statistics">
    /**
     * Get a single Count and Timestamp
     *
     * @param type
     * @param tablename
     * @param clause
     * @return
     */
    public CountTimestamp getCountTimestamp(MetaDataType type, String tablename, String clause) {
        if (StringUtils.isBlank(tablename)) {
            return null;
        }

        StringBuilder sql = new StringBuilder("SELECT '").append(type).append("' as type, ");
        sql.append("count(*) as counter, ");
        sql.append("MAX(create_timestamp) as createTimestamp, MAX(update_timestamp) as updateTimestamp, MAX(id) as lastId ");
        sql.append("FROM ").append(tablename);
        if (StringUtils.isNotBlank(clause)) {
            sql.append(" WHERE ").append(clause);
        }

        SqlScalars sqlScalars = new SqlScalars(sql);

        sqlScalars.addScalar(TYPE, StringType.INSTANCE);
        sqlScalars.addScalar("counter", LongType.INSTANCE);
        sqlScalars.addScalar(CREATE_TIMESTAMP, TimestampType.INSTANCE);
        sqlScalars.addScalar(UPDATE_TIMESTAMP, TimestampType.INSTANCE);
        sqlScalars.addScalar("lastId", LongType.INSTANCE);

        List<CountTimestamp> results = executeQueryWithTransform(CountTimestamp.class, sqlScalars);
        if (CollectionUtils.isEmpty(results)) {
            return new CountTimestamp(type);
        }

        return results.get(0);
    }

    /**
     * Get a count of the jobs along with a count
     *
     * @param requiredJobs
     * @return
     */
    public List<CountGeneric> getJobCount(List<String> requiredJobs) {
        LOG.info("getJobCount: Required Jobs: {}", (requiredJobs == null) ? "all" : requiredJobs);
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT job AS item, COUNT(*) AS counter");
        sqlScalars.addToSql("FROM cast_crew");
        if (CollectionUtils.isNotEmpty(requiredJobs)) {
            sqlScalars.addToSql("WHERE job IN (:joblist)");
            sqlScalars.addParameter("joblist", requiredJobs);
        }
        sqlScalars.addToSql("GROUP BY job");

        sqlScalars.addScalar("item", StringType.INSTANCE);
        sqlScalars.addScalar("counter", LongType.INSTANCE);

        return executeQueryWithTransform(CountGeneric.class, sqlScalars);
    }

    public void statSeriesCount() {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT s.id AS seriesId, title, start_year AS year FROM series s");

        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(YEAR, IntegerType.INSTANCE);

        // Get the results
        List<ApiSeriesInfoDTO> seriesResults = executeQueryWithTransform(ApiSeriesInfoDTO.class, sqlScalars);
        if (!seriesResults.isEmpty()) {
            // Set the default oldest and newest
            ApiSeriesInfoDTO oldest = seriesResults.get(0);
            ApiSeriesInfoDTO newest = seriesResults.get(0);

            for (ApiSeriesInfoDTO series : seriesResults) {
                if (series.getYear() > newest.getYear()) {
                    newest = series;
                }
                if (series.getYear() < oldest.getYear()) {
                    oldest = series;
                }
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Utility Functions">
    /**
     * Takes a list and generates a map of the ID and item
     *
     * @param idList
     * @return
     */
    @SuppressWarnings("unused")
    private static <T extends AbstractApiIdentifiableDTO> Map<Long, T> generateIdMap(List<T> idList) {
        Map<Long, T> results = new HashMap<>(idList.size());

        for (T idSingle : idList) {
            results.put(idSingle.getId(), idSingle);
        }

        return results;
    }

    /**
     * Take a list and generate a map of the ID and a list of the items for that ID
     *
     * @param <T> source type
     * @param idList List of the source type
     * @return
     */
    private static <T extends AbstractApiIdentifiableDTO> Map<Long, List<T>> generateIdMapList(List<T> idList) {
        Map<Long, List<T>> results = new HashMap<>();

        for (T idSingle : idList) {
            Long sourceId = idSingle.getId();
            if (results.containsKey(sourceId)) {
                results.get(sourceId).add(idSingle);
            } else {
                // ID didn't exist so add a new list
                List<T> list = new ArrayList<>(1);
                list.add(idSingle);
                results.put(sourceId, list);
            }
        }

        return results;
    }

    /**
     * Generate a list of the IDs from a list
     *
     * @param idList
     * @return
     */
    private static <T extends AbstractApiIdentifiableDTO> List<Long> generateIdList(List<T> idList) {
        List<Long> results = new ArrayList<>(idList.size());

        for (T idSingle : idList) {
            results.add(idSingle.getId());
        }

        return results;
    }
    
    private static void addExistsOrNot(boolean include, StringBuilder sb) {
        if (include) {
            sb.append(" AND exists (");
        } else {
            sb.append(" AND not exists (");
        }
    }
    
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="BoxSet methods">
    @Cacheable(value=API_BOXEDSETS, key="{#type, #id}")
    public List<ApiBoxedSetDTO> getBoxedSetsForMetadata(MetaDataType type, Long id) {
        StringBuilder sql = new StringBuilder("SELECT bs.id, bs.name,(select count(bo2.id) from boxed_set_order bo2 where bo2.boxedset_id=bs.id) as memberCount ");
        sql.append("FROM boxed_set bs JOIN boxed_set_order bo ON bs.id=bo.boxedset_id ");
        if (type == MetaDataType.SERIES) {
            sql.append("WHERE bo.series_id=:id ");
        } else if (type == MetaDataType.SEASON) {
            sql.append("JOIN season sea ON sea.series_id=bo.series_id AND sea.id=:id ");
        } else if (type == MetaDataType.EPISODE) {
            sql.append("JOIN season sea ON sea.series_id=bo.series_id JOIN videodata vd ON vd.season_id=sea.id AND vd.id=:id ");
        } else {
            // defaults to movie
            sql.append("WHERE bo.videodata_id=:id ");
        }
        sql.append("GROUP BY bs.id, bs.name");
        
        SqlScalars sqlScalars = new SqlScalars(sql);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(NAME, StringType.INSTANCE);
        sqlScalars.addScalar("memberCount", IntegerType.INSTANCE);
        sqlScalars.addParameter(ID, id);

        return executeQueryWithTransform(ApiBoxedSetDTO.class, sqlScalars);
    }

    public List<ApiBoxedSetDTO> getBoxedSets(ApiWrapperList<ApiBoxedSetDTO> wrapper) {
        OptionsBoxedSet options = (OptionsBoxedSet) wrapper.getOptions();
        SqlScalars sqlScalars = generateSqlForBoxedSet(options);

        List<ApiBoxedSetDTO> boxedSets = executeQueryWithTransform(ApiBoxedSetDTO.class, sqlScalars, wrapper);
        
        if (!boxedSets.isEmpty() && options.hasDataItem(DataItem.ARTWORK)) {
            for (ApiBoxedSetDTO boxedSet : boxedSets) {
                Map<Long, List<ApiArtworkDTO>> artworkList;
                if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
                    artworkList = getArtworkForMetadata(MetaDataType.BOXSET, boxedSet.getId(), options.getArtworkTypes(), options.getArtworksortdir());
                } else {
                    artworkList = getArtworkForMetadata(MetaDataType.BOXSET, boxedSet.getId(), options.getArtworksortdir());
                }
                boxedSet.addArtwork(artworkList.get(boxedSet.getId()));
            }
        }
        
        return boxedSets;
    }

    public ApiBoxedSetDTO getBoxedSet(ApiWrapperSingle<ApiBoxedSetDTO> wrapper) {
        OptionsBoxedSet options = (OptionsBoxedSet) wrapper.getOptions();
        SqlScalars sqlScalars = generateSqlForBoxedSet(options);

        List<ApiBoxedSetDTO> boxsets = executeQueryWithTransform(ApiBoxedSetDTO.class, sqlScalars, wrapper);
        if (CollectionUtils.isEmpty(boxsets)) {
            return null;
        }
        
        // get the first boxed set which has been retrieved by the given id
        ApiBoxedSetDTO boxedSet = boxsets.get(0);
        
        if (options.hasDataItem(DataItem.MEMBER)) {
            // get members
            sqlScalars = new SqlScalars();
            sqlScalars.addToSql("SELECT vd.id");
            sqlScalars.addToSql(SQL_COMMA_SPACE_QUOTE + MetaDataType.MOVIE + SQL_AS_VIDEO_TYPE);
            sqlScalars.addToSql(", bo1.ordering, vd.title, vd.title_original AS originalTitle, vd.publication_year AS year,vd.release_date AS releaseDate,vd.watched");
            sqlScalars.addToSql(DataItemTools.addSqlDataItems(options.splitDataItems(), "vd").toString());
            sqlScalars.addToSql("FROM boxed_set_order bo1 JOIN videodata vd ON bo1.videodata_id=vd.id");
            sqlScalars.addToSql("WHERE bo1.boxedset_id=" + options.getId());
            sqlScalars.addToSql(SQL_UNION);
            sqlScalars.addToSql("SELECT ser.id");
            sqlScalars.addToSql(SQL_COMMA_SPACE_QUOTE + MetaDataType.SERIES + SQL_AS_VIDEO_TYPE);
            sqlScalars.addToSql(", bo2.ordering, ser.title, ser.title_original AS originalTitle, ser.start_year AS year,null as releaseDate,");
            sqlScalars.addToSql("(SELECT min(vid.watched) from videodata vid,season sea where vid.season_id=sea.id and sea.series_id=ser.id) as watched");
            sqlScalars.addToSql(DataItemTools.addSqlDataItems(options.splitDataItems(), "ser").toString());
            sqlScalars.addToSql("FROM boxed_set_order bo2 JOIN series ser ON bo2.series_id=ser.id");
            sqlScalars.addToSql("WHERE bo2.boxedset_id=" + options.getId());
            sqlScalars.addToSql(options.getSortString());

            sqlScalars.addScalar(ID, LongType.INSTANCE);
            sqlScalars.addScalar(VIDEO_TYPE, StringType.INSTANCE);
            sqlScalars.addScalar("ordering", IntegerType.INSTANCE);
            sqlScalars.addScalar(TITLE, StringType.INSTANCE);
            sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
            sqlScalars.addScalar(YEAR, IntegerType.INSTANCE);
            sqlScalars.addScalar(RELEASE_DATE, DateType.INSTANCE);
            sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);
            DataItemTools.addDataItemScalars(sqlScalars, options.splitDataItems());

            List<ApiBoxedSetMemberDTO> members = this.executeQueryWithTransform(ApiBoxedSetMemberDTO.class, sqlScalars);
            boxedSet.setMembers(members);

            if (options.hasDataItem(DataItem.ARTWORK)) {
                for (ApiBoxedSetMemberDTO member : members) {
                    Map<Long, List<ApiArtworkDTO>> artworkList;
                    if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
                        artworkList = getArtworkForMetadata(member.getVideoType(), member.getId(), options.getArtworkTypes(), options.getArtworksortdir());
                    } else {
                        artworkList = getArtworkForMetadata(member.getVideoType(), member.getId(), options.getArtworksortdir());
                    }
                    member.addArtwork(artworkList.get(member.getId()));
                }
            }
        }

        if (options.hasDataItem(DataItem.ARTWORK)) {
            LOG.trace("Adding artwork for ID {}", options.getId());
            Map<Long, List<ApiArtworkDTO>> artworkList;
            if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
                artworkList = getArtworkForMetadata(MetaDataType.BOXSET, options.getId(), options.getArtworkTypes(), options.getArtworksortdir());
            } else {
                artworkList = getArtworkForMetadata(MetaDataType.BOXSET, options.getId(), options.getArtworksortdir());
            }
            boxedSet.addArtwork(artworkList.get(options.getId()));
        }

        return boxedSet;
    }

    private static SqlScalars generateSqlForBoxedSet(OptionsBoxedSet options) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT s.id, s.name, count(s.member) as memberCount, min(s.watched_set) as watched FROM (");
        sqlScalars.addToSql("SELECT bs1.id, bs1.name, vd1.id as member, vd1.watched as watched_set");
        sqlScalars.addToSql("FROM boxed_set bs1");
        sqlScalars.addToSql("LEFT OUTER JOIN boxed_set_order bo1 ON bs1.id=bo1.boxedset_id");
        sqlScalars.addToSql("LEFT OUTER JOIN videodata vd1 ON bo1.videodata_id=vd1.id");
        if (options.getId() > 0L) {
            sqlScalars.addToSql("WHERE bs1.id=" + options.getId());
        }
        sqlScalars.addToSql(SQL_UNION);
        sqlScalars.addToSql("SELECT bs2.id, bs2.name, ser.id as member,");
        sqlScalars.addToSql("(SELECT min(vid.watched) from videodata vid,season sea where vid.season_id=sea.id and sea.series_id=ser.id) as watched_set");
        sqlScalars.addToSql("FROM boxed_set bs2");
        sqlScalars.addToSql("LEFT OUTER JOIN boxed_set_order bo2 ON bs2.id=bo2.boxedset_id");
        sqlScalars.addToSql("LEFT OUTER JOIN series ser ON bo2.series_id=ser.id");
        if (options.getId() > 0L) {
            sqlScalars.addToSql("WHERE bs2.id=" + options.getId());
        }
        sqlScalars.addToSql(") AS s");
        sqlScalars.addToSql("GROUP BY s.id, s.name");
        if (options.getId() <= 0L) {
            if (options.getWatched() != null) {
                if (options.getWatched()) {
                    sqlScalars.addToSql(" HAVING min(s.watched_set)=1");
                } else {
                    sqlScalars.addToSql(" HAVING min(s.watched_set)=0");
                }
            }
            sqlScalars.addToSql(options.getSortString());
        }

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar(NAME, StringType.INSTANCE);
        sqlScalars.addScalar("memberCount", IntegerType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);

        return sqlScalars;
    }
    //</editor-fold>

    public List<ApiNameDTO> getAlphabeticals(ApiWrapperList<ApiNameDTO> wrapper) {
        final OptionsMultiType options = (OptionsMultiType) wrapper.getOptions();
        final Set<MetaDataType> mdt = options.getMetaDataTypes();
        final StringBuilder sbSQL = new StringBuilder();

        // add the movie entries
        if (mdt.contains(MetaDataType.MOVIE)) {
            sbSQL.append("SELECT DISTINCT UPPER(left(vd.title_sort,1)) AS name FROM videodata vd WHERE vd.episode<0 ");
        }

        // add the TV series entries
        if (mdt.contains(MetaDataType.SERIES)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT distinct upper(left(ser.title_sort,1)) as name FROM series ser ");
        }

        // add the TV season entries
        if (mdt.contains(MetaDataType.SEASON)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT distinct upper(left(sea.title_sort,1)) as name FROM season sea ");
        }

        // add the TV episode entries
        if (mdt.contains(MetaDataType.EPISODE)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT distinct upper(left(vd.title_sort,1)) as name FROM videodata vd WHERE vd.episode>-1 ");
        }

        // add the Person entries
        if (mdt.contains(MetaDataType.PERSON)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }

            sbSQL.append("SELECT distinct upper(left(p.last_name,1)) as name FROM person p ");
            sbSQL.append("WHERE p.status"+SQL_IGNORE_STATUS_SET);
            sbSQL.append("AND p.last_name IS NOT NULL AND LEFT(p.last_name,1) NOT IN ('''','\"') ");
        }

        // If there were no types added, then return an empty list
        if (sbSQL.length() == 0) {
            return Collections.emptyList();
        }

        sbSQL.append(options.getSortString(NAME));

        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        sqlScalars.addScalar(NAME, StringType.INSTANCE);

        return executeQueryWithTransform(ApiNameDTO.class, sqlScalars, wrapper);
    }

    public List<ApiYearDecadeDTO> getYears(ApiWrapperList<ApiYearDecadeDTO> wrapper) {
        final OptionsMultiType options = (OptionsMultiType) wrapper.getOptions();
        final Set<MetaDataType> mdt = options.getMetaDataTypes();
        final StringBuilder sbSQL = new StringBuilder();

        // add the movie entries
        if (mdt.contains(MetaDataType.MOVIE)) {
            sbSQL.append("SELECT DISTINCT vd.publication_year AS year FROM videodata vd ");
            sbSQL.append("WHERE vd.episode < 0 AND vd.publication_year > 0" );
        }

        // add the TV series entries
        if (mdt.contains(MetaDataType.SERIES)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT DISTINCT ser.start_year as year FROM series ser WHERE ser.start_year>0 ");
        }

        // add the TV season entries
        if (mdt.contains(MetaDataType.SEASON)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT DISTINCT sea.publication_year AS year FROM season sea WHERE sea.publication_year>0" );
        }

        // add the TV episode entries
        if (mdt.contains(MetaDataType.EPISODE)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT DISTINCT vd.publication_year AS year FROM videodata vd ");
            sbSQL.append("WHERE vd.episode >= 0 AND vd.publication_year > 0" );
        }

        // If there were no types added, then return an empty list
        if (sbSQL.length() == 0) {
            return Collections.emptyList();
        }

        sbSQL.append(options.getSortString("year"));

        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        sqlScalars.addScalar("year", IntegerType.INSTANCE);

        return executeQueryWithTransform(ApiYearDecadeDTO.class, sqlScalars, wrapper);
    }

    public List<ApiYearDecadeDTO> getDecades(ApiWrapperList<ApiYearDecadeDTO> wrapper) {
        final OptionsMultiType options = (OptionsMultiType) wrapper.getOptions();
        final Set<MetaDataType> mdt = options.getMetaDataTypes();
        final StringBuilder sbSQL = new StringBuilder();

        // add the movie entries
        if (mdt.contains(MetaDataType.MOVIE)) {
            sbSQL.append("SELECT DISTINCT CONCAT(LEFT(CAST(vd.publication_year AS CHAR(4)),3),'0') AS decade ");
            sbSQL.append("FROM videodata vd WHERE vd.episode<0 AND vd.publication_year > 1000" );
        }

        // add the TV series entries
        if (mdt.contains(MetaDataType.SERIES)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT DISTINCT CONCAT(LEFT(CAST(ser.start_year AS CHAR(4)),3),'0') as decade ");
            sbSQL.append("FROM series ser WHERE ser.start_year > 1000 ");
        }

        // add the TV season entries
        if (mdt.contains(MetaDataType.SEASON)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT DISTINCT CONCAT(LEFT(CAST(sea.publication_year AS CHAR(4)),3),'0') AS decade ");
            sbSQL.append("FROM season sea WHERE sea.publication_year > 1000" );
        }

        // add the TV episode entries
        if (mdt.contains(MetaDataType.EPISODE)) {
            if (sbSQL.length() > 0) {
                sbSQL.append(SQL_UNION);
            }
            sbSQL.append("SELECT DISTINCT CONCAT(LEFT(CAST(vd.publication_year AS CHAR(4)),3),'0') AS decade ");
            sbSQL.append("FROM videodata vd WHERE vd.episode>-1 AND vd.publication_year > 1000" );
        }

        // If there were no types added, then return an empty list
        if (sbSQL.length() == 0) {
            return Collections.emptyList();
        }

        sbSQL.append(options.getSortString("decade"));

        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        sqlScalars.addScalar("decade", IntegerType.INSTANCE);

        return executeQueryWithTransform(ApiYearDecadeDTO.class, sqlScalars, wrapper);
    }
    
    public void rescanAll() {
        this.executeUpdate(Person.UPDATE_RESCAN_ALL);
        this.executeUpdate(VideoData.UPDATE_RESCAN_ALL);
        this.executeUpdate(Season.UPDATE_RESCAN_ALL);
        this.executeUpdate(Series.UPDATE_RESCAN_ALL);
        this.executeUpdate(Artwork.UPDATE_RESCAN_ALL);
    }
}
