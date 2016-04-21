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
package org.yamj.plugin.api.metadata;

import java.util.Date;
import org.yamj.plugin.api.type.JobType;
import org.yamj.plugin.api.type.ParticipationType;

public class FilmographyDTO {

    private String id;
    private JobType jobType;
    private String role;
    private boolean voiceRole = false;
    private ParticipationType participationType;
    private String title;
    private String originalTitle;
    private String description;
    private int year = -1;
    private int yearEnd = -1;
    private String releaseCountry;
    private Date releaseDate;

    public String getId() {
        return id;
    }

    public FilmographyDTO setId(String id) {
        this.id = id;
        return this;
    }

    public JobType getJobType() {
        return jobType;
    }

    public FilmographyDTO setJobType(JobType jobType) {
        this.jobType = jobType;
        return this;
    }

    public String getRole() {
        return role;
    }

    public FilmographyDTO setRole(String role) {
        this.role = role;
        return this;
    }

    public boolean isVoiceRole() {
        return voiceRole;
    }

    public FilmographyDTO setVoiceRole(boolean voiceRole) {
        this.voiceRole = voiceRole;
        return this;
    }

    public ParticipationType getParticipationType() {
        return participationType;
    }

    public FilmographyDTO setParticipationType(ParticipationType participationType) {
        this.participationType = participationType;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FilmographyDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public FilmographyDTO setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public FilmographyDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    public int getYear() {
        return year;
    }

    public FilmographyDTO setYear(int year) {
        this.year = year;
        return this;
    }

    public int getYearEnd() {
        return yearEnd;
    }

    public FilmographyDTO setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
        return this;
    }

    public String getReleaseCountry() {
        return releaseCountry;
    }

    public FilmographyDTO setReleaseCountry(String releaseCountry) {
        this.releaseCountry = releaseCountry;
        return this;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public FilmographyDTO setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }
}