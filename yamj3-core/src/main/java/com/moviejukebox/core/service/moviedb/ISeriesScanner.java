package com.moviejukebox.core.service.moviedb;

import com.moviejukebox.core.database.model.Series;

public interface ISeriesScanner extends IMovieDatabaseScanner {

    public String getSeriesId(Series series);

    public String getSeriesId(String title, int year);

    public ScanResult scan(Series series);
}
