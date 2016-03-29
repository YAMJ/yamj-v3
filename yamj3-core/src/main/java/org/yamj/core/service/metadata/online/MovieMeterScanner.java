/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package org.yamj.core.service.metadata.online;

import com.omertron.moviemeter.model.Actor;
import com.omertron.moviemeter.model.FilmInfo;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.service.metadata.nfo.InfoDTO;
import org.yamj.core.tools.OverrideTools;
import org.yamj.core.web.apis.MovieMeterApiWrapper;

@Service("movieMeterScanner")
public class MovieMeterScanner implements IMovieScanner {

    private static final String SCANNER_ID = "moviemeter";
    private static final Logger LOG = LoggerFactory.getLogger(MovieMeterScanner.class);
    
    @Autowired
    private OnlineScannerService onlineScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper; 
    @Autowired
    private MovieMeterApiWrapper movieMeterApiWrapper;
    @Autowired
    private LocaleService localeService;
    
    @Override
    public String getScannerName() {
        return SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.trace("Initialize MovieMeter scanner");

        // register this scanner
        onlineScannerService.registerMetadataScanner(this);
    }
    
    @Override
    public String getMovieId(VideoData videoData) {
        return getMovieId(videoData, false);
    }

    private String getMovieId(VideoData videoData, boolean throwTempError) {
        String movieMeterId = videoData.getSourceDbId(SCANNER_ID);
        if (StringUtils.isNumeric(movieMeterId)) {
            return movieMeterId;
        }
        
        // try to get the MovieMeter ID using the IMDB ID
        String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isNotBlank(imdbId)) {
            movieMeterId = movieMeterApiWrapper.getMovieIdByIMDbId(imdbId, throwTempError);
        }

        // try to get the MovieMeter ID using title and year
        if (!StringUtils.isNumeric(movieMeterId)) {
            movieMeterId = movieMeterApiWrapper.getMovieIdByTitleAndYear(videoData.getTitle(), videoData.getPublicationYear(), throwTempError);
        }

        // try to get the MovieMeter ID using original title and year
        if (!StringUtils.isNumeric(movieMeterId) && videoData.isTitleOriginalScannable()) {
            movieMeterId = movieMeterApiWrapper.getMovieIdByTitleAndYear(videoData.getTitleOriginal(), videoData.getPublicationYear(), throwTempError);
        }
        
        videoData.setSourceDbId(SCANNER_ID, movieMeterId);
        return movieMeterId;
    }
    
    @Override
    public ScanResult scanMovie(VideoData videoData, boolean throwTempError) {
        // get movie id
        String movieMeterId = getMovieId(videoData, throwTempError);
        if (!StringUtils.isNumeric(movieMeterId)) {
            LOG.debug("MovieMeter ID not available '{}'", videoData.getIdentifier());
            return ScanResult.MISSING_ID;
        }

        // get movie info 
        FilmInfo filmInfo = movieMeterApiWrapper.getFilmInfo(movieMeterId, throwTempError);
        if (filmInfo == null) {
            LOG.error("Can't find informations for movie '{}'", videoData.getIdentifier());
            return ScanResult.NO_RESULT;
        }

        // set IMDb id if not set before
        String imdbId = videoData.getSourceDbId(ImdbScanner.SCANNER_ID);
        if (StringUtils.isBlank(imdbId)) {
            videoData.setSourceDbId(ImdbScanner.SCANNER_ID, filmInfo.getImdbId());
        }

        // NOTE: MovieMeter has a rating from 0 to 5; but YAMJ use rating up to 100 internally
        videoData.addRating(SCANNER_ID, Math.round(filmInfo.getAverage() * 20f));

        if (OverrideTools.checkOverwriteTitle(videoData, SCANNER_ID)) {
            videoData.setTitle(filmInfo.getDisplayTitle(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(videoData, SCANNER_ID)) {
            videoData.setTitleOriginal(filmInfo.getAlternativeTitle(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteYear(videoData, SCANNER_ID)) {
            videoData.setPublicationYear(filmInfo.getYear(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwritePlot(videoData, SCANNER_ID)) {
            videoData.setPlot(filmInfo.getPlot(), SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteOutline(videoData, SCANNER_ID)) {
            videoData.setOutline(filmInfo.getPlot(), SCANNER_ID);
        }

        if (filmInfo.getCountries() != null && OverrideTools.checkOverwriteCountries(videoData, SCANNER_ID)) {
            Set<String> countryCodes = new HashSet<>();
            for (String country : filmInfo.getCountries()) {
                final String countryCode = localeService.findCountryCode(country);
                if (countryCode != null) countryCodes.add(countryCode);
            }
            videoData.setCountryCodes(countryCodes, SCANNER_ID);
        }

        if (OverrideTools.checkOverwriteGenres(videoData, SCANNER_ID)) {
            videoData.setGenreNames(filmInfo.getGenres(), SCANNER_ID);
        }

        if (this.configServiceWrapper.isCastScanEnabled(JobType.ACTOR)) {
            for (Actor actor : filmInfo.getActors()) {
                if (StringUtils.isNotBlank(actor.getName())) {
                    CreditDTO creditDTO = new CreditDTO(SCANNER_ID, JobType.ACTOR, actor.getName());
                    creditDTO.setVoice(actor.isVoice());
                    videoData.addCreditDTO(creditDTO);
                }
            }
        }

        if (this.configServiceWrapper.isCastScanEnabled(JobType.DIRECTOR)) {
            for (String director : filmInfo.getDirectors()) {
                if (StringUtils.isNotBlank(director)) {
                    videoData.addCreditDTO(new CreditDTO(SCANNER_ID, JobType.DIRECTOR, director));
                }
            }
        }

        return ScanResult.OK;
    }

    @Override
    public boolean scanNFO(String nfoContent, InfoDTO dto, boolean ignorePresentId) {
        // if we already have the ID, skip the scanning of the NFO file
        if (!ignorePresentId && StringUtils.isNumeric(dto.getId(SCANNER_ID))) {
            return true;
        }

        LOG.trace("Scanning NFO for MovieMeter ID");
        
        try {
            int beginIndex = nfoContent.indexOf("www.moviemeter.nl/film/");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfoContent.substring(beginIndex + 23), "/ \n,:!&é\"'(--è_çà)=$");
                String sourceId = st.nextToken();
                LOG.debug("MovieMeter ID found in NFO: {}", sourceId);
                dto.addId(SCANNER_ID, sourceId);
                return true;
            }
        } catch (Exception ex) {
            LOG.trace("NFO scanning error", ex);
        }

        LOG.debug("No MovieMeter ID found in NFO");
        return false;
    }
}
