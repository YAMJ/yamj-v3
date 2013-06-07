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
package org.yamj.core.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.yamj.common.tools.DateTimeTools;

/**
 * Default wrapper for a list returned from the API
 *
 * @author stuart.boston
 */
public final class ListWrapper<T> {

    private List<T> results = Collections.EMPTY_LIST;
    private Parameters parameters = new Parameters();
    private int count = 0;
    private DateTime queryTime = DateTime.now();

    //<editor-fold defaultstate="collapsed" desc="Getter Methods">
    public List<T> getResults() {
        return results;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public int getCount() {
        return count;
    }

    public String getQueryTime() {
        return DateTimeTools.convertDateToString(queryTime, DateTimeTools.BUILD_FORMAT);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Setter Methods">
    public void setResults(List<T> results) {
        this.results = results;

        // Add the list's size
        if (CollectionUtils.isNotEmpty(results)) {
            this.count = results.size();
        } else {
            this.count = 0;
        }
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public void setQueryTime(DateTime queryTime) {
        this.queryTime = queryTime;
    }
    //</editor-fold>
}
