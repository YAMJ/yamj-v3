package com.yamj.core.service.artwork;

import com.yamj.common.type.StatusType;
import com.yamj.core.database.dao.ArtworkDao;
import com.yamj.core.database.model.Artwork;
import com.yamj.core.database.model.IMetadata;
import com.yamj.core.database.model.dto.QueueDTO;
import com.yamj.core.database.model.type.ArtworkType;
import com.yamj.core.service.artwork.poster.IMoviePosterScanner;
import com.yamj.core.service.artwork.poster.IPosterScanner;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("artworkScannerService")
public class ArtworkScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScannerService.class);
    
    @Autowired
    private ArtworkDao artworkDao;

    private HashMap<String, IMoviePosterScanner> registeredMoviePosterScanner = new HashMap<String, IMoviePosterScanner>();

    public void registerMoviePosterScanner(IMoviePosterScanner posterScanner) {
        registeredMoviePosterScanner.put(posterScanner.getScannerName().toLowerCase(), posterScanner);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void scanArtwork(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        if (queueElement.isArtworkType(ArtworkType.POSTER)) {
            this.scanPoster(queueElement.getId());
//        } else if (queueElement.isArtworkType(ArtworkType.FANART)) {
//            this.scanSeries(queueElement.getId());
//        } else if (queueElement.isArtworkType(ArtworkType.BANNER)) {
//            this.scanPerson(queueElement.getId());
//        } else if (queueElement.isArtworkType(ArtworkType.VIDEOIMAGE)) {
//            this.scanPerson(queueElement.getId());
        } else {
           throw new RuntimeException("No valid element for scanning artwork '"+queueElement+"'");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        Artwork artwork = artworkDao.getArtwork(queueElement.getId());
        if (artwork != null) {
            artwork.setStatus(StatusType.ERROR);
            artworkDao.updateEntity(artwork);
        }
    }

    private void scanPoster(Long id) {
        Artwork artwork = artworkDao.getArtwork(id);
        
        // TODO local scan for poster
        
        IMetadata metadata;
        if (artwork.getVideoData() != null) {
            metadata = artwork.getVideoData();
        } else if (artwork.getSeason() != null) {
            metadata = artwork.getSeason();
        } else if (artwork.getSeries() != null) {
            metadata = artwork.getSeries();
        } else {
            throw new RuntimeException("Artwork has no associated metadata");
        }
        
        // only scan if status is OK
        // TODO should be checked in database query
        if (!StatusType.DONE.equals(metadata.getStatus())) {
            return;
        }
        
        // TODO evaluate search priority
        LOG.debug("Scan for poster: {}", artwork);
        String url = null;

        for (IPosterScanner scanner : this.registeredMoviePosterScanner.values()) {
            url = scanner.getPosterUrl(metadata);
            if (StringUtils.isNotBlank(url)) {
                break;
            }
        }
        
        if (StringUtils.isBlank(url)) {
            artwork.setStatus(StatusType.MISSING);
        } else {
            artwork.setUrl(url);
            artwork.setStatus(StatusType.DONE);
        }
        artworkDao.updateEntity(artwork);
    }
}
