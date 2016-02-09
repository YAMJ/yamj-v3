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
package org.yamj.core.pages;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.core.database.model.ExecutionTask;
import org.yamj.core.database.service.ExecutionTaskStorageService;

@Controller
@RequestMapping(value = "/task")
public class TaskPagesController extends AbstractPagesController {

    @Autowired
    private ExecutionTaskStorageService executionTaskStorageService;

    @RequestMapping("/list")
    public ModelAndView taskList() {
        ModelAndView view = withInfo(new ModelAndView("task/task-list"));
        view.addObject("tasklist", executionTaskStorageService.getAllTasks());
        return view;
    }

    @RequestMapping(value = "/enqueue/{name}", method = RequestMethod.GET)
    public ModelAndView taskExecute(@PathVariable String name) {
        ModelAndView view = new ModelAndView("redirect:/task/list");
        if (StringUtils.isNotBlank(name)) {
            ExecutionTask task = executionTaskStorageService.getExecutionTask(name);
            if (task != null) {
                LocalDateTime nextExec = LocalDateTime.fromDateFields(task.getNextExecution());
                task.setNextExecution(nextExec.minusYears(2).toDate());
                this.executionTaskStorageService.updateEntity(task);
            }
        }
        return view;
    }
}
