package com.moviejukebox.filescanner.dto;

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
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPlayerpath() {
        return playerpath;
    }

    public void setPlayerpath(String playerpath) {
        this.playerpath = playerpath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public boolean isScrape() {
        return scrape;
    }

    public void setScrape(boolean scrape) {
        this.scrape = scrape;
    }

    public boolean isWatch() {
        return watch;
    }

    public void setWatch(boolean watch) {
        this.watch = watch;
    }
    //</editor-fold>

    @Override
    public String toString() {
        return "LibraryFile{" + "path=" + path + ", playerpath=" + playerpath + ", description=" + description + ", include=" + include + ", exclude=" + exclude + ", scrape=" + scrape + ", watch=" + watch + '}';
    }
}
