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
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.common.tools.StringTools;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.tools.MetadataTools;
import org.yamj.core.tools.PersonNameDTO;

public class CreditDTO {

    private final String source;
    private final String sourceId;
    private final JobType jobType;
    private final String name;
    private final String identifier;
    private String firstName;
    private String lastName;
    private String realName;
    private String role;
    private Long personId;
    private final Set<String> photoURLS = new HashSet<>();
    
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
        this.identifier = MetadataTools.cleanIdentifier(name);
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
    
    public String getIdentifier() {
        return identifier;
    }

    public String getFirstName() {
        return firstName;
    }

    public final void setFirstName(String firstName) {
        this.firstName = StringUtils.trimToNull(firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public final void setLastName(String lastName) {
        this.lastName = StringUtils.trimToNull(lastName);
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = StringUtils.trimToNull(realName);
    }

    public String getRole() {
        return role;
    }

    public final void setRole(final String role) {
        String fixed = StringUtils.trimToNull(role);
        if (fixed == null) return;
        
        // (as ... = alternate name
        int idx = StringUtils.indexOfIgnoreCase(fixed, "(as ");
        if (idx > 0) {
            fixed = fixed.substring(0, idx);
        }
        // uncredited cast member
        idx = StringUtils.indexOfIgnoreCase(fixed, "(uncredit");
        if (idx > 0) {
            fixed = fixed.substring(0, idx);
        }
        // season marker
        idx = StringUtils.indexOfIgnoreCase(fixed, "(Season");
        if (idx > 0) {
            fixed = fixed.substring(0, idx);
        }

        // double characters
        idx = StringUtils.indexOf(fixed, "/");
        if (idx > 0) {
            List<String> characters = StringTools.splitList(fixed, "/");
            fixed = StringUtils.join(characters.toArray(), " / ");
        }
        
        fixed = MetadataTools.fixScannedValue(fixed);
        fixed = fixed.replaceAll("( )+", " ").trim();
        
        if (StringUtils.isNotEmpty(fixed)) {
            this.role = fixed;
        }
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
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
        return new HashCodeBuilder()
            .append(jobType)
            .append(source)
            .append(identifier)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CreditDTO)) {
            return false;
        }
        CreditDTO other = (CreditDTO) obj;
        if (this.jobType != other.jobType) return false;
        if (!StringUtils.equals(this.source, other.source)) return false;
        return StringUtils.equalsIgnoreCase(this.identifier, other.identifier);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
