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
package org.yamj.core.service.artwork.online;

import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.service.artwork.ArtworkScannerService;
import com.moviejukebox.allocine.model.PersonInfos;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.Person;
import org.yamj.core.service.metadata.online.AllocineApiWrapper;
import org.yamj.core.service.metadata.online.AllocineScanner;

@Service("allocineArtworkScanner")
public class AllocineArtworkScanner implements IPhotoScanner {

    private static final Logger LOG = LoggerFactory.getLogger(AllocineArtworkScanner.class);

    @Autowired
    private ArtworkScannerService artworkScannerService;
    @Autowired
    private AllocineScanner allocineScanner;
    @Autowired
    private AllocineApiWrapper allocineApiWrapper;

    @Override
    public String getScannerName() {
        return AllocineScanner.SCANNER_ID;
    }

    @PostConstruct
    public void init() {
        LOG.info("Initialize allocine artwork scanner");

        // register this scanner
        artworkScannerService.registerArtworkScanner(this);
    }
    
    @Override
    public List<ArtworkDetailDTO> getPhotos(Person person) {
        String allocineId = allocineScanner.getPersonId(person);
        if (StringUtils.isBlank(allocineId)) {
            return null;
        }
        
        PersonInfos personInfos = allocineApiWrapper.getPersonInfos(allocineId, false);
        if (personInfos == null || personInfos.isNotValid()) {
            return null;
        }
        
        if (StringUtils.isBlank(personInfos.getPhotoURL())) {
            return null;
        }

        ArtworkDetailDTO dto = new ArtworkDetailDTO(getScannerName(), personInfos.getPhotoURL());
        return Collections.singletonList(dto);
    }
}
