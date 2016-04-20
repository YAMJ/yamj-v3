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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.core.config.LocaleService;
import org.yamj.core.database.model.FilmParticipation;
import org.yamj.core.database.model.Person;
import org.yamj.plugin.api.metadata.FilmographyScanner;
import org.yamj.plugin.api.metadata.dto.FilmographyDTO;
import org.yamj.plugin.api.type.ParticipationType;

public class PluginFilmographyScanner implements IFilmographyScanner {

    private final Logger LOG = LoggerFactory.getLogger(PluginFilmographyScanner.class);
    private final FilmographyScanner filmographyScanner;
    private final LocaleService localeService;
    
    public PluginFilmographyScanner(FilmographyScanner filmographyScanner, LocaleService localeService) {
        this.filmographyScanner = filmographyScanner;
        this.localeService = localeService;
    }
    
    @Override
    public String getScannerName() {
        return filmographyScanner.getScannerName();
    }
    

    private String getPersonId(Person person, boolean throwTempError) {
        String personId = filmographyScanner.getPersonId(person.getName(), person.getIdMap(), throwTempError);
        person.setSourceDbId(getScannerName(), personId);
        return personId;
    }

    @Override
    public ScanResult scanFilmography(Person person, boolean throwTempError) {
        String personId = getPersonId(person, throwTempError);
        if (StringUtils.isBlank(personId)) {
            LOG.debug("{} id not available '{}'", getScannerName(), person.getName());
            return ScanResult.MISSING_ID;
        }

        // get filmography
        List<FilmographyDTO> filmography = filmographyScanner.scanFilmography(personId, throwTempError);
        if (filmography == null || filmography.isEmpty()) {
            LOG.trace("No {} filmography for person '{}'", getScannerName(), person.getName());
            return ScanResult.NO_RESULT;
        }
        
        Set<FilmParticipation> newFilmography = new HashSet<>();
        for (FilmographyDTO dto : filmography) {
            FilmParticipation filmo = new FilmParticipation();
            filmo.setPerson(person);
            filmo.setSourceDb(getScannerName());
            filmo.setSourceDbId(dto.getId());
            filmo.setJobType(dto.getJobType());
            filmo.setParticipationType(dto.getParticipationType());
            
            if (ParticipationType.SERIES.equals(dto.getParticipationType())) {
                filmo.setYear(dto.getYear());
                filmo.setYearEnd(dto.getYearEnd());
            } else {
                filmo.setYear(dto.getYear());
            }
            
            filmo.setTitle(dto.getTitle());
            filmo.setTitleOriginal(StringUtils.trimToNull(dto.getOriginalTitle()));
            filmo.setDescription(StringUtils.trimToNull(dto.getDescription()));
            filmo.setReleaseDate(dto.getReleaseDate());
            filmo.setReleaseCountryCode(localeService.findCountryCode(dto.getReleaseCountry()));
            newFilmography.add(filmo);
        }
        
        person.setNewFilmography(newFilmography);
        
        return ScanResult.OK;
    }
}
