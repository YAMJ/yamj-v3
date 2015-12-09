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
package org.yamj.core.api.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.database.service.CommonStorageService;
import org.yamj.core.service.staging.StagingService;

@RestController
@RequestMapping(value = "/api/file", produces = "application/json; charset=utf-8")
public class FileController {

    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);
    
    @Autowired
    private StagingService stagingService;
    @Autowired
    private CommonStorageService commonStorageService;

    /**
     * Mark a stage file as deleted.
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.DELETE})
    public ApiStatus deleteFile(@PathVariable("id") Long id) {
        if (id <= 0L) {
            return ApiStatus.INVALID_ID;
        }
        
        LOG.info("Deleting file {}", id);
        
        final ApiStatus status;
        if (this.stagingService.deleteStageFile(id)) {
            status = statusOK(id, "deleted");
        } else {
            status = statusNotFound(id);
        }
        return status;
    }

    /**
     * Mark a stage file as updated.
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/update/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus updateFile(@PathVariable("id") Long id) {
        if (id <= 0L) {
            return ApiStatus.INVALID_ID;
        }

        LOG.info("Updating file {}", id);

        final ApiStatus status;
        if (this.stagingService.updateStageFile(id)) {
            status = statusOK(id, "updated");
        } else {
            status = statusNotFound(id);
        }
        return status;
    }

    /**
     * Mark a stage file as watched.
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/watched/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus watchedFile(@PathVariable("id") Long id) {
        if (id <= 0L) {
            return ApiStatus.INVALID_ID;
        }

        LOG.info("Watched file {}", id);

        final ApiStatus status;
        if (this.commonStorageService.toogleWatchedStatus(id, true, true)) {
            status = statusOK(id, "watched");
        } else {
            status = statusNotFound(id);
        }
        return status;
    }

    /**
     * Mark a stage file as unwatched.
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/unwatched/{id}", method = {RequestMethod.GET, RequestMethod.PUT})
    public ApiStatus unwatchedFile(@PathVariable("id") Long id) {
        if (id <= 0L) {
            return ApiStatus.INVALID_ID;
        }

        LOG.info("Unwatched file {}", id);

        final ApiStatus status;
        if (this.commonStorageService.toogleWatchedStatus(id, false, true)) {
            status = statusOK(id, "unwatched");
        } else {
            status = statusNotFound(id);
        }
        return status;
    }
    
    private static ApiStatus statusOK(Long id, String status) {
        return ApiStatus.ok("Sucessfully marked file " + id + " as " + status);
    }

    private static ApiStatus statusNotFound(Long id) {
        return ApiStatus.badRequest("File " + id + " not found");
    }
}
