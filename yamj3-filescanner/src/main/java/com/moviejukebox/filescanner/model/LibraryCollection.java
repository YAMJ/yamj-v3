package com.moviejukebox.filescanner.model;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryCollection {

    private static final Logger LOG = LoggerFactory.getLogger(LibraryCollection.class);
    private static final String LOG_MESSAGE = "LibraryCollection: ";
    private static List<Library> libraries = new ArrayList<Library>();

    private LibraryCollection() {
        throw new UnsupportedOperationException("Unable to instatiate class");
    }

    /**
     * Add a library to the collection
     *
     * @param library
     */
    public static void add(Library library) {
        libraries.add(library);
    }

    /**
     * Remove a library entry from the collection
     *
     * @param library
     */
    public static void remove(Library library) {
        libraries.remove(library);
    }

    /**
     * Remove libraries from the collection by path
     *
     * @param path
     */
    public static void remove(String path) {
        List<Library> toRemove = new ArrayList<Library>();
        for (Library library : libraries) {
            if (library.getPath().equalsIgnoreCase(path)) {
                toRemove.add(library);
            }
        }
        libraries.removeAll(toRemove);
    }

    /**
     * Get the list of libraries
     *
     * @return
     */
    public static List<Library> getLibraries() {
        return libraries;
    }

    /**
     * Clear the list of libraries
     */
    public static void clear() {
        libraries.clear();
    }

    /**
     * Get the number of libraries in the collection
     *
     * @return
     */
    public static int size() {
        return libraries.size();
    }

    /**
     * Read the library files (multiple) / command line properties and set up the library
     *
     * @param libraryFilenames The name and location of the library files to process
     * @param directoryProperty A single directory from the command line
     * @param defaultWatchState The default watch status
     */
    public static void processLibraryList(List<String> libraryFilenames, boolean defaultWatchState) {
        LOG.info("{}Library files: {}", LOG_MESSAGE, libraryFilenames);
        LOG.info("{}Default watch: {}", LOG_MESSAGE, defaultWatchState);

        // Process the library files
        for (String singleLibrary : libraryFilenames) {
            processLibraryFile(singleLibrary, defaultWatchState);
        }
    }

    /**
     * Read the library file from disk and return the library object
     *
     * @param libraryFilename the file to read
     * @param defaultWatchState the default watched state if not provided in the file
     * @return
     */
    public static void processLibraryFile(String libraryFilename, boolean defaultWatchState) {
        LOG.warn("{}processLibraryFile - Not supported yet.", LOG_MESSAGE);
        LOG.warn("{}Library Filename    : {}", LOG_MESSAGE, libraryFilename);
        LOG.warn("{}Default watch state : {}", LOG_MESSAGE, defaultWatchState);

        // process the library file
//        Library library = new Library();

        // add the library file
//        add(library);
    }
}
