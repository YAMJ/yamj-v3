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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.yamj.core.database.model.Series;

@Entity
@Table(name = "series_awards")
public class SeriesAward extends AbstractAward {

    private static final long serialVersionUID = 3672505190965714376L;

    @EmbeddedId
    private SeriesAwardPK seriesAwardPK;
    
    public SeriesAward() {
        super();
    }

    public SeriesAward(Series series, Award award, int year) {
      setSeriesAwardPK(new SeriesAwardPK(series, award, year));
    }

    // GETTER and SETTER
    
    public SeriesAwardPK getSeriesAwardPK() {
        return seriesAwardPK;
    }
  
    public void setSeriesAwardPK(SeriesAwardPK seriesAwardPK) {
        this.seriesAwardPK = seriesAwardPK;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getSeriesAwardPK())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SeriesAward) {
            return new EqualsBuilder()
                    .append(getSeriesAwardPK(), ((SeriesAward)obj).getSeriesAwardPK())
                    .isEquals();
        }
        return false;
    }
}
