package com.moviejukebox.core.scanner.moviedb;

import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.remote.service.FileImportServiceImpl;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("movieDatabaseController")
public class MovieDatabaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileImportServiceImpl.class);

    private HashMap<String,IMovieScanner> registeredMovieScanner = new HashMap<String,IMovieScanner>();
    private HashMap<String,ISeasonScanner> registeredTvShowScanner = new HashMap<String,ISeasonScanner>();
    
    public void registerMovieScanner(IMovieScanner movieScanner) {
        registeredMovieScanner.put(movieScanner.getScannerName().toLowerCase(), movieScanner);
    }

    public void registerTvShowScanner(ISeasonScanner tvShowScanner) {
        registeredTvShowScanner.put(tvShowScanner.getScannerName().toLowerCase(), tvShowScanner);
    }

    public void scanMovie(VideoData videoData) {
        LOGGER.debug("Scanning video data for : " + videoData.getTitle());
        
        IMovieScanner scanner = new OfdbScanner();
        scanner.scan(videoData);
    }
}
