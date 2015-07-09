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

import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class DeletionDTO {

    private final Set<String> filesToDelete;
    private boolean updateTrigger = false;
    
    public DeletionDTO(Set<String> filesToDelete) {
        this.filesToDelete = filesToDelete;
    }

    public DeletionDTO(Set<String> filesToDelete, boolean updateTrigger) {
        this(filesToDelete);
        this.updateTrigger = updateTrigger;
    }

    public Set<String> getFilesToDelete() {
        return filesToDelete;
    }
    
    public boolean isUpdateTrigger() {
        return updateTrigger;
    }

    public void setUpdateTrigger(boolean updateTrigger) {
        this.updateTrigger = updateTrigger;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
