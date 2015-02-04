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
package org.yamj.core.database.model.dto;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.PersonNameDTO;

public class CreditDTO {

    private final String source;
    private final String sourceId;
    private final JobType jobType;
    private final String name;
    private String firstName;
    private String lastName;
    private String realName;
    private String role;
    private Set<String> photoURLS = new HashSet<>();

    public CreditDTO(String source, JobType jobType, String name) {
      this(source, null, jobType, name, null);
  }

    public CreditDTO(String source, String sourceId, JobType jobType, String name) {
        this(source, sourceId, jobType, name, null);
    }

    public CreditDTO(String source, JobType jobType, String name, String role) {
        this(source, null, jobType, name, role);
    }

    public CreditDTO(String source, String sourceId, JobType jobType, String name, String role) {
        this.source = source;
        this.sourceId = sourceId;
        this.jobType = jobType;
        PersonNameDTO dto = MetadataTools.splitFullName(name.trim());
        this.name = dto.getName();
        setFirstName(dto.getFirstName());
        setLastName(dto.getLastName());
        setRole(role);
    }

    public String getSource() {
        return source;
    }

    public String getSourceId() {
      return sourceId;
    }

    public JobType getJobType() {
        return jobType;
    }

    public String getName() {
        return name;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (StringUtils.isNotBlank(firstName)) {
            this.firstName = firstName.trim();
        }
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (StringUtils.isNotBlank(lastName)) {
            this.lastName = lastName.trim();
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (StringUtils.isNotBlank(role)) {
            this.role = role.trim();
        }
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        if (StringUtils.isNotBlank(realName)) {
            this.realName = realName.trim();
        }
    }

    public Set<String> getPhotoURLS() {
        return photoURLS;
    }

    public void addPhotoURL(String photoURL) {
        if (StringUtils.isNotBlank(photoURL)) {
            this.photoURLS.add(photoURL.trim());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (this.name == null ? 0 : this.name.toLowerCase().hashCode());
        result = prime * result + (this.source == null ? 0 : this.source.hashCode());
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
        // check source
        if (!StringUtils.equals(this.source, castOther.source)) {
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
