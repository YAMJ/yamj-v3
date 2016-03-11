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
package org.yamj.core.database.model;

import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;

@NamedQueries({
    @NamedQuery(name = ArtworkGenerated.QUERY_GET,
        query = "SELECT gen FROM ArtworkGenerated gen JOIN FETCH gen.artworkProfile prof WHERE gen.artworkLocated.id=:locatedId "+
                "AND prof.profileName=:profileName AND prof.artworkType=gen.artworkLocated.artwork.artworkType"
    )
})

@Entity
@Table(name = "artwork_generated",
    uniqueConstraints = @UniqueConstraint(name = "UIX_ARTWORK_GENERATED", columnNames = {"located_id", "profile_id"}),
    indexes = @Index(name = "IX_ARTWORKGENERATED_STATUS", columnList = "status")
)
public class ArtworkGenerated extends AbstractStateful {

    private static final long serialVersionUID = 2326614430648326340L;
    public static final String QUERY_GET = "artworkGenerated.getArtworkGenerated";
    
    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "located_id", foreignKey = @ForeignKey(name = "FK_ARTWORKGENERATED_LOCATED"))
    private ArtworkLocated artworkLocated;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "profile_id", foreignKey = @ForeignKey(name = "FK_ARTWORKGENERATED_PROFILE"))
    private ArtworkProfile artworkProfile;

    @Column(name = "cache_filename", nullable = false, length = 500)
    private String cacheFilename;

    @Column(name = "cache_dir", nullable = false, length = 50)
    private String cacheDirectory;
    
    // GETTER and SETTER

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public String getCacheFilename() {
        return cacheFilename;
    }

    public void setCacheFilename(String cacheFilename) {
        this.cacheFilename = cacheFilename;
    }

    public ArtworkLocated getArtworkLocated() {
        return artworkLocated;
    }

    public void setArtworkLocated(ArtworkLocated artworkLocated) {
        this.artworkLocated = artworkLocated;
    }

    public ArtworkProfile getArtworkProfile() {
        return artworkProfile;
    }

    public void setArtworkProfile(ArtworkProfile artworkProfile) {
        this.artworkProfile = artworkProfile;
    }

    // TRANSIENT METHODS
    
    public boolean isCached() {
        return !isNotCached();
    }

    public boolean isNotCached() {
        return StringUtils.isBlank(getCacheFilename()) || StringUtils.isBlank(getCacheDirectory());
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getArtworkLocated())
                .append(getArtworkProfile())
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
        if (!(obj instanceof ArtworkGenerated)) {
            return false;
        }
        ArtworkGenerated other = (ArtworkGenerated) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getArtworkLocated(), other.getArtworkLocated())
                .append(getArtworkProfile(), other.getArtworkProfile())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ArtworkGenerated [ID=");
        sb.append(getId());
        if (Hibernate.isInitialized(getArtworkLocated())) {
            sb.append(", located=");
            sb.append(getArtworkLocated().getId());
        }
        if (Hibernate.isInitialized(getArtworkProfile())) {
            sb.append(", profile=");
            sb.append(getArtworkProfile().getProfileName());
            sb.append(", artworkType=");
            sb.append(getArtworkProfile().getArtworkType());
        }
        sb.append("]");
        return sb.toString();
    }
}
