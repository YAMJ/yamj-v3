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
package org.yamj.core.tools;

import javax.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.CannotAcquireLockException;

/**
 * Exception tools
 */
public final class ExceptionTools {

    private ExceptionTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean isLockingError(Exception e) {
        if (e == null) {
            return false;
        }

        if (e instanceof org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException) {
            return true;
        }

        if (e instanceof org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException) {
            return true;
        }

        if (e instanceof OptimisticEntityLockException) {
            return true;
        }

        if (e instanceof StaleObjectStateException) {
            return true;
        }

        if (e instanceof StaleStateException) {
            return true;
        }

        if (e instanceof OptimisticLockException) {
            return true;
        }

        if (e instanceof CannotAcquireLockException) {
            return true;
        }

        if (e instanceof LockAcquisitionException) {
            return true;
        }

        return false;
    }
}
