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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.tools.MetadataTools;

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
    private String job;
    private String role;
    private List<ApiArtworkDTO> artwork = new ArrayList<>(0);
    private List<ApiFilmographyDTO> filmography = new ArrayList<>(0);
    private List<ApiExternalIdDTO> externalIds = new ArrayList<>(0);
    
    @JsonIgnore
    private JobType jobType;

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
    
    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(String job) {
        this.jobType = JobType.fromString(job);
    }
}
