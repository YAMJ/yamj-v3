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
package org.yamj.core.api.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.yamj.common.type.MetaDataType;

/**
 * List of the options available for the type
 *
 * @author modmax
 */
@JsonInclude(Include.NON_DEFAULT)
public class OptionsLibrary extends OptionsAbstractSortSearch {

    private Boolean used = Boolean.TRUE;
    private Boolean full = Boolean.FALSE;
    private MetaDataType type;

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public Boolean getFull() {
        return full;
    }

    public void setFull(Boolean full) {
        this.full = full;
    }

    public MetaDataType getType() {
        return type;
    }

    public void setType(String type) {
        try {
            this.type = MetaDataType.valueOf(type.trim().toUpperCase());
        } catch (Exception ignore) { //NOSONAR
            // ignore error if type is null
        }
    }
}
