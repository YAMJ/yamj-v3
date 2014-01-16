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
package org.yamj.filescanner.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Class to represent the library XML file on disk
 *
 * @author stuart.boston
 */
public class LibraryDTO implements Serializable {

    private List<LibraryEntryDTO> libraries = new ArrayList<LibraryEntryDTO>();

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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
