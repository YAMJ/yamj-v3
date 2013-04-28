package com.moviejukebox.core.database.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "person")
public class Person extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 660066902996412843L;
    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "birth_day")
    private Date birthDay;
    @Column(name = "birth_place", length = 255)
    private String birthPlace;
    @Column(name = "birth_name", length = 255)
    private String birthName;
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "death_day")
    private Date deathDay;
    @Lob
    @Column(name = "biography", length = 50000)
    private String biography;
    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "person_ids", joinColumns =
            @JoinColumn(name = "person_id"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length = 40)
    @Column(name = "moviedb_id", length = 40)
    private Map<String, String> personIds = new HashMap<String, String>(0);

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

    public Map<String, String> getPersonIds() {
        return personIds;
    }

    public void setPersonIds(Map<String, String> personIds) {
        this.personIds = personIds;
    }

    public void setPersonId(String moviedb, String personId) {
        this.personIds.put(moviedb, personId);
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        final int PRIME = 17;
        int result = 1;
        result = PRIME * result + (this.name == null ? 0 : this.name.hashCode());
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
}
