package com.moviejukebox.filescanner.model;

public class Library {

    private String path;
    private boolean watch;
    private LibraryStatistics statistics;

    public Library() {
        this.path = "";
        this.watch = Boolean.FALSE;
        this.statistics = new LibraryStatistics();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isWatch() {
        return watch;
    }

    public void setWatch(boolean watch) {
        this.watch = watch;
    }

    public LibraryStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(LibraryStatistics statistics) {
        this.statistics = statistics;
    }

    @Override
    public String toString() {
        return "Library{" + "path=" + path + ", watch=" + watch + '}';
    }
}
