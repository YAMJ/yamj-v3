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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.core.tools.MetadataTools;

public final class BoxedSetDTO {

    private final String source;
    private final String identifier;
    private final String name;
    private final Integer ordering;
    private final String sourceId;
    private Long boxedSetId;
    
    public BoxedSetDTO(String source, String name) {
        this(source, name, null, null);
    }

    public BoxedSetDTO(String source, String name, Integer ordering) {
        this(source, name, ordering, null);
    }

    public BoxedSetDTO(String source, String name, Integer ordering, String sourceId) {
        this.source = source;
        this.name = StringUtils.trimToNull(name);
        this.identifier = MetadataTools.cleanIdentifier(name);
        this.ordering = ordering;
        this.sourceId = StringUtils.trimToNull(sourceId);
    }

    public String getSource() {
        return source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getName() {
        return name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Integer getOrdering() {
        return ordering;
    }
    
    public Long getBoxedSetId() {
        return boxedSetId;
    }

    public void setBoxedSetId(Long boxedSetId) {
        this.boxedSetId = boxedSetId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(source)
            .append(identifier)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BoxedSetDTO)) {
            return false;
        }
        BoxedSetDTO other = (BoxedSetDTO) obj;
        return StringUtils.equalsIgnoreCase(this.source, other.source) &&
               StringUtils.equalsIgnoreCase(this.identifier, other.identifier);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
