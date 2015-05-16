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
import java.util.List;
import org.yamj.common.type.MetaDataType;

/**
 * List of the options available for the type
 *
 * @author modmax
 */
@JsonInclude(Include.NON_DEFAULT)
public class OptionsMultiType extends OptionsAbstractSortSearch {

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the filtered types
     *
     * @return
     */
    public List<MetaDataType> splitTypes() {
        return this.splitTypes(type);
    }

    /**
     * Get the meta data types
     *
     * @return
     */
    public List<MetaDataType> getMetaDataTypes() {
        return this.getMetaDataTypes(type);
    }
}
