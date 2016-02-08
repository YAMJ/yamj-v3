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
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yamj.core.database.model.ExecutionTask;
import org.yamj.core.database.model.type.IntervalType;
import org.yamj.core.database.service.ExecutionTaskStorageService;

/**
 * Just used for initialization of execution tasks on startup.
 */
@Component
public class ExecutionTaskInitialization {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionTaskInitialization.class);
    
    @Autowired
    private ExecutionTaskStorageService executionTaskStorageService;

    @PostConstruct
    public void init() throws Exception {
        LOG.debug("Initialize execution tasks");
        storeExecutionTask("recheck", "recheck", IntervalType.DAILY, -1, new LocalDateTime(2016,1,1,3,0));
        storeExecutionTask("delete", "delete", IntervalType.HOURS, 2, new LocalDateTime(2016,1,1,0,0));
        storeExecutionTask("artworksanity", "artworksanity", IntervalType.MONTHLY, -1, new LocalDateTime(2016,1,1,2,0));
        storeExecutionTask("trakttv", "trakttv", IntervalType.DAILY, -1, new LocalDateTime(2016,1,1,4,0));
    }
    
    private void storeExecutionTask(String name, String taskName, IntervalType interval, int delay, LocalDateTime nextExec) {
        try {
            ExecutionTask task = executionTaskStorageService.getExecutionTask(name);
            if (task == null) {
                task = new ExecutionTask();
                task.setName(name);
                task.setTaskName(taskName);
                task.setIntervalType(interval);
                task.setDelay(delay);
                task.setNextExecution(nextExec.toDate());
                this.executionTaskStorageService.saveEntity(task);
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize task: "+name, e);
        }
    }
}
