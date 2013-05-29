package com.yamj.core.service.artwork.poster;

import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.MovieDb;
import com.yamj.common.tools.PropertyTools;
import com.yamj.common.tools.web.PoolingHttpClient;
import com.yamj.core.database.model.IMetadata;
import com.yamj.core.service.artwork.ArtworkScannerService;
import com.yamj.core.service.plugin.ImdbScanner;
import com.yamj.core.service.plugin.TheMovieDbScanner;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("tmdbPosterScanner")
public class TheMovieDbPosterScanner extends AbstractMoviePosterScanner
    implements InitializingBean
{

    private static final Logger LOG = LoggerFactory.getLogger(TheMovieDbPosterScanner.class);
    private static final String DEFAULT_POSTER_SIZE = "original";
    private static final String API_KEY = PropertyTools.getProperty("APIKEY.themoviedb", "");
    private String languageCode;
    private TheMovieDbApi tmdbApi;

    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private PoolingHttpClient httpClient;
    @Autowired
    private TheMovieDbScanner tmdbScanner;
    
    @Override
    public String getScannerName() {
        return TheMovieDbScanner.TMDB_SCANNER_ID;
    }

    @Override
    public void afterPropertiesSet() {
        if (StringUtils.isNotBlank(API_KEY)) {
            try {
                tmdbApi = new TheMovieDbApi(API_KEY, httpClient);
                // register this scanner
                artworkScannerService.registerMoviePosterScanner(this);
            } catch (MovieDbException ex) {
                LOG.error("Unable to initialise TheMovieDbPosterScanner, error: {}", ex.getMessage());
            }
        } else {
            LOG.error("Failed to initialise TheMovieDbPosterScanner, no API KEY available");
        }
    }

    @Override
    public String getId(String title, int year) {
        return tmdbScanner.getMovieId(title, year);
    }

    @Override
    public String getPosterUrl(String title, int year) {
        String id = this.getId(title, year);
        return this.getPosterUrl(id);
    }
    
    @Override
    public String getPosterUrl(String id) {
        String url = null;
        if (StringUtils.isNumeric(id)) {
            try {
                MovieDb moviedb = tmdbApi.getMovieInfo(Integer.parseInt(id), languageCode);
                LOG.debug("Movie found on TheMovieDB.org: http://www.themoviedb.org/movie/" + id);
                URL posterURL = tmdbApi.createImageUrl(moviedb.getPosterPath(), DEFAULT_POSTER_SIZE);
                url = posterURL.toString();
            } catch (MovieDbException ex) {
                LOG.warn("Failed to get the poster URL for TMDb ID " + id + " " + ex.getMessage());
            }
        }
        return url;
    }

    @Override
    public String getPosterUrl(IMetadata metadata) {
        String id = getId(metadata);

        if (StringUtils.isBlank(id)) {
            id = getId(metadata.getTitleOriginal(), metadata.getYear());
            if (StringUtils.isNotBlank(id)) {
                metadata.setSourcedbId(getScannerName(), id);
            }
        }

        if (StringUtils.isNotBlank(id)) {
            return getPosterUrl(id);
        }
        return null;
    }

    private String getId(IMetadata metadata) {
        // First look to see if we have a TMDb ID as this will make looking the film up easier
        String tmdbID = metadata.getSourcedbId(getScannerName());
        if (StringUtils.isNumeric(tmdbID)) {
            return tmdbID;
        }

        String imdbID = metadata.getSourcedbId(ImdbScanner.IMDB_SCANNER_ID);
        if (StringUtils.isNotBlank(imdbID)) {
            // Search based on IMDb ID
            MovieDb moviedb = null;
            try {
                moviedb = tmdbApi.getMovieInfoImdb(imdbID, languageCode);
            } catch (MovieDbException ex) {
                LOG.warn("Failed to get TMDb ID for " + imdbID + " - " + ex.getMessage());
            }
            
            if (moviedb != null) {
                tmdbID = String.valueOf(moviedb.getId());
                if (StringUtils.isNumeric(tmdbID)) {
                    return tmdbID;
                }
            }
        }
        
        LOG.warn("No TMDb id found for movie");
        return null;
    }
}
