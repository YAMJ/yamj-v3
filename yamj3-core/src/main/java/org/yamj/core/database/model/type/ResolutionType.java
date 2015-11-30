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
package org.yamj.core.database.model.type;

import org.apache.commons.lang3.StringUtils;

public enum ResolutionType {

    SD(-1, 1024, "SD"), // cause anamorphic videos 
    HD(1025, 1280, "HD", "HD720"),
    FULLHD(1281, 1920, "FullHD", "HD1080"),
    UHD1(1921, 3840,"UHD", "UHD1", "4K"),
    UHD2(3841, 7680, "UHD2", "8K"),
    ALL(-1, Integer.MAX_VALUE, "All");

    private final int minWidth;
    private final int maxWidth;
    private final String[] synonym;
    
    private ResolutionType(int minWidth, int maxWidth, String... synonym) {
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.synonym = synonym;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public static ResolutionType fromString(final String synonym) {
        for (ResolutionType value : ResolutionType.values()) {
            for (String syn : value.synonym) {
                if (StringUtils.equalsIgnoreCase(syn, synonym)) return value;
            }
        }
        return ALL;
    }
}
