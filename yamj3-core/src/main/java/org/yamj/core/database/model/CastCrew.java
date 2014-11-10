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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.yamj.core.database.model.type.JobType;

@Entity
@Table(name = "cast_crew")
public class CastCrew implements Serializable {

    private static final long serialVersionUID = -3941301942248344131L;

    @EmbeddedId
    private CastCrewPK castCrewPK;

    @Column(name = "role", length = 255)
    private String role;

    @Column(name = "ordering", nullable = false)
    private int ordering;

    // GETTER and SETTER

    public CastCrew() {}
    
    public CastCrew(Person person, VideoData videoData, JobType jobType) {
        setCastCrewPK(new CastCrewPK(person, videoData, jobType));
    }
                    
    private CastCrewPK getCastCrewPK() {
        return castCrewPK;
    }

    private void setCastCrewPK(CastCrewPK castCrewPK) {
        this.castCrewPK = castCrewPK;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }
    
    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (getCastCrewPK() == null ? 0 : getCastCrewPK().hashCode());
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
        if (!(other instanceof CastCrew)) {
            return false;
        }
        CastCrew castOther = (CastCrew) other;
        return getCastCrewPK().equals(castOther.getCastCrewPK());
    }
}
