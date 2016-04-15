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
package org.yamj.plugin.api.metadata;

import java.util.Collection;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;
import org.yamj.plugin.api.tools.MetadataTools;
import org.yamj.plugin.api.tools.PersonNameDTO;
import org.yamj.plugin.api.type.JobType;

public final class Credit {

    private final String id;
    private final String name;
    private final JobType jobType;
    private String firstName;
    private String lastName;
    private String realName;
    private String role;
    private boolean voice;
    private Collection<String> photos;
    
    public Credit(JobType jobType, String name) {
        this(null, jobType, name, null);
    }

    public Credit(String id, JobType jobType, String name) {
        this(id, jobType, name, null);
    }

    public Credit(JobType jobType, String name, String role) {
        this(null, jobType, name, role);
    }

    public Credit(String id, JobType jobType, String name, String role) {
        this.id = id;
        this.jobType = jobType;
        PersonNameDTO dto = MetadataTools.splitFullName(name.trim());
        this.name = dto.getName();
        setFirstName(dto.getFirstName());
        setLastName(dto.getLastName());
        setRole(role);
        setVoice(MetadataTools.isVoiceRole(role));
    }


    public String getId() {
        return id;
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

    public final Credit setFirstName(String firstName) {
        this.firstName = StringUtils.trimToNull(firstName);
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public final Credit setLastName(String lastName) {
        this.lastName = StringUtils.trimToNull(lastName);
        return this;
    }

    public String getRealName() {
        return realName;
    }

    public Credit setRealName(String realName) {
        this.realName = StringUtils.trimToNull(realName);
        return this;
    }

    public String getRole() {
        return role;
    }

    public Credit setRole(String role) {
        this.role = MetadataTools.cleanRole(role);
        return this;
    }
    
    public boolean isVoice() {
        return voice;
    }

    public Credit setVoice(boolean voice) {
        this.voice = voice;
        return this;
    }

    public Collection<String> getPhotos() {
        return photos;
    }

    public Credit addPhoto(String photoURL) {
        if (StringUtils.isNotBlank(photoURL)) {
            if (this.photos == null) {
                this.photos = new HashSet<>(); 
            }
            this.photos.add(photoURL);
        }
        return this;
    }
}
