/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.api.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.database.model.type.ArtworkType;

/**
 * List of the options available for the indexes
 *
 * @author stuart.boston
 */
public class OptionsIdArtwork extends OptionsId {

    private String artwork = "";
    @JsonIgnore
    private List<String> artworkTypes = new ArrayList<String>();

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
            // Remove the unknown type
            artworkTypes.remove(ArtworkType.UNKNOWN.toString());
        } else {
            for (String param : StringUtils.split(artwork, ",")) {
                // Validate that the string passed is a correct artwork type
                ArtworkType at = ArtworkType.fromString(param);
                if (at != ArtworkType.UNKNOWN) {
                    artworkTypes.add(at.toString());
                }
            }
        }
    }

    /**
     * Get a list of the artwork types to search for
     *
     * @return
     */
    @JsonIgnore
    public List<String> getArtworkTypes() {
        return artworkTypes;
    }
}
