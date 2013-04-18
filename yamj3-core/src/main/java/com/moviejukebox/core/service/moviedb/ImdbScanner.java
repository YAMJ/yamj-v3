package com.moviejukebox.core.service.moviedb;

import com.moviejukebox.core.database.model.Series;
import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.tools.web.CommonHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("imdbScanner")
public class ImdbScanner implements IMovieScanner, ISeriesScanner, InitializingBean {

    public static final String IMDB_SCANNER_ID = "imdb";
    private static final Logger LOGGER = LoggerFactory.getLogger(ImdbScanner.class);

    @Autowired
    private CommonHttpClient httpClient;
    @Autowired
    private ImdbSearchEngine imdbSearchEngine;
    @Autowired
    private MovieDatabaseService movieDatabaseService;

    @Override
    public String getScannerName() {
        return IMDB_SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // register this scanner
        movieDatabaseService.registerMovieScanner(this);
        movieDatabaseService.registerSeriesScanner(this);
    }

    @Override
    public String getMovieId(VideoData videoData) {
        String imdbId = videoData.getMoviedbId(IMDB_SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = getMovieId(videoData.getTitle(), videoData.getPublicationYear());
            videoData.setMoviedbId(IMDB_SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getSeriesId(Series series) {
        String imdbId = series.getMoviedbId(IMDB_SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            int year = -1; // TODO: get form firsAired value
            imdbId = getSeriesId(series.getTitle(), year);
            series.setMoviedbId(IMDB_SCANNER_ID, imdbId);
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
            LOGGER.debug("IMDb id not available : " + videoData.getTitle());
            return ScanResult.MISSING_ID;
        }
        
        // TODO Auto-generated method stub
        return ScanResult.ERROR;
    }

    @Override
    public ScanResult scan(Series series) {
        String imdbId = getSeriesId(series);
        if (StringUtils.isBlank(imdbId)) {
            LOGGER.debug("IMDb id not available : " + series.getTitle());
            return ScanResult.MISSING_ID;
        }

        // TODO Auto-generated method stub
        return ScanResult.ERROR;
    }

}