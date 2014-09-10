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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;

@Entity
@Table(name = "person",
    uniqueConstraints = @UniqueConstraint(name = "UIX_PERSON_NATURALID", columnNames = {"name"})
)
@SuppressWarnings("unused")
public class Person extends AbstractAuditable implements Serializable {

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
    
    @Lob
    @Column(name = "biography", length = 50000)
    private String biography;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "person_ids", joinColumns = @JoinColumn(name = "person_id"))
    @ForeignKey(name = "FK_PERSON_SOURCEIDS")
    @Fetch(FetchMode.SELECT)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 40)
    private Map<String, String> personIds = new HashMap<String, String>(0);
    
    @Index(name = "IX_PERSON_STATUS")
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;

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

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getPersonId(String sourcedb) {
        if (personIds.containsKey(sourcedb)) {
            return personIds.get(sourcedb);
        } else {
            return "";
        }
    }

    private Map<String, String> getPersonIdMap() {
        return personIds;
    }

    private void setPersonIdMap(Map<String, String> personIds) {
        this.personIds = personIds;
    }

    public boolean addPersonIds(Map<String,String> personIdMap) {
        boolean changed  = false;
        if (MapUtils.isNotEmpty(personIdMap)) {
            for (Entry<String,String> entry : personIdMap.entrySet()) {
                if (this.addPersonId(entry.getKey(), entry.getValue())) {
                    changed = true;
                }
            }
        }
        return changed;
    }
    
    public boolean addPersonId(String sourceDb, String personId) {
        if (StringUtils.isBlank(sourceDb) || StringUtils.isBlank(personId)) {
            return false;
        }
        this.personIds.put(sourceDb, personId);
        return true;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
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
        return StringUtils.equals(this.name, castOther.name);
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
