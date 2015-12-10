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

/**
 * The meta data type used for the data scanning in the database
 *
 * @author stuart.boston
 */
public enum MetaDataType {

    /**
     * This is a movie
     */
    MOVIE(true, true, true, true, true),
    /**
     * This is a TV Series
     */
    SERIES(true, true, true, true, true),
    /**
     * This is a season of a TV series
     */
    SEASON(true, true, true, true, false),
    /**
     * This is an episode of a TV season
     */
    EPISODE(true, true, true, true, false),
    /**
     * This is a person, an actor or crew member
     */
    PERSON(true, true, false, true, false),
    /**
     * This is a person filmography
     */
    FILMOGRAPHY(false, true, false, false, false),
    /**
     * This is a boxed set
     */
    BOXSET(false, false, false, true, false),
    /**
     * The type is unknown
     */
    UNKNOWN(false, false, false, false, false);

    private final boolean realMetaData;
    private final boolean rescanMetaData;
    private final boolean withVideos;
    private final boolean withArtwork;
    private final boolean withTrailer;
    
    private MetaDataType(boolean realMetaData, boolean rescanMetaData, boolean withVideos, boolean withArtwork, boolean withTrailer) {
        this.realMetaData = realMetaData;
        this.rescanMetaData = rescanMetaData;
        this.withVideos = withVideos;
        this.withArtwork = withArtwork;
        this.withTrailer = withTrailer; 
    }

    public static MetaDataType fromString(String type) {
        try {
            return MetaDataType.valueOf(type.trim().toUpperCase());
        } catch (Exception ex) { //NOSONAR
            return UNKNOWN;
        }
    }

    public boolean isRealMetaData() {
        return realMetaData;
    }
    
    public boolean isRescanMetaData() {
        return rescanMetaData;
    }

    public boolean isWithVideos() {
        return withVideos;
    }

    public boolean isWithArtwork() {
        return withArtwork;
    }

    public boolean isWithTrailer() {
        return withTrailer;
    }
}
