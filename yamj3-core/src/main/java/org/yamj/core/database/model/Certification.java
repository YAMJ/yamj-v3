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
package org.yamj.core.database.model;

import java.io.Serializable;
import javax.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "certification",
    uniqueConstraints= @UniqueConstraint(name="UIX_CERTIFICATION_NATURALID", columnNames={"videodata_id","series_id","country"})
)
public class Certification extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 5949467240717893584L;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_CERTIFICATION_VIDEODATA")
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "videodata_id")
    private VideoData videoData;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_CERTIFICATION_SERIES")
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "series_id")
    private Series series;

    @NaturalId
    @Column(name = "country", length = 100, nullable = false)
    private String country;

    @Column(name = "certification_text", nullable = false, length = 50)
    private String certificationText;
    
    // GETTER and SETTER

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCertificationText() {
        return certificationText;
    }

    public void setCertificationText(String certificationText) {
        this.certificationText = certificationText;
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (videoData == null ? 0 : videoData.hashCode());
        result = prime * result + (series == null ? 0 : series.hashCode());
        result = prime * result + (country == null ? 0 : country.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Certification)) {
            return false;
        }
        Certification castOther = (Certification) other;
        // first check the id
        if ((this.getId() > 0) && (castOther.getId() > 0)) {
            return this.getId() == castOther.getId();
        }
        // check country
        if (!StringUtils.equals(this.country, castOther.country)) {
            return false;
        }
        // check videoData
        if (this.videoData == null && castOther.videoData != null) {
            return false;
        }
        if (this.videoData != null && castOther.videoData == null) {
            return false;
        }
        if (this.videoData != null && castOther.videoData != null) {
            if (!this.videoData.equals(castOther.videoData)) {
                return false;
            }
        }
        // check series
        if (this.series == null && castOther.series != null) {
            return false;
        }
        if (this.series != null && castOther.series == null) {
            return false;
        }
        if (this.series != null && castOther.series != null) {
            if (!this.series.equals(castOther.series)) {
                return false;
            }
        }
        // all checks passed
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Certification [ID=");
        sb.append(getId());
        sb.append(", country=");
        sb.append(getCountry());
        sb.append(", text=");
        sb.append(this.getCertificationText());
        if (this.getVideoData() != null) {
            sb.append(", type=movie");
        }
        if (this.getSeries() != null) {
            sb.append(", type=series");
        }
        sb.append("]");
        return sb.toString();
    }
}
