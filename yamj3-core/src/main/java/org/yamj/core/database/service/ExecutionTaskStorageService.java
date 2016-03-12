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
package org.yamj.core.database.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.ExecutionTask;

@Service("executionTaskStorageService")
public class ExecutionTaskStorageService {

    @Autowired
    private CommonDao commonDao;

    @Transactional
    public void saveEntity(ExecutionTask executionTask) {
        this.commonDao.saveEntity(executionTask);
    }

    @Transactional
    public void updateEntity(ExecutionTask executionTask) {
        this.commonDao.updateEntity(executionTask);
    }

    @Transactional
    public void deleteEntity(ExecutionTask executionTask) {
        this.commonDao.deleteEntity(executionTask);
    }

    @Transactional(readOnly = true)
    public ExecutionTask getExecutionTask(String name) {
        return commonDao.getByNaturalIdCaseInsensitive(ExecutionTask.class, "name", name);
    }
    
    @Transactional(readOnly = true)
    public List<ExecutionTask> getAllTasks() {
        return this.commonDao.getAll(ExecutionTask.class, "name");
    }

    @Transactional(readOnly = true)
    public List<ExecutionTask> getTasksForExecution() {
        String query = "from ExecutionTask et where et.nextExecution <= :actualDate";
        Map<String,Object> params = Collections.singletonMap("actualDate", (Object)new Date());
        return this.commonDao.findByNamedParameters(query, params);
    }
}
