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

/**
 * List of the options available for the indexes
 *
 * @author stuart.boston
 */
public class OptionsIndexPerson extends OptionsAbstract implements IOptionsSort {

    private String sortby = "";
    private String sortdir = "ASC";
    private Long id = -1L;

    @Override
    public String getSortby() {
        return sortby;
    }

    @Override
    public void setSortby(String sortby) {
        this.sortby = sortby;
    }

    @Override
    public String getSortdir() {
        return sortdir;
    }

    @Override
    public void setSortdir(String sortdir) {
        this.sortdir = sortdir;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getSortString() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(sortby)) {
            sb.append(" ORDER BY ").append(sortby);
            sb.append(" ").append(sortdir);
        }
        return sb.toString();
    }
}
