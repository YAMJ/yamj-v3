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

    private static final String TYPE_MOVIE_SCANNER = "movie_scanner";
    private static final String TYPE_SERIES_SCANNER = "series_scanner";
    private static final String TYPE_PERSON_SCANNER = "person_scanner";

    /**
     * Get the hash code of an URL.
     * 
     * @param url
     * @return the hash code
     */
    public static String getUrlHashCode(String url) {
        // hash code of URL
        int hash = url.hashCode();
        return String.valueOf((hash < 0 ? 0 - hash : hash));
    }
    
    /**
     * Get a part of the URL as hash code.
     * 
     * @param url
     * @return the hash code
     */
    public static String getPartialHashCode(String url) {
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
        
        if (StringUtils.isEmpty(hashCode)) {
            hashCode = getUrlHashCode(url);
        }
        
        return hashCode;
    }
    
    public static Set<String> determinePriorities(final String configValue, Set<String> allowedForScan) {
        final Set<String> result;
        if (StringUtils.isBlank(configValue)) {
            result = Collections.emptySet();
        } else {
            result = new LinkedHashSet<>();
            for (String config : configValue.toLowerCase().split(",")) {
                final Set<String> checkPrios;
                if (config.equalsIgnoreCase(TYPE_MOVIE_SCANNER)) {
                    checkPrios = OnlineScannerService.MOVIE_SCANNER;
                } else if (config.equalsIgnoreCase(TYPE_SERIES_SCANNER)) {
                    checkPrios = OnlineScannerService.SERIES_SCANNER;
                } else if (config.equalsIgnoreCase(TYPE_PERSON_SCANNER)) {
                    checkPrios = OnlineScannerService.PERSON_SCANNER;
                } else {
                    checkPrios = Collections.singleton(config);
                }
                
                for (String check : checkPrios) {
                    if (allowedForScan.contains(check)) {
                        result.add(check);
                    }
                }
            }
        }
        return result;
    }
}
