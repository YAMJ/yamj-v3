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

/**
 * The meta data type used for the data scanning in the database
 *
 * @author stuart.boston
 */
public enum MetaDataType {

    /**
     * This is a movie
     */
    MOVIE(true),
    /**
     * This is a TV Series
     */
    SERIES(true),
    /**
     * This is a season of a TV series
     */
    SEASON(true),
    /**
     * This is an episode of a TV season
     */
    EPISODE(true),
    /**
     * This is a person, an actor or crew member
     */
    PERSON(true),
    /**
     * This is a person filmography
     */
    FILMOGRAPHY(false),
    /**
     * This is a boxed set
     */
    BOXSET(false),
    /**
     * The type is unknown
     */
    UNKNOWN(false);

    private final boolean realMetaData;

    private MetaDataType(boolean realMetaData) {
        this.realMetaData = realMetaData;
    }

    public static MetaDataType fromString(String type) {
        try {
            return MetaDataType.valueOf(type.trim().toUpperCase());
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }

    public boolean isRealMetaData() {
        return realMetaData;
    }
}
