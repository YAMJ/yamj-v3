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
package org.yamj.core.service.tasks;

import java.util.Calendar;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.scheduling.ScanningScheduler;

/**
 * Task for checking if video, series or person is older than x days
 * and marks those data entries as updated in order to force a rescan. 
 */
@Component
public class RecheckTask implements ITask {

    private static final Logger LOG = LoggerFactory.getLogger(RecheckTask.class);
    
    @Autowired
    private ExecutionTaskService executionTaskService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private MetadataStorageService metadataStorageService;
    @Autowired
    private ScanningScheduler scanningScheduler;
    
    @Override
    public String getTaskName() {
        return "recheck";
    }

    @PostConstruct
    public void init() {
        executionTaskService.registerTask(this);
    }

    @Override
    public void execute(String options) throws Exception {
        LOG.debug("Execute recheck task");

        int recheck = this.configService.getIntProperty("yamj3.recheck.movie.maxDays", 45);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, (0-recheck));
        boolean updatedMovies = this.metadataStorageService.recheckMovie(cal.getTime());

        recheck = this.configService.getIntProperty("yamj3.recheck.tvshow.maxDays", 45);
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, (0-recheck));
        boolean updatedSeries = this.metadataStorageService.recheckTvShow(cal.getTime());

        recheck = this.configService.getIntProperty("yamj3.recheck.person.maxDays", 90);
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, (0-recheck));
        boolean updatedPersons = this.metadataStorageService.recheckPerson(cal.getTime());
        
        if (updatedMovies || updatedSeries) {
            scanningScheduler.triggerScanMetaData();
        }
        if (updatedPersons) {
            scanningScheduler.triggerScanPeopleData();
        }
    }
}
