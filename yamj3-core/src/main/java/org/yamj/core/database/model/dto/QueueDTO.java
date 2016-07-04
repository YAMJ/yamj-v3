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

import org.apache.commons.lang3.builder.*;
import org.yamj.common.type.MetaDataType;

public final class QueueDTO {

    private final Long id;
    private MetaDataType metadataType;
    private Boolean locatedArtwork;

    public QueueDTO(Long id) {
        this.id = id;
    }

    public QueueDTO(Long id, String metadataType) {
        this.id = id;
        this.metadataType = MetaDataType.fromString(metadataType);
    }

    public Long getId() {
        return id;
    }

    public boolean isMetadataType(MetaDataType metadataType) {
        return this.metadataType == metadataType;
    }

    public Boolean getLocatedArtwork() {
        return locatedArtwork;
    }

    public void setLocatedArtwork(Boolean locatedArtwork) {
        this.locatedArtwork = locatedArtwork;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getId())
                .append(this.metadataType)
                .append(getLocatedArtwork())
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
        if (!(obj instanceof QueueDTO)) {
            return false;
        }
        QueueDTO other = (QueueDTO)obj;
        return new EqualsBuilder()
            .append(getId(), other.getId())
            .append(this.metadataType, other.metadataType)
            .append(getLocatedArtwork(), other.getLocatedArtwork())
            .isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
