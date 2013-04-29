package com.moviejukebox.core.service.moviedb;

import com.moviejukebox.core.database.model.VideoData;

public interface IMovieScanner extends IMovieDatabaseScanner {

    public String getMovieId(VideoData videoData);

    public String getMovieId(String title, int year);

    public ScanResult scan(VideoData videoData);
}
