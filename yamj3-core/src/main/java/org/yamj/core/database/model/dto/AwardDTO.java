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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class AwardDTO {

    private final String event;
    private final String source;
    private int year;
    private String award;

    public AwardDTO(String event, String source) {
        this.event = event;
        this.source = source;
    }

    public AwardDTO(String event, String source, int year, String award) {
        this(event, source);
        this.year = year;
        this.award = award;
    }

    public String getEvent() {
        return event;
    }

    public String getSource() {
        return source;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getAward() {
        return award;
    }

    public void setAward(String award) {
        this.award = award;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(event)
                .append(source)
                .append(year)
                .append(award)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AwardDTO) {
            final AwardDTO other = (AwardDTO) obj;
            return new EqualsBuilder()
                    .append(getEvent(), other.getEvent())
                    .append(getSource(), other.getSource())
                    .append(getYear(), other.getYear())
                    .append(getAward(), other.getAward())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
