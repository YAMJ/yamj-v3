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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Abstract class for the query options
 *
 * @author stuart.boston
 */
public abstract class OptionsAbstract implements IOptions {

    private static final String DEFAULT_SPLIT = ",|;";  // Used for splitting strings
    private Integer start = -1;
    private Integer max = -1;

    @Override
    public void setStart(Integer start) {
        this.start = start;
    }

    @Override
    public void setMax(Integer max) {
        this.max = max;
    }

    @Override
    public Integer getStart() {
        return start;
    }

    @Override
    public Integer getMax() {
        return max;
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
    protected Map<String, String> splitDashList(String dashList) {
        Map<String, String> values = new HashMap<String, String>();
        for (String inc : StringUtils.split(dashList, ",")) {
            int pos = inc.indexOf('-');
            if (pos >= 0) {
                values.put(inc.substring(0, pos), inc.substring(pos + 1));
            }
        }
        return values;
    }

    /**
     * Split a list using the default split characters of ",|;"
     * @param list
     * @return
     */
    protected List<String> splitList(String list) {
        return Arrays.asList(StringUtils.split(list, DEFAULT_SPLIT));
    }
}
