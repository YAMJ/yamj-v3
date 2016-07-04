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
import org.hibernate.annotations.Type;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.dto.QueueDTO;

/**
 * Abstract implementation of a stateful object.
 */
@SqlResultSetMapping(name="id.queue", classes={
    @ConstructorResult(
        targetClass=QueueDTO.class, 
        columns={@ColumnResult(name="id", type=Long.class)}
    )}
)

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
    
    public StatusType getStatus() {
        return status;
    }
    
    public void setStatus(StatusType status) {
        this.status = status;
    }
    
    // TRANSIENT METHODS
    
    @Transient
    public final boolean isDeleted() {
        return StatusType.DELETED.equals(getStatus());
    }

    @Transient
    public final boolean isNotFound() {
        return StatusType.NOTFOUND.equals(getStatus());
    }

    @Transient
    public final boolean isDuplicate() {
        return StatusType.DUPLICATE.equals(getStatus());
    }
    
    @Transient
    public final boolean isUpdated() {
        return StatusType.NEW.equals(getStatus()) || StatusType.UPDATED.equals(getStatus());
    }

    @Transient
    public final boolean isNotUpdated() {
        return !isUpdated();
    }

    @Transient
    public final boolean isNew() {
        return StatusType.NEW.equals(getStatus());
    }
    
    @Transient
    public final boolean isValid() {
        return StatusType.DONE.equals(getStatus()) || StatusType.NEW.equals(getStatus()) || StatusType.UPDATED.equals(getStatus());
    }
}
