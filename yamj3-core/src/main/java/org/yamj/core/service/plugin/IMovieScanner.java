package org.yamj.core.service.plugin;

import org.yamj.core.database.model.VideoData;

public interface IMovieScanner extends IPluginDatabaseScanner {

    String getMovieId(VideoData videoData);

    String getMovieId(String title, int year);

    ScanResult scan(VideoData videoData);
}
