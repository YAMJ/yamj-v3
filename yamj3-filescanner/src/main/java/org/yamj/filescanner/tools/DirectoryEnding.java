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
package org.yamj.filescanner.tools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.yamj.common.type.DirectoryType;

/**
 * Check the ending of a directory against known types
 *
 * @author Stuart
 */
public final class DirectoryEnding {

    // Directory endings for DVD and Blurays
    private static final Map<String, DirectoryType> DIR_ENDINGS = new HashMap<>(3);

    static {
        // The ending of the directory & Type
        DIR_ENDINGS.put("BDMV", DirectoryType.BLURAY);
        DIR_ENDINGS.put("AUDIO_TS", DirectoryType.DVD);
        DIR_ENDINGS.put("VIDEO_TS", DirectoryType.DVD);
    }

    private DirectoryEnding() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Add a directory ending to the list
     *
     * @param ending
     * @param dirType
     */
    public static void add(String ending, DirectoryType dirType) {
        DIR_ENDINGS.put(ending, dirType);
    }

    /**
     * Return the DirectoryType of the directory
     *
     * @param directory
     * @return
     */
    public static DirectoryType check(File directory) {
        if (DIR_ENDINGS.containsKey(directory.getName())) {
            return DIR_ENDINGS.get(directory.getName());
        }
        return DirectoryType.STANDARD;
    }
}
