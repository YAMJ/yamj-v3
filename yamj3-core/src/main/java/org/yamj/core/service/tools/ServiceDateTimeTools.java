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
package org.yamj.core.service.tools;

import java.util.Calendar;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.pojava.datetime.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServiceDateTimeTools {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceDateTimeTools.class);
    private static final Pattern DATE_COUNTRY = Pattern.compile("(.*)(\\s*?\\(\\w*\\))");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(?:.*?)(\\d{4})(?:.*?)");

    private ServiceDateTimeTools() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Format the duration passed as ?h?m format
     *
     * @param duration Duration in seconds
     * @return
     */
    public static String formatDuration(int duration) {
        StringBuilder returnString = new StringBuilder();

        int nbHours = duration / 3600;
        if (nbHours != 0) {
            returnString.append(nbHours).append("h");
        }

        int nbMinutes = (duration - (nbHours * 3600)) / 60;
        if (nbMinutes != 0) {
            if (nbHours != 0) {
                returnString.append(" ");
            }
            returnString.append(nbMinutes).append("m");
        }

        LOG.trace("Formatted duration " + duration + " to " + returnString.toString());
        return returnString.toString();
    }

    /**
     * Take a string runtime in various formats and try to output this in minutes
     *
     * @param runtime
     * @return
     */
    public static int processRuntime(String runtime) {
        return processRuntime(runtime, -1);
    }

    /**
     * Take a string runtime in various formats and try to output this in minutes
     *
     * @param runtime
     * @param defaultValue
     * @return
     */
    public static int processRuntime(String runtime, int defaultValue) {
        if (StringUtils.isBlank(runtime)) {
            // No string to parse
            return defaultValue;
        }

        int returnValue;
        // See if we can convert this to a number and assume it's correct if we can
        returnValue = NumberUtils.toInt(runtime, defaultValue);

        if (returnValue < 0) {
            // This is for the format xx(hour/hr/min)yy(min), e.g. 1h30, 90mins, 1h30m
            Pattern hrmnPattern = Pattern.compile("(?i)(\\d+)(\\D*)(\\d*)(.*?)");

            Matcher matcher = hrmnPattern.matcher(runtime);
            if (matcher.find()) {
                int first = NumberUtils.toInt(matcher.group(1), -1);
                String divide = matcher.group(2);
                int second = NumberUtils.toInt(matcher.group(3), -1);

                if (first > -1 && second > -1) {
                    returnValue = (first > -1 ? first * 60 : 0) + (second > -1 ? second : 0);
                } else if (StringUtils.isBlank(divide)) {
                    // No divider value, so assume this is a straight minute value
                    returnValue = first;
                } else if (second > -1 && StringUtils.isNotBlank(divide)) {
                    // this is xx(text) so we need to work out what the (text) is
                    if (divide.toLowerCase().contains("h")) {
                        // Assume it is a form of "hours", so convert to minutes
                        returnValue = first * 60;
                    } else {
                        // Assume it's a form of "minutes"
                        returnValue = first;
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Convert a string to date to
     *
     * @param dateToParse
     * @return
     */
    public static Date parseToDate(String dateToParse) {
        Date parsedDate = null;
        
        String parseDate = StringUtils.normalizeSpace(dateToParse);
        if (StringUtils.isNotBlank(parseDate)) {
            try {
                DateTime dateTime;
                if (parseDate.length() == 4 && StringUtils.isNumeric(parseDate)) {
                    // assume just the year an append "-01-01" to the end
                    dateTime = new DateTime(parseDate + "-01-01");
                } else {
                    // look for the date as "dd MMMM yyyy (Country)" and remove the country
                    Matcher m = DATE_COUNTRY.matcher(dateToParse);
                    if (m.find()) {
                        parseDate = m.group(1);
                    }
                    dateTime = new DateTime(parseDate);
                }
                parsedDate = dateTime.toDate();
            } catch (Exception ex) {
                LOG.debug("Failed to parse date '{}', error: {}", dateToParse, ex.getMessage());
                LOG.trace("Error", ex);
            }
        }

        return parsedDate;
    }

    /**
     * Get the year as string from given date.
     * 
     * @param date
     * @return 
     */
    public static String extractYearAsString(Date date) {
        if (date == null) return null;
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.valueOf(cal.get(Calendar.YEAR));
    }

    /**
     * Get the year as integer from given date.
     * 
     * @param date
     * @return 
     */
    public static int extractYearAsInt(Date date) {
        if (date == null) return -1;
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    /**
     * Locate a 4 digit year in a date string.
     *
     * @param date
     * @return
     */
    public static int extractYear(String date) {
        int year = 0;
        Matcher m = YEAR_PATTERN.matcher(date);
        if (m.find()) {
            year = Integer.valueOf(m.group(1)).intValue();
        }

        // Give up and return 0
        return year;
    }

}
