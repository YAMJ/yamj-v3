/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.api.model.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author stuart.boston
 */
public class ApiPersonDTO extends AbstractApiIdentifiableDTO {

    private String name;
    private String biography;
    private Date birthDay;
    private String birthPlace;
    private String birthName;
    private Date deathDay;
    private String job = "";
    private String role = "";
    List<ApiArtworkDTO> artwork = new ArrayList<ApiArtworkDTO>(0);
    List<ApiFilmographyDTO> filmography = new ArrayList<ApiFilmographyDTO>(0);

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

    public Date getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Date birthDay) {
        this.birthDay = birthDay;
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

    public Date getDeathDay() {
        return deathDay;
    }

    public void setDeathDay(Date deathDay) {
        this.deathDay = deathDay;
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

    public void addArtwrok(ApiArtworkDTO artwork) {
        this.artwork.add(artwork);
    }

    public List<ApiFilmographyDTO> getFilmography() {
        return filmography;
    }

    public void setFilmography(List<ApiFilmographyDTO> filmography) {
        this.filmography = filmography;
    }

    public void addFilmography(ApiFilmographyDTO film) {
        this.filmography.add(film);
    }
}
