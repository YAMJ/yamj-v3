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
package org.yamj.core.database.model.award;

import java.io.Serializable;
import javax.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.yamj.core.database.model.Series;

@Embeddable
public class SeriesAwardPK implements Serializable {

    private static final long serialVersionUID = 6009169674740619506L;

    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "series_id", nullable = false, insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "FK_SERIESAWARD_SERIES"))
    private Series series;

    @ManyToOne(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "award_id", nullable = false, insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "FK_SERIESAWARD_AWARD"))
    private Award award;

    @Column(name = "year", nullable = false)
    private int year = -1;

    public SeriesAwardPK() {
        // empty constructor
    }

    public SeriesAwardPK(Series series, Award award, int year) {
        this.series = series;
        this.award = award;
        this.year = year;
    }

    // GETTER AND SETTER
    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public Award getAward() {
        return award;
    }

    public void setAward(Award award) {
        this.award = award;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    // EQUALITY CHECKS
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getSeries().getId())
                .append(getAward().getId())
                .append(getYear())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SeriesAwardPK) {
            SeriesAwardPK other = (SeriesAwardPK) obj;
            return new EqualsBuilder()
                    .append(getSeries().getId(), other.getSeries().getId())
                    .append(getAward().getId(), other.getAward().getId())
                    .append(getYear(), other.getYear())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SeriesAwardPK [series=");
        sb.append(getSeries().getIdentifier());
        sb.append(", event=");
        sb.append(getAward().getEvent());
        sb.append(", category=");
        sb.append(getAward().getCategory());
        sb.append(", year=");
        sb.append(getYear());
        sb.append("]");
        return sb.toString();
    }
}
