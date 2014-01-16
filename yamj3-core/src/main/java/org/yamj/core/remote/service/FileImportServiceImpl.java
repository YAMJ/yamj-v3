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
package org.yamj.core.remote.service;

import org.yamj.core.service.staging.StagingService;

import org.yamj.common.dto.ImportDTO;
import org.yamj.common.remote.service.FileImportService;
import org.yamj.core.database.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("fileImportService")
public class FileImportServiceImpl implements FileImportService {

    private static final Logger LOG = LoggerFactory.getLogger(FileImportServiceImpl.class);
    @Autowired
    private StagingService stagingService;

    @Override
    public void importScanned(ImportDTO importDTO) {
        Library library;
        try {
            library = stagingService.storeLibrary(importDTO);
            stagingService.storeStageDirectory(importDTO.getStageDirectory(), library);
            LOG.debug("Imported scanned directory: {}", importDTO.getStageDirectory().getPath());
        } catch (Exception error) {
            LOG.error("Failed to import scanned directory: {}", importDTO.getStageDirectory().getPath(), error);
            throw new RuntimeException("Failed to import scanned directory: "+importDTO.getStageDirectory().getPath());
        }
    }
}
