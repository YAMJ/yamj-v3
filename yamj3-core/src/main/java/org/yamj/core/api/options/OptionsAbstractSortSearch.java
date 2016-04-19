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
package org.yamj.core.api.options;

import static org.yamj.plugin.api.common.Constants.ALL;
import static org.yamj.plugin.api.common.Constants.DEFAULT_SPLITTER;
import org.yamj.plugin.api.common.JobType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.model.builder.DataItem;
/**
 * Abstract class for the query options
 *
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT)
public abstract class OptionsAbstractSortSearch extends OptionsAbstract implements IOptionsSort, IOptionsSearch {

    private String sortby;
    private String sortdir;
    private String artworksortdir;
    private String field;
    private String search;
    // TODO: Change this to MatchMode
    private String mode;

    private List<String> dataitems;
    private List<String> jobs;

    @JsonIgnore
    private List<DataItem> dataitemList;
    @JsonIgnore
    private Map<JobType, Integer> jobTypes;
    @JsonIgnore
    private Set<MetaDataType> metaDataTypes;
    @JsonIgnore
    private boolean allJobTypes;

    //<editor-fold defaultstate="collapsed" desc="Sort Setters/Getters">
    /**
     * Get field to sort on
     *
     * @return
     */
    @Override
    public String getSortby() {
        return sortby;
    }

    /**
     * Set field to sort on
     *
     * @param sortby
     */
    @Override
    public void setSortby(String sortby) {
        this.sortby = sortby;
    }

    /**
     * Get the sort direction
     *
     * @return
     */
    @Override
    public String getSortdir() {
        return sortdir;
    }

    /**
     * Set the sort direction
     *
     * @param sortdir
     */
    @Override
    public void setSortdir(String sortdir) {
        this.sortdir = sortdir;
    }

    /**
     * Get the sort direction for artwork
     *
     * @return
     */
    @Override
    public String getArtworksortdir() {
        return artworksortdir;
    }

    /**
     * Set the sort direction for artwork
     *
     * @param artworksortdir
     */
    @Override
    public void setArtworksortdir(String artworksortdir) {
        this.artworksortdir = artworksortdir;
    }

    /**
     * Get the sort string to append to the SQL statement
     *
     * @return
     */
    @JsonIgnore
    @Override
    public String getSortString() {
        return getSortString(sortby);
    }

    /**
     * Get the sort string to append to the SQL statement
     *
     * @param sortBy
     * @return
     */
    @JsonIgnore
    @Override
    public String getSortString(String sortBy) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(sortBy)) {
            sb.append(" ORDER BY ");
            sb.append(sortBy);
            
            if (!StringUtils.containsIgnoreCase(sortBy, " ASC") && !StringUtils.containsIgnoreCase(sortBy, " DESC")) {  
                if ("DESC".equalsIgnoreCase(sortdir)) {
                    sb.append(" DESC");
                } else {
                    sb.append(" ASC");
                }
            }
        }
        return sb.toString();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Search Setters/Getters">
    /**
     * Get the field to search on
     *
     * @return
     */
    @Override
    public String getField() {
        return field;
    }

    /**
     * Set the field to search on
     *
     * @param field
     */
    @Override
    public void setField(String field) {
        this.field = field;
    }

    /**
     * Get the search term to use
     *
     * @return
     */
    @Override
    public String getSearch() {
        return search;
    }

    /**
     * Set the search term
     *
     * @param search
     */
    @Override
    public void setSearch(String search) {
        this.search = search;
    }

    /**
     * Get the match mode
     *
     * @return
     */
    @Override
    public String getMode() {
        return mode;
    }

    /**
     * Set the match mode
     *
     * @param mode
     */
    @Override
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Get the search string to append to the SQL statement.
     *
     * @param addWhere Add "WHERE" to the statement (true) or "AND" (false)
     * @return
     */
    @JsonIgnore
    @Override
    public String getSearchString(boolean addWhere) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(field) && StringUtils.isNotBlank(search)) {
            if (addWhere) {
                sb.append(" WHERE lower(");
            } else {
                sb.append(" AND lower(");
            }
            sb.append(field).append(") LIKE '");

            if (StringUtils.equalsIgnoreCase("START", mode)) {
                sb.append(StringEscapeUtils.escapeSql(search).toLowerCase()).append("%");
            } else if (StringUtils.equalsIgnoreCase("END", mode)) {
                sb.append("%").append(StringEscapeUtils.escapeSql(search).toLowerCase());
            } else if (StringUtils.equalsIgnoreCase("EXACT", mode)) {
                sb.append(StringEscapeUtils.escapeSql(search).toLowerCase());
            } else {
                // Default to ANY
                sb.append("%").append(StringEscapeUtils.escapeSql(search).toLowerCase()).append("%");
            }
            sb.append("'");
        }
        return sb.toString();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="DataItem Methods">
    public List<String> getDataitems() {
        return dataitems;
    }

    public void setDataitems(List<String> dataitems) {
        this.dataitems = dataitems;
        this.dataitemList = null;
    }

    public List<DataItem> splitDataItems() {
        if (dataitemList == null) {
            dataitemList = new ArrayList<>();
            if (dataitems != null) {
                for (String item : dataitems) {
                    DataItem di = DataItem.fromString(item);
                    if (di != DataItem.UNKNOWN) {
                        dataitemList.add(di);
                    }
                }
            }
        }
        return dataitemList;
    }

    public boolean hasDataItem(DataItem di) {
        return splitDataItems().contains(di);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Jobs Methods">
    public List<String> getJobs() {
        return jobs;
    }

    public void setJobs(List<String> jobs) {
        this.jobs = jobs;
        this.jobTypes = null;
    }

    public boolean isAllJobTypes() {
        return this.allJobTypes;
    }

    public Map<JobType, Integer> splitJobs() {
        if (jobTypes != null) {
            return jobTypes;
        }
        
        jobTypes = new EnumMap<>(JobType.class);
        if (CollectionUtils.isNotEmpty(jobs)) {
            for (String job : jobs) {
                if (ALL.equalsIgnoreCase(job)) {
                    allJobTypes = true;
                    jobTypes.clear();
                    break;
                }

                String[] vals = StringUtils.split(job, "-");

                if (vals.length > 0) {
                    JobType jobType = JobType.fromString(vals[0]);
                    Integer amount = null;
                    
                    if (vals.length > 1) {
                        amount = NumberUtils.toInt(vals[1], -1);
                        if (amount <= 0) {
                            // ignore jobs with a given amount <= 0
                            jobType = JobType.UNKNOWN;
                        }
                    }
                    
                    if (jobType != JobType.UNKNOWN) {
                        jobTypes.put(jobType, amount);
                    }
                }
            }
        }
        return jobTypes;
    }

    @JsonIgnore
    public Set<String> getJobTypes() {
        Set<String> set = new HashSet<>();
        for (JobType jobType : this.splitJobs().keySet()) {
            set.add(jobType.toString());
        }
        return set;
    }
    //</editor-fold>

    protected Set<MetaDataType> getMetaDataTypes(String type) {
        HashSet<MetaDataType> types = new  HashSet<>();
        if (StringUtils.isEmpty(type) || StringUtils.containsIgnoreCase(type, ALL)) {
            types.addAll(Arrays.asList(MetaDataType.values()));
        } else {
            for (String param : StringUtils.split(type, DEFAULT_SPLITTER)) {
                MetaDataType mdt = MetaDataType.fromString(param);
                if (mdt.isRealMetaData()) {
                    types.add(mdt);
                }
            }
        }

        return types;
    }

    /**
     * Get a list of the meta data types to search for
     *
     * Note: This limits the ALL/blank type to Movie, Series & Season
     *
     * @param type
     * @return
     */
    protected Set<MetaDataType> splitTypes(String type) {
        if (CollectionUtils.isEmpty(metaDataTypes)) {
            metaDataTypes = new HashSet<>();
            if (StringUtils.isEmpty(type) || StringUtils.containsIgnoreCase(type, ALL)) {
                metaDataTypes.add(MetaDataType.MOVIE);
                metaDataTypes.add(MetaDataType.SERIES);
                metaDataTypes.add(MetaDataType.SEASON);
            } else {
                for (String param : StringUtils.split(type, DEFAULT_SPLITTER)) {
                    // validate that the string passed is a correct type
                    MetaDataType mdt = MetaDataType.fromString(param);
                    if (mdt.isRealMetaData()) {
                        metaDataTypes.add(mdt);
                    }
                }
            }
        }
        return metaDataTypes;
    }
}
