package com.moviejukebox.core.database.model;

public interface IMoviedbIdentifiable {

    String getMoviedbId(String moviedb);

    void setMoviedbId(String moviedb, String id);
}