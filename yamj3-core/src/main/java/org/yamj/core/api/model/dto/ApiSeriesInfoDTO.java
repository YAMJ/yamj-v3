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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Studio;
import org.yamj.core.database.model.type.ArtworkType;

@JsonInclude(Include.NON_DEFAULT) 
public class ApiSeriesInfoDTO extends AbstractApiDTO {

    private Long seriesId;
    private String title;
    private String originalTitle;
    private String plot;
    private String outline;
    private Integer year;
    private Boolean watched;
    private List<ApiTargetDTO> genres = new ArrayList<>(0);
    private List<Studio> studios = new ArrayList<>(0);
    private List<ApiTargetDTO> countries = new ArrayList<>(0);
    private List<Certification> certifications = new ArrayList<>(0);
    private List<ApiAwardDTO> awards = new ArrayList<>(0);
    private List<ApiRatingDTO> ratings = new ArrayList<>(0);
    private List<ApiSeasonInfoDTO> seasonList = new ArrayList<>(0);
    
    private Map<ArtworkType, List<ApiArtworkDTO>> artwork = new EnumMap<>(ArtworkType.class);

    public Long getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(Long seriesId) {
        this.seriesId = seriesId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setSeriesYear(Integer seriesYear) {
        this.year = seriesYear;
    }
    
    public Boolean getWatched() {
        return watched;
    }

    public void setWatched(Boolean watched) {
        this.watched = watched;
    }

    public List<ApiTargetDTO> getGenres() {
        return genres;
    }

    public void setGenres(List<ApiTargetDTO> genres) {
        this.genres = genres;
    }

    public int getGenreCount() {
        return genres.size();
    }

    public List<Studio> getStudios() {
        return studios;
    }

    public void setStudios(List<Studio> studios) {
        this.studios = studios;
    }

    public int getStudioCount() {
        return studios.size();
    }

    public List<ApiTargetDTO> getCountries() {
        return countries;
    }
  
    public void setCountries(List<ApiTargetDTO> countries) {
        this.countries = countries;
    }

    public int getCountriesCount() {
        return countries.size();
    }

    public List<Certification> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<Certification> certifications) {
        this.certifications = certifications;
    }

    public int getCertificationCount() {
        return certifications.size();
    }
    
    public List<ApiRatingDTO> getRatings() {
        return ratings;
    }

    public void setRatings(List<ApiRatingDTO> ratings) {
        this.ratings = ratings;
    }

    public int getRatingCount() {
        return ratings.size();
    }

    public List<ApiAwardDTO> getAwards() {
        return awards;
    }

    public void setAwards(List<ApiAwardDTO> awards) {
        this.awards = awards;
    }

    public int getAwardCount() {
        return awards.size();
    }

    public List<ApiSeasonInfoDTO> getSeasonList() {
        return seasonList;
    }

    public void setSeasonList(List<ApiSeasonInfoDTO> seasonList) {
        this.seasonList = seasonList;
    }

    public Map<ArtworkType, List<ApiArtworkDTO>> getArtwork() {
        return artwork;
    }

    public void setArtwork(Map<ArtworkType, List<ApiArtworkDTO>> artwork) {
        this.artwork = artwork;
    }

    public void addArtwork(ApiArtworkDTO newArtwork) {
        // Add a blank list if it doesn't already exist
        if (!artwork.containsKey(newArtwork.getArtworkType())) {
            artwork.put(newArtwork.getArtworkType(), new ArrayList<ApiArtworkDTO>(1));
        }
        this.artwork.get(newArtwork.getArtworkType()).add(newArtwork);
    }

    public int getArtworkCount() {
        int count = 0;
        for (Map.Entry<ArtworkType, List<ApiArtworkDTO>> entry : artwork.entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }
}
