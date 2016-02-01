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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public final class DateTimeTools {

    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";
    private static final PeriodFormatter TIME_FORMAT_COLON = createPeriodFormatter(":", ":", "");
    private static final PeriodFormatter TIME_FORMAT_TEXT = createPeriodFormatter("h", "m", "s");
    // some default formats in use
    public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String BUILD_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

    private DateTimeTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Create a Period Formatter with the given delimiters
     *
     * @param hourText
     * @param minuteText
     * @param secondText
     * @return
     */
    private static PeriodFormatter createPeriodFormatter(final String hourText, final String minuteText, final String secondText) {
        return new PeriodFormatterBuilder()
                .appendHours()
                .appendSeparator(hourText)
                .minimumPrintedDigits(2)
                .appendMinutes()
                .appendSeparator(minuteText)
                .appendSecondsWithOptionalMillis()
                .appendSuffix(secondText)
                .toFormatter();
    }

    /**
     * Convert a date to a string using the DATE_FORMAT
     *
     * @param convertDate
     * @return converted date in the format specified in DATE_FORMAT_STRING
     */
    public static String convertDateToString(final Date convertDate) {
        return convertDateToString(convertDate, DATE_FORMAT_STRING);
    }

    /**
     * Convert a date to a string using the supplied format
     *
     * @param convertDate
     * @param dateFormat
     * @return
     */
    public static String convertDateToString(final Date convertDate, final String dateFormat) {
        return convertDateToString(new DateTime(convertDate), dateFormat);
    }

    /**
     * Convert a date to a string using the DATE_FORMAT
     *
     * @param convertDate
     * @return converted date in the format specified in DATE_FORMAT_STRING
     */
    public static String convertDateToString(final DateTime convertDate) {
        return convertDateToString(convertDate, DATE_FORMAT_STRING);
    }

    /**
     * Convert a date to a string using the supplied format
     *
     * @param convertDate
     * @param dateFormat
     * @return
     */
    public static String convertDateToString(final DateTime convertDate, final String dateFormat) {
        return DateTimeFormat.forPattern(dateFormat).print(convertDate);
    }

    /**
     * Get the duration between two Java Dates
     *
     * @param start
     * @param end
     * @return
     */
    public static long getDuration(final Date start, final Date end) {
        return getDuration(new DateTime(start.getTime()), new DateTime(end.getTime()));
    }

    /**
     * Get the duration between two Long Dates
     *
     * @param start
     * @param end
     * @return
     */
    public static long getDuration(final Long start, final Long end) {
        return getDuration(new DateTime(start.longValue()), new DateTime(end.longValue()));
    }

    /**
     * Get the duration between tow Joda Dates
     *
     * @param start
     * @param end
     * @return the difference (in milliseconds) or -1 if "start" is after "end"
     */
    public static long getDuration(final DateTime start, final DateTime end) {
        if (start.isBefore(end)) {
            return new Interval(start, end).toDurationMillis();
        }
        return -1L;
    }

    /**
     * Format the duration in milliseconds as ?:?:? format
     *
     * @param milliseconds
     * @return
     */
    public static String formatDurationColon(final long milliseconds) {
        return formatDuration(milliseconds, TIME_FORMAT_COLON);
    }

    /**
     * Format the duration in milliseconds as ?h?m?s format
     *
     * @param milliseconds
     * @return
     */
    public static String formatDurationText(long milliseconds) {
        return formatDuration(milliseconds, TIME_FORMAT_TEXT);
    }

    /**
     * Format the duration in milliseconds in the given format
     *
     * @param milliseconds
     * @param format
     * @return
     */
    public static String formatDuration(final long milliseconds, final PeriodFormatter format) {
        return format.print(new Period(milliseconds, PeriodType.time()).normalizedStandard());
    }

    /**
     * Take a string runtime in various formats and try to output this in minutes
     *
     * @param runtime
     * @return
     */
    public static int processRuntime(final String runtime) {
        // see if we can convert this to a number and assume it's correct if we can
        int returnValue = (int)NumberUtils.toFloat(runtime, -1f);
        if (returnValue > 0) {
            return returnValue;
        }
        
        // this is for the format xx(hour/hr/min)yy(min), e.g. 1h30, 90mins, 1h30m
        Pattern hrmnPattern = Pattern.compile("(?i)(\\d+)(\\D*)(\\d*)(.*?)");

        Matcher matcher = hrmnPattern.matcher(runtime);
        if (matcher.find()) {
            String first = matcher.group(1);
            String divide = matcher.group(2);
            String second = matcher.group(3);

            if (StringUtils.isNotBlank(second)) {
                // assume that this is HH(text)MM
                return (Integer.parseInt(first) * 60) + Integer.parseInt(second);
            }

            if (StringUtils.isBlank(divide)) {
                // no divider value, so assume this is a straight minute value
                return Integer.parseInt(first);
            }

            if (divide.toLowerCase().contains("h")) {
                // assume it is a form of "hours", so convert to minutes
                return Integer.parseInt(first) * 60;
            }
            
            // assume it's a form of "minutes"
            return Integer.parseInt(first);
        }

        return -1;
    }

    /**
     * Parses a string in the provided format to a DateTime
     *
     * @param stringDate the date to parse
     * @param datePattern the pattern to parse
     * @return
     */
    public static DateTime parseDate(final String stringDate, final String datePattern) {
        return DateTimeFormat.forPattern(datePattern).parseDateTime(stringDate);
    }
}
