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
package org.yamj.core.api.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import org.yamj.common.type.MetaDataType;

/**
 * List of the options available for the indexes
 *
 * @author stuart.boston
 */
@JsonInclude(Include.NON_DEFAULT)
public class OptionsIndexVideo extends OptionsIdArtwork {

    private String include;
    private String exclude;
    private Boolean watched;
    private String type;
    
    public String getInclude() {
        return include;
    }
    
    public void setInclude(String include) {
        this.include = include;
    }

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public Boolean getWatched() {
        return watched;
    }

    public void setWatched(String watched) {
        if ("true".equalsIgnoreCase(watched)) {
            this.watched = Boolean.TRUE;
        } else if ("false".equalsIgnoreCase(watched)) {
            this.watched = Boolean.FALSE;
        }
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Split the include list into a map of values
     *
     * @return
     */
    public Map<String, String> splitIncludes() {
        return splitDashList(include);
    }

    /**
     * Split the exclude list into a map of values
     *
     * @return
     */
    public Map<String, String> splitExcludes() {
        return splitDashList(exclude);
    }

    /**
     * Split the video types
     * 
     * @return
     */
    public List<MetaDataType> splitTypes() {
        return this.splitTypes(type);
    }
}
