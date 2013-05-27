package com.yamj.core.service.plugin;

import com.yamj.core.database.model.VideoData;

public interface IMovieScanner extends IPluginDatabaseScanner {

    String getMovieId(VideoData videoData);

    String getMovieId(String title, int year);

    ScanResult scan(VideoData videoData);
}
