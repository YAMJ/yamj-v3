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
package org.yamj.common.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tools that are useful to the classes themselves, not necessary for
 * functionality
 *
 * @author stuart.boston
 */
public class ClassTools {

    private static final Logger LOG = LoggerFactory.getLogger(ClassTools.class);

    private ClassTools() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Helper method to print the stack trace to the log file
     *
     * @param tw
     * @return
     */
    public static String getStackTrace(Throwable tw) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        tw.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    /**
     * Check to see if a system property has been set, and set it if not.
     *
     * @param property
     * @param defaultValue
     * @return
     */
    public static String checkSystemProperty(String property, String defaultValue) {
        // Check to see if the yamj3.home property is set
        String systemProperty = System.getProperty(property, "");
        if (StringUtils.isBlank(systemProperty)) {
            LOG.debug("System property '{}' not found. Setting to '{}'", property, defaultValue);
            System.setProperty(property, defaultValue);
            return defaultValue;
        } else {
            return systemProperty;
        }
    }
}
