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
package org.yamj.filescanner.dto;

import java.io.Serializable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.stereotype.Service;

/**
 * Class used to represent a library entry
 *
 * @author stuart.boston
 */
@Service("libraryFile")
public class LibraryEntryDTO implements Serializable {
	
    private static final long serialVersionUID = 1811957207452221890L;
    
    // Properties
    private String path;
    private String playerpath;
    private String description;
    private String include;
    private String exclude;
    private boolean scrape;
    private boolean watch;

    /**
     * Construct an empty library file
     */
    public LibraryEntryDTO() {
        this.path = "";
        this.playerpath = "";
        this.description = "";
        this.include = "";
        this.exclude = "";
        this.scrape = true;
        this.watch = true;
    }

    //<editor-fold defaultstate="collapsed" desc="Getter and Setter Methods">
    /**
     * Get the scanner path location
     *
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the scanner path location.
     *
     * This will be specific to the scanner and nothing to do with the actual player path
     *
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the player path
     *
     * @return
     */
    public String getPlayerpath() {
        return playerpath;
    }

    /**
     * Set the player path.
     *
     * This is currently copied from the existing YAMJ implementation. We will probably need to get rid of this at some point
     *
     * @param playerpath
     */
    public void setPlayerpath(String playerpath) {
        this.playerpath = playerpath;
    }

    /**
     * Get the description for the library
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description for the library
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the regex pattern of files to include
     *
     * @return
     */
    public String getInclude() {
        return include;
    }

    /**
     * Set the regex pattern of files to include
     *
     * @param include
     */
    public void setInclude(String include) {
        this.include = include;
    }

    /**
     * Get the regex pattern of files to exclude
     *
     * @return
     */
    public String getExclude() {
        return exclude;
    }

    /**
     * Set the regex pattern of files to exclude
     *
     * @param exclude
     */
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    /**
     * Determines if the library be scraped.
     *
     * This is currently copied from the existing YAMJ implementation. We will probably need to get rid of this at some point
     *
     * @return
     */
    public boolean isScrape() {
        return scrape;
    }

    /**
     * Sets the library be scraped.
     *
     * This is currently copied from the existing YAMJ implementation. We will probably need to get rid of this at some point
     *
     * @param scrape
     */
    public void setScrape(boolean scrape) {
        this.scrape = scrape;
    }

    /**
     * Determines if the library should be watched for changes
     *
     * @return
     */
    public boolean isWatch() {
        return watch;
    }

    /**
     * Set the flag to watch the library for changes
     *
     * @param watch
     */
    public void setWatch(boolean watch) {
        this.watch = watch;
    }
    //</editor-fold>

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
