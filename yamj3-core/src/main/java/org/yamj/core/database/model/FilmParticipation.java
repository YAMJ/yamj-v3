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
package org.yamj.core.database.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.yamj.plugin.api.model.type.JobType;
import org.yamj.plugin.api.model.type.ParticipationType;

@Entity
@Table(name = "participation",
        uniqueConstraints = @UniqueConstraint(name = "UIX_PARTICIPATION_NATURALID", columnNames = {"person_id", "sourcedb", "sourcedb_id", "job"})
)
public class FilmParticipation extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 8182882526775933702L;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "person_id", nullable = false, foreignKey = @ForeignKey(name = "FK_PARTICIPATION_PERSON"))
    private Person person;

    @NaturalId(mutable = true)
    @Column(name = "sourcedb", nullable = false, length = 40)
    private String sourceDb;

    @NaturalId(mutable = true)
    @Column(name = "sourcedb_id", nullable = false, length = 40)
    private String sourceDbId;

    @NaturalId(mutable = true)
    @Type(type = "jobType")
    @Column(name = "job", nullable = false, length = 30)
    private JobType jobType;

    @Column(name = "role", length = 255)
    private String role;

    @Column(name = "voice_role", nullable = false)
    private boolean voiceRole = false;

    @Type(type = "participationType")
    @Column(name = "participation_type", nullable = false, length = 15)
    private ParticipationType participationType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "title_original", length = 255)
    private String titleOriginal;

    @Lob
    @Column(name = "description", length = 50000)
    private String description;

    @Column(name = "year", nullable = false)
    private int year = -1;

    @Column(name = "year_end", nullable = false)
    private int yearEnd = -1;

    @Column(name = "release_country_code", length = 4)
    private String releaseCountryCode;

    @Temporal(value = TemporalType.DATE)
    @Column(name = "release_date")
    private Date releaseDate;

    // GETTER and SETTER
    
    public String getSourceDb() {
        return sourceDb;
    }

    public void setSourceDb(String sourceDb) {
        this.sourceDb = sourceDb;
    }

    public String getSourceDbId() {
        return sourceDbId;
    }

    public void setSourceDbId(String sourceDbId) {
        this.sourceDbId = sourceDbId;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (role != null) {
            // Truncate the role to 255 characters
            this.role = StringUtils.substring(role, 0, Math.min(role.length(), 254));
        } else {
            this.role = null;
        }
    }

    public boolean isVoiceRole() {
        return voiceRole;
    }

    public void setVoiceRole(boolean voiceRole) {
        this.voiceRole = voiceRole;
    }

    public ParticipationType getParticipationType() {
        return participationType;
    }

    public void setParticipationType(ParticipationType participationType) {
        this.participationType = participationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getYearEnd() {
        return yearEnd;
    }

    public void setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
    }

    public String getReleaseCountryCode() {
        return releaseCountryCode;
    }

    public void setReleaseCountryCode(String releaseCountryCode) {
        this.releaseCountryCode = releaseCountryCode;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    // TRANSIENT METHODS
    public void merge(FilmParticipation newFilmo) {
        setRole(newFilmo.getRole());
        setVoiceRole(newFilmo.isVoiceRole());
        setParticipationType(newFilmo.getParticipationType());
        setYear(newFilmo.getYear());
        setYearEnd(newFilmo.getYearEnd());
        setTitle(newFilmo.getTitle());
        setTitleOriginal(newFilmo.getTitleOriginal());
        setDescription(newFilmo.getDescription());
        setReleaseDate(newFilmo.getReleaseDate());
        setReleaseCountryCode(newFilmo.getReleaseCountryCode());
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getSourceDb())
                .append(getSourceDbId())
                .append(getJobType())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FilmParticipation) {
            FilmParticipation other = (FilmParticipation) obj;
            return new EqualsBuilder()
                    .append(getSourceDb(), other.getSourceDb())
                    .append(getSourceDbId(), other.getSourceDbId())
                    .append(getJobType(), other.getJobType())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Participation [ID=");
        sb.append(getId());
        sb.append(", sourceDb=");
        sb.append(getSourceDb());
        sb.append(", sourceDbId=");
        sb.append(getSourceDbId());
        sb.append(", title=");
        sb.append(getTitle());
        sb.append(", year=");
        sb.append(getYear());
        sb.append("]");
        return sb.toString();
    }
}
