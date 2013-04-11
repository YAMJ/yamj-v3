package com.moviejukebox.core.scanner.moviedb;

import com.moviejukebox.core.database.model.VideoData;

public interface IMovieScanner extends IMovieDatabaseScanner {

    public String getMoviedbId(VideoData videoData);

    public String getMoviedbId(String title, int year);

    public ScanResult scan(VideoData videoData);
}
