package com.yamj.core.service.plugin;

import com.yamj.core.database.model.Series;

public interface ISeriesScanner extends IPluginDatabaseScanner {

    public String getSeriesId(Series series);

    public String getSeriesId(String title, int year);

    public ScanResult scan(Series series);
}
