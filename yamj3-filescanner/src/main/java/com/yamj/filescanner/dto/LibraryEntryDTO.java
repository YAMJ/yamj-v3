package com.yamj.filescanner.dto;

import java.io.Serializable;
import org.springframework.stereotype.Service;

/**
 * Class used to represent a library entry
 *
 * @author stuart.boston
 */
@Service("libraryFile")
public class LibraryEntryDTO implements Serializable {

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
        this.scrape = Boolean.TRUE;
        this.watch = Boolean.TRUE;
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
        StringBuilder tos = new StringBuilder("LibraryFile{");
        tos.append("path=").append(path);
        tos.append(", playerpath=").append(playerpath);
        tos.append(", description=").append(description);
        tos.append(", include=").append(include);
        tos.append(", exclude=").append(exclude);
        tos.append(", scrape=").append(scrape);
        tos.append(", watch=").append(watch);
        tos.append('}');
        return tos.toString();
    }
}
