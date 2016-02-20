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
package org.yamj.core.api.wrapper;

import org.joda.time.DateTime;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.options.IOptions;

/**
 *
 * @author stuart.boston
 */
public interface IApiWrapper {

    int getCount();

    String getQueryTime();

    ApiStatus getStatus();

    IOptions getOptions();

    String getQueryDuration();

    int getTotalCount();

    String getBaseArtworkUrl();

    String getBaseMediainfoUrl();

    String getBasePhotoUrl();

    String getBaseTrailerUrl();

    void setQueryTime(DateTime queryTime);

    void setQueryEnd();

    void setStatusCheck(ApiStatus status);

    void setCount(int count);

    void setOptions(IOptions options);

    void setTotalCount(int totalCount);
}
