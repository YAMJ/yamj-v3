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
package org.yamj.core.database.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.ExecutionTask;

@Service("executionTaskStorageService")
public class ExecutionTaskStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionTaskStorageService.class);
    @Autowired
    private CommonDao commonDao;

    @Transactional
    public void storeExecutionTask(ExecutionTask executionTask) {
        ExecutionTask stored = commonDao.getByNameCaseInsensitive(ExecutionTask.class, executionTask.getName());
        if (stored == null) {
            this.commonDao.saveEntity(executionTask);
            LOG.info("Stored: {}", executionTask);
        } else {
            stored.setTaskName(executionTask.getTaskName());
            stored.setOptions(executionTask.getOptions());
            stored.setIntervalType(executionTask.getIntervalType());
            stored.setDelay(executionTask.getDelay());
            if (executionTask.getNextExecution().after(stored.getNextExecution())) {
                stored.setNextExecution(executionTask.getNextExecution());
            }
            this.commonDao.updateEntity(stored);
            LOG.info("Updated: {}", stored);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<ExecutionTask> getTasksForExecution() {
        String query = "from ExecutionTask et where et.nextExecution <= :actualDate";
        Map<String,Object> params = Collections.singletonMap("actualDate", (Object)new Date());
        return (List<ExecutionTask>)this.commonDao.findByNamedParameters(query, params);
    }
    
    @Transactional
    public void updateEntity(Object entity) {
        this.commonDao.updateEntity(entity);
    }
}
