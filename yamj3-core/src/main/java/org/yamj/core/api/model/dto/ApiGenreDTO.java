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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.yamj.core.database.model.Genre;

/**
 * @author Stuart
 */
@JsonInclude(Include.NON_DEFAULT)
public class ApiGenreDTO extends AbstractApiIdentifiableDTO {

    private String name;
    private String target;

    public ApiGenreDTO() {}

    public ApiGenreDTO(Genre genre) {
        this.setId(genre.getId());
        this.name = genre.getName();
        if (genre.getTargetApi() != null) {
            this.target = genre.getTargetApi();
        } else if (genre.getTargetXml() != null) {
            this.target = genre.getTargetXml();
        } else {
            this.target = genre.getName();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
