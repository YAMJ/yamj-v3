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
package org.yamj.core.tools;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


public final class PersonNameDTO {

    private final String name;
    private String firstName;
    private String lastName;
  
    public PersonNameDTO(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
  
    public String getFirstName() {
        return firstName;
    }
  
    public void setFirstName(String firstName) {
        if (StringUtils.isNotBlank(firstName)) {
            this.firstName = firstName;
        }
    }
  
    public String getLastName() {
        return lastName;
    }
  
    public void setLastName(String lastName) {
        if (StringUtils.isNotBlank(lastName)) {
            this.lastName = lastName;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
