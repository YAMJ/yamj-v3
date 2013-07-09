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
package org.yamj.core.database.dao;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.hibernate.type.BasicType;
import org.hibernate.type.DateType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yamj.core.api.model.CountTimestamp;
import org.yamj.core.api.model.dto.IndexVideoDTO;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.ApiWrapperList;
import org.yamj.core.api.model.IApiWrapper;
import org.yamj.core.api.model.dto.IndexArtworkDTO;
import org.yamj.core.api.model.dto.IndexPersonDTO;
import org.yamj.core.api.options.IOptions;
import org.yamj.core.api.options.OptionsIndexVideo;
import org.yamj.core.database.model.Configuration;
import org.yamj.core.hibernate.HibernateDao;

@Service("apiDao")
public class ApiDao extends HibernateDao {

    private static final Logger LOG = LoggerFactory.getLogger(ApiDao.class);

    /**
     * Generate the query and load the results into the wrapper
     *
     * @param sqlString
     * @param wrapper
     */
    public void getVideoList(ApiWrapperList<IndexVideoDTO> wrapper) {
        String sql = generateSqlForVideoList(wrapper);

        Map<String, BasicType> scalars = new HashMap<String, BasicType>();
        scalars.put("id", LongType.INSTANCE);
        scalars.put("videoTypeString", StringType.INSTANCE);
        scalars.put("title", StringType.INSTANCE);
        scalars.put("videoYear", IntegerType.INSTANCE);

        List<IndexVideoDTO> queryResults = executeQueryWithTransform(IndexVideoDTO.class, sql.toString(), wrapper, scalars);
        wrapper.setResults(queryResults);

        // Create and populate the ID list
        Map<MetaDataType, List<Long>> ids = new EnumMap<MetaDataType, List<Long>>(MetaDataType.class);
        for (MetaDataType mdt : MetaDataType.values()) {
            ids.put(mdt, new ArrayList());
        }

        Map<String, IndexVideoDTO> results = new HashMap<String, IndexVideoDTO>();

        for (IndexVideoDTO single : queryResults) {
            // Add the item to the map for further processing
            results.put(IndexArtworkDTO.makeKey(single), single);
            // Add the ID to the list
            ids.get(single.getVideoType()).add(single.getId());
        }

        LOG.info("Found IDs: {}", ids);

        generateArtworkList(ids, results);

    }

    private String generateSqlForVideoList(ApiWrapperList<IndexVideoDTO> wrapper) {
        OptionsIndexVideo options = (OptionsIndexVideo) wrapper.getParameters();
        Map<String, String> includes = options.splitIncludes();
        Map<String, String> excludes = options.splitExcludes();

        StringBuilder sbSQL = new StringBuilder();

        // Add the movie entries
        if (options.getType().equals("ALL") || options.getType().equals("MOVIE")) {
            sbSQL.append("SELECT vd.id");
            sbSQL.append(", '").append(MetaDataType.MOVIE).append("' AS videoTypeString");
            sbSQL.append(", vd.title");
            sbSQL.append(", vd.publication_year as videoYear");
            sbSQL.append(" FROM videodata vd");
            // Add genre tables for include and exclude
            if (includes.containsKey("genre") || excludes.containsKey("genre")) {
                sbSQL.append(", videodata_genres vg, genre g");
            }

            sbSQL.append(" WHERE vd.episode < 0");
            // Add joins for genres
            if (includes.containsKey("genre") || excludes.containsKey("genre")) {
                sbSQL.append(" AND vd.id=vg.data_id");
                sbSQL.append(" AND vg.genre_id=g.id");
                sbSQL.append(" AND g.name='");
                if (includes.containsKey("genre")) {
                    sbSQL.append(includes.get("genre"));
                } else {
                    sbSQL.append(excludes.get("genre"));
                }
                sbSQL.append("'");
            }

            if (includes.containsKey("year")) {
                sbSQL.append(" AND vd.publication_year=").append(includes.get("year"));
            }

            if (excludes.containsKey("year")) {
                sbSQL.append(" AND vd.publication_year!=").append(includes.get("year"));
            }
        }

        if (options.getType().equals("ALL")) {
            sbSQL.append(" UNION ");
        }

        // Add the TV entires
        if (options.getType().equals("ALL") || options.getType().equals("TV")) {
            sbSQL.append("SELECT ser.id");
            sbSQL.append(", '").append(MetaDataType.SERIES).append("' AS videoTypeString");
            sbSQL.append(", ser.title");
            sbSQL.append(", ser.start_year as videoYear");
            sbSQL.append(" FROM series ser ");
            sbSQL.append(" WHERE 1=1"); // To make it easier to add the optional include and excludes

            if (includes.containsKey("year")) {
                sbSQL.append(" AND ser.start_year=").append(includes.get("year"));
            }

            if (excludes.containsKey("year")) {
                sbSQL.append(" AND ser.start_year!=").append(includes.get("year"));
            }
        }

        if (StringUtils.isNotBlank(options.getSortby())) {
            sbSQL.append(" ORDER BY ");
            sbSQL.append(options.getSortby()).append(" ");
            sbSQL.append(options.getSortdir().toUpperCase());
        }

        return sbSQL.toString();
    }

    private String generateArtworkList(Map<MetaDataType, List<Long>> ids, Map<String, IndexVideoDTO> artworkList) {
        StringBuilder sbSQL = new StringBuilder();
        boolean hasMovie = CollectionUtils.isNotEmpty(ids.get(MetaDataType.MOVIE));
        boolean hasSeries = CollectionUtils.isNotEmpty(ids.get(MetaDataType.SERIES));

        if (hasMovie) {
            sbSQL.append("SELECT 'MOVIE' as sourceString, v.id as videoId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
            sbSQL.append(" FROM videodata v, artwork a");
            sbSQL.append(" LEFT JOIN artwork_located al ON a.id=al.artwork_id");
            sbSQL.append(" LEFT JOIN artwork_generated ag ON al.id=ag.located_id");
            sbSQL.append(" WHERE v.id=a.videodata_id");
            sbSQL.append(" AND v.episode<0");
            sbSQL.append(" AND v.id IN (:movielist)");
        }

        if (hasMovie && hasSeries) {
            sbSQL.append(" UNION");
        }

        if (hasSeries) {
            sbSQL.append(" SELECT 'SERIES' as sourceString, s.id as videoId, a.id as artworkId, al.id as locatedId, ag.id as generatedId, a.artwork_type as artworkTypeString, ag.cache_dir as cacheDir, ag.cache_filename as cacheFilename");
            sbSQL.append(" FROM series s, artwork a");
            sbSQL.append(" LEFT JOIN artwork_located al ON a.id=al.artwork_id");
            sbSQL.append(" LEFT JOIN artwork_generated ag ON al.id=ag.located_id");
            sbSQL.append(" WHERE s.id=a.series_id");
            sbSQL.append(" AND s.id IN (:serieslist)");
        }

        SQLQuery query = getSession().createSQLQuery(sbSQL.toString());
        query.addScalar("sourceString", StringType.INSTANCE);
        query.addScalar("videoId", LongType.INSTANCE);
        query.addScalar("artworkId", LongType.INSTANCE);
        query.addScalar("locatedId", LongType.INSTANCE);
        query.addScalar("generatedId", LongType.INSTANCE);
        query.addScalar("artworkTypeString", StringType.INSTANCE);
        query.addScalar("cacheDir", StringType.INSTANCE);
        query.addScalar("cacheFilename", StringType.INSTANCE);

        if (hasMovie) {
            query.setParameterList("movielist", ids.get(MetaDataType.MOVIE));
        }

        if (hasSeries) {
            query.setParameterList("serieslist", ids.get(MetaDataType.SERIES));
        }

        List<IndexArtworkDTO> results = executeQueryWithTransform(IndexArtworkDTO.class, query, null, null);

        LOG.trace("Found {} artworks", results.size());
        for (IndexArtworkDTO ia : results) {
            LOG.trace("  {} = {}", ia.Key(), ia.toString());
            artworkList.get(ia.Key()).addArtwork(ia);
        }

        return sbSQL.toString();
    }

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

        Map<String, BasicType> scalars = new HashMap<String, BasicType>();
        scalars.put("typeString", StringType.INSTANCE);
        scalars.put("count", LongType.INSTANCE);
        scalars.put("createTimestamp", TimestampType.INSTANCE);
        scalars.put("updateTimestamp", TimestampType.INSTANCE);
        scalars.put("lastId", LongType.INSTANCE);

        List<CountTimestamp> results = executeQueryWithTransform(CountTimestamp.class, sql.toString(), null, scalars);
        if (CollectionUtils.isEmpty(results)) {
            return new CountTimestamp(type);
        }
        return results.get(0);
    }

    public void getPersonList(ApiWrapperList<IndexPersonDTO> wrapper) {
        StringBuilder sql = new StringBuilder();

        // Make sure to set the alias for the files for the Transformation into the class
        sql.append("SELECT p.id,");
        sql.append(" p.name,");
        sql.append(" p.biography, ");
        sql.append(" p.birth_day as birthDay, ");
        sql.append(" p.birth_place as birthPlace, ");
        sql.append(" p.birth_name as birthName, ");
        sql.append(" p.death_day as deathDay ");
        sql.append(" FROM person p");
        sql.append(" WHERE 1=1");

        Map<String, BasicType> scalars = new HashMap<String, BasicType>();
        scalars.put("id", LongType.INSTANCE);
        scalars.put("birthDay", DateType.INSTANCE);
        scalars.put("birthPlace", DateType.INSTANCE);
        scalars.put("birthName", StringType.INSTANCE);
        scalars.put("deathDay", DateType.INSTANCE);

        List<IndexPersonDTO> results = executeQueryWithTransform(IndexPersonDTO.class, sql.toString(), wrapper, scalars);
        wrapper.setResults(results);
    }

    public List<Configuration> getConfiguration(String property) {
        if (StringUtils.isBlank(property)) {
            LOG.info("Getting all configuration entries");
        } else {
            LOG.info("Getting configuration for {}", property);
        }

        StringBuilder sbSQL = new StringBuilder("FROM org.yamj.core.database.model.Configuration");
        if (StringUtils.isNotBlank(property)) {
            sbSQL.append(" WHERE config_key=").append(property);
        }

        Query q = getSession().createQuery(sbSQL.toString());
        List<Configuration> queryResults = q.list();

        return queryResults;
    }

    /**
     * Execute a query return the results.
     *
     * Puts the total count returned from the query into the wrapper
     *
     * @param sql
     * @param wrapper
     * @return
     */
    private List<Object[]> executeQuery(String sql, IApiWrapper wrapper) {
        return executeQueryWithTransform(Object[].class, sql, wrapper, null);
    }

    private <T> List<T> executeQueryWithTransform(Class T, String sql, IApiWrapper wrapper) {
        return executeQueryWithTransform(T, sql, wrapper, null);
    }

    private <T> List<T> executeQueryWithTransform(Class T, String sql, IApiWrapper wrapper, Map<String, BasicType> scalars) {
        SQLQuery query = getSession().createSQLQuery(sql);
        return executeQueryWithTransform(T, query, wrapper, scalars);
    }

    /**
     * Execute a query return the results
     *
     * Puts the total count returned from the query into the wrapper
     *
     * @param T The class to return the transformed results of.
     * @param sql
     * @param wrapper
     * @param scalars
     * @return
     */
    private <T> List<T> executeQueryWithTransform(Class T, SQLQuery query, IApiWrapper wrapper, Map<String, BasicType> scalars) {
        query.setReadOnly(true);
        query.setCacheable(true);

        // Add a transformation if the class is not "Object"
        if (T != null && !T.equals(Object[].class)) {
            query.setResultTransformer(Transformers.aliasToBean(T));
        }

        // Add any required scalars
        if (scalars != null && !scalars.isEmpty()) {
            for (Map.Entry<String, BasicType> entry : scalars.entrySet()) {
                if (entry.getValue() == null) {
                    query.addScalar(entry.getKey());
                } else {
                    query.addScalar(entry.getKey(), entry.getValue());
                }
            }
        }

        List<T> queryResults = query.list();
        if (wrapper != null) {
            wrapper.setTotalCount(queryResults.size());

            // If there is a start or max set, we will need to re-run the query after setting the options
            IOptions options = (IOptions) wrapper.getParameters();
            if (options.getStart() > 0 || options.getMax() > 0) {
                if (options.getStart() > 0) {
                    query.setFirstResult(options.getStart());
                }

                if (options.getMax() > 0) {
                    query.setMaxResults(options.getMax());
                }
                // This will get the trimmed list
                queryResults = query.list();
            }
        }

        return queryResults;
    }
}
