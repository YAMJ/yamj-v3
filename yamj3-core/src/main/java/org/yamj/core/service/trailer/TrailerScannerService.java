/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.service.trailer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.MetaDataType;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.Trailer;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.dto.TrailerDTO;
import org.yamj.core.database.service.TrailerStorageService;
import org.yamj.core.service.trailer.online.*;

@Service("trailerScannerService")
public class TrailerScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(TrailerScannerService.class);
    
    private final HashMap<String, IMovieTrailerScanner> registeredMovieTrailerScanner = new HashMap<>();
    private final HashMap<String, ISeriesTrailerScanner> registeredSeriesTrailerScanner = new HashMap<>();
    
    @Autowired
    private ConfigService configService;
    @Autowired
    private TrailerStorageService trailerStorageService;
    
    public void registerTrailerScanner(ITrailerScanner trailersScanner) {
        if (trailersScanner instanceof IMovieTrailerScanner) {
            LOG.trace("Registered movie trailer scanner: {}", trailersScanner.getScannerName().toLowerCase());
            registeredMovieTrailerScanner.put(trailersScanner.getScannerName().toLowerCase(), (IMovieTrailerScanner)trailersScanner);
        }
        if (trailersScanner instanceof ISeriesTrailerScanner) {
            LOG.trace("Registered series trailer scanner: {}", trailersScanner.getScannerName().toLowerCase());
            registeredSeriesTrailerScanner.put(trailersScanner.getScannerName().toLowerCase(), (ISeriesTrailerScanner)trailersScanner);
        }
    }

    public void scanMovieTrailer(Long id) {
        if (id == null) {
            // nothing to
            return;
        }

        // get required movie
        VideoData videoData = trailerStorageService.getRequiredVideoData(id);
        
        List<Trailer> trailers = new ArrayList<>();
        // scan trailers
        this.scanTrailerLocal(videoData, trailers);
        this.scanTrailerOnline(videoData, trailers);
        // store trailers
        this.trailerStorageService.updateTrailer(videoData, trailers);
    }

    public void scanSeriesTrailer(Long id) {
        if (id == null) {
            // nothing to
            return;
        }

        // get required series
        Series series = trailerStorageService.getRequiredSeries(id);
        
        List<Trailer> trailers = new ArrayList<>();
        // scan trailers
        this.scanTrailerLocal(series, trailers);
        this.scanTrailerOnline(series, trailers);
        // store trailers
        this.trailerStorageService.updateTrailer(series, trailers);
    }

    @SuppressWarnings("unused")
    private void scanTrailerLocal(VideoData videoData, List<Trailer> trailers) {
        if (!configService.getBooleanProperty("yamj3.trailer.scan.local.movie", Boolean.TRUE)) {
            LOG.trace("Local movie trailer scan disabled");
            return;
        }

        LOG.trace("Scan local for trailer of movie {}-'{}'", videoData.getId(), videoData.getTitle());

        // TODO local scan for movie trailer
    }

    private void scanTrailerOnline(VideoData videoData, List<Trailer> trailers) {
        if (!configService.getBooleanProperty("yamj3.trailer.scan.online.movie", Boolean.TRUE)) {
            LOG.trace("Online movie trailer scan disabled");
            return;
        }

        LOG.trace("Scan online for trailer of movie {}-'{}'", videoData.getId(), videoData.getTitle());

        List<TrailerDTO> trailerDTOs = null;
        for (String prio : this.configService.getPropertyAsList("yamj3.trailer.scanner.movie.priorities", YouTubeTrailerScanner.SCANNER_ID)) {
            IMovieTrailerScanner scanner = registeredMovieTrailerScanner.get(prio);
            if (scanner != null) {
                LOG.debug("Scanning movie trailers for '{}' using {}", videoData.getTitle(), scanner.getScannerName());
                trailerDTOs = scanner.getTrailers(videoData);
                if (CollectionUtils.isNotEmpty(trailerDTOs)) {
                    break;
                }
            } else {
                LOG.warn("Desired movie trailer scanner {} not registerd", prio);
            }
        }

        if (CollectionUtils.isEmpty(trailerDTOs)) {
            LOG.info("No trailers found for movie {}-'{}'", videoData.getId(), videoData.getTitle());
            return;
        }

        int maxResults = this.configService.getIntProperty("yamj3.trailer.scanner.movie.maxResults", 5);
        if (maxResults > 0 && trailerDTOs.size() > maxResults) {
            LOG.info("Limited movie trailers to {}, actually retrieved {} for {}-'{}'", maxResults, trailerDTOs.size(), videoData.getId(), videoData.getTitle());
            trailerDTOs = trailerDTOs.subList(0, maxResults);
        } else {
            LOG.info("Found {} trailers for movie {}-'{}'", trailerDTOs.size(), videoData.getId(), videoData.getTitle());
        }

        // create trailers
        boolean downloadEnabled = configService.getBooleanProperty("yamj3.trailer.download", Boolean.FALSE);
        for (TrailerDTO dto : trailerDTOs) {
            Trailer trailer = new Trailer();
            trailer.setVideoData(videoData);
            trailer.setSource(dto.getSource());
            trailer.setUrl(dto.getUrl());
            trailer.setTitle(dto.getTitle());
            trailer.setHashCode(dto.getHashCode());
            trailer.setStatus(downloadEnabled ? StatusType.NEW : StatusType.DONE);
            trailers.add(trailer);
        }
     }

    @SuppressWarnings("unused")
    private void scanTrailerLocal(Series series, List<Trailer> trailers) {
        if (!configService.getBooleanProperty("yamj3.trailer.scan.local.series", Boolean.TRUE)) {
            LOG.trace("Local series trailer scan disabled");
            return;
        }

        LOG.trace("Scan local for trailer of series {}-'{}'", series.getId(), series.getTitle());

        // TODO local scan for series trailer
    }

    private void scanTrailerOnline(Series series, List<Trailer> trailers) {
        if (!configService.getBooleanProperty("yamj3.trailer.scan.online.series", Boolean.TRUE)) {
            LOG.trace("Online series trailer scan disabled");
            return;
        }

        LOG.trace("Scan online for trailer of series {}-'{}'", series.getId(), series.getTitle());

        List<TrailerDTO> trailerDTOs = null;
        for (String prio : this.configService.getPropertyAsList("yamj3.trailer.scanner.series.priorities", YouTubeTrailerScanner.SCANNER_ID)) {
            ISeriesTrailerScanner scanner = registeredSeriesTrailerScanner.get(prio);
            if (scanner != null) {
                LOG.debug("Scanning series trailers for '{}' using {}", series.getTitle(), scanner.getScannerName());
                trailerDTOs = scanner.getTrailers(series);
                if (CollectionUtils.isNotEmpty(trailerDTOs)) {
                    break;
                }
            } else {
                LOG.warn("Desired series trailer scanner {} not registerd", prio);
            }
        }

        if (CollectionUtils.isEmpty(trailerDTOs)) {
            LOG.info("No trailers found for series {}-'{}'", series.getId(), series.getTitle());
            return;
        }

        int maxResults = this.configService.getIntProperty("yamj3.trailer.scanner.series.maxResults", 5);
        if (maxResults > 0 && trailerDTOs.size() > maxResults) {
            LOG.info("Limited series trailers to {}, actually retrieved {} for {}-'{}'", maxResults, trailerDTOs.size(), series.getId(), series.getTitle());
            trailerDTOs = trailerDTOs.subList(0, maxResults);
        } else {
            LOG.info("Found {} trailers for series {}-'{}'", trailerDTOs.size(), series.getId(), series.getTitle());
        }

        // create trailers
        boolean downloadEnabled = configService.getBooleanProperty("yamj3.trailer.download", Boolean.FALSE);
        for (TrailerDTO dto : trailerDTOs) {
            Trailer trailer = new Trailer();
            trailer.setSeries(series);
            trailer.setSource(dto.getSource());
            trailer.setUrl(dto.getUrl());
            trailer.setHashCode(dto.getHashCode());
            trailer.setStatus(downloadEnabled ? StatusType.NEW : StatusType.DONE);
            trailers.add(trailer);
        }
    }
    
    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        if (queueElement.isMetadataType(MetaDataType.MOVIE)) {
            trailerStorageService.errorTrailerVideoData(queueElement.getId());
        } else if (queueElement.isMetadataType(MetaDataType.SERIES)) {
            trailerStorageService.errorTrailerSeries(queueElement.getId());
        }
    }
}

