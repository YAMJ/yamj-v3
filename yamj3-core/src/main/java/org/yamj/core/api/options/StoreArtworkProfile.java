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

public class StoreArtworkProfile {

    private Integer height;
    private Integer width;
    private String scaling;
    private Boolean reflection;
    private Boolean roundedCorners;
    private Integer quality;

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getScaling() {
        return scaling;
    }

    public void setScaling(String scaling) {
        this.scaling = scaling;
    }

    public Boolean getReflection() {
        return reflection;
    }

    public void setReflection(Boolean reflection) {
        this.reflection = reflection;
    }

    public Boolean getRoundedCorners() {
        return roundedCorners;
    }

    public void setRoundedCorners(Boolean roundedCorners) {
        this.roundedCorners = roundedCorners;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }
}
