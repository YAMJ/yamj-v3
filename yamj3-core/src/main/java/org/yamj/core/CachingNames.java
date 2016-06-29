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
package org.yamj.core;

public final class CachingNames {

    public static final String DB_GENRE = "db_genre";
    public static final String DB_STUDIO = "db_studio";
    public static final String DB_COUNTRY = "db_country";
    public static final String DB_CERTIFICATION = "db_certification";
    public static final String DB_PERSON = "db_person";
    public static final String DB_BOXEDSET = "db_boxed_set";
    public static final String DB_AWARD = "db_award";
    public static final String DB_STAGEFILE = "db_stagefile";
    public static final String DB_ARTWORK_PROFILE = "db_artwork_profile";
    public static final String DB_ARTWORK_IMAGE = "db_artwork_image";

    public static final String API_GENRES = "api_genres";
    public static final String API_STUDIOS = "api_studios";
    public static final String API_COUNTRIES = "api_countries";
    public static final String API_CERTIFICATIONS = "api_certifications";
    public static final String API_EXTERNAL_IDS = "api_external_ids"; 
    public static final String API_RATINGS = "api_ratings"; 
    public static final String API_AWARDS = "api_awards"; 
    public static final String API_BOXEDSETS = "api_boxsets"; 
    public static final String API_TRAILERS = "api_trailers"; 
    public static final String API_VIDEOSOURCE = "api_videosource"; 
    
    private CachingNames() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
