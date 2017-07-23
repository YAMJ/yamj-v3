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
package org.yamj.common.type;

import org.apache.commons.lang3.StringUtils;

/**
 * Enum for the exit codes for the services
 */
public enum ExitType {

    /**
     * Program exited normally
     */
    SUCCESS(0),
    /**
     * Error with a command line property
     */
    CMDLINE_ERROR(1),
    /**
     * Error with a configuration
     */
    CONFIG_ERROR(2),
    /**
     * No directory found
     */
    NO_DIRECTORY(3),
    /**
     * Failed to connect to the server
     */
    CONNECT_FAILURE(4),
    /**
     * Failed to start
     */
    STARTUP_FAILURE(4),
    /**
     * Unable to watch a directory
     */
    WATCH_FAILURE(5),
    /**
     * Unable to send files to the core
     */
    SEND_FAILURE(6),
	/**
     * LOOP send files to the core
     */
    LOOP(7);
    private final int returnValue;

    private ExitType(int returnValue) {
        this.returnValue = returnValue;
    }

    public int getReturn() {
        return this.returnValue;
    }

    /**
     * Get an output string of the exit values
     *
     * @return
     */
    public static String getDescriptions() {
        final StringBuilder sb = new StringBuilder("Exit codes: \n");

        for (ExitType et : ExitType.values()) {
            sb.append("    ");
            sb.append(et.getReturn());
            sb.append(" = ");
            sb.append(StringUtils.capitalize(StringUtils.replace(et.toString(), "_", " ").toLowerCase()));
            sb.append("\n");
        }

        return sb.toString();
    }
}
