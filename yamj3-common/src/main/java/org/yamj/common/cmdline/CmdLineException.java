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
package org.yamj.common.cmdline;

/**
 * This class defines a command line exception.
 */
public class CmdLineException extends Exception {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 5975436536951550183L;

    /**
     * Constructor of the exception.
     *
     * @param message the detail message
     */
    public CmdLineException(final String message) {
        super(message);
    }

    /**
     * Constructor of the exception.
     *
     * @param cause the cause
     */
    public CmdLineException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructor of the exception.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public CmdLineException(final String message, final Throwable cause) {
        super(message, cause);
    }
}