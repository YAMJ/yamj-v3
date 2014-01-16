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
package org.yamj.core.api.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.api.model.builder.DataItem;

/**
 * Abstract class for the query options
 *
 * @author stuart.boston
 */
public abstract class OptionsAbstractSortSearch extends OptionsAbstract implements IOptionsSort, IOptionsSearch {

    private String sortby = "";
    private String sortdir = "ASC";
    private String field = "";
    private String search = "";
    private String mode = "";
    private List<String> dataitems = new ArrayList<String>();
    @JsonIgnore
    private final List<DataItem> dataitemList = new ArrayList<DataItem>();

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
     * Get the sort string to append to the SQL statement
     *
     * @return
     */
    @JsonIgnore
    @Override
    public String getSortString() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(sortby)) {
            sb.append(" ORDER BY ").append(sortby);
            sb.append(" ").append(sortdir);
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
                sb.append(" WHERE ");
            } else {
                sb.append(" AND ");
            }
            sb.append(field).append(" LIKE '");

            if (StringUtils.equalsIgnoreCase("START", mode)) {
                sb.append(search).append("%");
            } else if (StringUtils.equalsIgnoreCase("END", mode)) {
                sb.append("%").append(search);
            } else if (StringUtils.equalsIgnoreCase("EXACT", mode)) {
                sb.append(search);
            } else {
                // Default to ANY
                sb.append("%").append(search).append("%");
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
        genarateDataItemsList();
    }

    private void genarateDataItemsList() {
        dataitemList.clear();
        for (String item : dataitems) {
            DataItem di = DataItem.fromString(item);
            if (di != DataItem.UNKNOWN) {
                dataitemList.add(di);
            }
        }
    }

    /**
     * Split the additionalDataItems into a list of DataItems
     *
     * @return
     */
    public List<DataItem> splitDataitems() {
        return dataitemList;
    }

    public boolean hasDataItem(DataItem di) {
        return dataitemList.contains(di);
    }
    //</editor-fold>
}
