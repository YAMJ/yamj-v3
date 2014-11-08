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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.ArrayList;
import java.util.List;

/**
 * List of the options available for the indexes
 *
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT)
public class OptionsIndexArtwork extends OptionsAbstract {

    private List<String> artwork = new ArrayList<String>();
    private List<String> video = new ArrayList<String>();
    private Long id = -1L;

    public OptionsIndexArtwork() {
    }

    public OptionsIndexArtwork(Long id) {
        this.id = id;
    }

    public List<String> getArtwork() {
        return artwork;
    }

    public void setArtwork(List<String> artwork) {
        this.artwork = artwork;
    }

    public void setArtwork(String artworkList) {
        this.artwork = splitList(artworkList);
    }

    public List<String> getVideo() {
        return video;
    }

    public void setVideo(List<String> videoList) {
        this.video = videoList;
    }

    public void setVideo(String videoList) {
        this.video = splitList(videoList);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
