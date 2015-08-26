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
package org.yamj.core.database.service;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yamj.core.database.dao.UpgradeDatabaseDao;

@Component
public class UpgradeDatabaseService {

    private static final Logger LOG = LoggerFactory.getLogger(UpgradeDatabaseService.class);

    @Autowired
    private UpgradeDatabaseDao upgradeDatabaseDao;
    
    @PostConstruct
    public void init() {
        // Issues: #234, #237, enhancements
        // Date:   10.08.2015
        try {
            upgradeDatabaseDao.patchConfiguration();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchConfiguration'", ex);
        }

        // Issues: #222
        // Date:   18.07.2015
        try {
            upgradeDatabaseDao.patchTrailers();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchTrailers'", ex);
        }

        // Issues: #234
        // Date:   25.07.2015
        try {
            upgradeDatabaseDao.patchCertifications();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchCertifications'", ex);
        }

        // Issues: #234
        // Date:   26.07.2015
        try {
            upgradeDatabaseDao.patchCountries();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchCountries'", ex);
        }

        // Issues: #234
        // Date:   27.07.2015
        try {
            upgradeDatabaseDao.patchReleaseCountryFilmo();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchReleaseCountryFilmo'", ex);
        }

        // Issues: #234
        // Date:   28.07.2015
        try {
            upgradeDatabaseDao.patchLanguageAudioCodes();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchLanguageAudioCodes'", ex);
        }

        // Issues: #234
        // Date:   28.07.2015
        try {
            upgradeDatabaseDao.patchLanguageSubtitles();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchLanguageSubtitles'", ex);
        }

        // Issues: enhancement
        // Date:   10.08.2015
        try {
            upgradeDatabaseDao.patchArtworkLocated();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchArtworkLocated'", ex);
        }

        // Issues: enhancement
        // Date:   10.08.2015
        try {
            upgradeDatabaseDao.patchBoxedSetIdentifier();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchBoxedSetIdentifier'", ex);
        }

        // Issues: database schema
        // Date:   10.08.2015
        try {
            upgradeDatabaseDao.patchStudio();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchStudio'", ex);
        }

        // Issues: enhancement
        // Date:   15.08.2015
        try {
            upgradeDatabaseDao.patchDatabaseLongVarchars();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchDatabaseLongVarchars'", ex);
        }

        // Issues:  #193
        // Date:   25.08.2015
        try {
            upgradeDatabaseDao.patchWatched();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchWatched'", ex);
        }

        // Issues: enhancement
        // Date:   26.08.2015
        try {
            upgradeDatabaseDao.patchInvalidForeinKeys();
        } catch (Exception ex) {
            LOG.warn("Failed upgrade 'patchInvalidForeinKeys'", ex);
        }
}
}
