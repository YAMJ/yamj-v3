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
package org.yamj.plugin.api.tools;

import java.nio.charset.Charset;

public final class Constants {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static final String SPACE_SLASH_SPACE = " / ";
    public static final String DEFAULT_SPLITTER = ",";

    public static final String UNDEFINED = "Undefined";
    public static final String ALL = "all";
    public static final String UNKNOWN = "unknown";
    
    public static final String LANGUAGE_EN = "en";
    public static final String LANGUAGE_UNTERTERMINED = "und";

    private Constants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
