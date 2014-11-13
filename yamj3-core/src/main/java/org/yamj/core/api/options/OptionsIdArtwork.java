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
package org.yamj.core.api.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.database.model.type.ArtworkType;

/**
 * List of the options available for the indexes
 *
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT)
public class OptionsIdArtwork extends OptionsId {

    private String artwork = "";
    @JsonIgnore
    private final Set<String> artworkTypes = new HashSet<String>();

    public String getArtwork() {
        return artwork;
    }

    public void setArtwork(String artwork) {
        this.artwork = artwork.toUpperCase();
        splitArtwork();
    }

    public boolean hasArtwork(ArtworkType artworkType) {
        return artworkTypes.contains(artworkType.toString());
    }

    private void splitArtwork() {
        this.artworkTypes.clear();
        if (StringUtils.containsIgnoreCase(artwork, "ALL")) {
            // Add all the types to the list
            for (ArtworkType at : ArtworkType.values()) {
                artworkTypes.add(at.toString());
            }
        } else {
            for (String param : StringUtils.split(artwork, ",")) {
                artworkTypes.add(ArtworkType.fromString(param).toString());
            }
        }
        // Remove the unknown type
        artworkTypes.remove(ArtworkType.UNKNOWN.toString());
    }

    /**
     * Get a list of the artwork types to search for
     *
     * @return
     */
    @JsonIgnore
    public Set<String> getArtworkTypes() {
        return artworkTypes;
    }
}
