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
package org.yamj.core.api.wrapper;

import org.joda.time.DateTime;
import org.yamj.common.model.YamjInfo;
import org.yamj.common.tools.DateTimeTools;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.options.IOptions;

/**
 *
 * @author Stuart
 */
public abstract class ApiWrapperAbstract implements IApiWrapper {

    private int count = 0;
    private int totalCount = 0;
    private long queryDuration = 0L;
    private DateTime queryTime = DateTime.now();
    private ApiStatus status = new ApiStatus();
    private IOptions options = null;
    private String baseArtworkUrl = "";
    private String baseMediainfoUrl = "";
    private String basePhotoUrl = "";

    public ApiWrapperAbstract() {
        YamjInfo yi = new YamjInfo(ApiWrapperAbstract.class);
        baseArtworkUrl = yi.getBaseArtworkUrl();
        baseMediainfoUrl = yi.getBaseMediainfoUrl();
        basePhotoUrl = yi.getBasePhotoUrl();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String getQueryTime() {
        return DateTimeTools.convertDateToString(queryTime, DateTimeTools.BUILD_FORMAT);
    }

    @Override
    public ApiStatus getStatus() {
        return status;
    }

    @Override
    public IOptions getOptions() {
        return options;
    }

    @Override
    public String getQueryDuration() {
        StringBuilder dur = new StringBuilder();
        dur.append(queryDuration);
        dur.append("ms");
        return dur.toString();
    }

    @Override
    public int getTotalCount() {
        return totalCount;
    }

    @Override
    public String getBaseArtworkUrl() {
        return baseArtworkUrl;
    }

    @Override
    public String getBaseMediainfoUrl() {
        return baseMediainfoUrl;
    }

    @Override
    public String getBasePhotoUrl() {
        return basePhotoUrl;
    }

    @Override
    public void setQueryTime(DateTime queryTime) {
        this.queryTime = queryTime;
    }

    @Override
    public void setQueryEnd() {
        DateTime duration = DateTime.now().minus(queryTime.getMillis());
        this.queryDuration = duration.getMillis();
    }

    @Override
    public void setStatus(ApiStatus status) {
        this.status = status;
    }

    /**
     * Shorthand method to create a default "OK" status or "FAIL" status
     *
     * Also marks the end of the query
     */
    @Override
    public abstract void setStatusCheck();

    /**
     * Set the status to a specific value
     *
     * Also marks the end of the query
     *
     * @param status
     */
    @Override
    public void setStatusCheck(ApiStatus status) {
        setQueryEnd();
        setStatus(status);
    }

    @Override
    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public void setOptions(IOptions options) {
        this.options = options;
    }

    @Override
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
