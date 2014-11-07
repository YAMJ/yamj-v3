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
package org.yamj.core.database.model;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.OverrideFlag;

@Entity
@Table(name = "person",
    uniqueConstraints = @UniqueConstraint(name = "UIX_PERSON_NATURALID", columnNames = {"name"})
)
@SuppressWarnings("unused")
public class Person extends AbstractAuditable implements IScannable, Serializable {

    private static final long serialVersionUID = 660066902996412843L;
    
    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Temporal(value = TemporalType.DATE)
    @Column(name = "birth_day")
    private Date birthDay;
    
    @Column(name = "birth_place", length = 255)
    private String birthPlace;
    
    @Column(name = "birth_name", length = 255)
    private String birthName;
    
    @Temporal(value = TemporalType.DATE)
    @Column(name = "death_day")
    private Date deathDay;

    @Column(name = "death_place", length = 255)
    private String deathPlace;

    @Lob
    @Column(name = "biography", length = 50000)
    private String biography;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "person_ids", joinColumns = @JoinColumn(name = "person_id"))
    @ForeignKey(name = "FK_PERSON_SOURCEIDS")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 40)
    private Map<String, String> sourceDbIdMap = new HashMap<String, String>(0);
    
    @Index(name = "IX_PERSON_STATUS")
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "last_scanned")
    private Date lastScanned;
    
    @Column(name = "retries", nullable = false)
    private int retries = 0;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "person_override", joinColumns = @JoinColumn(name = "person_id"))
    @ForeignKey(name = "FK_PERSON_OVERRIDE")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<OverrideFlag, String>(OverrideFlag.class);

    @Index(name = "IX_PERSON_FILMOGRAPHY_STATUS")
    @Type(type = "statusType")
    @Column(name = "filmography_status", length = 30)
    private StatusType filmographyStatus;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
    private Set<FilmParticipation> filmography = new HashSet<FilmParticipation>(0);
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
    private Artwork photo;
    
    @Transient
    private Map<String,String> photoURLS = new HashMap<String,String>(0);

    @Transient
    private Set<FilmParticipation> newFilmography = new HashSet<FilmParticipation>(0);
    
    // GETTER and SETTER
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBirthDay() {
        return birthDay;
    }

    private void setBirthDay(Date birthDay) {
        this.birthDay = birthDay;
    }

    public void setBirthDay(Date birthDay, String source) {
        if (birthDay != null) {
            this.birthDay = birthDay;
            setOverrideFlag(OverrideFlag.BIRTHDAY, source);
        }
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    private void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public void setBirthPlace(String birthPlace, String source) {
        if (StringUtils.isNotBlank(birthPlace)) {
            this.birthPlace = birthPlace.trim();
            setOverrideFlag(OverrideFlag.BIRTHPLACE, source);
        }
    }

    public String getBirthName() {
        return birthName;
    }

    private void setBirthName(String birthName) {
        this.birthName = birthName;
    }

    public void setBirthName(String birthName, String source) {
        if (StringUtils.isNotBlank(birthName)) {
            this.birthName = birthName.trim();
            setOverrideFlag(OverrideFlag.BIRTHNAME, source);
        }
    }

    public Date getDeathDay() {
        return deathDay;
    }

    private void setDeathDay(Date deathDay) {
        this.deathDay = deathDay;
    }
    
    public void setDeathDay(Date deathDay, String source) {
        if (deathDay != null) {
            this.deathDay = deathDay;
            setOverrideFlag(OverrideFlag.DEATHDAY, source);
        }
    }

    public String getDeathPlace() {
        return deathPlace;
    }

    private void setDeathPlace(String deathPlace) {
        this.deathPlace = deathPlace;
    }

    public void setDeathPlace(String deathPlace, String source) {
        if (StringUtils.isNotBlank(deathPlace)) {
            this.deathPlace = deathPlace.trim();
            setOverrideFlag(OverrideFlag.DEATHPLACE, source);
        }
    }

    public String getBiography() {
        return biography;
    }

    private void setBiography(String biography) {
        this.biography = biography;
    }

    public void setBiography(String biography, String source) {
        if (StringUtils.isNotBlank(biography)) {
            this.biography = biography.trim();
            setOverrideFlag(OverrideFlag.BIOGRAPHY, source);
        }
    }

    private Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    @Override
    public String getSourceDbId(String sourceDb) {
        return sourceDbIdMap.get(sourceDb);
    }

    private void setSourceDbIdMap(Map<String, String> sourceDbIdMap) {
        this.sourceDbIdMap = sourceDbIdMap;
    }

    @Override
    public void setSourceDbId(String sourceDb, String id) {
        if (StringUtils.isBlank(sourceDb) || StringUtils.isBlank(id)) {
            return;
        }
        this.sourceDbIdMap.put(sourceDb, id.trim());
    }

    public boolean setSourceDbIds(Map<String,String> sourceDbIdMap) {
        boolean changed  = false;
        if (MapUtils.isNotEmpty(sourceDbIdMap)) {
            for (Entry<String,String> entry : sourceDbIdMap.entrySet()) {
                String sourceDb = entry.getKey();
                String newId = StringUtils.trimToNull(entry.getValue());
                if (StringUtils.isNotBlank(sourceDb) && StringUtils.isNotBlank(newId)) {
                    String oldId = this.sourceDbIdMap.put(sourceDb, newId);
                    if (!StringUtils.equals(oldId, newId)) {
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    @Override
    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }
    
    @Override
    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    public void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    @Override
    public void setOverrideFlag(OverrideFlag overrideFlag, String source) {
        this.overrideFlags.put(overrideFlag, source.toLowerCase());
    }

    @Override
    public String getOverrideSource(OverrideFlag overrideFlag) {
        return overrideFlags.get(overrideFlag);
    }

    public StatusType getFilmographyStatus() {
        return filmographyStatus;
    }

    public void setFilmographyStatus(StatusType filmographyStatus) {
        this.filmographyStatus = filmographyStatus;
    }

    public Set<FilmParticipation> getFilmography() {
        return filmography;
    }

    public void setFilmography(Set<FilmParticipation> filmography) {
        this.filmography = filmography;
    }

    public Artwork getPhoto() {
        return photo;
    }

    public void setPhoto(Artwork photo) {
        this.photo = photo;
    }
    
    // TRANSIENT METHODS

    public Map<String, String> getPhotoURLS() {
        return photoURLS;
    }

    public void addPhotoURL(String photoURL, String source) {
        if (StringUtils.isNotBlank(photoURL)) {
            this.photoURLS.put(photoURL, source);
        }
    }

    public Set<FilmParticipation> getNewFilmography() {
        return newFilmography;
    }

    public void setNewFilmography(Set<FilmParticipation> newFilmography) {
        this.newFilmography = newFilmography;
    }
    
    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Person)) {
            return false;
        }
        Person castOther = (Person) other;
        return StringUtils.equalsIgnoreCase(this.name, castOther.name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Person [ID=");
        sb.append(getId());
        sb.append(", name=");
        sb.append(getName());
        sb.append("]");
        return sb.toString();
    }
}
