/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package org.yamj.filescanner.comparator;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two files and sorts by directory (last) or file (first)
 *
 * This is to allow the scanner to process all the files in a directory first
 *
 * @author Stuart
 */
public class FileTypeComparator implements Comparator<File>, Serializable {

    private static final long serialVersionUID = 1L;
    private boolean directoriesFirst;

    /**
     * Sorts files based on the type (directory/file).
     *
     * Default is to sort with directories first
     */
    public FileTypeComparator() {
        directoriesFirst = Boolean.TRUE;
    }

    /**
     * Sorts files based on the type (directory/file).
     *
     * @param directoriesFirst
     */
    public FileTypeComparator(boolean directoriesFirst) {
        this.directoriesFirst = directoriesFirst;
    }

    public boolean isDirectoriesFirst() {
        return directoriesFirst;
    }

    public void setDirectoriesFirst(boolean directoriesFirst) {
        this.directoriesFirst = directoriesFirst;
    }

    @Override
    public int compare(File file1, File file2) {
        if (file1.isDirectory() && file2.isFile()) {
            return (directoriesFirst ? -1 : 1);
        }

        if (file1.isDirectory() && file2.isDirectory()) {
            return 0;
        }

        if (file1.isFile() && file2.isFile()) {
            return 0;
        }

        return (directoriesFirst ? 1 : -1);
    }
}
