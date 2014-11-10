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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "certification",
    uniqueConstraints= @UniqueConstraint(name="UIX_CERTIFICATION_NATURALID", columnNames={"country","certificate"})
)
public class Certification extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 5949467240717893584L;

    @NaturalId
    @Column(name = "country", length = 100, nullable = false)
    private String country;

    @NaturalId
    @Column(name = "certificate", nullable = false, length = 255)
    private String certificate;
    
    // GETTER and SETTER

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (getCountry() == null ? 0 : getCountry().hashCode());
        result = prime * result + (getCertificate() == null ? 0 : getCertificate().hashCode());
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
        if (!StringUtils.equals(getCountry(), castOther.getCountry())) {
            return false;
        }
        // check text
        if (!StringUtils.equals(getCertificate(), castOther.getCertificate())) {
            return false;
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
        sb.append(", certificate=");
        sb.append(getCertificate());
        sb.append("]");
        return sb.toString();
    }
}
