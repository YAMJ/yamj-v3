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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.core.database.model.ExecutionTask;
import org.yamj.core.database.model.type.IntervalType;
import org.yamj.core.database.service.ExecutionTaskStorageService;
import org.yamj.core.pages.form.TaskForm;

@Controller
@RequestMapping(value = "/task")
public class TaskPagesController extends AbstractPagesController {

    private static final Logger LOG = LoggerFactory.getLogger(TaskPagesController.class);
    private static final SimpleDateFormat DATEPICKER_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    
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
        String errorMessage = null;
        String successMessage = null;
        if (StringUtils.isNotBlank(name)) {
            ExecutionTask task = executionTaskStorageService.getExecutionTask(name);
            if (task != null) {
                LocalDateTime nextExec = LocalDateTime.fromDateFields(task.getNextExecution());
                task.setNextExecution(nextExec.minusYears(2).toDate());
                this.executionTaskStorageService.updateEntity(task);
                successMessage = "Enqueued task '"+name+"'";
            } else {
                errorMessage = "Task name '"+name+"' not found";
            }
        } else {
            errorMessage = "No valid task name provided";
        }
        
        ModelAndView view = withInfo(new ModelAndView("task/task-list"));
        view.addObject("tasklist", executionTaskStorageService.getAllTasks());
        view.addObject(ERROR_MESSAGE, errorMessage);
        view.addObject(SUCCESS_MESSAGE, successMessage);
        return view;
    }

    @RequestMapping(value = "/edit/{name}", method = RequestMethod.GET)
    public ModelAndView taskEditPage(@PathVariable String name) {
        
        if (StringUtils.isNotBlank(name)) {
            ExecutionTask task = executionTaskStorageService.getExecutionTask(name);
            if (task != null) {
                TaskForm taskForm = new TaskForm();
                taskForm.setName(task.getName());
                taskForm.setTaskName(task.getTaskName());
                taskForm.setInterval(task.getIntervalType().name());
                if (task.getDelay() > 0) {
                    taskForm.setDelay(Integer.toString(task.getDelay()));
                }
                taskForm.setNextExecDate(DATEPICKER_FORMAT.format(task.getNextExecution()));
                
                ModelAndView view = withInfo(new ModelAndView("task/task-edit"));
                view.addObject("task", taskForm);
                return view;
            }
        }
        
        LOG.warn("No valid task name '{}' provided for editing", name);
        return new ModelAndView("redirect:/task/list");
    }

    @RequestMapping(value = "/edit/{name}", method = RequestMethod.POST)
    public ModelAndView taskEditUpdate(@ModelAttribute("task") TaskForm taskForm) {
        LOG.trace("Submitted form: {}", taskForm);
        
        // holds the error message
        String errorMessage = null;

        // get the execution task
        ExecutionTask executionTask = executionTaskStorageService.getExecutionTask(taskForm.getName());

        // check the interval
        final IntervalType intervalType = IntervalType.fromString(taskForm.getInterval());
        final int delay = NumberUtils.toInt(taskForm.getDelay(), -1);
        if (intervalType == IntervalType.UNKNOWN) {
            errorMessage = "Interval is not valid";
            taskForm.setInterval(executionTask.getIntervalType().name());
        } else if (intervalType.needsDelay() && delay <1) {
            errorMessage = "Delay must be a positive number";
        } else {
            executionTask.setIntervalType(intervalType);
            executionTask.setDelay(intervalType.needsDelay() ? delay : -1);
        }
        
        if (StringUtils.isNotBlank(taskForm.getNextExecDate())) {
            try {
                Date nextExecution = DATEPICKER_FORMAT.parse(taskForm.getNextExecDate());
                executionTask.setNextExecution(nextExecution);
            } catch (Exception e) {
                errorMessage = "Invalid datetime provided";
            }
        }
        
        if (errorMessage == null) {
            // no error so just update the task and return to list
            executionTaskStorageService.updateEntity(executionTask);
            return new ModelAndView("redirect:/task/list");
        }
        
        ModelAndView view = withInfo(new ModelAndView("task/task-edit"));
        taskForm.setNextExecDate(DATEPICKER_FORMAT.format(executionTask.getNextExecution()));
        view.addObject(ERROR_MESSAGE, errorMessage);
        view.addObject("task", taskForm);
        return view;
    }
}
