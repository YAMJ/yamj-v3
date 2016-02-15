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

import java.io.Serializable;
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.ImageType;
import org.yamj.core.database.model.type.ScalingType;

@Entity
@Table(name = "artwork_profile",
    uniqueConstraints = @UniqueConstraint(name = "UIX_ARTWORKPROFILE_NATURALID", columnNames = {"profile_name", "artwork_type"})
)
public class ArtworkProfile extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -5178511945599751914L;

    @NaturalId
    @Column(name = "profile_name", length = 100)
    private String profileName;

    @NaturalId
    @Type(type = "artworkType")
    @Column(name = "artwork_type", length = 20)
    private ArtworkType artworkType;

    @Column(name = "width", nullable = false)
    private int width = -1;

    @Column(name = "height", nullable = false)
    private int height = -1;

    @Column(name = "apply_to_movie", nullable = false)
    private boolean applyToMovie = false;

    @Column(name = "apply_to_series", nullable = false)
    private boolean applyToSeries = false;

    @Column(name = "apply_to_season", nullable = false)
    private boolean applyToSeason = false;

    @Column(name = "apply_to_boxexset", nullable = false)
    private boolean applyToBoxedSet = false;

    @Type(type = "scalingType")
    @Column(name = "scaling", length = 20, nullable=true) // TODO set nullable=false later on
    private ScalingType scalingType;

    @Column(name = "reflection", nullable = false)
    private boolean reflection = false;

    @Column(name = "rounded_corners", nullable = false)
    private boolean roundedCorners = false;

    @Column(name = "pre_process", nullable = false)
    private boolean preProcess = false;

    @Column(name = "quality", nullable = true) // TODO set nullable=false later on
    private int quality = -1;

    // GETTER and SETTER

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public ArtworkType getArtworkType() {
        return artworkType;
    }

    public void setArtworkType(ArtworkType artworkType) {
        this.artworkType = artworkType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isApplyToMovie() {
        return applyToMovie;
    }

    public void setApplyToMovie(boolean applyToMovie) {
        this.applyToMovie = applyToMovie;
    }

    public boolean isApplyToSeries() {
        return applyToSeries;
    }

    public void setApplyToSeries(boolean applyToSeries) {
        this.applyToSeries = applyToSeries;
    }

    public boolean isApplyToSeason() {
        return applyToSeason;
    }

    public void setApplyToSeason(boolean applyToSeason) {
        this.applyToSeason = applyToSeason;
    }

    public boolean isApplyToBoxedSet() {
        return applyToBoxedSet;
    }

    public void setApplyToBoxedSet(boolean applyToBoxedSet) {
        this.applyToBoxedSet = applyToBoxedSet;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public void setScalingType(ScalingType scalingType) {
        this.scalingType = scalingType;
    }

    public boolean isReflection() {
        return reflection;
    }

    public void setReflection(boolean reflection) {
        this.reflection = reflection;
    }

    public boolean isRoundedCorners() {
        return roundedCorners;
    }

    public void setRoundedCorners(boolean roundedCorners) {
        this.roundedCorners = roundedCorners;
    }

    public boolean isPreProcess() {
        return preProcess;
    }

    public void setPreProcess(boolean preProcess) {
        this.preProcess = preProcess;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    // TODO
    public int getCornerQuality() {
        return 0;
    }

    // TODO
    public ImageType getImageType() {
        return ImageType.JPG;
    }

    // COMMON METHODS

    public float getRatio() {
        return (float) getWidth() / (float) getHeight();
    }

    public float getRounderCornerQuality() {
        // determine RCQ factor
        if (isRoundedCorners()) {
            return (float) getCornerQuality() / 10 + 1;
        }
        return 1f;
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getProfileName())
                .append(getArtworkType().name())
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
        if (!(obj instanceof ArtworkProfile)) {
            return false;
        }
        ArtworkProfile other = (ArtworkProfile) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getProfileName(), other.getProfileName())
                .append(getArtworkType().name(), other.getArtworkType().name())
                .isEquals();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ArtworkProfile [ID=");
        sb.append(getId());
        sb.append(", name=");
        sb.append(getProfileName());
        sb.append(", type=");
        sb.append(getArtworkType());
        sb.append(", width=");
        sb.append(getWidth());
        sb.append(", height=");
        sb.append(getHeight());
        sb.append(", scaling=");
        sb.append(getScalingType());
        sb.append(", preProcess=");
        sb.append(isPreProcess());
        sb.append("]");
        return sb.toString();
    }
}
