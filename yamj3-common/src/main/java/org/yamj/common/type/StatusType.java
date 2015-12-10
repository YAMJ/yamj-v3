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
package org.yamj.common.type;

public enum StatusType {

    /**
     * new record
     */
    NEW,
    /**
     * updated record
     */
    UPDATED,
    /**
     * deleted record
     */
    DELETED,
    /**
     * something went wrong
     */
    ERROR,
    /**
     * something hasn't been found
     */
    NOTFOUND,
    /**
     * invalid record
     */
    INVALID,
    /**
     * duplicate record
     */
    DUPLICATE,
    /**
     * temporary done
     */
    TEMP_DONE,
    /**
     * ignored entry
     */
    IGNORE,
    /**
     * all is done
     */
    DONE;

    /**
     * Get the type from a String.
     *
     * Returns NEW by default
     *
     * @param type
     * @return
     */
    public static StatusType fromString(String type) {
        try {
            return StatusType.valueOf(type.trim().toUpperCase());
        } catch (Exception ex) { //NOSONAR
            return NEW;
        }
    }
}
