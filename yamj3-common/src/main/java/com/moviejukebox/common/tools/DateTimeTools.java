package com.moviejukebox.common.tools;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateTimeTools {

    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";

    private DateTimeTools() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Convert a date to a string using the DATE_FORMAT
     *
     * @param convertDate
     * @return converted date in the format specified in DATE_FORMAT_STRING
     */
    public static String convertDateToString(Date convertDate) {
        return convertDateToString(convertDate, DATE_FORMAT_STRING);
    }

    /**
     * Convert a date to a string using the supplied format
     *
     * @param convertDate
     * @param dateFormat
     * @return
     */
    public static String convertDateToString(Date convertDate, final String dateFormat) {
        DateTime dt = new DateTime(convertDate);
        return convertDateToString(dt, dateFormat);
    }

    /**
     * Convert a date to a string using the DATE_FORMAT
     *
     * @param convertDate
     * @return converted date in the format specified in DATE_FORMAT_STRING
     */
    public static String convertDateToString(DateTime convertDate) {
        return convertDateToString(convertDate, DATE_FORMAT_STRING);
    }

    /**
     * Convert a date to a string using the supplied format
     *
     * @param convertDate
     * @param dateFormat
     * @return
     */
    public static String convertDateToString(DateTime convertDate, final String dateFormat) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(dateFormat);
        return fmt.print(convertDate);
    }

    public static String getDuration(Date start, Date end) {
        return getDuration(new DateTime(start), new DateTime(end));
    }

    public static String getDuration(Long start, Long end) {
        return getDuration(new DateTime(start), new DateTime(end));
    }

    public static String getDuration(DateTime start, DateTime end) {
        Interval interval = new Interval(start, end);
        Period period = interval.toPeriod();
        period = period.normalizedStandard();
        return String.format("%02d:%02d:%02d", period.getHours(), period.getMinutes(), period.getSeconds());
    }

    /**
     * Format the duration passed as ?h?m format
     *
     * @param seconds
     * @return
     */
    public static String formatDuration(int seconds) {
        Period period = new Period(seconds * 1000);
        period = period.normalizedStandard();
        return String.format("%02dh%02dm", period.getHours(), period.getMinutes(), period.getSeconds());
    }

    /**
     * Take a string runtime in various formats and try to output this in minutes
     *
     * @param runtime
     * @return
     */
    public static int processRuntime(String runtime) {
        int returnValue;
        // See if we can convert this to a number and assume it's correct if we can
        try {
            returnValue = Integer.parseInt(runtime);
            return returnValue;
        } catch (Exception ignore) {
            returnValue = -1;
        }

        // This is for the format xx(hour/hr/min)yy(min), e.g. 1h30, 90mins, 1h30m
        Pattern hrmnPattern = Pattern.compile("(?i)(\\d+)(\\D*)(\\d*)(.*?)");

        Matcher matcher = hrmnPattern.matcher(runtime);
        if (matcher.find()) {
            String first = matcher.group(1);
            String divide = matcher.group(2);
            String second = matcher.group(3);

            if (StringUtils.isNotBlank(second)) {
                // Assume that this is HH(text)MM
                returnValue = (Integer.parseInt(first) * 60) + Integer.parseInt(second);
                return returnValue;
            }

            if (StringUtils.isBlank(divide)) {
                // No divider value, so assume this is a straight minute value
                returnValue = Integer.parseInt(first);
                return returnValue;
            }

            if (StringUtils.isBlank(second) && StringUtils.isNotBlank(divide)) {
                // this is xx(text) so we need to work out what the (text) is
                if (divide.toLowerCase().contains("h")) {
                    // Assume it is a form of "hours", so convert to minutes
                    returnValue = Integer.parseInt(first) * 60;
                } else {
                    // Assume it's a form of "minutes"
                    returnValue = Integer.parseInt(first);
                }
                return returnValue;
            }
        }

        return returnValue;
    }
}
