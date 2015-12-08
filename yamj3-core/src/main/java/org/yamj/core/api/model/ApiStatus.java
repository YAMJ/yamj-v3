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
package org.yamj.core.api.model;

import org.apache.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ApiStatus {

    public static final ApiStatus OK = new ApiStatus("OK");
    public static final ApiStatus NO_RECORD = new ApiStatus(HttpStatus.SC_BAD_REQUEST, "No record found");
    public static final ApiStatus INVALID_ID = new ApiStatus(HttpStatus.SC_GONE, "Not a valid ID");
    
    private final int status;
    private final String message;

    public ApiStatus(String message) {
        this.status = HttpStatus.SC_OK;
        this.message = message;
    }

    public ApiStatus(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @JsonIgnore
    public boolean isSuccessful() {
        return (status == HttpStatus.SC_OK);
    }
}
