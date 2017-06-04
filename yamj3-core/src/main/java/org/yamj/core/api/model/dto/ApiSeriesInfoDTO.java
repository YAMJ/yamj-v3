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
import java.util.Collections;
import java.util.List;
import org.yamj.core.database.model.Studio;
import org.yamj.core.database.model.Library;

@JsonInclude(Include.NON_DEFAULT) 
public class ApiSeriesInfoDTO extends AbstractMetaDataDTO {

    private Long seriesId;
    private List<ApiGenreDTO> genres = Collections.emptyList();
    private List<Studio> studios = Collections.emptyList();
	private List<Library> libraries = Collections.emptyList();
    private List<ApiCountryDTO> countries = Collections.emptyList();
    private List<ApiCertificationDTO> certifications = Collections.emptyList();
    private List<ApiAwardDTO> awards = Collections.emptyList();
    private List<ApiRatingDTO> ratings = Collections.emptyList();
    private List<ApiSeasonInfoDTO> seasonList = Collections.emptyList();

    public Long getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(Long seriesId) {
        this.seriesId = seriesId;
    }

    public List<ApiGenreDTO> getGenres() {
        return genres;
    }

    public void setGenres(List<ApiGenreDTO> genres) {
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
	
	//add getLibraries
	public List<Library> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<Library> libraries) {
        this.libraries = libraries;
    }

    public int getLibraryCount() {
        return libraries.size();
    }
	// end library

    public List<ApiCountryDTO> getCountries() {
        return countries;
    }
  
    public void setCountries(List<ApiCountryDTO> countries) {
        this.countries = countries;
    }

    public int getCountriesCount() {
        return countries.size();
    }

    public List<ApiCertificationDTO> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<ApiCertificationDTO> certifications) {
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
}
