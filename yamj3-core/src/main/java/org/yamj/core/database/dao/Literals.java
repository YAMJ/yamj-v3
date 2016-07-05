/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General protected License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General protected License for more details.
 *
 *      You should have received a copy of the GNU General protected License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.dao;

final class Literals {

    // LITERALS
    protected static final String LITERAL_IDENTIFIER = "identifier";
    protected static final String LITERAL_ID = "id";
    protected static final String LITERAL_NAME = "name";
    protected static final String LITERAL_ARTWORK_TYPE = "artworkType";
    protected static final String LITERAL_FILE_TYPE = "fileType";
    protected static final String LITERAL_WATCHED = "watched";
    protected static final String LITERAL_SOURCE = "source";
    protected static final String LITERAL_EXTRA = "extra";
    protected static final String LITERAL_STATUS = "status";
    protected static final String LITERAL_CACHE_FILENAME = "cacheFilename";
    protected static final String LITERAL_TYPE = "type";
    protected static final String LITERAL_YEAR = "year";
    protected static final String LITERAL_TITLE = "title";
    protected static final String LITERAL_EPISODE = "episode";
    protected static final String LITERAL_SEASON = "season";
    protected static final String LITERAL_SEASON_ID = "seasonId";
    protected static final String LITERAL_SERIES_ID = "seriesId";
    protected static final String LITERAL_VIDEODATA_ID = "videodataId";
    protected static final String LITERAL_VIDEO_YEAR = "videoYear";
    protected static final String LITERAL_RELEASE_DATE = "releaseDate";
    protected static final String LITERAL_ORIGINAL_TITLE = "originalTitle";
    protected static final String LITERAL_SORT_TITLE = "sortTitle";
    protected static final String LITERAL_CACHE_DIR = "cacheDir";
    protected static final String LITERAL_VIDEO_TYPE = "videoType";               
    protected static final String LITERAL_JOB = "job";
    protected static final String LITERAL_CREATION = "creation";
    protected static final String LITERAL_LASTSCAN = "lastscan";
    protected static final String LITERAL_NEWEST_DATE = "newestDate";
    protected static final String LITERAL_ARTWORK_ID = "artworkId";
    protected static final String LITERAL_LOCATED_ID = "locatedId";
    protected static final String LITERAL_GENERATED_ID = "generatedId";
    protected static final String LITERAL_VOICE_ROLE = "voiceRole";
    protected static final String LITERAL_FIRST_NAME = "firstName";
    protected static final String LITERAL_LAST_NAME = "lastName"; 
    protected static final String LITERAL_BIRTH_DAY = "birthDay";
    protected static final String LITERAL_BIRTH_PLACE = "birthPlace";
    protected static final String LITERAL_BIRTH_NAME = "birthName"; 
    protected static final String LITERAL_DEATH_DAY = "deathDay"; 
    protected static final String LITERAL_DEATH_PLACE = "deathPlace"; 
    protected static final String LITERAL_METADATA_TYPE = "metaDataType";
    protected static final String LITERAL_COUNTRY_CODE = "countryCode";
    protected static final String LITERAL_CERTIFICATE = "certificate";
    protected static final String LITERAL_PLOT = "plot";
    protected static final String LITERAL_OUTLINE = "outline";
    protected static final String LITERAL_COMBINED = "combined";
    
    // SQL
    protected static final String SQL_UNION = " UNION ";
    protected static final String SQL_UNION_ALL = " UNION ALL ";
    protected static final String SQL_AS_VIDEO_TYPE = "' AS videoType";
    protected static final String SQL_COMMA_SPACE_QUOTE = ", '";
    protected static final String SQL_ARTWORK_TYPE_IN_ARTWORKLIST = " AND a.artwork_type IN (:artworklist) ";
    protected static final String SQL_LEFT_JOIN_ARTWORK_GENERATED = " LEFT JOIN artwork_generated ag ON al.id=ag.located_id ";
    protected static final String SQL_LEFT_JOIN_ARTWORK_LOCATED = " LEFT JOIN artwork_located al ON a.id=al.artwork_id and al.status not in ('INVALID','NOTFOUND','ERROR','IGNORE','DELETED') ";
    protected static final String SQL_IGNORE_STATUS_SET = " NOT IN ('DELETED','INVALID','DUPLICATE') ";

    private Literals() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
