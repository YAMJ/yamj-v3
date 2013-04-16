package com.moviejukebox.core.service.moviedb;

import com.moviejukebox.core.database.dao.CommonDao;
import com.moviejukebox.core.database.dao.MediaDao;
import com.moviejukebox.core.database.model.Genre;
import com.moviejukebox.core.database.model.VideoData;
import com.moviejukebox.core.database.model.type.StatusType;
import java.util.HashMap;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("movieDatabaseService")
public class MovieDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovieDatabaseService.class);

    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private CommonDao commonDao;
    
    private HashMap<String,IMovieScanner> registeredMovieScanner = new HashMap<String,IMovieScanner>();
    private HashMap<String,ISeasonScanner> registeredTvShowScanner = new HashMap<String,ISeasonScanner>();
    
    public void registerMovieScanner(IMovieScanner movieScanner) {
        registeredMovieScanner.put(movieScanner.getScannerName().toLowerCase(), movieScanner);
    }

    public void registerTvShowScanner(ISeasonScanner tvShowScanner) {
        registeredTvShowScanner.put(tvShowScanner.getScannerName().toLowerCase(), tvShowScanner);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void scanMetadata(Long id) {
        if (id == null) {
            // nothing to 
            return;
        }
        
        VideoData videoData = mediaDao.getVideoData(id);
        LOGGER.debug("Scanning video data for: " + videoData.getTitle());

        // SCAN
        
        // TODO use configured scanner only
        ScanResult scanResult = ScanResult.OK;
        for (IMovieScanner scanner : registeredMovieScanner.values()) {
            scanResult = scanner.scan(videoData);
        }
        
        // STORAGE
        
        // update genres
        HashSet<Genre> genres = new HashSet<Genre>(0);
        for (Genre genre : videoData.getGenres()) {
            Genre stored = commonDao.getGenre(genre.getName());
            if (stored != null) {
                genres.add(stored);
            } else {
                commonDao.saveEntity(genre);
                genres.add(genre);
            }
        }
        videoData.setGenres(genres);

        // update video data and reset status
        if (ScanResult.OK.equals(scanResult)) {
            videoData.setStatus(StatusType.DONE);
        } else {
            videoData.setStatus(StatusType.ERROR);
        }
        mediaDao.updateEntity(videoData);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processingError(Long id) {
        VideoData videoData = mediaDao.getVideoData(id);
        if (videoData != null) {
            videoData.setStatus(StatusType.ERROR);
            mediaDao.updateEntity(videoData);
        }
    }
}
