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

import org.apache.commons.lang3.StringUtils;

public class ArtworkTools {

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
        } catch (Exception ignore) {}
        return hashCode;
    }
}
