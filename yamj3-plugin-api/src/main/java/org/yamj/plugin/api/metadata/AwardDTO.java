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
package org.yamj.plugin.api.metadata;

import org.apache.commons.lang3.builder.*;

public final class AwardDTO {

    private final String source;
    private final String event;
    private final String category;
    private final int year;
    private boolean won = true;
    private boolean nominated = false;

    public AwardDTO(String source, String event, String category, int year) {
        this.source = source;
        this.event = event;
        this.category = category;
        this.year = year;
    }

    public String getSource() {
        return source;
    }

    public String getEvent() {
        return event;
    }

    public String getCategory() {
        return category;
    }

    public int getYear() {
        return year;
    }
    
    public boolean isWon() {
        return won;
    }

    public AwardDTO setWon(boolean won) {
        this.won = won;
        return this;
    }

    public boolean isNominated() {
        return nominated;
    }

    public AwardDTO setNominated(boolean nominated) {
        this.nominated = nominated;
        return this;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(source)
            .append(event)
            .append(category)
            .append(year)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AwardDTO) {
            final AwardDTO other = (AwardDTO) obj;
            return new EqualsBuilder()
                .append(source, other.source)
                .append(event, other.event)
                .append(category, other.category)
                .append(year, other.year)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
