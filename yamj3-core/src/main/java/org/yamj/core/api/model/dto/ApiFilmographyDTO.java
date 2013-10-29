/*
 *      Copyright (c) 2004-2013 YAMJ Members
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

import org.yamj.common.type.MetaDataType;

public class ApiFilmographyDTO extends AbstractApiDTO {

    private MetaDataType videoType = MetaDataType.UNKNOWN;
    private String job = "";
    private String role = "";
    private Long videoId = -1L;
    private String videoTitle = "";
    private Integer videoYear = -1;
    private Long seasonId = -1L;
    private Integer season = -1;
    private Integer episode = -1;

    public void setVideoType(MetaDataType videoType) {
        this.videoType = videoType;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public void setVideoYear(Integer videoYear) {
        this.videoYear = videoYear;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public void setSeasonId(Long seasonId) {
        this.seasonId = seasonId;
    }

    public void setEpisode(Integer episode) {
        this.episode = episode;
    }

    public MetaDataType getVideoType() {
        return videoType;
    }

    public String getJob() {
        return job;
    }

    public String getRole() {
        return role;
    }

    public Long getVideoId() {
        return videoId;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public Integer getVideoYear() {
        return videoYear;
    }

    public Integer getSeason() {
        return season;
    }

    public Long getSeasonId() {
        return seasonId;
    }

    public Integer getEpisode() {
        return episode;
    }
}
