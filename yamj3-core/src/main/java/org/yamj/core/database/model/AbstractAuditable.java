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

import java.util.Date;
import javax.persistence.*;
import org.yamj.core.hibernate.Auditable;

/**
 * Abstract implementation of an identifiable and auditable object.
 */
@MappedSuperclass
@SuppressWarnings("unused")
public abstract class AbstractAuditable extends AbstractIdentifiable implements Auditable {

    @Version
    @Column(name = "lock_no", nullable = false)
    private int lockNo = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_timestamp", nullable = false, updatable = false)
    private Date createTimestamp;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_timestamp")
    private Date updateTimestamp;

    // GETTER AND SETTER
    
    public final Date getCreateTimestamp() {
        return this.createTimestamp;
    }

    private final void setCreateTimestamp(final Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public final Date getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    private final void setUpdateTimestamp(final Date updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    private final int getLockNo() {
        return lockNo;
    }

    private final void setLockNo(int lockNo) {
        this.lockNo = lockNo;
    }
}