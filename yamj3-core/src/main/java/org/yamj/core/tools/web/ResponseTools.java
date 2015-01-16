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
package org.yamj.core.tools.web;

import org.yamj.api.common.http.DigestedResponse;

public final class ResponseTools {

    private ResponseTools() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isOK(final DigestedResponse response) {
        return isOK(response.getStatusCode());
    }

    public static boolean isOK(final int statusCode) {
        return (statusCode == 200);
    }

    public static boolean isNotOK(final DigestedResponse response) {
        return !isOK(response);
    }

    public static boolean isNotOK(final int statusCode) {
        return !isOK(statusCode);
    }

    public static boolean isTemporaryError(final DigestedResponse response) {
        return isTemporaryError(response.getStatusCode());
    }

    public static boolean isTemporaryError(final int statusCode) {
        switch (statusCode) {
            case 408:
            case 419:
            case 500:
            case 502:
            case 503:
            case 504:
            case 509:
            case 598:
            case 599:
                return true;
            default:
                return false;
        }
    }
}
