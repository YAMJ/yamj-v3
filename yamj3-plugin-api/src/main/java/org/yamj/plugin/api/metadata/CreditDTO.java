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

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.yamj.plugin.api.artwork.ArtworkDTO;
import org.yamj.plugin.api.type.JobType;

public final class CreditDTO {

    private final String source;
    private final String id;
    private final String name;
    private final JobType jobType;
    private String firstName;
    private String lastName;
    private String realName;
    private String role;
    private boolean voice;
    private Set<ArtworkDTO> photos;
    
    public CreditDTO(String source, JobType jobType, String name) {
        this(source, null, jobType, name, null);
    }

    public CreditDTO(String source, String id, JobType jobType, String name) {
        this(source, id, jobType, name, null);
    }

    public CreditDTO(String source, JobType jobType, String name, String role) {
        this(source, null, jobType, name, role);
    }

    public CreditDTO(String source, String id, JobType jobType, String name, String role) {
        this.source = source;
        this.id = id;
        this.jobType = jobType;
        PersonName personName = MetadataTools.splitFullName(name.trim());
        this.name = personName.getName();
        setFirstName(personName.getFirstName());
        setLastName(personName.getLastName());
        setRole(role);
        setVoice(MetadataTools.isVoiceRole(role));
    }

    public String getSource() {
        return source;
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

    public final CreditDTO setFirstName(String firstName) {
        this.firstName = StringUtils.trimToNull(firstName);
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public final CreditDTO setLastName(String lastName) {
        this.lastName = StringUtils.trimToNull(lastName);
        return this;
    }

    public String getRealName() {
        return realName;
    }

    public CreditDTO setRealName(String realName) {
        this.realName = StringUtils.trimToNull(realName);
        return this;
    }

    public String getRole() {
        return role;
    }

    public CreditDTO setRole(String role) {
        this.role = MetadataTools.cleanRole(role);
        return this;
    }
    
    public boolean isVoice() {
        return voice;
    }

    public CreditDTO setVoice(boolean voice) {
        this.voice = voice;
        return this;
    }

    public Set<ArtworkDTO> getPhotos() {
        return photos;
    }

    public void addPhoto(String source, String url) {
        if (StringUtils.isNotBlank(url)) {
            if (photos == null) {
                photos = new HashSet<>(1);
            }
            this.photos.add(new ArtworkDTO(source, url));
        }
    }
}
