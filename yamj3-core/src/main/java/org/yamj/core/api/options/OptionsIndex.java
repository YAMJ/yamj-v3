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
package org.yamj.core.api.options;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * List of the options available for the indexes
 *
 * @author stuart.boston
 */
public class OptionsIndex extends OptionsAbstract {

    private String type = "ALL";
    private String include = "";
    private String exclude = "";
    private String sortBy = "";

    public void setInclude(String include) {
        this.include = include;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getInclude() {
        return include;
    }

    public String getExclude() {
        return exclude;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (StringUtils.containsIgnoreCase(type, "MOVIE")
                || StringUtils.containsIgnoreCase(type, "TV")
                || StringUtils.containsIgnoreCase(type, "ALL")) {
            this.type = type.toUpperCase();
        } else {
            this.type = "ALL";
        }
    }

}
