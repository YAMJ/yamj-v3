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

import org.yamj.plugin.api.metadata.tools.MetadataTools;

import org.yamj.plugin.api.common.JobType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.Studio;

/**
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT)
public class ApiVideoDTO extends AbstractMetaDataDTO {

    private MetaDataType videoType;
    private String sortTitle;
    private Integer videoYear = -1;
    private Date releaseDate;
    private String quote;
    private String tagline;
    private Integer topRank = -1;
    private Long seriesId;
    private Long seasonId;
    private Long season;
    private Long episode = -1L;
    private Date newest;
    private String status;
    private String videoSource;
    private List<ApiGenreDTO> genres = Collections.emptyList();
    private List<Studio> studios = Collections.emptyList();
    private List<ApiCountryDTO> countries = Collections.emptyList();
    private List<ApiCertificationDTO> certifications = Collections.emptyList();
    private List<ApiRatingDTO> ratings = Collections.emptyList();
    private List<ApiAwardDTO> awards = Collections.emptyList();
    private List<ApiFileDTO> files = Collections.emptyList();
    private final Map<JobType,List<ApiPersonDTO>> cast = new EnumMap<>(JobType.class);
    private List<ApiExternalIdDTO> externalIds = Collections.emptyList();
    private List<ApiBoxedSetDTO> boxedSets = Collections.emptyList();
    private List<ApiTrailerDTO> trailers = Collections.emptyList();

    //<editor-fold defaultstate="collapsed" desc="Getter Methods">
    public MetaDataType getVideoType() {
        return videoType;
    }

    public String getSortTitle() {
        return sortTitle;
    }

    public Integer getVideoYear() {
        return videoYear;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public Map<JobType, List<ApiPersonDTO>> getCast() {
        return cast;
    }

    public int getCastCount() {
        int count = 0;
        for (Map.Entry<JobType, List<ApiPersonDTO>> entry : cast.entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }

    public List<ApiGenreDTO> getGenres() {
        return genres;
    }

    public int getGenreCount() {
        return genres.size();
    }

    public List<Studio> getStudios() {
        return studios;
    }

    public int getStudioCount() {
        return studios.size();
    }

    public List<ApiCountryDTO> getCountries() {
        return countries;
    }

    public int getCountriesCount() {
        return countries.size();
    }

    public List<ApiCertificationDTO> getCertifications() {
        return certifications;
    }

    public int getCertificationCount() {
        return certifications.size();
    }

    public List<ApiRatingDTO> getRatings() {
        return ratings;
    }

    public int getRatingCount() {
        return ratings.size();
    }

    public List<ApiAwardDTO> getAwards() {
        return awards;
    }

    public int getAwardCount() {
        return awards.size();
    }

    public String getQuote() {
        return quote;
    }

    public String getTagline() {
        return tagline;
    }

    public Integer getTopRank() {
        return topRank;
    }

    public Long getSeriesId() {
        return seriesId;
    }

    public Long getSeasonId() {
        return seasonId;
    }

    public Long getSeason() {
        return season;
    }

    public Long getEpisode() {
        return episode;
    }

    public String getNewest() {
        return MetadataTools.formatDateLong(this.newest);
    }

    public String getStatus() {
        return status;
    }

    public String getVideoSource() {
        return videoSource;
    }

    public List<ApiFileDTO> getFiles() {
        return files;
    }

    public List<ApiExternalIdDTO> getExternalIds() {
        return externalIds;
    }
    
    public List<ApiBoxedSetDTO> getBoxedSets() {
        return boxedSets;
    }
    
    public int getBoxedSetCount() {
        return boxedSets.size();
    }

    public List<ApiTrailerDTO> getTrailers() {
        return trailers;
    }
    
    public int getTrailerCount() {
        return trailers.size();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Setter Methods">
    public void setVideoType(String videoType) {
        this.videoType = MetaDataType.fromString(videoType);
    }

    public void setSortTitle(String sortTitle) {
        this.sortTitle = sortTitle;
    }

    public void setVideoYear(Integer videoYear) {
        this.videoYear = videoYear;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setFirstAired(String firstAired) {
        if (StringUtils.isNotBlank(firstAired) && firstAired.length() >= 4) {
            String year = firstAired.substring(0, 4);
            if (StringUtils.isNumeric(year)) {
                setVideoYear(Integer.parseInt(year));
            }
        }
    }

    public void setArtwork(List<ApiArtworkDTO> artworkList) {
        for (ApiArtworkDTO dto : artworkList) {
            addArtwork(dto);
        }
    }

    public void setCast(List<ApiPersonDTO> castList) {
        for (ApiPersonDTO dto : castList) {
            addCast(dto);
        }
    }

    public void setGenres(List<ApiGenreDTO> genres) {
        this.genres = genres;
    }

    public void setCountries(List<ApiCountryDTO> countries) {
        this.countries = countries;
    }

    public void setStudios(List<Studio> studios) {
        this.studios = studios;
    }

    public void setCertifications(List<ApiCertificationDTO> certifications) {
        this.certifications = certifications;
    }

    public void setRatings(List<ApiRatingDTO> ratings) {
        this.ratings = ratings;
    }

    public void setAwards(List<ApiAwardDTO> awards) {
        this.awards = awards;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public void setTopRank(Integer topRank) {
        this.topRank = topRank;
    }

    public void setSeriesId(Long seriesId) {
        this.seriesId = seriesId;
    }

    public void setSeasonId(Long seasonId) {
        this.seasonId = seasonId;
    }

    public void setSeason(Long season) {
        this.season = season;
    }

    public void setEpisode(Long episode) {
        this.episode = episode;
    }

    public void setNewest(Date newest) {
        this.newest = newest;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setVideoSource(String videoSource) {
        this.videoSource = videoSource;
    }

    public void setFiles(List<ApiFileDTO> files) {
        this.files = files;
    }

    public void setExternalIds(List<ApiExternalIdDTO> externalIds) {
        this.externalIds = externalIds;
    }
    
    public void setBoxedSets(List<ApiBoxedSetDTO> boxedSets) {
        this.boxedSets = boxedSets;
    }
    
    public void setTrailers(List<ApiTrailerDTO> trailers) {
        this.trailers = trailers;
    }
    //</editor-fold>

    public void addCast(ApiPersonDTO newCast) {
        // Add a blank list if it doesn't already exist
        if (!cast.containsKey(newCast.getJob())) {
            cast.put(newCast.getJob(), new ArrayList<ApiPersonDTO>(1));
        }
        this.cast.get(newCast.getJob()).add(newCast);
    }
}
