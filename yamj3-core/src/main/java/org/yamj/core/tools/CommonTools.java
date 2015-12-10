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
package org.yamj.core.tools;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public final class CommonTools {

    private CommonTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Get the hash code of an URL.
     * 
     * @param string
     * @return the hash code
     */
    public static String getSimpleHashCode(String string) {
        // hash code of string
        return Integer.toString(Math.abs(string.hashCode()));
    }
    
    /**
     * Get a part of the string as hash code.
     * 
     * @param string
     * @return the hash code
     */
    public static String getPartialHashCode(String string) {
        String hashCode = null;
        try {
            int index = StringUtils.lastIndexOf(string, "/");
            if (index > -1) {
                String tmp = string.substring(index+1);
                index = tmp.indexOf(".");
                if (index > -1) {
                    hashCode = tmp.substring(0, index);
                }
            }
        } catch (Exception ignore) {
            // ignore any exception
        }
        
        if (StringUtils.isEmpty(hashCode)) {
            hashCode = getSimpleHashCode(string);
        }
        
        return hashCode;
    }
    
    public static <T extends Object> T getEqualObject(Collection<T> coll, T object) {
        if (CollectionUtils.isEmpty(coll)) {
            return null;
        }
        for (T col : coll) {
            if (col.equals(object)) {
                return col;
            }
        }
        return null;
   }
}
