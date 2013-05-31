package org.yamj.core.service.plugin;

import org.yamj.core.database.model.Series;

public interface ISeriesScanner extends IPluginDatabaseScanner {

    String getSeriesId(Series series);

    String getSeriesId(String title, int year);

    ScanResult scan(Series series);
}
