package org.yamj.common.tools;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public final class DateTimeTools {

    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";
    private static final PeriodFormatter TIME_FORMAT_COLON = createPeriodFormatter(":", ":", "");
    private static final PeriodFormatter TIME_FORMAT_TEXT = createPeriodFormatter("h", "m", "s");
    // Some default formats in use
    public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String BUILD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private DateTimeTools() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Create a Period Formatter with the given delimiters
     *
     * @param hourText
     * @param minuteText
     * @param secondText
     * @return
     */
    private static PeriodFormatter createPeriodFormatter(String hourText, String minuteText, String secondText) {
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

    /**
     * Get the duration between two Java Dates
     *
     * @param start
     * @param end
     * @return
     */
    public static long getDuration(Date start, Date end) {
        return getDuration(new DateTime(start), new DateTime(end));
    }

    /**
     * Get the duration between two Long Dates
     *
     * @param start
     * @param end
     * @return
     */
    public static long getDuration(Long start, Long end) {
        return getDuration(new DateTime(start), new DateTime(end));
    }

    /**
     * Get the duration between tow Joda Dates
     *
     * @param start
     * @param end
     * @return
     */
    public static long getDuration(DateTime start, DateTime end) {
        Interval interval = new Interval(start, end);
        return interval.toDurationMillis();
    }

    /**
     * Format the duration in milliseconds as ?:?:? format
     *
     * @param milliseconds
     * @return
     */
    public static String formatDurationColon(long milliseconds) {
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
    public static String formatDuration(long milliseconds, PeriodFormatter format) {
        Period period = new Period(milliseconds, PeriodType.time());
        period = period.normalizedStandard();
        return format.print(period);
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

    /**
     * Parses a string in the provided format to a DateTime
     *
     * @param stringDate the date to parse
     * @param datePattern the pattern to parse
     * @return
     */
    public static DateTime parseDate(String stringDate, String datePattern) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(datePattern);
        return formatter.parseDateTime(stringDate);
    }
}
