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
package org.yamj.core.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.List;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.database.model.type.ArtworkType;

@JsonInclude(Include.NON_DEFAULT)
public class ApiArtworkProfileDTO extends AbstractApiIdentifiableDTO {

    private String name;
    private ArtworkType artworkType;
    private int width = -1;
    private int height = -1;
    private List<MetaDataType> applyTo = new ArrayList<>();
    private Boolean preProcess;
    private Boolean normalize;
    private Boolean stretch;
    private Boolean reflection;
    private Boolean roundedCorners;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<MetaDataType> getApplyTo() {
        return applyTo;
    }

    public void setApplyTo(List<MetaDataType> applyTo) {
        this.applyTo = applyTo;
    }

    public Boolean isPreProcess() {
        return preProcess;
    }

    public void setPreProcess(Boolean preProcess) {
        this.preProcess = preProcess;
    }

    public Boolean isNormalize() {
        return normalize;
    }

    public void setNormalize(Boolean normalize) {
        this.normalize = normalize;
    }

    public Boolean isStretch() {
        return stretch;
    }

    public void setStretch(Boolean stretch) {
        this.stretch = stretch;
    }

    public Boolean isReflection() {
        return reflection;
    }

    public void setReflection(Boolean reflection) {
        this.reflection = reflection;
    }

    public Boolean isRoundedCorners() {
        return roundedCorners;
    }

    public void setRoundedCorners(Boolean roundedCorners) {
        this.roundedCorners = roundedCorners;
    }
    
    public void addMetaDataType(MetaDataType metaDataType) {
        this.applyTo.add(metaDataType);
    }
}
