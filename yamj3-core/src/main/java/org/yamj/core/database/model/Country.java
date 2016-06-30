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
import org.yamj.core.api.model.dto.ApiCountryDTO;

@NamedNativeQueries({    
    @NamedNativeQuery(name = Country.DELETE_ORPHANS,
        query = "DELETE FROM country WHERE not exists (select 1 from videodata_countries vc where vc.country_id=id) "+
                "AND not exists (select 1 from series_countries sc where sc.country_id=id)"
    ),
    @NamedNativeQuery(name = "metadata.country.series", resultSetMapping="metadata.country",
        query = "SELECT c.id, c.country_code as countryCode FROM country c "+
                "JOIN series_countries sc ON c.id=sc.country_id and sc.series_id=:id "
    ),
    @NamedNativeQuery(name = "metadata.country.season", resultSetMapping="metadata.country",
        query = "SELECT c.id, c.country_code as countryCode FROM country c "+
                "JOIN season sea ON sea.id=:id JOIN series_countries sc ON c.id=sc.country_id and sc.series_id=sea.series_id "
    ),
    @NamedNativeQuery(name = "metadata.country.movie", resultSetMapping="metadata.country",
        query = "SELECT c.id, c.country_code as countryCode FROM country c "+
                "JOIN videodata_countries vc ON c.id=vc.country_id and vc.data_id=:id "
    )
})

@SqlResultSetMapping(name="metadata.country", classes={
    @ConstructorResult(
        targetClass=ApiCountryDTO.class,
        columns={
             @ColumnResult(name="id", type=Long.class),
             @ColumnResult(name="countryCode", type=String.class)
        }
    )}
)

@Entity
@Table(name = "country",
        uniqueConstraints = @UniqueConstraint(name = "UIX_COUNTRY_NATURALID", columnNames = {"country_code"})
)
public class Country extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 474957956935900650L;
    public static final String DELETE_ORPHANS = "country.deleteOrphans";
    
    @NaturalId(mutable = true)
    @Column(name = "country_code", nullable = false, length = 4)
    private String countryCode;

    public Country() {
        // empty constructor
    }

    public Country(String countryCode) {
        this.countryCode = countryCode;
    }

    // GETTER and SETTER
    
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getCountryCode())
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
        if (!(obj instanceof Country)) {
            return false;
        }
        Country other = (Country) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values        
        return new EqualsBuilder()
                .append(getCountryCode(), other.getCountryCode())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Country [ID=");
        sb.append(getId());
        sb.append(", countryCode=");
        sb.append(getCountryCode());
        sb.append("]");
        return sb.toString();
    }
}
