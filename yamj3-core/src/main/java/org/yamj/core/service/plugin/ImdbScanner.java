package org.yamj.core.service.plugin;

import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.tools.web.PoolingHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("imdbScanner")
public class ImdbScanner implements IMovieScanner, ISeriesScanner, InitializingBean {

    public static final String IMDB_SCANNER_ID = "imdb";
    private static final Logger LOG = LoggerFactory.getLogger(ImdbScanner.class);
    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private PluginDatabaseService pluginDatabaseService;

    @Override
    public String getScannerName() {
        return IMDB_SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // register this scanner
        pluginDatabaseService.registerMovieScanner(this);
        pluginDatabaseService.registerSeriesScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String imdbId = videoData.getSourcedbId(IMDB_SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = getMovieId(videoData.getTitle(), videoData.getPublicationYear());
            videoData.setSourcedbId(IMDB_SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getSeriesId(Series series) {
        String imdbId = series.getSourcedbId(IMDB_SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            int year = -1; // TODO: get form firsAired value
            imdbId = getSeriesId(series.getTitle(), year);
            series.setSourcedbId(IMDB_SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getMovieId(String title, int year) {
        return imdbSearchEngine.getImdbId(title, year, false);
    }

    @Override
    public String getSeriesId(String title, int year) {
        return imdbSearchEngine.getImdbId(title, year, true);
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        String imdbId = getMovieId(videoData);
        if (StringUtils.isBlank(imdbId)) {
            LOG.debug("IMDb id not available : {}", videoData.getTitle());
            return ScanResult.MISSING_ID;
        }

        // TODO Auto-generated method stub
        return ScanResult.ERROR;
    }

    @Override
    public ScanResult scan(Series series) {
        String imdbId = getSeriesId(series);
        if (StringUtils.isBlank(imdbId)) {
            LOG.debug("IMDb id not available: {}", series.getTitle());
            return ScanResult.MISSING_ID;
        }

        // TODO Auto-generated method stub
        return ScanResult.ERROR;
    }
}