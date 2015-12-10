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
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.Type;
import org.yamj.common.type.StatusType;

/**
 * Abstract implementation of a stateful object.
 */
@MappedSuperclass
public abstract class AbstractStateful extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = -1388803900038754325L;
    
    @Type(type = "statusType")
    @Column(name = "status", nullable = false, length = 30)
    private StatusType status;
    
    // CONSTRUCTORS
    
    public AbstractStateful() {
        super();
    }
    
    // GETTER and SETTER
    
    public final StatusType getStatus() {
        return status;
    }
    
    public void setStatus(StatusType status) {
        this.status = status;
    }
    
    // TRANSIENT METHODS
    
    public final boolean isValidStatus() {
        return StatusType.DONE.equals(status) || StatusType.NEW.equals(status) || StatusType.UPDATED.equals(status);
    }

}
