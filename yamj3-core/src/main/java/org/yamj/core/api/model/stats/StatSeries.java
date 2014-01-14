/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package org.yamj.core.api.model.stats;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.yamj.core.api.model.dto.ApiSeriesInfoDTO;

/**
 * Provides statistics for a TV series
 *
 * @author stuart.boston
 */
public class StatSeries extends StatList<ApiSeriesInfoDTO> {

    @Override
    public void setItems(List<ApiSeriesInfoDTO> items) {
        super.setItems(items);

        if (CollectionUtils.isNotEmpty(items)) {
            // Set the default oldest and newest
            setFirst(items.get(0));
            setLast(items.get(0));

            for (ApiSeriesInfoDTO series : items) {
                if (series.getYear() > getLast().getYear()) {
                    setLast(series);
                }
                if (series.getYear() < getFirst().getYear()) {
                    setFirst(series);
                }
            }
        }
    }

    /**
     * Get the range of years for the list of series
     * @return
     */
    public String getRange() {
        StringBuilder range = new StringBuilder();
        boolean separator = false;

        if (getFirst() != null) {
            range.append(getFirst().getYear());
            separator = true;
        }

        if (getLast() != null) {
            if (separator) {
                range.append("-");
            }

            range.append(getLast().getYear());
        }

        return range.toString();
    }
}
