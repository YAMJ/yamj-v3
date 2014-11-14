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
package org.yamj.core.api.model.builder;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Stuart
 */
public class DataItemTools {

    private static final Logger LOG = LoggerFactory.getLogger(DataItemTools.class);

    /**
     * Create a fragment of SQL from the list of data items
     *
     * @param dataItems
     * @param tablePrefix
     * @return
     */
    public static StringBuilder addSqlDataItems(List<DataItem> dataItems, String tablePrefix) {
        StringBuilder sbSQL = new StringBuilder();

        if (CollectionUtils.isNotEmpty(dataItems)) {
            LOG.trace("Adding dataitems {} to table prefix {}", dataItems, tablePrefix);
            for (DataItem item : dataItems) {
                if (item.isNotColumn()) {
                    // This is not a specific SQL statement and is not needed
                    continue;
                }
                if (item == DataItem.TOP_RANK) {
                    sbSQL.append(", ").append(tablePrefix).append(".top_rank as topRank");
                    continue;
                }
                // Default approach
                sbSQL.append(", ").append(tablePrefix).append(".").append(item.toString().toLowerCase());
            }
        } else {
            LOG.trace("No dataitems to add to table prefix {}", tablePrefix);
        }
        LOG.trace("Added '{}' to SQL statement", sbSQL);
        return sbSQL;
    }

    /**
     * Add scalars for the data items
     *
     * @param sqlScalars
     * @param dataItems
     */
    public static void addDataItemScalars(SqlScalars sqlScalars, List<DataItem> dataItems) {
        for (DataItem item : dataItems) {
            if (item.isNotColumn()) {
                // This is not a specific scalar and is not needed
                continue;
            }
            if (item == DataItem.TOP_RANK) {
                sqlScalars.addScalar("topRank", IntegerType.INSTANCE);
                continue;
            }
            // This is the default approach
            sqlScalars.addScalar(item.toString().toLowerCase(), StringType.INSTANCE);
        }
    }
}
