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

import java.util.Date;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.type.ArtworkType;

public final class QueueDTO implements Comparable<QueueDTO> {

    private final Long id;
    private Date date;
    private MetaDataType metadataType;
    private ArtworkType artworkType;

    // GETTER and SETTER

    public QueueDTO(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MetaDataType getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = MetaDataType.fromString(metadataType);
    }

    public void setMetadataType(MetaDataType metadataType) {
        this.metadataType = metadataType;
    }

    public boolean isMetadataType(MetaDataType metadataType) {
        return this.metadataType == metadataType;
    }

    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(String artworkType) {
        this.artworkType = ArtworkType.fromString(artworkType);
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public boolean isArtworkType(ArtworkType artworkType) {
        return this.artworkType == artworkType;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
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
        return (id == ((QueueDTO) obj).id);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }

    @Override
    public int compareTo(QueueDTO obj) {
        if (getDate() == null && obj.getDate() == null) {
            return 0;
        }
        if (getDate() != null && obj.getDate() == null) {
            return 1;
        }
        if (getDate() == null && obj.getDate() != null) {
            return -1;
        }
        return getDate().compareTo(obj.getDate());
    }
}
