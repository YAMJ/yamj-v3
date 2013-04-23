package com.moviejukebox.filescanner.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to represent the library XML file on disk
 *
 * @author stuart.boston
 */
public class LibraryDTO {

    List<LibraryEntryDTO> libraries = new ArrayList<LibraryEntryDTO>();

    public List<LibraryEntryDTO> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<LibraryEntryDTO> libraries) {
        this.libraries = libraries;
    }

    public void addLibrary(LibraryEntryDTO libraryFile) {
        this.libraries.add(libraryFile);
    }
}
