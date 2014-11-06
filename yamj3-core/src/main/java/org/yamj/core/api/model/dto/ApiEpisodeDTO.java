/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT) 
public class ApiEpisodeDTO extends AbstractApiIdentifiableDTO {

    private Long seriesId = -1L;
    private Long seasonId = -1L;
    private Long season = -1L;
    private Long episode = -1L;
    private String title;
    private String originalTitle;
    private String outline;
    private String plot;
    private Date firstAired;
    private Boolean watched;
    private String cacheFilename;
    private String cacheDir;
    private String videoimage;
    private List<ApiFileDTO> files = new ArrayList<ApiFileDTO>();
    private List<ApiGenreDTO> genres = new ArrayList<ApiGenreDTO>();

    //<editor-fold defaultstate="collapsed" desc="Setter Methods">
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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }
    
    public void setFirstAired(Date firstAired) {
        this.firstAired = firstAired;
    }

    public Boolean getWatched() {
        return watched;
    }

    public void setWatched(Boolean watched) {
        this.watched = watched;
    }

    @JsonIgnore
    public void setCacheFilename(String cacheFilename) {
        this.cacheFilename = cacheFilename;
    }

    @JsonIgnore
    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public void setFiles(List<ApiFileDTO> files) {
        this.files = files;
    }

    public void setGenres(List<ApiGenreDTO> genres) {
        this.genres = genres;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getter Methods">
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

    public String getTitle() {
        return title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getOutline() {
        return outline;
    }

    public String getPlot() {
        return plot;
    }

    public Date getFirstAired() {
        return firstAired;
    }

    public String getVideoimage() {
        if (StringUtils.isBlank(videoimage) && (StringUtils.isNotBlank(cacheDir) && StringUtils.isNotBlank(cacheFilename))) {
            this.videoimage = FilenameUtils.normalize(FilenameUtils.concat(this.cacheDir, this.cacheFilename), Boolean.TRUE);
        }
        return videoimage;
    }

    public List<ApiFileDTO> getFiles() {
        return files;
    }
    
    public List<ApiGenreDTO> getGenres() {
        return genres;
    }
    //</editor-fold>
}
