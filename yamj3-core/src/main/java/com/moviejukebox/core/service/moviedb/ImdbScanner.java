package com.moviejukebox.core.service.moviedb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.core.database.model.Season;
import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.tools.web.HttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("imdbScanner")
public class ImdbScanner implements IMovieScanner, ISeasonScanner, InitializingBean {

    public static final String IMDB_SCANNER_ID = "imdb";
    private static final Logger LOGGER = LoggerFactory.getLogger(ImdbScanner.class);

    @Autowired
    private HttpClient httpClient;
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
        movieDatabaseService.registerSeasonScanner(this);
    }

    @Override
    public String getMoviedbId(VideoData videoData) {
        String imdbId = videoData.getMoviedbId(IMDB_SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            imdbId = getMoviedbId(videoData.getTitle(), videoData.getPublicationYear());
            videoData.setMoviedbId(IMDB_SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getMoviedbId(Season season) {
        String imdbId = season.getMoviedbId(IMDB_SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            int year = -1; // TODO: get form firsAired value
            imdbId = getMoviedbId(season.getTitle(), year, season.getSeason());
            season.setMoviedbId(IMDB_SCANNER_ID, imdbId);
        }
        return imdbId;
    }

    @Override
    public String getMoviedbId(String title, int year) {
        return getMoviedbId(title, year, -1);
    }

    @Override
    public String getMoviedbId(String title, int year, int season) {
        return imdbSearchEngine.getImdbId(title, year, (season>-1));
    }

    @Override
    public ScanResult scan(VideoData videoData) {
        String imdbId = getMoviedbId(videoData);
        if (StringUtils.isBlank(imdbId)) {
            LOGGER.debug("IMDb id not available : " + videoData.getTitle());
            return ScanResult.MISSING_ID;
        }
        
        // TODO Auto-generated method stub
        return ScanResult.ERROR;
    }

    @Override
    public ScanResult scan(Season season) {
        String imdbId = getMoviedbId(season);
        if (StringUtils.isBlank(imdbId)) {
            LOGGER.debug("IMDb id not available : " + season.getTitle());
            return ScanResult.MISSING_ID;
        }

        // TODO Auto-generated method stub
        return ScanResult.ERROR;
    }

}