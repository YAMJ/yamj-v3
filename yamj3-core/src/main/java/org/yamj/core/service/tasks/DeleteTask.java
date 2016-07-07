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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yamj.core.service.various.DeletionService;

/**
 * Task for checking if video, series or person is older than x days and marks
 * those data entries as updated in order to force a rescan.
 */
@Component
public class DeleteTask implements ITask {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteTask.class);

    @Autowired
    private ExecutionTaskService executionTaskService;
    @Autowired
    private DeletionService deletionService;
    
    @Override
    public String getTaskName() {
        return "delete";
    }

    @PostConstruct
    public void init() {
        executionTaskService.registerTask(this);
    }

    @Override
    public void execute(String options) {
        LOG.debug("Execute delete task");
        final long startTime = System.currentTimeMillis();
        
        this.deletionService.executeAllDeletions();
        
        LOG.debug("Finished delete task after {} ms", System.currentTimeMillis()-startTime);
    }
}
