/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.model;


import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.*;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.model.type.ImageFormat;
import org.yamj.core.hibernate.usertypes.EnumStringUserType;

@TypeDefs({
    @TypeDef(name = "artworkType", typeClass = EnumStringUserType.class,
            parameters = {@Parameter(name = "enumClassName", value = "org.yamj.core.database.model.type.ArtworkType")}),
})

@Entity
@Table(name = "artwork_profile",
    uniqueConstraints =
    @UniqueConstraint(name = "UIX_ARTWORKPROFILE_NATURALID", columnNames = {"profile_name", "artwork_type"}))
public class ArtworkProfile extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -5178511945599751914L;

    @NaturalId
    @Column(name = "profile_name")
    private String profileName;

    @NaturalId
    @Type(type = "artworkType")
    @Column(name = "artwork_type")
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

    @Column(name = "apply_to_episode", nullable = false)
    private boolean applyToEpisode = false;

    @Column(name = "pre_process", nullable = false)
    private boolean preProcess = false;
    
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

    public boolean isApplyToEpisode() {
        return applyToEpisode;
    }

    public void setApplyToEpisode(boolean applyToEpisode) {
        this.applyToEpisode = applyToEpisode;
    }

    public boolean isPreProcess() {
        return preProcess;
    }

    public void setPreProcess(boolean preProcess) {
        this.preProcess = preProcess;
    }

    // TODO
    
    public ImageFormat getImageFormat() {
        return ImageFormat.JPEG;
    }
    
    public boolean isRoundedCorners() {
        return false;
    }

    public boolean isReflection() {
        return false;
    }

    public boolean isImageNormalize() {
        return false;
    }

    public boolean isImageStretch() {
        return false;
    }

    public int getCornerQuality() {
        return 0;
    }
    
    public int getQuality() {
        return 75;
    }
    
    // COMMON METHODS

    public float getRatio() {
        return ((float)getWidth() /(float) getHeight());
    }
    
    public float getRounderCornerQuality() { 
        // determine RCQ factor
        if (isRoundedCorners()) {
            return (float) getCornerQuality() / 10 + 1;
        }
        return 1f;
    }

    public boolean hasRelevantChanges(ArtworkProfile profile) {
        if (getWidth() != profile.getWidth()) {
            return true;
        } else if (getHeight() != profile.getHeight()) {
            return true;
        }
        return false;
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
        sb.append(", preProcess=");
        sb.append(isPreProcess());
        sb.append("]");
        return sb.toString();
    }
}
