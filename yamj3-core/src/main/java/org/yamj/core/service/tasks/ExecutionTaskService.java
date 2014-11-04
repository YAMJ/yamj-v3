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
package org.yamj.core.service.tasks;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.core.database.model.ExecutionTask;
import org.yamj.core.database.model.type.IntervalType;
import org.yamj.core.database.service.ExecutionTaskStorageService;

@Service("executionTaskService")
public class ExecutionTaskService {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionTaskService.class);
    private Map<String,ITask> registeredTasks = new HashMap<String,ITask>();

    @Autowired
    private ExecutionTaskStorageService executionTaskStorageService;

    /**
     * Register a task.
     *
     * @param task
     */
    public void registerTask(ITask task) {
        LOG.debug("Registered task: {}", task.getTaskName().toLowerCase());
        registeredTasks.put(task.getTaskName().toLowerCase(), task);
    }

    public List<ExecutionTask> getTasksForExecution() {
        return this.executionTaskStorageService.getTasksForExecution();
    }
    
    public void executeTask(ExecutionTask executionTask) {
        ITask task = registeredTasks.get(executionTask.getTaskName().toLowerCase());

        if (task == null) {
            LOG.warn("Task " + executionTask.getTaskName() + " not registered");
        } else {
            try {
                task.execute(executionTask.getOptions());
            } catch (Exception ex) {
                LOG.error("Failed to execute task '" + task.getTaskName() + "'", ex);
            }
        }

        if (IntervalType.ONCE == executionTask.getIntervalType()) {
            // just delete the task after executed once
            try {
                this.executionTaskStorageService.deleteEntity(executionTask);
            } catch (Exception ex) {
                LOG.error("Failed to delete execution task: " + executionTask.getName(), ex);
            }
            // nothing to do for this task anymore
            return;
        }

        try {
            Calendar nextCal = Calendar.getInstance();
            nextCal.setTime(executionTask.getNextExecution());

            Calendar cal = Calendar.getInstance();
            if (IntervalType.MONTHLY == executionTask.getIntervalType()) {
                cal.add(Calendar.MONTH, 1);
                cal.set(Calendar.DAY_OF_MONTH, nextCal.get(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, nextCal.get(Calendar.HOUR_OF_DAY));
                cal.set(Calendar.MINUTE, nextCal.get(Calendar.MINUTE));
                cal.set(Calendar.SECOND, nextCal.get(Calendar.SECOND));
                cal.set(Calendar.MILLISECOND, 0);
            }
            else if (IntervalType.DAILY == executionTask.getIntervalType()) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, nextCal.get(Calendar.HOUR_OF_DAY));
                cal.set(Calendar.MINUTE, nextCal.get(Calendar.MINUTE));
                cal.set(Calendar.SECOND, nextCal.get(Calendar.SECOND));
                cal.set(Calendar.MILLISECOND, 0);
            }
            else if (IntervalType.DAYS == executionTask.getIntervalType()) {
                cal.add(Calendar.DAY_OF_MONTH, executionTask.getDelay());
                cal.set(Calendar.HOUR_OF_DAY, nextCal.get(Calendar.HOUR_OF_DAY));
                cal.set(Calendar.MINUTE, nextCal.get(Calendar.MINUTE));
                cal.set(Calendar.SECOND, nextCal.get(Calendar.SECOND));
                cal.set(Calendar.MILLISECOND, 0);
            }
            else if (IntervalType.HOURS == executionTask.getIntervalType()) {
                cal.add(Calendar.HOUR_OF_DAY, executionTask.getDelay());
                cal.set(Calendar.MINUTE, nextCal.get(Calendar.MINUTE));
                cal.set(Calendar.SECOND, nextCal.get(Calendar.SECOND));
                cal.set(Calendar.MILLISECOND, 0);
            }
            else if (IntervalType.MINUTES == executionTask.getIntervalType()) {
                cal.add(Calendar.MINUTE, executionTask.getDelay());
                cal.set(Calendar.SECOND, nextCal.get(Calendar.SECOND));
                cal.set(Calendar.MILLISECOND, 0);
            }
            
            executionTask.setLastExecution(new Date());
            executionTask.setNextExecution(cal.getTime());
            this.executionTaskStorageService.updateEntity(executionTask);
        } catch (Exception ex) {
            LOG.error("Failed to update: {}", executionTask);
            LOG.warn("Storage error", ex);
        }
    }
}
