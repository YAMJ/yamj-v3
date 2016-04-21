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
import java.util.Map;

public class PersonDTO {

    private final Map<String, String> ids;
    private String name;
    private String firstName;
    private String lastName;
    private Date birthDay;
    private String birthPlace;
    private String birthName;
    private Date deathDay;
    private String deathPlace;
    private String biography;
    
    public PersonDTO(Map<String, String> ids) {
        this.ids = ids;
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public PersonDTO addId(String source, String id) {
        if (id != null && id.length() > 0) {
            this.ids.put(source, id);
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public PersonDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public PersonDTO setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public PersonDTO setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Date getBirthDay() {
        return birthDay;
    }

    public PersonDTO setBirthDay(Date birthDay) {
        this.birthDay = birthDay;
        return this;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public PersonDTO setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
        return this;
    }

    public String getBirthName() {
        return birthName;
    }

    public PersonDTO setBirthName(String birthName) {
        this.birthName = birthName;
        return this;
    }

    public Date getDeathDay() {
        return deathDay;
    }

    public PersonDTO setDeathDay(Date deathDay) {
        this.deathDay = deathDay;
        return this;
    }

    public String getDeathPlace() {
        return deathPlace;
    }

    public PersonDTO setDeathPlace(String deathPlace) {
        this.deathPlace = deathPlace;
        return this;
    }

    public String getBiography() {
        return biography;
    }

    public PersonDTO setBiography(String biography) {
        this.biography = biography;
        return this;
    }
}