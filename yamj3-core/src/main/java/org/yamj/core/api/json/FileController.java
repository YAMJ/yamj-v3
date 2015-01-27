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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.yamj.core.api.model.ApiStatus;
import org.yamj.core.api.options.OptionsId;
import org.yamj.core.database.service.CommonStorageService;
import org.yamj.core.service.staging.StagingService;

@Controller
@ResponseBody
@RequestMapping(value = "/api/file/**", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
public class FileController {

    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);
    @Autowired
    private StagingService stagingService;
    @Autowired
    private CommonStorageService commonStorageService;
    
    /**
     * Mark a stage file as deleted.
     *
     * @param options
     * @return
     */
    @RequestMapping("/delete/{id}")
    public ApiStatus deleteFileById(@ModelAttribute("options") OptionsId options) {
        ApiStatus status = new ApiStatus();
        Long id = options.getId();
        if (id != null && id > 0L) {
            LOG.info("Deleting file '{}'", id);
            boolean result = this.stagingService.deleteStageFile(id);
            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully marked file '" + id + "' as deleted");
            } else {
                status.setStatus(400);
                status.setMessage("File not found: " + id);
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid file id specified");
        }
        return status;
    }

    /**
     * Mark a stage file as updated.
     *
     * @param options
     * @return
     */
    @RequestMapping("/update/{id}")
    public ApiStatus updateFileById(@ModelAttribute("options") OptionsId options) {
        ApiStatus status = new ApiStatus();
        Long id = options.getId();
        if (id != null && id > 0L) {
            LOG.info("Updating file '{}'", id);
            boolean result = this.stagingService.updateStageFile(id);
            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully marked file '" + id + "' as updated");
            } else {
                status.setStatus(400);
                status.setMessage("File not found: " + id);
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid file id specified");
        }
        return status;
    }

    /**
     * Mark a stage file as watched.
     *
     * @param options
     * @return
     */
    @RequestMapping("/watched/{id}")
    public ApiStatus watchedFileById(@ModelAttribute("options") OptionsId options) {
        ApiStatus status = new ApiStatus();
        Long id = options.getId();
        if (id != null && id > 0L) {
            LOG.info("Watched file '{}'", id);
            boolean result = this.commonStorageService.toogleWatchedStatus(id, true, true);
            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully marked file '" + id + "' as watched");
            } else {
                status.setStatus(400);
                status.setMessage("File not found or invalid: " + id);
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid file id specified");
        }
        return status;
    }

    /**
     * Mark a stage file as watched.
     *
     * @param options
     * @return
     */
    @RequestMapping("/unwatched/{id}")
    public ApiStatus unwatchedFileById(@ModelAttribute("options") OptionsId options) {
        ApiStatus status = new ApiStatus();
        Long id = options.getId();
        if (id != null && id > 0L) {
            LOG.info("Unwatched file '{}'", id);
            boolean result = this.commonStorageService.toogleWatchedStatus(id, false, true);
            if (result) {
                status.setStatus(200);
                status.setMessage("Successfully marked file '" + id + "' as unwatched");
            } else {
                status.setStatus(400);
                status.setMessage("File not found or invalid: " + id);
            }
        } else {
            status.setStatus(400);
            status.setMessage("Invalid file id specified");
        }
        return status;
    }
}
