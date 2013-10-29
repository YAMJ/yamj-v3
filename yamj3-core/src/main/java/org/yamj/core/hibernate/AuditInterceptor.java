/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package org.yamj.core.hibernate;

import java.io.Serializable;
import java.sql.Timestamp;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

/**
 * Interceptor for entity audits.
 */
public class AuditInterceptor extends EmptyInterceptor {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 6892420119984901561L;

    /**
     * @see org.hibernate.Interceptor#onFlushDirty(Object, Serializable, Object[], Object[], String[], org.hibernate.type.Type[])
     */
    @Override
    public boolean onFlushDirty(final Object entity, final Serializable id, final Object[] currentState,
            final Object[] previousState, final String[] propertyNames, final Type[] types) {
        if (entity instanceof Auditable) {
            for (int i = 0; i < propertyNames.length; i++) {
                if ("updateTimestamp".equals(propertyNames[i])) {
                    currentState[i] = new Timestamp(System.currentTimeMillis());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @see org.hibernate.Interceptor#onSave(Object, Serializable, Object[], String[], org.hibernate.type.Type[])
     */
    @Override
    public boolean onSave(final Object entity, final Serializable id, final Object[] state,
            final String[] propertyNames, final Type[] types) {

        if (entity instanceof Auditable) {
            for (int i = 0; i < propertyNames.length; i++) {
                if ("createTimestamp".equals(propertyNames[i])) {
                    state[i] = new Timestamp(System.currentTimeMillis());
                    return true;
                }
            }
        }
        return false;
    }
}