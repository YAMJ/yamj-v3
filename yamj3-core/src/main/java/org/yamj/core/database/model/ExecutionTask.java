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
package org.yamj.core.database.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.yamj.core.database.model.type.IntervalType;

@Entity
@Table(name = "execution_task")
public class ExecutionTask implements Serializable {

    private static final long serialVersionUID = 5730223895964642472L;

    @Id
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "task_name", nullable = false, length = 50)
    private String taskName;

    @Column(name = "options", length = 255)
    private String options;

    @Type(type = "intervalType")
    @Column(name = "interval_type", nullable = false, length = 20)
    private IntervalType intervalType;
    
    @Column(name = "delay", nullable = false)
    private int delay = -1;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "next_execution", nullable = false)
    private Date nextExecution;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "last_execution")
    private Date lastExecution;

    // GETTER AND SETTER
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public IntervalType getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(IntervalType intervalType) {
        this.intervalType = intervalType;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public Date getNextExecution() {
        return nextExecution;
    }

    public void setNextExecution(Date nextExecution) {
        this.nextExecution = nextExecution;
    }

    public Date getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(Date lastExecution) {
        this.lastExecution = lastExecution;
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (getName() == null ? 0 : getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof ExecutionTask)) {
            return false;
        }
        ExecutionTask castOther = (ExecutionTask) other;
        return StringUtils.equalsIgnoreCase(getName(), castOther.getName());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExecutionTask [Name=");
        sb.append(getName());
        sb.append(", taskName=");
        sb.append(getTaskName());
        sb.append(", intervalType=");
        sb.append(getIntervalType());
        sb.append(", delay=");
        sb.append(getDelay());
        sb.append(", nextExecution=");
        sb.append(getNextExecution());
        sb.append(", lastExecution=");
        sb.append(getLastExecution());
        sb.append("]");
        return sb.toString();
    }
}
