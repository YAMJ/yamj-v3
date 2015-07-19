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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TrailerDTO {

    private final String source;
    private final String url;
    private final String title;
    private final String sourceHash;
    
    public TrailerDTO(String source, String url) {
        this(source, url, null);
    }

    public TrailerDTO(String source, String url, String title) {
        this.source = source;
        this.url = url;
        this.title = title;
        
        int iHashCode = url.hashCode();
        this.sourceHash = String.valueOf(iHashCode < 0 ? 0-iHashCode : iHashCode);
    }

    public TrailerDTO(String source, String url, String title, String sourceHash) {
        this.source = source;
        this.url = url;
        this.title = title;
        this.sourceHash = sourceHash;
    }

    public String getSource() {
        return source;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getSource())
                .append(getUrl())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrailerDTO) {
            final TrailerDTO other = (TrailerDTO) obj;
            return new EqualsBuilder()
                    .append(getSource(), other.getSource())
                    .append(getUrl(), other.getUrl())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
