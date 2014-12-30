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
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.core.hibernate.Auditable;
import org.yamj.core.hibernate.Identifiable;

/**
 * Abstract implementation of an identifiable and auditable object.
 */
@MappedSuperclass
@SuppressWarnings("unused")
public abstract class AbstractAuditable implements Auditable, Identifiable {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private long id;

    @Version
    @Column(name = "lock_no", nullable = false)
    private int lockNo = 0;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "create_timestamp", nullable = false, updatable = false)
    private Date createTimestamp;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "update_timestamp")
    private Date updateTimestamp;

    // GETTER AND SETTER

    @Override
    public long getId() {
        return this.id;
    }

    private void setId(long id) {
        this.id = id;
    }

    public boolean isNewlyCreated() {
        return (this.id <= 0);
    }

    private Date getCreateTimestamp() {
        return this.createTimestamp;
    }

    private void setCreateTimestamp(final Date createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    private Date getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    private void setUpdateTimestamp(final Date updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    private int getLockNo() {
        return lockNo;
    }

    private void setLockNo(int lockNo) {
        this.lockNo = lockNo;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}