/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.api.model.CountGeneric;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.builder.DataItem;
import org.yamj.core.api.model.builder.DataItemTools;
import org.yamj.core.api.model.builder.SqlScalars;
import org.yamj.core.api.model.dto.*;
import org.yamj.core.api.options.*;
import org.yamj.core.api.wrapper.ApiWrapperList;
import org.yamj.core.api.wrapper.ApiWrapperSingle;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.FileType;
import org.yamj.core.database.model.type.ParticipationType;
import org.yamj.core.hibernate.HibernateDao;

@Repository("apiDao")
public class ApiDao extends HibernateDao {

    private static final Logger LOG = LoggerFactory.getLogger(ApiDao.class);
    private static final String ID = "id";
    private static final String YEAR = "year";
    private static final String GENRE = "genre";
    private static final String TITLE = "title";
    private static final String EPISODE = "episode";
    private static final String SEASON = "season";
    private static final String SEASON_ID = "seasonId";
    private static final String SERIES_ID = "seriesId";
    private static final String SERIES_YEAR = "seriesYear";
    private static final String VIDEO_YEAR = "videoYear";
    private static final String ORIGINAL_TITLE = "originalTitle";
    private static final String CACHE_FILENAME = "cacheFilename";
    private static final String CACHE_DIR = "cacheDir";
    private static final String WATCHED = "watched";
    // SQL
    private static final String SQL_UNION_ALL = " UNION ALL ";
    private static final String SQL_AS_VIDEO_TYPE_STRING = "' AS videoTypeString";
    private static final String SQL_WHERE_1_EQ_1 = " WHERE 1=1";
    private static final String SQL_COMMA_SPACE_QUOTE = ", '";
    private static final String SQL_ARTWORK_TYPE_IN_ARTWORKLIST = " AND a.artwork_type IN (:artworklist) ";
    private static final String SQL_LEFT_JOIN_ARTWORK_GENERATED = " LEFT JOIN artwork_generated ag ON al.id=ag.located_id ";
    private static final String SQL_LEFT_JOIN_ARTWORK_LOCATED = " LEFT JOIN artwork_located al ON a.id=al.artwork_id and al.status not in ('INVALID','NOTFOUND','ERROR','IGNORE') ";

    /**
     * Generate the query and load the results into the wrapper
     *
     * @param wrapper
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getVideoList(ApiWrapperList<ApiVideoDTO> wrapper) {
        SqlScalars sqlScalars = new SqlScalars(generateSqlForVideoList(wrapper));
        OptionsIndexVideo options = (OptionsIndexVideo) wrapper.getOptions();

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar("videoTypeString", StringType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(VIDEO_YEAR, IntegerType.INSTANCE);
        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON, LongType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);
        DataItemTools.addDataItemScalars(sqlScalars, options.splitDataitems());

        List<ApiVideoDTO> queryResults = executeQueryWithTransform(ApiVideoDTO.class, sqlScalars, wrapper);
        wrapper.setResults(queryResults);

        if (CollectionUtils.isNotEmpty(queryResults)) {
            if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
                // Create and populate the ID list
                Map<MetaDataType, List<Long>> ids = new EnumMap<MetaDataType, List<Long>>(MetaDataType.class);
                for (MetaDataType mdt : MetaDataType.values()) {
                    ids.put(mdt, new ArrayList());
                }

                Map<String, ApiVideoDTO> results = new HashMap<String, ApiVideoDTO>();

                for (ApiVideoDTO single : queryResults) {
                    // Add the item to the map for further processing
                    results.put(ApiArtworkDTO.makeKey(single), single);
                    // Add the ID to the list
                    ids.get(single.getVideoType()).add(single.getId());
                }

                boolean foundArtworkIds = Boolean.FALSE;    // Check to see that we have artwork to find
                // Remove any blank entries
                for (MetaDataType mdt : MetaDataType.values()) {
                    if (CollectionUtils.isEmpty(ids.get(mdt))) {
                        ids.remove(mdt);
                    } else {
                        // We've found an artwork, so we can continue
                        foundArtworkIds = Boolean.TRUE;
                    }
                }

                if (foundArtworkIds) {
                    LOG.debug("Found artwork to process, IDs: {}", ids);
                    addArtworks(ids, results, options);
                } else {
                    LOG.debug("No artwork found to process, skipping.");
                }
            } else {
                LOG.debug("Artwork not required, skipping.");
            }
        } else {
            LOG.debug("No results found to process.");
        }
    }

    /**
     * Generate the SQL for the video list
     *
     * Note: In this method MetaDataType.UNKNOWN will return all types
     *
     * @param wrapper
     * @return
     */
    private String generateSqlForVideoList(ApiWrapperList<ApiVideoDTO> wrapper) {
        OptionsIndexVideo options = (OptionsIndexVideo) wrapper.getOptions();
        Map<String, String> includes = options.splitIncludes();
        Map<String, String> excludes = options.splitExcludes();
        List<DataItem> dataItems = options.splitDataitems();

        List<MetaDataType> mdt = options.splitTypes();
        LOG.debug("Getting video list for types: {}", mdt.toString());
        if (CollectionUtils.isNotEmpty(dataItems)) {
            LOG.debug("Additional data items requested: {}", dataItems.toString());
        }

        boolean hasMovie = mdt.contains(MetaDataType.MOVIE);
        boolean hasSeries = mdt.contains(MetaDataType.SERIES);
        boolean hasSeason = mdt.contains(MetaDataType.SEASON);
        boolean hasEpisode = mdt.contains(MetaDataType.EPISODE);

        StringBuilder sbSQL = new StringBuilder();

        // Add the movie entries
        if (hasMovie) {
            sbSQL.append(generateSqlForVideo(true, options, includes, excludes, dataItems));
        }

        if (hasMovie && hasSeries) {
            sbSQL.append(SQL_UNION_ALL);
        }

        // Add the TV series entires
        if (hasSeries) {
            sbSQL.append(generateSqlForSeries(options, includes, excludes, dataItems));
        }

        if ((hasMovie || hasSeries) && hasSeason) {
            sbSQL.append(SQL_UNION_ALL);
        }

        // Add the TV season entires
        if (hasSeason) {
            sbSQL.append(generateSqlForSeason(options, includes, excludes, dataItems));
        }

        if ((hasMovie || hasSeries || hasSeason) && hasEpisode) {
            sbSQL.append(SQL_UNION_ALL);
        }

        // Add the TV episode entries
        if (hasEpisode) {
            sbSQL.append(generateSqlForVideo(false, options, includes, excludes, dataItems));
        }

        // Add the sort string, this will be empty if there is no sort required
        sbSQL.append(options.getSortString());

        LOG.trace("SqlForVideoList: {}", sbSQL);
        return sbSQL.toString();
    }

    /**
     * Create the SQL fragment for the selection of movies
     *
     * @param options
     * @param includes
     * @param excludes
     * @return
     */
    private String generateSqlForVideo(boolean isMovie, OptionsIndexVideo options, Map<String, String> includes, Map<String, String> excludes, List<DataItem> dataItems) {
        StringBuilder sbSQL = new StringBuilder();

        sbSQL.append("SELECT vd.id");
        if (isMovie) {
            sbSQL.append(SQL_COMMA_SPACE_QUOTE).append(MetaDataType.MOVIE).append(SQL_AS_VIDEO_TYPE_STRING);
        } else {
            sbSQL.append(SQL_COMMA_SPACE_QUOTE).append(MetaDataType.EPISODE).append(SQL_AS_VIDEO_TYPE_STRING);
        }
        sbSQL.append(", vd.title");
        sbSQL.append(", vd.title_original AS originalTitle");
        sbSQL.append(", vd.publication_year AS videoYear");
        sbSQL.append(", null AS seriesId");
        sbSQL.append(", vd.season_id AS seasonId");
        sbSQL.append(", null AS season");
        sbSQL.append(", vd.episode AS episode");
        sbSQL.append(", (vd.watched_nfo or vd.watched_file or vd.watched_api) as watched");
        sbSQL.append(DataItemTools.addSqlDataItems(dataItems, "vd"));
        sbSQL.append(" FROM videodata vd");

        if (isMovie) {
            sbSQL.append(" WHERE vd.episode < 0");
        } else {
            sbSQL.append(" WHERE vd.episode > -1");
        }
        
        if (options.getId() > 0L) {
            sbSQL.append(" AND vd.id=").append(options.getId());
        }

        if (includes.containsKey(YEAR)) {
            sbSQL.append(" AND vd.publication_year=").append(includes.get(YEAR));
        }

        if (excludes.containsKey(YEAR)) {
            sbSQL.append(" AND vd.publication_year!=").append(includes.get(YEAR));
        }

        // check genre
        if (includes.containsKey(GENRE) || excludes.containsKey(GENRE)) {
            String genre;
            if (includes.containsKey(GENRE)) {
                sbSQL.append(" AND exists(");
                genre = includes.get(GENRE).toLowerCase();
            } else {
                sbSQL.append(" AND not exists (");
                genre = excludes.get(GENRE).toLowerCase();
            }
            sbSQL.append("SELECT 1 FROM videodata_genres vg, genre g ");
            sbSQL.append("WHERE vd.id=vg.data_id ");
            sbSQL.append("AND vg.genre_id=g.id ");
            sbSQL.append("AND (lower(g.name)='").append(genre).append("'");
            sbSQL.append(" or (g.target_api is not null and lower(g.target_api)='").append(genre).append("')");
            sbSQL.append(" or (g.target_xml is not null and lower(g.target_xml)='").append(genre).append("')))");
        }

        // add the search string, this will be empty if there is no search required
        sbSQL.append(options.getSearchString(false));

        return sbSQL.toString();
    }

    /**
     * Create the SQL fragment for the selection of series
     *
     * @param options
     * @param includes
     * @param excludes
     * @return
     */
    private String generateSqlForSeries(OptionsIndexVideo options, Map<String, String> includes, Map<String, String> excludes, List<DataItem> dataItems) {
        StringBuilder sbSQL = new StringBuilder();

        sbSQL.append("SELECT ser.id");
        sbSQL.append(SQL_COMMA_SPACE_QUOTE).append(MetaDataType.SERIES).append(SQL_AS_VIDEO_TYPE_STRING);
        sbSQL.append(", ser.title");
        sbSQL.append(", ser.title_original AS originalTitle");
        sbSQL.append(", ser.start_year AS videoYear");
        sbSQL.append(", ser.id AS seriesId");
        sbSQL.append(", null AS seasonId");
        sbSQL.append(", null AS season");
        sbSQL.append(", null AS episode");
        sbSQL.append(", (select min(vid.watched_nfo or vid.watched_file or vid.watched_api) from videodata vid,season sea where vid.season_id=sea.id and sea.series_id=ser.id) as watched ");
        sbSQL.append(DataItemTools.addSqlDataItems(dataItems, "ser"));
        sbSQL.append(" FROM series ser ");
        
        sbSQL.append(SQL_WHERE_1_EQ_1); // To make it easier to add the optional include and excludes
        if (options.getId() > 0L) {
            sbSQL.append(" AND ser.id=").append(options.getId());
        }

        if (includes.containsKey(YEAR)) {
            sbSQL.append(" AND ser.start_year=").append(includes.get(YEAR));
        }

        if (excludes.containsKey(YEAR)) {
            sbSQL.append(" AND ser.start_year!=").append(includes.get(YEAR));
        }

        // check genre
        if (includes.containsKey(GENRE) || excludes.containsKey(GENRE)) {
            String genre;
            if (includes.containsKey(GENRE)) {
                sbSQL.append(" AND exists(");
                genre = includes.get(GENRE).toLowerCase();
            } else {
                sbSQL.append(" AND not exists (");
                genre = excludes.get(GENRE).toLowerCase();
            }
            sbSQL.append("SELECT 1 FROM series_genres sg, genre g ");
            sbSQL.append("WHERE ser.id=sg.series_id ");
            sbSQL.append("AND sg.genre_id=g.id ");
            sbSQL.append("AND (lower(g.name)='").append(genre).append("'");
            sbSQL.append(" or (g.target_api is not null and lower(g.target_api)='").append(genre).append("')");
            sbSQL.append(" or (g.target_xml is not null and lower(g.target_xml)='").append(genre).append("')))");
        }

        // add the search string, this will be empty if there is no search required
        sbSQL.append(options.getSearchString(false));

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
    private String generateSqlForSeason(OptionsIndexVideo options, Map<String, String> includes, Map<String, String> excludes, List<DataItem> dataItems) {
        StringBuilder sbSQL = new StringBuilder();

        sbSQL.append("SELECT sea.id");
        sbSQL.append(SQL_COMMA_SPACE_QUOTE).append(MetaDataType.SEASON).append(SQL_AS_VIDEO_TYPE_STRING);
        sbSQL.append(", sea.title");
        sbSQL.append(", sea.title_original AS originalTitle");
        sbSQL.append(", sea.publication_year as videoYear");
        sbSQL.append(", sea.series_id AS seriesId");
        sbSQL.append(", sea.id AS seasonId");
        sbSQL.append(", sea.season AS season");
        sbSQL.append(", null AS episode");
        sbSQL.append(", (select min(vid.watched_nfo or vid.watched_file or vid.watched_api) from videodata vid where vid.season_id=sea.id) as watched ");
        sbSQL.append(DataItemTools.addSqlDataItems(dataItems, "sea"));
        sbSQL.append(" FROM season sea");
        
        sbSQL.append(SQL_WHERE_1_EQ_1); // To make it easier to add the optional include and excludes
        if (options.getId() > 0L) {
            sbSQL.append(" AND sea.id=").append(options.getId());
        }

        if (includes.containsKey(YEAR)) {
            sbSQL.append(" AND sea.publication_year=").append(includes.get(YEAR));
        }

        if (excludes.containsKey(YEAR)) {
            sbSQL.append(" AND sea.publication_year!=").append(includes.get(YEAR));
        }

        // check genre
        if (includes.containsKey(GENRE) || excludes.containsKey(GENRE)) {
            String genre;
            if (includes.containsKey(GENRE)) {
                sbSQL.append(" AND exists(");
                genre = includes.get(GENRE).toLowerCase();
            } else {
                sbSQL.append(" AND not exists (");
                genre = excludes.get(GENRE).toLowerCase();
            }
            sbSQL.append("SELECT 1 FROM series_genres sg, genre g ");
            sbSQL.append("WHERE sea.series_id=sg.series_id ");
            sbSQL.append("AND sg.genre_id=g.id ");
            sbSQL.append("AND (lower(g.name)='").append(genre).append("'");
            sbSQL.append(" or (g.target_api is not null and lower(g.target_api)='").append(genre).append("')");
            sbSQL.append(" or (g.target_xml is not null and lower(g.target_xml)='").append(genre).append("')))");
        }
        
        // add the search string, this will be empty if there is no search required
        sbSQL.append(options.getSearchString(false));

        return sbSQL.toString();
    }

    /**
     * Search the list of IDs for artwork and add to the artworkList.
     *
     * @param ids
     * @param artworkList
     * @param options
     */
    private void addArtworks(Map<MetaDataType, List<Long>> ids, Map<String, ApiVideoDTO> artworkList, OptionsIndexVideo options) {
        List<String> artworkRequired = options.getArtworkTypes();
        LOG.debug("Artwork required: {}", artworkRequired.toString());

        if (CollectionUtils.isNotEmpty(artworkRequired)) {
            SqlScalars sqlScalars = new SqlScalars();
            boolean hasMovie = CollectionUtils.isNotEmpty(ids.get(MetaDataType.MOVIE));
            boolean hasSeries = CollectionUtils.isNotEmpty(ids.get(MetaDataType.SERIES));
            boolean hasSeason = CollectionUtils.isNotEmpty(ids.get(MetaDataType.SEASON));
            boolean hasEpisode = CollectionUtils.isNotEmpty(ids.get(MetaDataType.EPISODE));

            if (hasMovie) {
                sqlScalars.addToSql("SELECT 'MOVIE' as sourceString, v.id as sourceId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
                sqlScalars.addToSql(" FROM videodata v, artwork a");
                sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
                sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
                sqlScalars.addToSql(" WHERE v.id=a.videodata_id");
                sqlScalars.addToSql(" AND v.episode<0");
                sqlScalars.addToSql(" AND v.id IN (:movielist)");
                sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
            }

            if (hasMovie && hasSeries) {
                sqlScalars.addToSql(" UNION");
            }

            if (hasSeries) {
                sqlScalars.addToSql(" SELECT 'SERIES' as sourceString, s.id as sourceId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
                sqlScalars.addToSql(" FROM series s, artwork a");
                sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
                sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
                sqlScalars.addToSql(" WHERE s.id=a.series_id");
                sqlScalars.addToSql(" AND s.id IN (:serieslist)");
                sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
            }

            if ((hasMovie || hasSeries) && hasSeason) {
                sqlScalars.addToSql(" UNION");
            }

            if (hasSeason) {
                sqlScalars.addToSql(" SELECT 'SEASON' as sourceString, s.id as sourceId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
                sqlScalars.addToSql(" FROM season s, artwork a");
                sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
                sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
                sqlScalars.addToSql(" WHERE s.id=a.season_id");
                sqlScalars.addToSql(" AND s.id IN (:seasonlist)");
                sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
            }

            if ((hasMovie || hasSeries || hasSeason) && hasEpisode) {
                sqlScalars.addToSql(" UNION");
            }

            if (hasEpisode) {
                sqlScalars.addToSql("SELECT 'EPISODE' as sourceString, v.id as sourceId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
                sqlScalars.addToSql(" FROM videodata v, artwork a");
                sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
                sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
                sqlScalars.addToSql(" WHERE v.id=a.videodata_id");
                sqlScalars.addToSql(" AND v.episode>-1");
                sqlScalars.addToSql(" AND v.id IN (:episodelist)");
                sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
            }

            sqlScalars.addScalar("sourceString", StringType.INSTANCE);
            sqlScalars.addScalar("sourceId", LongType.INSTANCE);
            sqlScalars.addScalar("artworkId", LongType.INSTANCE);
            sqlScalars.addScalar("locatedId", LongType.INSTANCE);
            sqlScalars.addScalar("generatedId", LongType.INSTANCE);
            sqlScalars.addScalar("artworkTypeString", StringType.INSTANCE);
            sqlScalars.addScalar(CACHE_DIR, StringType.INSTANCE);
            sqlScalars.addScalar(CACHE_FILENAME, StringType.INSTANCE);

            if (hasMovie) {
                sqlScalars.addParameters("movielist", ids.get(MetaDataType.MOVIE));
            }

            if (hasSeries) {
                sqlScalars.addParameters("serieslist", ids.get(MetaDataType.SERIES));
            }

            if (hasSeason) {
                sqlScalars.addParameters("seasonlist", ids.get(MetaDataType.SEASON));
            }

            if (hasEpisode) {
                sqlScalars.addParameters("episodelist", ids.get(MetaDataType.EPISODE));
            }

            sqlScalars.addParameters("artworklist", artworkRequired);

            List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, null);

            LOG.trace("Found {} artworks", results.size());
            for (ApiArtworkDTO ia : results) {
                LOG.trace("  {} = {}", ia.key(), ia.toString());
                artworkList.get(ia.key()).addArtwork(ia);
            }
        }
    }

    /**
     * Get a list of the people
     *
     * @param wrapper
     */
    public void getPersonList(ApiWrapperList<ApiPersonDTO> wrapper) {
        OptionsIndexPerson options = (OptionsIndexPerson) wrapper.getOptions();
        SqlScalars sqlScalars = generateSqlForPerson(options);
        List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
        if (CollectionUtils.isNotEmpty(results)) {
            if (options.hasDataItem(DataItem.ARTWORK)) {
                LOG.trace("Adding photos");
                // Get the artwork associated with the IDs in the results
                Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.PERSON, generateIdList(results), Arrays.asList("PHOTO"));
                for (ApiPersonDTO p : results) {
                    if (artworkList.containsKey(p.getId())) {
                        p.setArtwork(artworkList.get(p.getId()));
                    }
                }
            }
        }

        wrapper.setResults(results);
    }

    /**
     * Get a single person using the ID in the wrapper options.
     *
     * @param wrapper
     */
    public void getPerson(ApiWrapperSingle<ApiPersonDTO> wrapper) {
        OptionsIndexPerson options = (OptionsIndexPerson) wrapper.getOptions();
        SqlScalars sqlScalars = generateSqlForPerson(options);
        List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
        if (CollectionUtils.isNotEmpty(results)) {
            ApiPersonDTO person = results.get(0);
            if (options.hasDataItem(DataItem.ARTWORK)) {
                LOG.info("Adding photo for {}", person.getName());
                // Add the artwork
                Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.PERSON, person.getId(), Arrays.asList("PHOTO"));
                if (artworkList.containsKey(options.getId())) {
                    LOG.info("Found {} artworks", artworkList.get(options.getId()).size());
                    person.setArtwork(artworkList.get(options.getId()));
                } else {
                    LOG.info("No artwork found for Person ID '{}'", options.getId());
                }
            }

            if (options.hasDataItem(DataItem.FILMOGRAPHY_INSIDE)) {
                LOG.info("Adding filmograpghy inside for '{}'", person.getName());
                person.setFilmography(getPersonFilmographyInside(person.getId(), options.getSortby(), options.getSortdir()));
            } else if (options.hasDataItem(DataItem.FILMOGRAPHY_SCANNED)) {
                LOG.info("Adding filmograpghy scanned for '{}'", person.getName());
                person.setFilmography(getPersonFilmographyScanned(person.getId(), options.getSortby(), options.getSortdir()));
            }

            wrapper.setResult(person);
        } else {
            wrapper.setResult(null);
        }
    }

    private List<ApiFilmographyDTO> getPersonFilmographyInside(long id, String sortBy, String sortDir) {
        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT DISTINCT '"+ParticipationType.MOVIE.name()+"' as typeString, c1.job as job, c1.role as role,");
        sbSQL.append("v1.title as title, v1.title_original as originalTitle, v1.publication_year as year, null as yearEnd,");
        sbSQL.append("v1.release_date as releaseDate, null as releaseState, v1.plot as description,");
        sbSQL.append("v1.id as videoDataId, null as seriesId ");
        sbSQL.append("FROM cast_crew c1, videodata v1 ");
        sbSQL.append("WHERE c1.person_id = :id and v1.id=c1.videodata_id and v1.episode<0 ");
        sbSQL.append("UNION ");
        sbSQL.append("SELECT DISTINCT '"+ParticipationType.SERIES.name()+"' as typeString, c2.job as job, c2.role as role,");
        sbSQL.append("ser.title as title, ser.title_original as originalTitle, ser.start_year as year, ser.end_year as yearEnd,");
        sbSQL.append("null as releaseDate, null as releaseState, ser.plot as description,");
        sbSQL.append("null as videoDataId, ser.id as seriesId ");
        sbSQL.append("FROM cast_crew c2, videodata v2, season sea, series ser ");
        sbSQL.append("WHERE c2.person_id = :id and v2.id=c2.videodata_id and v2.episode>=0 ");
        sbSQL.append("and v2.season_id=sea.id and sea.series_id=ser.id ");

        sbSQL.append("ORDER BY ");
        if ("title".equalsIgnoreCase(sortBy)) {
            sbSQL.append("title ");
            sbSQL.append(sortDir);
            sbSQL.append(", ");
        } else if ("type".equalsIgnoreCase(sortBy)) {
            sbSQL.append("typeString ");
            sbSQL.append(sortDir); 
            sbSQL.append(", ");
        } else if ("job".equalsIgnoreCase(sortBy)) {
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

    private List<ApiFilmographyDTO> getPersonFilmographyScanned(long id, String sortBy, String sortDir) {
        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT DISTINCT p.participation_type as typeString, p.job as job, p.role as role,");
        sbSQL.append("p.title as title, p.title_original as originalTitle, p.year as year,p.year_end as yearEnd,");
        sbSQL.append("p.release_date as releaseDate, p.release_state as releaseState,p.description as description,");
        sbSQL.append("movie.id as videoDataId, serids.series_id as seriesId ");
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
        sbSQL.append("ORDER BY ");
        if ("title".equalsIgnoreCase(sortBy)) {
            sbSQL.append("p.title ");
            sbSQL.append(sortDir);
            sbSQL.append(", ");
        } else if ("type".equalsIgnoreCase(sortBy)) {
            sbSQL.append("p.participation_type ");
            sbSQL.append(sortDir); 
            sbSQL.append(", ");
        } else if ("job".equalsIgnoreCase(sortBy)) {
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

    public List<ApiFilmographyDTO> retrieveFilmography(long id, SqlScalars sqlScalars) {
        sqlScalars.addScalar("typeString", StringType.INSTANCE);
        sqlScalars.addScalar("job", StringType.INSTANCE);
        sqlScalars.addScalar("role", StringType.INSTANCE);
        sqlScalars.addScalar("title", StringType.INSTANCE);
        sqlScalars.addScalar("originalTitle", StringType.INSTANCE);
        sqlScalars.addScalar("year", IntegerType.INSTANCE);
        sqlScalars.addScalar("yearEnd", IntegerType.INSTANCE);
        sqlScalars.addScalar("releaseDate", DateType.INSTANCE);
        sqlScalars.addScalar("releaseState", StringType.INSTANCE);
        sqlScalars.addScalar("description", StringType.INSTANCE);
        sqlScalars.addScalar("videoDataId", LongType.INSTANCE);
        sqlScalars.addScalar("seriesId", LongType.INSTANCE);

        sqlScalars.addParameters(ID, id);

        return executeQueryWithTransform(ApiFilmographyDTO.class, sqlScalars, null);
    }
    
    public void getPersonListByVideoType(MetaDataType metaDataType, ApiWrapperList<ApiPersonDTO> wrapper) {
        OptionsIndexPerson options = (OptionsIndexPerson) wrapper.getOptions();
        LOG.info("Getting person list for {} with ID '{}'", metaDataType, options.getId());

        SqlScalars sqlScalars = generateSqlForVideoPerson(metaDataType, options);
        List<ApiPersonDTO> results = executeQueryWithTransform(ApiPersonDTO.class, sqlScalars, wrapper);
        LOG.info("Found {} results for {} with id '{}'", results.size(), metaDataType, options.getId());

        if (options.hasDataItem(DataItem.ARTWORK) && results.size() > 0) {
            LOG.info("Looking for person artwork for {} with id '{}'", metaDataType, options.getId());

            Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.PERSON, generateIdList(results), Arrays.asList("PHOTO"));
            for (ApiPersonDTO person : results) {
                if (artworkList.containsKey(person.getId())) {
                    person.setArtwork(artworkList.get(person.getId()));
                }
            }
        } else {
            LOG.info("No artwork found/requested for {} with id '{}'", metaDataType, options.getId());
        }

        wrapper.setResults(results);
    }

    /**
     * Generates a list of people in a video
     *
     * @param metaDataType
     * @param options
     * @return
     */
    private SqlScalars generateSqlForVideoPerson(MetaDataType metaDataType, OptionsIndexPerson options) {
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT DISTINCT p.id,");
        sqlScalars.addToSql(" p.name,");
        if (options.hasDataItem(DataItem.BIOGRAPHY)) {
            sqlScalars.addToSql(" p.biography,");
            sqlScalars.addScalar("biography", StringType.INSTANCE);
        }
        sqlScalars.addToSql(" p.birth_day AS birthDay,");
        sqlScalars.addToSql(" p.birth_place AS birthPlace,");
        sqlScalars.addToSql(" p.birth_name AS birthName,");
        sqlScalars.addToSql(" p.death_day AS deathDay,");
        sqlScalars.addToSql(" c.job,");
        sqlScalars.addToSql(" c.role");
        sqlScalars.addToSql(" FROM person p, cast_crew c");
        sqlScalars.addToSql(" WHERE p.id=c.person_id");

        // TODO: Split by series/season/episode
        if (metaDataType == MetaDataType.MOVIE) {
            sqlScalars.addToSql(" AND c.videodata_id=:id");
        } else if (metaDataType == MetaDataType.SERIES) {
            sqlScalars.addToSql("AND c.videodata_id IN");
            sqlScalars.addToSql(" (SELECT DISTINCT v.id FROM season s, videodata v");
            sqlScalars.addToSql(" WHERE s.series_id = :id AND s.id = v.season_id)");
        } else if (metaDataType == MetaDataType.SEASON) {
            sqlScalars.addToSql("AND c.videodata_id IN");
            sqlScalars.addToSql(" (SELECT DISTINCT v.id FROM season s, videodata v");
            sqlScalars.addToSql(" WHERE s.id = :id AND s.id = v.season_id)");
        } else if (metaDataType == MetaDataType.EPISODE) {
            sqlScalars.addToSql(" AND c.videodata_id=:id");
        } else {
            throw new UnsupportedOperationException("Person list by '" + metaDataType.toString() + "' not supported.");
        }

        if (CollectionUtils.isNotEmpty(options.getJob())) {
            sqlScalars.addToSql(" AND c.job IN (:joblist)");
            sqlScalars.addParameters("joblist", options.getJob());
        }

        // Add the search string
        sqlScalars.addToSql(options.getSearchString(Boolean.FALSE));
        // This will default to blank if there's no  required
        sqlScalars.addToSql(options.getSortString());

        // Add the ID
        sqlScalars.addParameters(ID, options.getId());

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);
        sqlScalars.addScalar("birthDay", DateType.INSTANCE);
        sqlScalars.addScalar("birthPlace", StringType.INSTANCE);
        sqlScalars.addScalar("birthName", StringType.INSTANCE);
        sqlScalars.addScalar("deathDay", DateType.INSTANCE);
        sqlScalars.addScalar("job", StringType.INSTANCE);
        sqlScalars.addScalar("role", StringType.INSTANCE);

        LOG.debug("SQL ForVideoPerson: {}", sqlScalars.getSql());
        return sqlScalars;
    }

    /**
     * Generate the SQL for the information about a person
     *
     * @param options
     * @return
     */
    private SqlScalars generateSqlForPerson(OptionsIndexPerson options) {
        SqlScalars sqlScalars = new SqlScalars();
        List<DataItem> dataitems = options.splitDataitems();
        // Make sure to set the alias for the files for the Transformation into the class
        sqlScalars.addToSql("SELECT DISTINCT p.id,");
        sqlScalars.addToSql(" p.name,");
        if (dataitems.contains(DataItem.BIOGRAPHY)) {
            sqlScalars.addToSql(" p.biography, ");
            sqlScalars.addScalar("biography", StringType.INSTANCE);
        }
        sqlScalars.addToSql(" p.birth_day AS birthDay, ");
        sqlScalars.addToSql(" p.birth_place AS birthPlace, ");
        sqlScalars.addToSql(" p.birth_name AS birthName, ");
        sqlScalars.addToSql(" p.death_day AS deathDay ");
        sqlScalars.addToSql(" FROM person p");

        if (options.getId() > 0L) {
            sqlScalars.addToSql(" WHERE id=:id");
            sqlScalars.addParameters(ID, options.getId());
        } else {
            if (CollectionUtils.isNotEmpty(options.getJob())) {
                sqlScalars.addToSql(", cast_crew c");
            }

            sqlScalars.addToSql(SQL_WHERE_1_EQ_1);

            if (CollectionUtils.isNotEmpty(options.getJob())) {
                sqlScalars.addToSql(" AND p.id=c.person_id");
                sqlScalars.addToSql(" AND c.job IN (:joblist)");
                sqlScalars.addParameters("joblist", options.getJob());
            }
            // Add the search string
            sqlScalars.addToSql(options.getSearchString(Boolean.FALSE));
            // This will default to blank if there's no sort required
            sqlScalars.addToSql(options.getSortString());
        }

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar("name", StringType.INSTANCE);
        sqlScalars.addScalar("birthDay", DateType.INSTANCE);
        sqlScalars.addScalar("birthPlace", StringType.INSTANCE);
        sqlScalars.addScalar("birthName", StringType.INSTANCE);
        sqlScalars.addScalar("deathDay", DateType.INSTANCE);

        return sqlScalars;
    }

    //<editor-fold defaultstate="collapsed" desc="Artwork Methods">
    public ApiArtworkDTO getArtworkById(Long id) {
        SqlScalars sqlScalars = getSqlArtwork(new OptionsIndexArtwork(id));

        List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, null);
        if (CollectionUtils.isEmpty(results)) {
            return new ApiArtworkDTO();
        }

        return results.get(0);
    }

    public List<ApiArtworkDTO> getArtworkList(ApiWrapperList<ApiArtworkDTO> wrapper) {
        SqlScalars sqlScalars = getSqlArtwork((OptionsIndexArtwork) wrapper.getOptions());
        return executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, wrapper);
    }

    private SqlScalars getSqlArtwork(OptionsIndexArtwork options) {
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT a.id AS artworkId,");
        sqlScalars.addToSql(" al.id AS locatedId,");
        sqlScalars.addToSql(" ag.id AS generatedId,");
        sqlScalars.addToSql(" a.season_id AS seasonId,");
        sqlScalars.addToSql(" a.series_id AS seriesId,");
        sqlScalars.addToSql(" a.videodata_id AS videodataId,");
        sqlScalars.addToSql(" a.artwork_type AS artworkTypeString,");
        sqlScalars.addToSql(" ag.cache_filename AS cacheFilename,");
        sqlScalars.addToSql(" ag.cache_dir AS cacheDir");
        sqlScalars.addToSql(" FROM artwork a");
        sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
        sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
        sqlScalars.addToSql(SQL_WHERE_1_EQ_1); // Make appending restrictions easier
        if (options != null) {
            if (options.getId() > 0L) {
                sqlScalars.addToSql(" AND a.id=:id");
                sqlScalars.addParameters(ID, options.getId());
            }

            if (CollectionUtils.isNotEmpty(options.getArtwork())) {
                sqlScalars.addToSql(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);
                sqlScalars.addParameters("artworklist", options.getArtwork());
            }

            if (CollectionUtils.isNotEmpty(options.getVideo())) {
                StringBuilder sb = new StringBuilder("AND (");
                boolean first = Boolean.TRUE;
                for (String type : options.getVideo()) {
                    MetaDataType mdt = MetaDataType.fromString(type);
                    if (first) {
                        first = Boolean.FALSE;
                    } else {
                        sb.append(" OR");
                    }
                    if (mdt == MetaDataType.MOVIE) {
                        sb.append(" videodata_id IS NOT NULL");
                    } else if (mdt == MetaDataType.SERIES) {
                        sb.append(" series_id IS NOT NULL");
                    } else if (mdt == MetaDataType.SEASON) {
                        sb.append(" season_id IS NOT NULL");
                    }
                }
                sb.append(")");
                sqlScalars.addToSql(sb.toString());
            }
        }

        // Add the scalars
        sqlScalars.addScalar("artworkId", LongType.INSTANCE);
        sqlScalars.addScalar("locatedId", LongType.INSTANCE);
        sqlScalars.addScalar("generatedId", LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar("videodataId", LongType.INSTANCE);
        sqlScalars.addScalar("artworkTypeString", StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_DIR, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_FILENAME, StringType.INSTANCE);

        return sqlScalars;
    }
    //</editor-fold>

    public void getEpisodeList(ApiWrapperList<ApiEpisodeDTO> wrapper) {
        OptionsEpisode options = (OptionsEpisode) wrapper.getOptions();
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT ser.id AS seriesId, sea.id AS seasonId, sea.season, vid.episode, ");
        sqlScalars.addToSql("vid.id, vid.title, vid.title_original as originalTitle, vid.release_date as firstAired, ");
        sqlScalars.addToSql("(vid.watched_nfo or vid.watched_file or vid.watched_api) as watched, ");
        if (options.hasDataItem(DataItem.OUTLINE)) {
            sqlScalars.addToSql("vid.outline, ");
            sqlScalars.addScalar("outline", StringType.INSTANCE);
        }
        if (options.hasDataItem(DataItem.PLOT)) {
            sqlScalars.addToSql("vid.plot, ");
            sqlScalars.addScalar("plot", StringType.INSTANCE);
        }
        sqlScalars.addToSql("ag.cache_filename AS cacheFilename, ag.cache_dir AS cacheDir");
        sqlScalars.addToSql("FROM season sea, series ser, videodata vid, artwork a");
        sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_LOCATED);
        sqlScalars.addToSql(SQL_LEFT_JOIN_ARTWORK_GENERATED);
        sqlScalars.addToSql("WHERE sea.series_id=ser.id");
        sqlScalars.addToSql("AND vid.season_id=sea.id");
        sqlScalars.addToSql("AND a.videodata_id=vid.id");
        if (options.getSeriesid() > 0L) {
            sqlScalars.addToSql("AND ser.id=:seriesid");
            sqlScalars.addParameters("seriesid", options.getSeriesid());
            if (options.getSeason() > 0L) {
                sqlScalars.addToSql("AND sea.season=:season");
                sqlScalars.addParameters(SEASON, options.getSeason());
            }
        }
        if (options.getSeasonid() > 0L) {
            sqlScalars.addToSql("AND sea.id=:seasonid");
            sqlScalars.addParameters("seasonid", options.getSeasonid());
        }
        sqlScalars.addToSql(" ORDER BY seriesId, season, episode");
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
        
        List<ApiEpisodeDTO> results = executeQueryWithTransform(ApiEpisodeDTO.class, sqlScalars, wrapper);
        if (CollectionUtils.isNotEmpty(results)) {
            if (options.hasDataItem(DataItem.FILES)) {
                for (ApiEpisodeDTO episode : results) {
                    episode.setFiles(this.getFilesForId(MetaDataType.EPISODE, episode.getId()));
                }
            }
            if (options.hasDataItem(DataItem.GENRE)) {
                for (ApiEpisodeDTO episode : results) {
                    episode.setGenres(this.getGenresForId(MetaDataType.EPISODE, episode.getId()));
                }
            }
            if (options.hasDataItem(DataItem.CERTIFICATION)) {
                for (ApiEpisodeDTO episode : results) {
                    episode.setCertifications(this.getCertificationsForId(MetaDataType.EPISODE, episode.getId()));
                }
            }
        }
        wrapper.setResults(results);
    }

    public void getSingleVideo(ApiWrapperSingle<ApiVideoDTO> wrapper) {
        OptionsIndexVideo options = (OptionsIndexVideo) wrapper.getOptions();
        Map<String, String> includes = options.splitIncludes();
        Map<String, String> excludes = options.splitExcludes();
        MetaDataType type = MetaDataType.fromString(options.getType());

        List<DataItem> dataItems = options.splitDataitems();
        LOG.debug("Getting additional data items: {} ", dataItems.toString());

        String sql;
        if (type == MetaDataType.MOVIE) {
            sql = generateSqlForVideo(true, options, includes, excludes, dataItems);
        } else if (type == MetaDataType.SERIES) {
            sql = generateSqlForSeries(options, includes, excludes, dataItems);
        } else if (type == MetaDataType.SEASON) {
            sql = generateSqlForSeason(options, includes, excludes, dataItems);
        } else {
            throw new UnsupportedOperationException("Unable to process type '" + type + "' (Original: '" + options.getType() + "')");
        }
        LOG.debug("SQL for {}-{}: {}", type, options.getId(), sql);

        SqlScalars sqlScalars = new SqlScalars(sql);

        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar("videoTypeString", StringType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(ORIGINAL_TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(VIDEO_YEAR, IntegerType.INSTANCE);
        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON, LongType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);
        // Add Scalars for additional data item columns
        DataItemTools.addDataItemScalars(sqlScalars, dataItems);

        List<ApiVideoDTO> queryResults = executeQueryWithTransform(ApiVideoDTO.class, sqlScalars, wrapper);
        LOG.debug("Found {} results for ID '{}'", queryResults.size(), options.getId());
        if (CollectionUtils.isNotEmpty(queryResults)) {
            ApiVideoDTO video = queryResults.get(0);
            
            if (dataItems.contains(DataItem.GENRE)) {
                LOG.trace("Adding genres for ID '{}'", options.getId());
                video.setGenres(getGenresForId(type, options.getId()));
            }

            if (dataItems.contains(DataItem.CERTIFICATION)) {
                LOG.trace("Adding certifications for ID '{}'", options.getId());
                video.setCertifications(getCertificationsForId(type, options.getId()));
            }

            if (dataItems.contains(DataItem.ARTWORK)) {
                LOG.trace("Adding artwork for ID '{}'", options.getId());
                Map<Long, List<ApiArtworkDTO>> artworkList;
                if (CollectionUtils.isNotEmpty(options.getArtworkTypes())) {
                    artworkList = getArtworkForId(type, options.getId(), options.getArtworkTypes());
                } else {
                    artworkList = getArtworkForId(type, options.getId());
                }

                if (artworkList.containsKey(options.getId())) {
                    video.setArtwork(artworkList.get(options.getId()));
                }
            }

            if (dataItems.contains(DataItem.FILES)) {
                LOG.trace("Adding files for ID '{}'", options.getId());
                video.setFiles(getFilesForId(type, options.getId()));
            }

            wrapper.setResult(video);
        } else {
            wrapper.setResult(null);
        }
    }

    /**
     * Get a list of the files associated with a video ID.
     *
     * @param type
     * @param id
     * @return
     */
    private List<ApiFileDTO> getFilesForId(MetaDataType type, Long id) {
        // Build the SQL statement
        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT mf.id as id, mf.extra as extra, mf.part as part, mf.part_title as partTitle, mf.movie_version as version, ");
        sbSQL.append("mf.container as container, mf.codec as codec, mf.codec_format as codecFormat, mf.codec_profile as codecProfile, ");  
        sbSQL.append("mf.bitrate as bitrate, mf.overall_bitrate as overallBitrate, mf.fps as fps, ");
        sbSQL.append("mf.width as width, mf.height as height, mf.aspect_ratio as aspectRatio, mf.runtime as runtime, mf.video_source as videoSource, ");
        sbSQL.append("sf.id as fileId, sf.full_path as fileName, sf.file_date as fileDate, sf.file_size as fileSize, ");
        
        if (type == MetaDataType.MOVIE) {
            sbSQL.append("null as season, null as episode ");
            sbSQL.append("FROM mediafile_videodata mv, mediafile mf, stage_file sf ");
            sbSQL.append("WHERE mv.videodata_id=:id ");
        } else if (type == MetaDataType.SERIES) {
            sbSQL.append("sea.season, vd.episode ");
            sbSQL.append("FROM mediafile_videodata mv, mediafile mf, stage_file sf, season sea, videodata vd ");
            sbSQL.append("WHERE sea.series_id=:id ");
            sbSQL.append("and vd.season_id=sea.id ");
            sbSQL.append("and mv.videodata_id=vd.id ");
        } else if (type == MetaDataType.SEASON) {
            sbSQL.append("sea.season, vd.episode ");
            sbSQL.append("FROM mediafile_videodata mv, mediafile mf, stage_file sf, season sea, videodata vd ");
            sbSQL.append("WHERE sea.id=:id ");
            sbSQL.append("and vd.season_id=sea.id ");
            sbSQL.append("and mv.videodata_id=vd.id ");
        } else if (type == MetaDataType.EPISODE) {
            sbSQL.append("null as season, null as episode ");
            sbSQL.append("FROM mediafile_videodata mv, mediafile mf, stage_file sf ");
            sbSQL.append("WHERE mv.videodata_id=:id ");
        }
        
        sbSQL.append("and mv.mediafile_id=mf.id ");
        sbSQL.append("and sf.mediafile_id=mf.id ");
        sbSQL.append("and sf.file_type='");
        sbSQL.append(FileType.VIDEO.toString());
        sbSQL.append("' and sf.status not in ('");
        sbSQL.append(StatusType.DUPLICATE.toString());
        sbSQL.append("','");
        sbSQL.append(StatusType.DELETED.toString());
        sbSQL.append("') ");

        if (type == MetaDataType.SERIES || type == MetaDataType.SEASON) {
            sbSQL.append("ORDER BY sea.season ASC, vd.episode ASC");
        }


        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        sqlScalars.addScalar(ID, LongType.INSTANCE);
        sqlScalars.addScalar("extra", BooleanType.INSTANCE);
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
        sqlScalars.addParameters(ID, id);

        List<ApiFileDTO> results = executeQueryWithTransform(ApiFileDTO.class, sqlScalars, null);
        if (CollectionUtils.isNotEmpty(results)) {
            for (ApiFileDTO file : results)  {
                file.setAudioCodes(this.getAudioCodecs(file.getId()));
            }
        }
        return results;
    }
    
    private List<ApiAudioCodecDTO> getAudioCodecs(long mediaFileId) {
        // Build the SQL statement
        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT ac.codec, ac.codec_format as codecFormat, ac.bitrate, ac.channels, ac.language ");
        sbSQL.append("FROM audio_codec ac ");
        sbSQL.append("WHERE ac.mediafile_id=:id ");
        sbSQL.append("ORDER BY ac.counter ASC");

        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        sqlScalars.addScalar("codec", StringType.INSTANCE);
        sqlScalars.addScalar("codecFormat", StringType.INSTANCE);
        sqlScalars.addScalar("bitrate", IntegerType.INSTANCE);
        sqlScalars.addScalar("channels", IntegerType.INSTANCE);
        sqlScalars.addScalar("language", StringType.INSTANCE);
        sqlScalars.addParameters(ID, mediaFileId);

        return executeQueryWithTransform(ApiAudioCodecDTO.class, sqlScalars, null);
    }

    /**
     * Get a list of the genres for a given video ID
     *
     * @param type
     * @param id
     * @return
     */
    public List<ApiGenreDTO> getGenresForId(MetaDataType type, Long id) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT DISTINCT ");
        sqlScalars.addToSql("CASE ");
        sqlScalars.addToSql(" WHEN target_api is not null THEN target_api ");
        sqlScalars.addToSql(" WHEN target_xml is not null THEN target_xml ");
        sqlScalars.addToSql(" ELSE name ");
        sqlScalars.addToSql("END as name ");
        if (type == MetaDataType.SERIES) {
            sqlScalars.addToSql("FROM series_genres sg, genre g ");
            sqlScalars.addToSql("WHERE sg.series_id=:id ");
            sqlScalars.addToSql("AND sg.genre_id=g.id ");
        } else if (type == MetaDataType.SEASON) {
            sqlScalars.addToSql("FROM season sea, series_genres sg, genre g ");
            sqlScalars.addToSql("WHERE sea.id=:id ");
            sqlScalars.addToSql("AND sg.series_id=sea.series_id ");
            sqlScalars.addToSql("AND sg.genre_id=g.id ");
        } else {
            // defaults to movie/episode
            sqlScalars.addToSql("FROM videodata_genres vg, genre g ");
            sqlScalars.addToSql("WHERE vg.data_id=:id ");
            sqlScalars.addToSql("AND vg.genre_id=g.id ");
        }
        sqlScalars.addToSql("ORDER BY name");

        sqlScalars.addScalar("name", StringType.INSTANCE);
        sqlScalars.addParameters(ID, id);

        return executeQueryWithTransform(ApiGenreDTO.class, sqlScalars, null);
    }

    /**
     * Get a list of the certifications for a given video ID
     *
     * @param type
     * @param id
     * @return
     */
    public List<Certification> getCertificationsForId(MetaDataType type, Long id) {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT DISTINCT c.id, c.country, c.certificate ");
        sqlScalars.addToSql("FROM certification c ");
        if (type == MetaDataType.SERIES) {
            sqlScalars.addToSql("JOIN series_certifications sc ON c.id=sc.cert_id and sc.series_id=:id ");
        } else if (type == MetaDataType.SEASON) {
            sqlScalars.addToSql("JOIN season sea ON sea.id = :id ");
            sqlScalars.addToSql("JOIN series_certifications sc ON c.id=sc.cert_id and sc.series_id=sea.series_id ");
        } else {
            // defaults to movie/episode
            sqlScalars.addToSql("JOIN videodata_certifications vc ON c.id=vc.cert_id and vc.data_id=:id ");
        }
        sqlScalars.addToSql("ORDER BY country");

        sqlScalars.addScalar("id", LongType.INSTANCE);
        sqlScalars.addScalar("country", StringType.INSTANCE);
        sqlScalars.addScalar("certificate", StringType.INSTANCE);
        sqlScalars.addParameters(ID, id);

        return executeQueryWithTransform(Certification.class, sqlScalars, null);
    }

    /**
     * Get a list of all artwork available for a video ID
     *
     * @param type
     * @param id
     * @return
     */
    public Map<Long, List<ApiArtworkDTO>> getArtworkForId(MetaDataType type, Long id) {
        List<String> artworkRequired = new ArrayList<String>();
        for (ArtworkType at : ArtworkType.values()) {
            artworkRequired.add(at.toString());
        }
        // Remove the unknown type
        artworkRequired.remove(ArtworkType.UNKNOWN.toString());

        return getArtworkForId(type, id, artworkRequired);
    }

    /**
     * Get a select list of artwork available for a video ID
     *
     * @param type
     * @param id
     * @param artworkRequired
     * @return
     */
    public Map<Long, List<ApiArtworkDTO>> getArtworkForId(MetaDataType type, Object id, List<String> artworkRequired) {
        LOG.debug("Artwork required for {} ID '{}' is {}", type, id, artworkRequired);
        StringBuilder sbSQL = new StringBuilder();
        sbSQL.append("SELECT '").append(type.toString()).append("' AS sourceString,");
        sbSQL.append(" v.id AS sourceId, a.id AS artworkId, al.id AS locatedId, ag.id AS generatedId,");
        sbSQL.append(" a.artwork_type AS artworkTypeString, ag.cache_dir AS cacheDir, ag.cache_filename AS cacheFilename ");
        if (type == MetaDataType.MOVIE) {
            sbSQL.append("FROM videodata v ");
        } else if (type == MetaDataType.SERIES) {
            sbSQL.append("FROM series v ");
        } else if (type == MetaDataType.SEASON) {
            sbSQL.append("FROM season v ");
        } else if (type == MetaDataType.PERSON) {
            sbSQL.append("FROM person v");
        }
        sbSQL.append(", artwork a");    // Artwork must be last for the LEFT JOIN
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
        }
        sbSQL.append(" AND v.id IN (:id)");
        sbSQL.append(SQL_ARTWORK_TYPE_IN_ARTWORKLIST);

        SqlScalars sqlScalars = new SqlScalars(sbSQL);
        LOG.info("Artwork SQL: {}", sqlScalars.getSql());

        sqlScalars.addScalar("sourceString", StringType.INSTANCE);
        sqlScalars.addScalar("sourceId", LongType.INSTANCE);
        sqlScalars.addScalar("artworkId", LongType.INSTANCE);
        sqlScalars.addScalar("locatedId", LongType.INSTANCE);
        sqlScalars.addScalar("generatedId", LongType.INSTANCE);
        sqlScalars.addScalar("artworkTypeString", StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_DIR, StringType.INSTANCE);
        sqlScalars.addScalar(CACHE_FILENAME, StringType.INSTANCE);

        sqlScalars.addParameters(ID, id);
        sqlScalars.addParameters("artworklist", artworkRequired);

        List<ApiArtworkDTO> results = executeQueryWithTransform(ApiArtworkDTO.class, sqlScalars, null);

        Map<Long, List<ApiArtworkDTO>> artworkList = generateIdMapList(results);

        return artworkList;
    }

    public void getSeriesInfo(ApiWrapperList<ApiSeriesInfoDTO> wrapper) {
        OptionsIdArtwork options = (OptionsIdArtwork) wrapper.getOptions();
        Long id = options.getId();
        LOG.info("Getting series information for seriesId '{}'", id);

        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT s.id AS seriesId, title, start_year AS seriesYear, ");
        sqlScalars.addToSql("(select min(vid.watched_nfo or vid.watched_file or vid.watched_api) from videodata vid,season sea where vid.season_id=sea.id and sea.series_id=s.id) as watched ");
        sqlScalars.addToSql("FROM series s");
        sqlScalars.addToSql("WHERE id=:id");
        sqlScalars.addToSql("ORDER BY id");
        sqlScalars.addParameters(ID, options.getId());

        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(SERIES_YEAR, IntegerType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);
        List<ApiSeriesInfoDTO> seriesResults = executeQueryWithTransform(ApiSeriesInfoDTO.class, sqlScalars, wrapper);
        LOG.debug("Found {} series for SeriesId '{}'", seriesResults.size(), id);

        for (ApiSeriesInfoDTO series : seriesResults) {
            if (options.hasDataItem(DataItem.ARTWORK)) {
                Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.SERIES, id, options.getArtworkTypes());
                for (ApiArtworkDTO artwork : artworkList.get(id)) {
                    series.addArtwork(artwork);
                }
            }
            series.setSeasonList(getSeasonInfo(options));
        }
        wrapper.setResults(seriesResults);
    }

    private List<ApiSeasonInfoDTO> getSeasonInfo(OptionsIdArtwork options) {
        Long seriesId = options.getId();

        LOG.debug("Getting season information for seriesId '{}'", seriesId);
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT s.series_id AS seriesId, s.id AS seasonId, s.season, title,");
        sqlScalars.addToSql("(select min(vid.watched_nfo or vid.watched_file or vid.watched_api) from videodata vid where vid.season_id=s.id) as watched ");
        sqlScalars.addToSql("FROM season s");
        sqlScalars.addToSql("WHERE series_id=:id");
        sqlScalars.addToSql("ORDER BY series_id, season");
        sqlScalars.addParameters(ID, seriesId);

        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON_ID, LongType.INSTANCE);
        sqlScalars.addScalar(SEASON, IntegerType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(WATCHED, BooleanType.INSTANCE);

        List<ApiSeasonInfoDTO> seasonResults = executeQueryWithTransform(ApiSeasonInfoDTO.class, sqlScalars, null);
        LOG.debug("Found {} seasons for SeriesId '{}'", seasonResults.size(), seriesId);

        if (options.hasDataItem(DataItem.ARTWORK)) {
            for (ApiSeasonInfoDTO season : seasonResults) {
                Map<Long, List<ApiArtworkDTO>> artworkList = getArtworkForId(MetaDataType.SEASON, season.getSeasonId(), options.getArtworkTypes());
                if (artworkList == null || !artworkList.containsKey(seriesId) || CollectionUtils.isEmpty(artworkList.get(seriesId))) {
                    LOG.debug("No artwork found for seriesId '{}' season {}", seriesId, season.getSeason());
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
    /*
     Statistics functions to go in here:
     - Count of movies, series and seasons.
     - Series with most seasons (longest running)
     - Earliest movie/series
     - Latest movie/series
     - Most popular actors
     - Most popular writers
     - Most popular directors
     - Most popular producers
     */
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

        StringBuilder sql = new StringBuilder("SELECT '").append(type).append("' as typeString, ");
        sql.append("count(*) as count, ");
        sql.append("MAX(create_timestamp) as createTimestamp, ");
        sql.append("MAX(update_timestamp) as updateTimestamp, ");
        sql.append("MAX(id) as lastId ");
        sql.append("FROM ").append(tablename);
        if (StringUtils.isNotBlank(clause)) {
            sql.append(" WHERE ").append(clause);
        }

        SqlScalars sqlScalars = new SqlScalars(sql);

        sqlScalars.addScalar("typeString", StringType.INSTANCE);
        sqlScalars.addScalar("count", LongType.INSTANCE);
        sqlScalars.addScalar("createTimestamp", TimestampType.INSTANCE);
        sqlScalars.addScalar("updateTimestamp", TimestampType.INSTANCE);
        sqlScalars.addScalar("lastId", LongType.INSTANCE);

        List<CountTimestamp> results = executeQueryWithTransform(CountTimestamp.class, sqlScalars, null);
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
        LOG.info("getJobCount: Required Jobs: {}", (requiredJobs == null ? "all" : requiredJobs));
        SqlScalars sqlScalars = new SqlScalars();

        sqlScalars.addToSql("SELECT job AS item, COUNT(*) AS count");
        sqlScalars.addToSql("FROM  cast_crew");
        if (CollectionUtils.isNotEmpty(requiredJobs)) {
            sqlScalars.addToSql("WHERE job IN (:joblist)");
            sqlScalars.addParameters("joblist", requiredJobs);
        }
        sqlScalars.addToSql("GROUP BY job");

        sqlScalars.addScalar("item", StringType.INSTANCE);
        sqlScalars.addScalar("count", LongType.INSTANCE);

        return executeQueryWithTransform(CountGeneric.class, sqlScalars, null);
    }

    /**
     *
     */
    public void statSeriesCount() {
        SqlScalars sqlScalars = new SqlScalars();
        sqlScalars.addToSql("SELECT s.id AS seriesId, title, start_year AS seriesYear");
        sqlScalars.addToSql("FROM series s");

        sqlScalars.addScalar(SERIES_ID, LongType.INSTANCE);
        sqlScalars.addScalar(TITLE, StringType.INSTANCE);
        sqlScalars.addScalar(SERIES_YEAR, IntegerType.INSTANCE);

        // Get the results
        List<ApiSeriesInfoDTO> seriesResults = executeQueryWithTransform(ApiSeriesInfoDTO.class, sqlScalars, null);
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

        // Process the results into statistics
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
    private <T extends AbstractApiIdentifiableDTO> Map<Long, T> generateIdMap(List<T> idList) {
        Map<Long, T> results = new HashMap<Long, T>(idList.size());

        for (T idSingle : idList) {
            results.put(idSingle.getId(), idSingle);
        }

        return results;
    }

    /**
     * Take a list and generate a map of the ID and a list of the items for that ID
     *
     * @param <T> Source type
     * @param idList List of the source type
     * @return
     */
    private <T extends AbstractApiIdentifiableDTO> Map<Long, List<T>> generateIdMapList(List<T> idList) {
        Map<Long, List<T>> results = new HashMap<Long, List<T>>();

        for (T idSingle : idList) {
            Long sourceId = idSingle.getId();
            if (results.containsKey(sourceId)) {
                results.get(sourceId).add(idSingle);
            } else {
                // ID didn't exist so add a new list
                results.put(sourceId, new ArrayList<T>(Arrays.asList(idSingle)));
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
    private <T extends AbstractApiIdentifiableDTO> List<Long> generateIdList(List<T> idList) {
        List<Long> results = new ArrayList<Long>(idList.size());

        for (T idSingle : idList) {
            results.add(idSingle.getId());
        }

        return results;
    }
    //</editor-fold>
}
