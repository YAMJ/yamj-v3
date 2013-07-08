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
package org.yamj.core.api.model;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.common.tools.DateTimeTools;
import org.yamj.common.type.MetaDataType;

/**
 * Used to hold the count and timestamps for a table query
 *
 * @author stuart.boston
 */
public class CountTimestamp {

    private MetaDataType type;
    private long count = 0L;
    private Date createTimestamp = new Date(0);
    private String createTimestampString = "";
    private Date updateTimestamp = new Date(0);
    private String updateTimestampString = "";
    private long lastId = 0L;

    public CountTimestamp() {
    }

    public CountTimestamp(MetaDataType type) {
        this.type = type;
    }

    public MetaDataType getType() {
        return type;
    }

    public void setTypeString(String type) {
        this.type = MetaDataType.fromString(type);
    }

    public void setType(MetaDataType type) {
        this.type = type;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    public String getCreateTimestampString() {
        return createTimestampString;
    }

    public void setCreateTimestamp(Date createTimestamp) {
        this.createTimestamp = createTimestamp;
        this.createTimestampString = DateTimeTools.convertDateToString(createTimestamp, DateTimeTools.BUILD_FORMAT);
    }

    public Date getUpdateTimestamp() {
        return updateTimestamp;
    }

    public String getUpdateTimestampString() {
        return updateTimestampString;
    }

    public void setUpdateTimestamp(Date updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
        this.updateTimestampString = DateTimeTools.convertDateToString(updateTimestamp, DateTimeTools.BUILD_FORMAT);
    }

    public long getLastId() {
        return lastId;
    }

    public void setLastId(long lastId) {
        this.lastId = lastId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
