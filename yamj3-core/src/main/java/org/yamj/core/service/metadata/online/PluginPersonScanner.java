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

import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.core.database.model.Person;
import org.yamj.core.tools.OverrideTools;
import org.yamj.plugin.api.metadata.PersonScanner;
import org.yamj.plugin.api.metadata.dto.PersonDTO;

public class PluginPersonScanner implements IPersonScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginPersonScanner.class);
    private final PersonScanner personScanner;

    public PluginPersonScanner(PersonScanner personScanner) {
        this.personScanner = personScanner;
    }
    
    @Override
    public String getScannerName() {
        return personScanner.getScannerName();
    }
    
    @Override
    public String getPersonId(Person person) {
        return getPersonId(person, false);
    }

    private String getPersonId(Person person, boolean throwTempError) {
        String personId = personScanner.getPersonId(person.getName(), person.getSourceDbIdMap(), throwTempError);
        person.setSourceDbId(getScannerName(), personId);
        return personId;
    }

    @Override
    public ScanResult scanPerson(Person person, boolean throwTempError) {
        String personId = getPersonId(person, throwTempError);
        if (StringUtils.isBlank(personId)) {
            LOG.debug("{} id not available '{}'", getScannerName(), person.getName());
            return ScanResult.MISSING_ID;
        }

        final PersonDTO personDTO = new PersonDTO(person.getSourceDbIdMap());
        final boolean scanned = personScanner.scanPerson(personDTO, throwTempError);
        if (!scanned) {
            LOG.error("Can't find {} informations for person '{}'", getScannerName(), person.getName());
            return ScanResult.NO_RESULT;
        }
        
        // set possible scanned person IDs only if not set before   
        for (Entry<String,String> entry : personDTO.getIds().entrySet()) {
            if (getScannerName().equalsIgnoreCase(entry.getKey())) {
                person.setSourceDbId(entry.getKey(), entry.getValue());
            } else if (StringUtils.isBlank(person.getSourceDbId(entry.getKey()))) {
                person.setSourceDbId(entry.getKey(), entry.getValue());
            }
        }

        if (OverrideTools.checkOverwriteName(person, getScannerName())) {
            person.setName(personDTO.getName(), getScannerName());
        }

        if (OverrideTools.checkOverwriteFirstName(person, getScannerName())) {
            person.setFirstName(personDTO.getFirstName(), getScannerName());
        }

        if (OverrideTools.checkOverwriteLastName(person, getScannerName())) {
            person.setLastName(personDTO.getLastName(), getScannerName());
        }

        if (OverrideTools.checkOverwriteBirthDay(person, getScannerName())) {
            person.setBirthDay(personDTO.getBirthDay(), getScannerName());
        }

        if (OverrideTools.checkOverwriteBirthPlace(person, getScannerName())) {
            person.setBirthPlace(personDTO.getBirthPlace(), getScannerName());
        }

        if (OverrideTools.checkOverwriteBirthName(person, getScannerName())) {
            person.setBirthName(personDTO.getBirthName(), getScannerName());
        }

        if (OverrideTools.checkOverwriteDeathDay(person, getScannerName())) {
            person.setDeathDay(personDTO.getDeathDay(), getScannerName());
        }

        if (OverrideTools.checkOverwriteDeathPlace(person, getScannerName())) {
            person.setDeathPlace(personDTO.getDeathPlace(), getScannerName());
        }

        if (OverrideTools.checkOverwriteBiography(person, getScannerName())) {
            person.setBiography(personDTO.getBiography(), getScannerName());
        }
        
        return ScanResult.OK;
    }
}
