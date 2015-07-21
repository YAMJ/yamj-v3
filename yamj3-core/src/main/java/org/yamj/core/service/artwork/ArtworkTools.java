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
package org.yamj.core.service.artwork;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.service.metadata.online.OnlineScannerService;

public class ArtworkTools {

    private static final String TYPE_PLUGIN_MOVIE = "plugin_movie";
    private static final String TYPE_PLUGIN_SERIES = "plugin_series";
    private static final String TYPE_PLUGIN_PERSON = "plugin_person";
    private static final String TYPE_ALTERNATE_MOVIE = "alternate_movie";
    private static final String TYPE_ALTERNATE_SERIES = "alternate_series";
    private static final String TYPE_ALTERNATE_PERSON = "alternate_person";
    private static final String PLUGIN_MOVIE = OnlineScannerService.MOVIE_SCANNER;
    private static final String PLUGIN_MOVIE_ALT = OnlineScannerService.MOVIE_SCANNER_ALT;
    private static final String PLUGIN_SERIES = OnlineScannerService.SERIES_SCANNER;
    private static final String PLUGIN_SERIES_ALT = OnlineScannerService.SERIES_SCANNER_ALT;
    private static final String PLUGIN_PERSON = OnlineScannerService.PERSON_SCANNER;
    private static final String PLUGIN_PERSON_ALT = OnlineScannerService.PERSON_SCANNER_ALT;
    
    public enum HashCodeType {
        SIMPLE,
        PART;
    }

    /**
     * Get the hash code of an URL.
     * 
     * @param url
     * @param hashCodeType
     * @return the hash code
     */
    public static String getUrlHashCode(String url) {
        return getUrlHashCode(url, HashCodeType.SIMPLE);
    }

    /**
     * Get the hash code of an URL.
     * 
     * @param url
     * @param hashCodeType
     * @return the hash code
     */
    public static String getUrlHashCode(String url, HashCodeType hashCodeType) {
        if (hashCodeType == null || hashCodeType.equals(HashCodeType.SIMPLE)) {
            // hash code of URL
            int hash = url.hashCode();
            return String.valueOf((hash < 0 ? 0 - hash : hash));
        }
        
        // hash code is part of the URL
        String hc = ArtworkTools.getPartialHashCode(url);
        if (StringUtils.isEmpty(hc)) {
            // may not be empty, so use simple hash code
            int hash = url.hashCode();
            return String.valueOf((hash < 0 ? 0 - hash : hash));
        }
        return hc;
    }
    
    /**
     * Get a part of the URL as hash code.
     * 
     * @param url
     * @return the hash code
     */
    private static String getPartialHashCode(String url) {
        String hashCode = null;
        try {
            int index = StringUtils.lastIndexOf(url, "/");
            if (index > -1) {
                String tmp = url.substring(index+1);
                index = tmp.indexOf(".");
                if (index > -1) {
                    hashCode = tmp.substring(0, index);
                }
            }
        } catch (Exception ignore) {
            // ignore any exception
        }
        return hashCode;
    }
    
    public static Set<String> determinePriorities(final String configValue, Set<String> allowedForScan) {
        // replace settings
        String pattern = configValue.toLowerCase();
        pattern = StringUtils.replace(pattern, TYPE_PLUGIN_MOVIE, PLUGIN_MOVIE);
        pattern = StringUtils.replace(pattern, TYPE_ALTERNATE_MOVIE, PLUGIN_MOVIE_ALT);
        pattern = StringUtils.replace(pattern, TYPE_PLUGIN_SERIES, PLUGIN_SERIES);
        pattern = StringUtils.replace(pattern, TYPE_ALTERNATE_SERIES, PLUGIN_SERIES_ALT);
        pattern = StringUtils.replace(pattern, TYPE_PLUGIN_PERSON, PLUGIN_PERSON);
        pattern = StringUtils.replace(pattern, TYPE_ALTERNATE_PERSON, PLUGIN_PERSON_ALT);
        
        String[] splitted = StringUtils.split(pattern, ',');
        if (splitted == null || splitted.length == 0) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String split : splitted) {
            if (allowedForScan.contains(split)) {
                result.add(split);
            }
        }
        return result;
    }
}
