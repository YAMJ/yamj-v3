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

import javax.annotation.PostConstruct;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.service.MetadataStorageService;
import org.yamj.core.scheduling.MetadataScanScheduler;

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
    private MetadataScanScheduler metadataScanScheduler;
    
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
        final long startTime = System.currentTimeMillis();

        int limit = this.configService.getIntProperty("yamj3.recheck.movie.maxLimit", 50);
        if (limit > 0) {
            int recheck = this.configService.getIntProperty("yamj3.recheck.movie.maxDays", 60);
            if (metadataStorageService.recheckMovie(new DateTime().minusDays(recheck).toDate(), limit)) {
                metadataScanScheduler.triggerScanVideo();
            }
        }

        limit = this.configService.getIntProperty("yamj3.recheck.tvshow.maxLimit", 20);
        if (limit > 0) {
            int recheck = this.configService.getIntProperty("yamj3.recheck.tvshow.maxDays", 60);
            if (metadataStorageService.recheckTvShow(new DateTime().minusDays(recheck).toDate(), limit)) {
                metadataScanScheduler.triggerScanVideo();
            }
        }

        limit = this.configService.getIntProperty("yamj3.recheck.person.maxLimit", 100);
        if (limit > 0) {
            int recheck = this.configService.getIntProperty("yamj3.recheck.person.maxDays", 90);
            if (metadataStorageService.recheckPerson(new DateTime().minusDays(recheck).toDate(), limit)) {
                metadataScanScheduler.triggerScanPeople();
            }
        }

        LOG.debug("Finished recheck task after {} ms", System.currentTimeMillis()-startTime);
    }
}
