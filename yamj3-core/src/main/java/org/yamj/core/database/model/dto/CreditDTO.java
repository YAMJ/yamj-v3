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
package org.yamj.core.database.model.dto;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.core.database.model.type.JobType;

public class CreditDTO {

    private String name;
    private JobType jobType;
    private String role;
    private String aka;
    private String sourcedb;
    private String sourcedbId;

    public CreditDTO() {
    }

    public CreditDTO(JobType jobType, String name) {
        this(jobType, name, null);
    }

    public CreditDTO(JobType jobType, String name, String role) {
        this.jobType = jobType;
        this.name = StringUtils.trim(name);
        this.role = StringUtils.trimToNull(role);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAka() {
        return aka;
    }

    public void setAka(String aka) {
        this.aka = aka;
    }

    public String getSourcedb() {
        return sourcedb;
    }

    public void setSourcedb(String sourcedb) {
        this.sourcedb = sourcedb;
    }

    public String getSourcedbId() {
        return sourcedbId;
    }

    public void setSourcedbId(String sourcedbId) {
        this.sourcedbId = sourcedbId;
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (this.jobType == null ? 0 : this.jobType.hashCode());
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
        if (!(other instanceof CreditDTO)) {
            return false;
        }
        CreditDTO castOther = (CreditDTO) other;
        // check job
        if (this.jobType != castOther.jobType) {
            return false;
        }
        // check name
        return StringUtils.equalsIgnoreCase(this.name, castOther.name);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
