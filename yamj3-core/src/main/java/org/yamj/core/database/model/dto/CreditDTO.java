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
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.core.service.artwork.ArtworkDetailDTO;
import org.yamj.core.tools.YamjTools;
import org.yamj.plugin.api.common.JobType;
import org.yamj.plugin.api.metadata.tools.MetadataTools;
import org.yamj.plugin.api.metadata.tools.PersonName;

public final class CreditDTO {

    private final String source;
    private final String sourceId;
    private final JobType jobType;
    private final String name;
    private final String identifier;
    private String firstName;
    private String lastName;
    private String realName;
    private String role;
    private boolean voice = false;
    private Long personId;
    private Set<ArtworkDetailDTO> photoDTOS;
    
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
        PersonName personName = MetadataTools.splitFullName(name.trim());
        this.name = personName.getName();
        this.identifier = YamjTools.cleanIdentifier(this.name);
        setFirstName(personName.getFirstName());
        setLastName(personName.getLastName());
        setRole(role);
        setVoice(MetadataTools.isVoiceRole(role));
    }

    public CreditDTO(String source, org.yamj.plugin.api.metadata.dto.CreditDTO credit) {
        this.source = source;
        this.sourceId = credit.getId();
        this.jobType = credit.getJobType();
        this.name = credit.getName();
        this.identifier = YamjTools.cleanIdentifier(credit.getName());
        
        this.firstName = credit.getFirstName();
        this.lastName = credit.getLastName();
        this.role = credit.getRole();
        this.voice = credit.isVoice();
        this.realName = credit.getRealName();
        
        if (credit.getPhotos() != null) {
            for (String photo : credit.getPhotos()) {
                this.addPhoto(source, photo);
            }
        }
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

    public void setRole(String role) {
        this.role = MetadataTools.cleanRole(role);
    }
    
    public boolean isVoice() {
        return voice;
    }

    public void setVoice(boolean voice) {
        this.voice = voice;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public Set<ArtworkDetailDTO> getPhotoDTOS() {
        return photoDTOS;
    }

    public void addPhoto(String source, String url) {
        if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(url)) {
            if (photoDTOS == null) {
                photoDTOS = new HashSet<>(1);
            }
            this.photoDTOS.add(new ArtworkDetailDTO(source, url));
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
        return (this.jobType == other.jobType) &&
               StringUtils.equals(this.source, other.source) && 
               StringUtils.equalsIgnoreCase(this.identifier, other.identifier);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
