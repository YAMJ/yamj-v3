package com.moviejukebox.filescanner.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to represent the library XML file on disk
 *
 * @author stuart.boston
 */
public class LibraryDTO implements Serializable {

    List<LibraryEntryDTO> libraries = new ArrayList<LibraryEntryDTO>();

    /**
     * Get the list of libraries
     *
     * @return
     */
    public List<LibraryEntryDTO> getLibraries() {
        return libraries;
    }

    /**
     * Set the list of libraries
     *
     * @param libraries
     */
    public void setLibraries(List<LibraryEntryDTO> libraries) {
        this.libraries = libraries;
    }

    /**
     * Add a single library file to the list of libraries
     *
     * @param libraryFile
     */
    public void addLibrary(LibraryEntryDTO libraryFile) {
        this.libraries.add(libraryFile);
    }
}
