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
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.Certification;
import org.yamj.core.database.model.Studio;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.tools.MetadataTools;

/**
 *
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT)
public class ApiVideoDTO extends AbstractApiIdentifiableDTO {

    private MetaDataType videoType;
    private String title;
    private String originalTitle;
    private Integer videoYear;
    private String outline;
    private String plot;
    private String quote;
    private String tagline;
    private Integer topRank = -1;
    private Long seriesId;
    private Long seasonId;
    private Long season;
    private Long episode;
    private Boolean watched;
    private Date newest;
    private List<ApiTargetDTO> genres = new ArrayList<>();
    private List<Studio> studios = new ArrayList<>();
    private List<ApiTargetDTO> countries = new ArrayList<>();
    private List<Certification> certifications = new ArrayList<>();
    private List<ApiRatingDTO> ratings = new ArrayList<>();
    private List<ApiAwardDTO> awards = new ArrayList<>();
    private final Map<ArtworkType, List<ApiArtworkDTO>> artwork = new EnumMap<>(ArtworkType.class);
    private List<ApiFileDTO> files = new ArrayList<>();
    private final Map<JobType,List<ApiPersonDTO>> cast = new EnumMap<>(JobType.class);
    private List<ApiExternalIdDTO> externalids=new ArrayList<>();

    //<editor-fold defaultstate="collapsed" desc="Getter Methods">
    public MetaDataType getVideoType() {
        return videoType;
    }

    public String getTitle() {
        return title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public Integer getVideoYear() {
        return videoYear;
    }

    public Map<ArtworkType, List<ApiArtworkDTO>> getArtwork() {
        return artwork;
    }

    public int getArtworkCount() {
        int count = 0;
        for (Map.Entry<ArtworkType, List<ApiArtworkDTO>> entry : artwork.entrySet()) {
            count += entry.getValue().size();
        }
        return count;
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

    public List<ApiTargetDTO> getGenres() {
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

    public List<ApiTargetDTO> getCountries() {
        return countries;
    }

    public int getCountriesCount() {
        return countries.size();
    }

    public List<Certification> getCertifications() {
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

    public String getOutline() {
        return outline;
    }

    public String getPlot() {
        return plot;
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

    public List<ApiFileDTO> getFiles() {
        return files;
    }

    public List<ApiExternalIdDTO> getExternalids() {
        return externalids;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Setter Methods">
    public void setVideoType(MetaDataType videoType) {
        this.videoType = videoType;
    }

    public void setVideoTypeString(String videoType) {
        this.videoType = MetaDataType.fromString(videoType);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public void setVideoYear(Integer videoYear) {
        this.videoYear = videoYear;
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
        for (ApiArtworkDTO aadto : artworkList) {
            addArtwork(aadto);
        }
    }

    public void setCast(List<ApiPersonDTO> castList) {
        for (ApiPersonDTO acdto : castList) {
            addCast(acdto);
        }
    }

    public void setGenres(List<ApiTargetDTO> genres) {
        this.genres = genres;
    }

    public void setCountries(List<ApiTargetDTO> countries) {
        this.countries = countries;
    }

    public void setStudios(List<Studio> studios) {
        this.studios = studios;
    }

    public void setCertifications(List<Certification> certifications) {
        this.certifications = certifications;
    }

    public void setRatings(List<ApiRatingDTO> ratings) {
        this.ratings = ratings;
    }

    public void setAwards(List<ApiAwardDTO> awards) {
        this.awards = awards;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public void setPlot(String plot) {
        this.plot = plot;
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

    public Boolean getWatched() {
        return watched;
    }

    public void setWatched(Boolean watched) {
        this.watched = watched;
    }

    public String getNewest() {
        return MetadataTools.formatDateLong(this.newest);
    }

    public void setNewest(Date newest) {
        this.newest = newest;
    }

    public void setFiles(List<ApiFileDTO> files) {
        this.files = files;
    }

    public void setExternalids(List<ApiExternalIdDTO> externalids) {
        this.externalids = externalids;
    }
    //</editor-fold>

    public void addArtwork(ApiArtworkDTO newArtwork) {
        // Add a blank list if it doesn't already exist
        if (!artwork.containsKey(newArtwork.getArtworkType())) {
            artwork.put(newArtwork.getArtworkType(), new ArrayList<ApiArtworkDTO>(1));
        }
        this.artwork.get(newArtwork.getArtworkType()).add(newArtwork);
    }

    public void addCast(ApiPersonDTO newCast) {
        // Add a blank list if it doesn't already exist
        if (!cast.containsKey(newCast.getJobType())) {
            cast.put(newCast.getJobType(), new ArrayList<ApiPersonDTO>(1));
        }
        this.cast.get(newCast.getJobType()).add(newCast);
    }
}
