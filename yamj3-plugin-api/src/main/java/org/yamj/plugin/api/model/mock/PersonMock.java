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
package org.yamj.plugin.api.model.mock;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.plugin.api.model.IPerson;

public class PersonMock implements IPerson {

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

    public PersonMock() {
        this.ids = new HashMap<>(1);
    }

    public PersonMock(Map<String, String> ids) {
        this.ids = ids;
    }

    @Override
    public Map<String,String> getIds() {
        return ids;
    }

    @Override
    public String getId(String source) {
        return ids.get(source);
    }
    
    @Override
    public void addId(String source, String id) {
        if (StringUtils.isNotBlank(id)) {
            this.ids.put(source, id);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setNames(String name, String firstName, String lastName) {
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Date getBirthDay() {
        return birthDay;
    }

    @Override
    public void setBirthDay(Date birthDay) {
        this.birthDay = birthDay;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    @Override
    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getBirthName() {
        return birthName;
    }

    @Override
    public void setBirthName(String birthName) {
        this.birthName = birthName;
    }

    public Date getDeathDay() {
        return deathDay;
    }

    @Override
    public void setDeathDay(Date deathDay) {
        this.deathDay = deathDay;
    }

    public String getDeathPlace() {
        return deathPlace;
    }

    @Override
    public void setDeathPlace(String deathPlace) {
        this.deathPlace = deathPlace;
    }

    public String getBiography() {
        return biography;
    }

    @Override
    public void setBiography(String biography) {
        this.biography = biography;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}