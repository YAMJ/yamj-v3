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
package org.yamj.common.type;

public enum StatusType {

    NEW,            // new record
    UPDATED,        // updated record
    DELETED,        // deleted record
    ERROR,          // something went wrong
    NOTFOUND,       // something hasn't been found
    INVALID,        // invalid record
    DUPLICATE,      // duplicate record
    TEMP_DONE,      // temporary done
    IGNORE,         // ignored entry
    DONE;           // all is done
    
    public static StatusType fromString(String type) {
        try {
            return StatusType.valueOf(type.trim().toUpperCase());
        } catch (Exception ex) {
            return NEW;
        }
    }
}
