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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Abstract class for the query options
 *
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT)
public abstract class OptionsAbstract implements IOptions {

    private static final String DEFAULT_SPLIT = ",|;";  // Used for splitting strings
    private Integer start = -1;
    private Integer max = -1;
    @JsonIgnore
    private Integer page = -1;
    @JsonIgnore
    private Integer line = -1;
    @JsonIgnore
    private Integer perpage = -1;
    @JsonIgnore
    private Integer perline = -1;
    private String language;

    @Override
    public void setStart(Integer start) {
        this.start = start;
    }

    @Override
    public void setMax(Integer max) {
        this.max = max;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public void setPerpage(Integer perpage) {
        this.perpage = perpage;
    }

    public void setPerline(Integer perline) {
        this.perline = perline;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public Integer getStart() {
        int value = start;
        if (start < 0 && page >= 0) {
            // Calculate the start page
            value = (page > 0 ? page - 1 : 0) * perpage;
            // Add the start line (if required)
            value += (line > 0 ? line - 1 : 0) * perline;
        }
        return value;
    }

    @Override
    public Integer getMax() {
        int value = max;
        // Check to see if one of the "pers" is set
        if (max < 0 && (perpage > 0 || perline > 0)) {
            if (line > 0) {
                value = perline;
            } else {
                value = perpage;
            }
        }
        return value;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getPerpage() {
        return perpage;
    }

    public Integer getPerline() {
        return perline;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    /**
     * Split a comma separated list of "key-value" items into a Map
     *
     * @param dashList
     * @return
     */
    protected static Map<String, String> splitDashList(String dashList) {
        Map<String, String> values = new HashMap<String, String>();
        if (dashList != null) {
            for (String inc : StringUtils.split(dashList, ",")) {
                int pos = inc.indexOf('-');
                if (pos >= 0) {
                    values.put(inc.substring(0, pos).toLowerCase(), inc.substring(pos + 1).toLowerCase());
                }
            }
        }
        return values;
    }

    /**
     * Split a list using the default split characters of ",|;"
     *
     * @param list
     * @return
     */
    protected static List<String> splitList(String list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(StringUtils.split(list, DEFAULT_SPLIT));
    }
}
