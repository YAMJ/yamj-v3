/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General public License for more details.
 *
 *      You should have received a copy of the GNU General public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database;

public final class Literals {

    // LITERALS
    public static final String LITERAL_ARTWORK_ID = "artworkId";
    public static final String LITERAL_ARTWORK_TYPE = "artworkType";
    public static final String LITERAL_BIRTH_DAY = "birthDay";
    public static final String LITERAL_BIRTH_NAME = "birthName"; 
    public static final String LITERAL_BIRTH_PLACE = "birthPlace";
    public static final String LITERAL_CACHE_DIR = "cacheDir";
    public static final String LITERAL_CACHE_FILENAME = "cacheFilename";
    public static final String LITERAL_CERTIFICATE = "certificate";
    public static final String LITERAL_CHECK_DATE = "checkDate";
    public static final String LITERAL_COMBINED = "combined";
    public static final String LITERAL_COUNTRY_CODE = "countryCode";
    public static final String LITERAL_CREATION = "creation";
    public static final String LITERAL_DEATH_DAY = "deathDay"; 
    public static final String LITERAL_DEATH_PLACE = "deathPlace"; 
    public static final String LITERAL_EPISODE = "episode";
    public static final String LITERAL_EXTENSION = "extension";
    public static final String LITERAL_EXTRA = "extra";
    public static final String LITERAL_FILE_TYPE = "fileType";
    public static final String LITERAL_FIRST_NAME = "firstName";
    public static final String LITERAL_GENERATED_ID = "generatedId";
    public static final String LITERAL_ID = "id";
    public static final String LITERAL_IDENTIFIER = "identifier";
    public static final String LITERAL_JOB = "job";
    public static final String LITERAL_LAST_NAME = "lastName"; 
    public static final String LITERAL_LASTSCAN = "lastscan";
    public static final String LITERAL_LIBRARY = "library";
	public static final String LITERAL_LIBRARY_ITEM = "library_item";
	public static final String LITERAL_LIBRARY_BASE = "library_base";
    public static final String LITERAL_LOCATED_ID = "locatedId";
    public static final String LITERAL_METADATA_TYPE = "metaDataType";
    public static final String LITERAL_NAME = "name";
    public static final String LITERAL_NEWEST_DATE = "newestDate";
    public static final String LITERAL_PLOT = "plot";
    public static final String LITERAL_ORIGINAL_TITLE = "originalTitle";
    public static final String LITERAL_OUTLINE = "outline";
    public static final String LITERAL_RELEASE_DATE = "releaseDate";
    public static final String LITERAL_SEASON = "season";
    public static final String LITERAL_SEASON_ID = "seasonId";
    public static final String LITERAL_SERIES_ID = "seriesId";
    public static final String LITERAL_SOURCE = "source";
    public static final String LITERAL_SORT_TITLE = "sortTitle";
    public static final String LITERAL_STAGE_DIRECTORY = "stageDirectory";
    public static final String LITERAL_STATUS = "status";
    public static final String LITERAL_TITLE = "title";
    public static final String LITERAL_TYPE = "type";
    public static final String LITERAL_VIDEO_TYPE = "videoType";               
    public static final String LITERAL_VIDEO_YEAR = "videoYear";
    public static final String LITERAL_VIDEODATA_ID = "videodataId";
    public static final String LITERAL_VOICE_ROLE = "voiceRole";
    public static final String LITERAL_WATCHED = "watched";
    public static final String LITERAL_YEAR = "year";
    public static final String LITERAL_BASENAME = "baseName";
    
    // SQL
    public static final String SQL_UNION = " UNION ";
    public static final String SQL_UNION_ALL = " UNION ALL ";
    public static final String SQL_AS_VIDEO_TYPE = "' AS videoType";
    public static final String SQL_COMMA_SPACE_QUOTE = ", '";
    public static final String SQL_ARTWORK_TYPE_IN_ARTWORKLIST = " AND a.artwork_type IN (:artworklist) ";
    public static final String SQL_LEFT_JOIN_ARTWORK_GENERATED = " LEFT JOIN artwork_generated ag ON al.id=ag.located_id ";
    public static final String SQL_LEFT_JOIN_ARTWORK_LOCATED = " LEFT JOIN artwork_located al ON a.id=al.artwork_id and al.status not in ('INVALID','NOTFOUND','ERROR','IGNORE','DELETED') ";
    public static final String SQL_IGNORE_STATUS_SET = " NOT IN ('DELETED','INVALID','DUPLICATE') ";
    public static final String SQL_SELECTABLE_VIDEOS = " IN ('VIDEO','BLURAY','HDDVD','DVD') ";

    private Literals() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
