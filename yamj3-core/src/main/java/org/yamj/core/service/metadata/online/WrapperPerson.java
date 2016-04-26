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
package org.yamj.core.service.metadata.online;

import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.yamj.core.database.model.Person;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.metadata.MetadataTools;
import org.yamj.plugin.api.metadata.PersonName;
import org.yamj.plugin.api.model.IPerson;

public class WrapperPerson implements IPerson {

    private final Person person;
    private String scannerName;
    
    public WrapperPerson(Person person) {
        this.person = person;
    }

    public void setScannerName(String actualSource) {
        this.scannerName = actualSource;
    }
    
    @Override
    public String getId(String source) {
        return person.getSourceDbId(source);
    }

    @Override
    public void addId(String source, String id) {
        if (scannerName.equalsIgnoreCase(source)) { 
            person.setSourceDbId(source, id);
        } else if (StringUtils.isBlank(person.getSourceDbId(source))) {
            person.setSourceDbId(source, id);
        }
    }

    @Override
    public String getName() {
        return person.getName();
    }

    @Override
    public void setName(String name) {
        PersonName personName = MetadataTools.splitFullName(name);
        if (OverrideTools.checkOverwriteName(person, scannerName)) {
            person.setName(personName.getName(), scannerName);
        }
        if (OverrideTools.checkOverwriteFirstName(person, scannerName)) {
            person.setFirstName(personName.getFirstName(), scannerName);
        }
        if (OverrideTools.checkOverwriteLastName(person, scannerName)) {
            person.setLastName(personName.getLastName(), scannerName);
        }
    }

    @Override
    public void setNames(String name, String firstName, String lastName) {
        if (OverrideTools.checkOverwriteName(person, scannerName)) {
            person.setName(name, scannerName);
        }
        if (OverrideTools.checkOverwriteFirstName(person, scannerName)) {
            person.setFirstName(firstName, scannerName);
        }
        if (OverrideTools.checkOverwriteLastName(person, scannerName)) {
            person.setLastName(lastName, scannerName);
        }
    }

    @Override
    public void setBirthDay(Date birthDay) {
        if (OverrideTools.checkOverwriteBirthDay(person, scannerName)) {
            person.setBirthDay(birthDay, scannerName);
        }
    }

    @Override
    public void setBirthPlace(String birthPlace) {
        if (OverrideTools.checkOverwriteBirthPlace(person, scannerName)) {
            person.setBirthPlace(birthPlace, scannerName);
        }
    }

    @Override
    public void setBirthName(String birthName) {
        if (OverrideTools.checkOverwriteBirthName(person, scannerName)) {
            person.setBirthName(birthName, scannerName);
        }
    }

    @Override
    public void setDeathDay(Date deathDay) {
        if (OverrideTools.checkOverwriteDeathDay(person, scannerName)) {
            person.setDeathDay(deathDay, scannerName);
        }
    }

    @Override
    public void setDeathPlace(String deathPlace) {
        if (OverrideTools.checkOverwriteDeathPlace(person, scannerName)) {
            person.setDeathPlace(deathPlace, scannerName);
        }
    }

    @Override
    public void setBiography(String biography) {
        if (OverrideTools.checkOverwriteBiography(person, scannerName)) {
            person.setBiography(biography, scannerName);
        }
    }
}
