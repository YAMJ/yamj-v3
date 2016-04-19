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
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT) 
public class ApiPersonDTO extends AbstractApiIdentifiableDTO {

    private String name;
    private String firstName;
    private String lastName;
    private String biography;
    private String birthDay;
    private String birthPlace;
    private String birthName;
    private String deathDay;
    private String deathPlace;
    private String role;
    private Boolean voiceRole;
    private JobType job;
    private String status;
    private String filmography_status; //NOSONAR
    private List<ApiArtworkDTO> artwork = Collections.emptyList();
    private List<ApiFilmographyDTO> filmography = Collections.emptyList();
    private List<ApiExternalIdDTO> externalIds = Collections.emptyList();

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Date birthDay) {
        this.birthDay = MetadataTools.formatDateShort(birthDay);
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getBirthName() {
        return birthName;
    }

    public void setBirthName(String birthName) {
        this.birthName = birthName;
    }

    public String getDeathDay() {
        return deathDay;
    }

    public void setDeathDay(Date deathDay) {
        this.deathDay = MetadataTools.formatDateShort(deathDay);
    }

    public String getDeathPlace() {
        return deathPlace;
    }

    public void setDeathPlace(String deathPlace) {
        this.deathPlace = deathPlace;
    }
    
    public JobType getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = JobType.fromString(job);
        if (JobType.ACTOR != this.job) {
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
        if (this.job == null || this.job == JobType.ACTOR) {
            this.voiceRole = voiceRole;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilmography_status() {
        return filmography_status;
    }

    public void setFilmography_status(String filmography_status) { //NOSONAR
        this.filmography_status = filmography_status;
    }

    public List<ApiArtworkDTO> getArtwork() {
        return artwork;
    }

    public void setArtwork(List<ApiArtworkDTO> artwork) {
        this.artwork = artwork;
    }

    public List<ApiFilmographyDTO> getFilmography() {
        return filmography;
    }

    public void setFilmography(List<ApiFilmographyDTO> filmography) {
        this.filmography = filmography;
    }
    
    public List<ApiExternalIdDTO> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(List<ApiExternalIdDTO> externalIds) {
        this.externalIds = externalIds;
    }
}
