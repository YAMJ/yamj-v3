package com.moviejukebox.core.scanner.moviedb;

import com.moviejukebox.core.database.model.VideoData;

public class ImdbScanner implements IMovieScanner {

    public static final String IMDB_SCANNER_ID = "imdb";

    @Override
    public String getScannerName() {
        return IMDB_SCANNER_ID;
    }

    @Override
    public String getMoviedbId(VideoData videoData) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMoviedbId(String title, int year) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        // TODO Auto-generated method stub
        return null;
    }

}