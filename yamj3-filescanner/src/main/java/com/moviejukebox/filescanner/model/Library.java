package com.moviejukebox.filescanner.model;

import com.moviejukebox.common.dto.ImportDTO;

public class Library extends ImportDTO {

    private boolean watch;
    private LibraryStatistics statistics;

    public Library() {
        this.watch = Boolean.FALSE;
        this.statistics = new LibraryStatistics();
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

}
