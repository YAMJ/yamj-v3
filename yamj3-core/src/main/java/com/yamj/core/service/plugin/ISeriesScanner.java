package com.yamj.core.service.plugin;

import com.yamj.core.database.model.Series;

public interface ISeriesScanner extends IPluginDatabaseScanner {

    String getSeriesId(Series series);

    String getSeriesId(String title, int year);

    ScanResult scan(Series series);
}
