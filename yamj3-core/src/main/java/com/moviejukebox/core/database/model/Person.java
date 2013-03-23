package com.moviejukebox.core.database.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "person")
public class Person extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 660066902996412843L;

    @NaturalId(mutable = true)
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NaturalId(mutable = true)
    @Column(name = "birthDay", length = 255)
    private String birthDay;

    @Column(name = "birthPlace", length = 255)
    private String birthPlace;

    @Column(name = "birthName", length = 255)
    private String birthName;
   
    @Lob
    @Column(name = "biography")
    private String biography;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_PERSON_PHOTO")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "photoId")
    private Artwork photo;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_PERSON_BACKDROP")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "backdropId")
    private Artwork backdrop;

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "person_ids", joinColumns = @JoinColumn(name = "personId"))
    @Fetch(value = FetchMode.SELECT)
    @MapKeyColumn(name = "moviedb", length= 40)
    @Column(name = "id", length = 40)
    private Map<String, String> personIds = new HashMap<String, String>(0);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(String birthDay) {
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

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public Artwork getPhoto() {
        return photo;
    }

    public void setPhoto(Artwork photo) {
        this.photo = photo;
    }

    public Artwork getBackdrop() {
        return backdrop;
    }

    public void setBackdrop(Artwork backdrop) {
        this.backdrop = backdrop;
    }

    public Map<String, String> getPersonIds() {
        return personIds;
    }

    public void setPersonIds(Map<String, String> personIds) {
        this.personIds = personIds;
    }
}
