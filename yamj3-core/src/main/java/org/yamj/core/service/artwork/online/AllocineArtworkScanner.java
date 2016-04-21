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
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.Season;
import org.yamj.core.service.metadata.online.AllocineScanner;
import org.yamj.core.web.apis.AllocineApiWrapper;
import org.yamj.plugin.api.artwork.ArtworkDTO;
import org.yamj.plugin.api.artwork.ArtworkTools;

@Service("allocineArtworkScanner")
public class AllocineArtworkScanner implements IMovieArtworkScanner, ISeriesArtworkScanner, IPersonArtworkScanner {

    @Autowired
    private AllocineScanner allocineScanner;
    @Autowired
    private AllocineApiWrapper allocineApiWrapper;

    @Override
    public String getScannerName() {
        return allocineScanner.getScannerName();
    }

    @Override
    public List<ArtworkDTO> getPosters(VideoData videoData) {
        String allocineId = allocineScanner.getMovieId(videoData);
        if (StringUtils.isBlank(allocineId)) {
            return Collections.emptyList();
        }

        MovieInfos movieInfos = allocineApiWrapper.getMovieInfos(allocineId, false);
        if (movieInfos == null || movieInfos.isNotValid() || MapUtils.isEmpty(movieInfos.getPosters())) {
            return Collections.emptyList();
        }
        return buildArtworkDetails(movieInfos.getPosters());
    }

    @Override
    public List<ArtworkDTO> getFanarts(VideoData videoData) {
        return Collections.emptyList();
    }

    @Override
    public List<ArtworkDTO> getPosters(Series series) {
        String allocineId = allocineScanner.getSeriesId(series);
        if (StringUtils.isBlank(allocineId)) {
            return Collections.emptyList();
        }

        TvSeriesInfos tvSeriesInfos = allocineApiWrapper.getTvSeriesInfos(allocineId, false);
        if (tvSeriesInfos == null || tvSeriesInfos.isNotValid() || MapUtils.isEmpty(tvSeriesInfos.getPosters())) {
            return Collections.emptyList();
        }
        return buildArtworkDetails(tvSeriesInfos.getPosters());
    }

    @Override
    public List<ArtworkDTO> getPosters(Season season) {
        String allocineId = season.getSourceDbId(getScannerName());
        if (StringUtils.isBlank(allocineId)) {
            return Collections.emptyList();
        }

        TvSeasonInfos tvSeasonInfos = allocineApiWrapper.getTvSeasonInfos(allocineId);
        if (tvSeasonInfos == null || tvSeasonInfos.isNotValid() || MapUtils.isEmpty(tvSeasonInfos.getPosters())) {
            return Collections.emptyList();
        }
        return buildArtworkDetails(tvSeasonInfos.getPosters());
    }

    @Override
    public List<ArtworkDTO> getFanarts(Season season) {
        return Collections.emptyList();
    }

    @Override
    public List<ArtworkDTO> getFanarts(Series series) {
        return Collections.emptyList();
    }

    @Override
    public List<ArtworkDTO> getBanners(Season season) {
        return Collections.emptyList();
    }

    @Override
    public List<ArtworkDTO> getBanners(Series series) {
        return Collections.emptyList();
    }

    @Override
    public List<ArtworkDTO> getVideoImages(VideoData videoData) {
        return Collections.emptyList();
    }

    private List<ArtworkDTO> buildArtworkDetails(Map<String,Long> artworks) {
        List<ArtworkDTO> dtos = new ArrayList<>(artworks.size());
        for (Entry<String,Long> entry : artworks.entrySet()) {
            final String hashCode;
            if (entry.getValue() == null || entry.getValue().longValue() == 0) {
                hashCode = ArtworkTools.getSimpleHashCode(entry.getKey());
            } else {
                hashCode = entry.getValue().toString();
            }
            ArtworkDTO dto = new ArtworkDTO(getScannerName(), entry.getKey(), hashCode);
            dtos.add(dto);
        }
        return dtos;
    }
    
    @Override
    public List<ArtworkDTO> getPhotos(Person person) {
        String allocineId = allocineScanner.getPersonId(person);
        if (StringUtils.isBlank(allocineId)) {
            return Collections.emptyList();
        }
        
        PersonInfos personInfos = allocineApiWrapper.getPersonInfos(allocineId, false);
        if (personInfos == null || personInfos.isNotValid() || StringUtils.isBlank(personInfos.getPhotoURL())) {
            return Collections.emptyList();
        }

        ArtworkDTO dto = new ArtworkDTO(getScannerName(), personInfos.getPhotoURL(), allocineId);
        return Collections.singletonList(dto);
    }
}
