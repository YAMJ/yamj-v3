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

import javax.persistence.UniqueConstraint;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "certification",
    uniqueConstraints= @UniqueConstraint(name="UIX_CERTIFICATION_NATURALID", columnNames={"certification_text"})
)
public class Certification extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = 5949467240717893584L;

    @NaturalId(mutable = true)
    @Column(name = "certification_text", nullable = false, length = 50)
    private String certificationText;

    @Column(name = "country", length = 100, nullable = false)
    private String country;

    // GETTER and SETTER

    public String getCertificationText() {
        return certificationText;
    }

    public void setCertificationText(String certificationText) {
        this.certificationText = certificationText;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 17;
        int result = 1;
        result = prime * result + (this.certificationText == null ? 0 : this.certificationText.hashCode());
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
        return StringUtils.equals(this.certificationText, castOther.certificationText);
    }
}
