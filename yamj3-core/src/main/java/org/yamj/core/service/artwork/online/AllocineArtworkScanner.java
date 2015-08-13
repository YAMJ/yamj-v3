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
package org.yamj.core.service.artwork.online;

import com.moviejukebox.allocine.model.*;
import java.util.*;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import org.yamj.core.service.metadata.online.AllocineApiWrapper;
import org.yamj.core.service.metadata.online.AllocineScanner;

@Service("allocineArtworkScanner")
public class AllocineArtworkScanner implements IMoviePosterScanner, ITvShowPosterScanner, IPhotoScanner {

    private static final Logger LOG = LoggerFactory.getLogger(AllocineArtworkScanner.class);

    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private AllocineScanner allocineScanner;
    @Autowired
    private AllocineApiWrapper allocineApiWrapper;

    @Override
    public String getScannerName() {
        return allocineScanner.getScannerName();
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize Allocine artwork scanner");

        // register this scanner
        artworkScannerService.registerArtworkScanner(this);
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(VideoData videoData) {
        String allocineId = allocineScanner.getMovieId(videoData);
        if (StringUtils.isBlank(allocineId)) {
            return null;
        }

        MovieInfos movieInfos = allocineApiWrapper.getMovieInfos(allocineId, false);
        if (movieInfos == null || movieInfos.isNotValid() || MapUtils.isEmpty(movieInfos.getPosters())) {
            return null;
        }
        return buildArtworkDetails(movieInfos.getPosters());
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Series series) {
        String allocineId = allocineScanner.getSeriesId(series);
        if (StringUtils.isBlank(allocineId)) {
            return null;
        }

        TvSeriesInfos tvSeriesInfos = allocineApiWrapper.getTvSeriesInfos(allocineId, false);
        if (tvSeriesInfos == null || tvSeriesInfos.isNotValid() || MapUtils.isEmpty(tvSeriesInfos.getPosters())) {
            return null;
        }
        return buildArtworkDetails(tvSeriesInfos.getPosters());
    }

    @Override
    public List<ArtworkDetailDTO> getPosters(Season season) {
        String allocineId = season.getSourceDbId(getScannerName());
        if (StringUtils.isBlank(allocineId)) {
            return null;
        }

        TvSeasonInfos tvSeasonInfos = allocineApiWrapper.getTvSeasonInfos(allocineId, false);
        if (tvSeasonInfos == null || tvSeasonInfos.isNotValid() || MapUtils.isEmpty(tvSeasonInfos.getPosters())) {
            return null;
        }
        return buildArtworkDetails(tvSeasonInfos.getPosters());
    }

    private List<ArtworkDetailDTO> buildArtworkDetails(Map<String,Long> artworks) {
        List<ArtworkDetailDTO> dtos = new ArrayList<>(artworks.size());
        for (Entry<String,Long> entry : artworks.entrySet()) {
            final String hashCode = (entry.getValue() == null ? null : entry.getValue().toString());
            ArtworkDetailDTO dto = new ArtworkDetailDTO(getScannerName(), entry.getKey(), hashCode);
            dtos.add(dto);
        }
        return dtos;
    }
    
    @Override
    public List<ArtworkDetailDTO> getPhotos(Person person) {
        String allocineId = allocineScanner.getPersonId(person);
        if (StringUtils.isBlank(allocineId)) {
            return null;
        }
        
        PersonInfos personInfos = allocineApiWrapper.getPersonInfos(allocineId, false);
        if (personInfos == null || personInfos.isNotValid() || StringUtils.isBlank(personInfos.getPhotoURL())) {
            return null;
        }

        ArtworkDetailDTO dto = new ArtworkDetailDTO(getScannerName(), allocineId, personInfos.getPhotoURL());
        return Collections.singletonList(dto);
    }
}
