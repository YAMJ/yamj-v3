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
package org.yamj.core.api.wrapper;

import java.util.Collection;
import java.util.Collections;
import org.apache.commons.collections.CollectionUtils;
import org.yamj.core.api.model.ApiStatus;

/**
 * Default wrapper for a list returned from the API
 *
 * @author stuart.boston
 * @param <T>
 */
public final class ApiWrapperList<T> extends ApiWrapperAbstract {

    private Collection<T> results = Collections.emptyList();

    public ApiWrapperList() {
        super();
    }

    public Collection<T> getResults() {
        return results;
    }

    public void setResults(Collection<T> results) {
        this.results = results;

        // Add the list's size
        if (CollectionUtils.isNotEmpty(results)) {
            setCount(results.size());
            if (getCount() > getTotalCount()) {
                setTotalCount(getCount());
            }
        } else {
            setCount(0);
            setTotalCount(0);
        }
    }

    @Override
    public void setStatusCheck() {
        setQueryEnd();
        if (CollectionUtils.isEmpty(results)) {
            setStatus(new ApiStatus(400, "No records found"));
        } else {
            setStatus(new ApiStatus(200, "OK"));
        }
    }
}
