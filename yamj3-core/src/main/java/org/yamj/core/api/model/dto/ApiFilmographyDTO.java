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
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.yamj.plugin.api.metadata.MetadataTools;
import org.yamj.plugin.api.model.type.JobType;
import org.yamj.plugin.api.model.type.ParticipationType;

@JsonInclude(Include.NON_DEFAULT) 
public class ApiFilmographyDTO extends AbstractApiDTO {

    private ParticipationType type;
    private String job;
    private String role;
    private Boolean voiceRole = null;
    private String title;
    private String originalTitle;
    private Integer year;
    private Integer yearEnd = -1;
    private String releaseDate;
    private String releaseCountryCode;
    private String releaseCountry;
    private String description;
    private Long videoDataId;
    private Long seriesId;

    public ParticipationType getType() {
        return type;
    }

    public void setType(String type) {
        this.type = ParticipationType.fromString(type);
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
        if (!StringUtils.equalsIgnoreCase(this.job, JobType.ACTOR.name())) {
            this.voiceRole = null;
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getVoiceRole() {
        return voiceRole;
    }

    public void setVoiceRole(Boolean voiceRole) {
        if (this.job == null || this.job.equalsIgnoreCase(JobType.ACTOR.name())) {
            this.voiceRole = voiceRole;
        }
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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getYearEnd() {
        return yearEnd;
    }

    public void setYearEnd(Integer yearEnd) {
        this.yearEnd = yearEnd;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = MetadataTools.formatDateShort(releaseDate);
    }

    public String getReleaseCountryCode() {
        return releaseCountryCode;
    }

    public void setReleaseCountryCode(String releaseCountryCode) {
        this.releaseCountryCode = releaseCountryCode;
    }

    public String getReleaseCountry() {
        return releaseCountry;
    }

    public void setReleaseCountry(String releaseCountry) {
        this.releaseCountry = releaseCountry;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVideoDataId() {
        return videoDataId;
    }

    public void setVideoDataId(Long videoDataId) {
        this.videoDataId = videoDataId;
    }

    public Long getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(Long seriesId) {
        this.seriesId = seriesId;
    }
}