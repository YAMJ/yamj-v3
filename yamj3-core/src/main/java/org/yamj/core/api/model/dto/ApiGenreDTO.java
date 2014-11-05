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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.yamj.core.database.model.Genre;

/**
 * @author Stuart
 */
@JsonInclude(Include.NON_DEFAULT) 
public class ApiGenreDTO {

    private Long id;
    private String name;
    private String targetApi;
    private String targetXml;
    
    public ApiGenreDTO() {}
    
    public ApiGenreDTO(Genre genre) {
        this.id = genre.getId();
        this.name = genre.getName();
        this.targetApi = genre.getTargetApi();
        this.targetXml = genre.getTargetXml();
    }
    
    @JsonProperty
    public Long getId() {
        return id;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public String getTarget() {
        if (this.targetApi != null) {
            return this.targetApi;
        }
        if (this.targetXml != null) {
            return this.targetXml;
        }
        return null;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setTargetApi(String targetApi) {
        this.targetApi = targetApi;
    }

    public void setTargetXml(String targetXml) {
        this.targetXml = targetXml;
    }
}
