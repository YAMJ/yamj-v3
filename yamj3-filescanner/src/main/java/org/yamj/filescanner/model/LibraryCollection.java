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
package org.yamj.filescanner.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.dto.ImportDTO;
import org.yamj.filescanner.dto.LibraryDTO;
import org.yamj.filescanner.dto.LibraryEntryDTO;
import org.yamj.filescanner.tools.XmlTools;

@Service
public class LibraryCollection implements Serializable {

    private static final long serialVersionUID = -134476506971169954L;
    private static final Logger LOG = LoggerFactory.getLogger(LibraryCollection.class);

    private final List<Library> libraries;
    private String defaultPlayerPath = "";
    private String defaultClient = "";

    @Autowired
    private XmlTools xmlTools;

    public LibraryCollection() {
        libraries = new ArrayList<>();
    }

    /**
     * Add a library to the collection
     *
     * @param library
     */
    public void add(Library library) {
        libraries.add(library);
    }

    /**
     * Remove a library entry from the collection
     *
     * @param library
     */
    public void remove(Library library) {
        libraries.remove(library);
    }

    /**
     * Remove libraries from the collection by path
     *
     * @param path
     */
    public void remove(String path) {
        List<Library> toRemove = new ArrayList<>();
        for (Library library : libraries) {
            if (library.getImportDTO().getBaseDirectory().equalsIgnoreCase(path)) {
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
    public List<Library> getLibraries() {
        return libraries;
    }

    /**
     * Clear the list of libraries
     */
    public void clear() {
        libraries.clear();
    }

    /**
     * Get the number of libraries in the collection
     *
     * @return
     */
    public int size() {
        return libraries.size();
    }

    /**
     * Read the library files (multiple) / command line properties and set up
     * the library
     *
     * @param libraryFilenames The name and location of the library files to
     * process
     * @param defaultWatchState The default watch status
     */
    public void processLibraryList(List<String> libraryFilenames, boolean defaultWatchState) {
        LOG.info("Library files: {}", libraryFilenames);
        LOG.info("Default watch: {}", defaultWatchState);

        // Process the library files
        for (String singleLibrary : libraryFilenames) {
            processLibraryFile(singleLibrary, defaultWatchState);
        }
    }

    /**
     * Read the library file from disk and return the library object
     *
     * @param libraryFilename the file to read
     * @param defaultWatchState the default watched state if not provided in the
     * file
     */
    public void processLibraryFile(String libraryFilename, boolean defaultWatchState) {
        LOG.info("Processing library: {}", libraryFilename);
        LOG.info("Default watch     : {}", defaultWatchState);

        LibraryDTO lib = xmlTools.read(libraryFilename, LibraryDTO.class);

        if (lib == null) {
            LOG.warn("Failed to read library file '{}'", libraryFilename);
        } else {
            for (LibraryEntryDTO single : lib.getLibraries()) {
                LOG.info("Adding library '{}'", single.getDescription());
                addLibraryEntry(single);
            }
        }
    }

    public void saveLibraryFile(String libraryFilename) {
        LibraryDTO lib = new LibraryDTO();
        for (Library libraryEntry : libraries) {
            LibraryEntryDTO le = new LibraryEntryDTO();
            le.setDescription(libraryEntry.getImportDTO().getClient());
            le.setExclude("none");
            le.setInclude("all");
            le.setPath(libraryEntry.getImportDTO().getBaseDirectory());
            le.setPlayerpath(libraryEntry.getImportDTO().getPlayerPath());
            le.setScrape(true);
            le.setWatch(libraryEntry.isWatch());
            lib.addLibrary(le);
        }

        xmlTools.save(libraryFilename, lib);
    }

    public void addLibraryDirectory(String baseDirectory, boolean defaultWatchState) {
        LibraryEntryDTO le = new LibraryEntryDTO();
        le.setPath(baseDirectory);
        le.setWatch(defaultWatchState);
        le.setPlayerpath(defaultPlayerPath);
        le.setDescription(defaultClient);

        addLibraryEntry(le);
    }

    public void addLibraryEntry(LibraryEntryDTO libraryEntryDto) {
        Library library = new Library();
        // Set up the ImportDTO
        ImportDTO importDto = new ImportDTO();
        importDto.setBaseDirectory(libraryEntryDto.getPath());
        importDto.setClient(defaultClient);
        importDto.setPlayerPath(libraryEntryDto.getPlayerpath());

        // Set up the remaining library settings
        library.setImportDTO(importDto);
        library.setWatch(libraryEntryDto.isWatch());
        library.setDescription(libraryEntryDto.getDescription());
        add(library);
    }

    public String getDefaultPlayerPath() {
        return defaultPlayerPath;
    }

    public void setDefaultPlayerPath(String defaultPlayerPath) {
        this.defaultPlayerPath = defaultPlayerPath;
    }

    public String getDefaultClient() {
        return defaultClient;
    }

    public void setDefaultClient(String defaultClient) {
        this.defaultClient = defaultClient;
    }

    /**
     * Is the library collection empty?
     *
     * @return
     */
    public boolean isEmpty() {
        return libraries.isEmpty();
    }
}
