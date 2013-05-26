package com.yamj.filescanner.comparator;

import java.io.File;
import java.util.Comparator;

/**
 * Compares two files and sorts by directory (last) or file (first)
 *
 * This is to allow the scanner to process all the files in a directory first
 *
 * @author Stuart
 */
public class FileTypeComparator implements Comparator<File> {

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
