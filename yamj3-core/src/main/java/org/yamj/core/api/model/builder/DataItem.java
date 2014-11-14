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

/**
 * List of columns to add to the output for the API
 *
 * @author Stuart
 */
public enum DataItem {

    ARTWORK(false),
    PLOT(true),
    OUTLINE(true),
    COUNTRY(true),
    QUOTE(true),
    TAGLINE(true),
    TOP_RANK(true),
    GENRE(false),
    STUDIO(false),
    CERTIFICATION(false),
    RATING(false),
    BIOGRAPHY(true),
    FILMOGRAPHY_INSIDE(false),
    FILMOGRAPHY_SCANNED(false),
    FILES(false),
    UNKNOWN(false);
    
    private final boolean column; // Is the DataItem a column or a collection (e.g. artwork, genres, etc)

    DataItem(boolean isColumn) {
        this.column = isColumn;
    }

    public boolean isColumn() {
        return column;
    }

    public boolean isNotColumn() {
        return !isColumn();
    }

    public static DataItem fromString(String item) {
        try {
            String cleanItem = item.trim().toUpperCase();
            return DataItem.valueOf(cleanItem);
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }
}
