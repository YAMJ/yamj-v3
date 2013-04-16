package com.moviejukebox.core.service.moviedb;

import com.moviejukebox.core.database.model.Season;

public interface ISeasonScanner extends IMovieDatabaseScanner {

    public String getMoviedbId(Season season);

    public String getMoviedbId(String title, int year, int season);

    public ScanResult scan(Season season);
}
