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

import static org.yamj.core.tools.Constants.DEFAULT_SPLITTER;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.ExecutionTask;
import org.yamj.core.database.model.type.IntervalType;
import org.yamj.core.database.service.ExecutionTaskStorageService;
import org.yamj.core.tools.MetadataTools;

/**
 * Just used for initialization of artwork profiles at startup.
 */
@Component
public class ExecutionTaskInitialization {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionTaskInitialization.class);
    @Autowired
    private ExecutionTaskStorageService executionTaskStorageService;

    @PostConstruct
    public void init() throws Exception {
        LOG.debug("Initialize execution tasks");
        
        String[] tasks = PropertyTools.getProperty("execution.task.init.tasks", "").split(DEFAULT_SPLITTER);
        if (tasks.length > 0) {
            for (String task : tasks) {
                boolean valid = true;

                String name = PropertyTools.getProperty("execution.task." + task + ".name");
                if (StringUtils.isBlank(name)) {
                    LOG.warn("Property 'execution.task.{}.name' is not present", task);
                    valid = false;
                }

                String taskName = PropertyTools.getProperty("execution.task." + task + ".taskName");
                if (StringUtils.isBlank(taskName)) {
                    LOG.warn("Property 'execution.task.{}.taskName' is not present", task);
                    valid = false;
                }

                String type = PropertyTools.getProperty("execution.task." + task + ".type", IntervalType.UNKNOWN.toString());
                IntervalType intervalType = IntervalType.fromString(type);
                if (IntervalType.UNKNOWN == intervalType) {
                    LOG.warn("Property 'execution.task.{}.type' denotes invalid interval type: {}", task, type);
                    valid = false;
                }

                int delay = -1;
                if (intervalType.needsDelay()) {
                    String delayString = PropertyTools.getProperty("execution.task." + task + ".delay");
                    if (!StringUtils.isNumeric(delayString)) {
                        LOG.warn("Property 'execution.task.{}.delay' is not numeric: {}", name, delayString);
                        valid = false;
                    } else {
                        delay = Integer.parseInt(delayString);
                    }
                }

                String dateString = PropertyTools.getProperty("execution.task." + task + ".nextExecution");
                Date nextExecution = MetadataTools.parseToDate(dateString);
                if (nextExecution == null) {
                    LOG.warn("Property 'execution.task.{}.nextExecution' is no valid date: {}", name, dateString);
                    valid = false;
                }
                
                if (!valid) {
                    LOG.warn("Execution task {} has no valid setup, so skipping", task);
                    continue;
                }

                ExecutionTask executionTask = new ExecutionTask();
                executionTask.setName(name);
                executionTask.setTaskName(taskName);
                executionTask.setIntervalType(intervalType);
                executionTask.setDelay(delay);
                executionTask.setNextExecution(nextExecution);
                executionTask.setOptions(PropertyTools.getProperty("execution.task." + task + ".options"));
                
                try {
                    // call another service to handle transactions
                    this.executionTaskStorageService.storeExecutionTask(executionTask);
                } catch (Exception error) {
                    LOG.error("Failed to store execution task {}", executionTask);
                    LOG.warn("Storage error", error);
                }
            }
        }
    }
}
