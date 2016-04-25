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
package org.yamj.plugin.api.metadata;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.pojava.datetime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetadataTools {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataTools.class);

    private static final Pattern DATE_COUNTRY = Pattern.compile("(.*)(\\s*?\\(\\w*\\))");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(?:.*?)(\\d{4})(?:.*?)");
    private static final Pattern LASTNAME_PATTERN = Pattern.compile("((?:(?:d[aeiu]|de la|mac|zu|v[ao]n(?: de[nr])?) *)?[^ ]+) *(.*)");
    private static final String FROM_WIKIPEDIA = "From Wikipedia, the free encyclopedia";
    private static final String WIKIPEDIA_DESCRIPTION_ABOVE = "Description above from the Wikipedia";
    private static final String MPPA_RATED = "Rated";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final IDateTimeConfig DATETIME_CONFIG_DEFAULT;
    private static final IDateTimeConfig DATETIME_CONFIG_FALLBACK;

    static {
        // default date and time configuration
        DATETIME_CONFIG_DEFAULT = DateTimeConfig.getGlobalDefault();
        // fall-back configuration
        DateTimeConfigBuilder builder = DateTimeConfigBuilder.newInstance();
        builder.setDmyOrder(!DATETIME_CONFIG_DEFAULT.isDmyOrder());
        DATETIME_CONFIG_FALLBACK = DateTimeConfig.fromBuilder(builder);
    }

    private MetadataTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Format the date into short format
     *
     * @param date
     * @return
     */
    public static String formatDateShort(Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(date);
    }

    /**
     * Format the date into short format
     *
     * @param date
     * @return
     */
    public static String formatDateLong(Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT + " HH:mm:ss");
        return sdf.format(date);
    }

    public static int toYear(String string) {
        int year;

        if (StringUtils.isNotBlank(string) && StringUtils.isNumeric(string)) {
            try {
                year = Integer.parseInt(string);
            } catch (NumberFormatException ex) {
                year = -1;
            }
        } else {
            year = -1;
        }
        return year;
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
    public static Date parseToDate(final String dateToParse) {
        Date parsedDate = null;

        String parseDate = StringUtils.normalizeSpace(dateToParse);
        if (StringUtils.isNotBlank(parseDate)) {
            try {
                if (parseDate.length() == 4 && StringUtils.isNumeric(parseDate)) {
                    // assume just the year an append "-01-01" to the end
                    parseDate = parseDate + "-01-01";
                } else {
                    // look for the date as "dd MMMM yyyy (Country)" and remove the country
                    Matcher m = DATE_COUNTRY.matcher(parseDate);
                    if (m.find()) {
                        parseDate = m.group(1);
                    }
                }
                parsedDate = parseToDate(parseDate, DATETIME_CONFIG_DEFAULT);
                if (parsedDate == null) {
                    // try with fall-back configuration
                    parsedDate = parseToDate(parseDate, DATETIME_CONFIG_FALLBACK);
                }
            } catch (Exception ex) {
                LOG.debug("Failed to parse date '{}', error: {}", dateToParse, ex.getMessage());
                LOG.trace("Error", ex);
            }
        }

        return parsedDate;
    }
    
    /**
     * Convert the string date using DateTools parsing
     *
     * @param dateToParse
     * @param config
     * @return
     */
    private static Date parseToDate(String dateToParse, IDateTimeConfig config) {
        Date parsedDate = null;
        try {
            parsedDate = DateTime.parse(dateToParse, config).toDate();
            LOG.trace("Converted date '{}' using {} order", dateToParse, (config.isDmyOrder() ? "DMY" : "MDY"));
        } catch (IllegalArgumentException ex) {
            LOG.debug("Failed to convert date '{}' using {} order", dateToParse, (config.isDmyOrder() ? "DMY" : "MDY"));
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
        if (date == null) {
            return null;
        }

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
        if (date == null) {
            return -1;
        }

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
    public static int extractYearAsInt(String date) {
        if (StringUtils.isBlank(date)) {
            return -1;
        }
        if (StringUtils.isNumeric(date) && (date.length() == 4)) {
            return NumberUtils.toInt(date, -1);
        }

        int year = -1;
        Matcher m = YEAR_PATTERN.matcher(date);
        if (m.find()) {
            year = NumberUtils.toInt(m.group(1), -1);
        }

        return year;
    }
    
    /**
     * Parse a string value and convert it into an integer rating
     *
     * The rating should be between 0 and 10 inclusive.<br/>
     * Invalid values or values less than 0 will return -1
     *
     * @param rating the converted rating or -1 if there was an error
     * @return
     */
    public static int parseRating(String rating) {
        if (StringUtils.isBlank(rating)) {
            // Rating isn't valid, so skip it
            return -1;
        }
        return parseRating(NumberUtils.toFloat(rating.replace(',', '.'), -1));
    }

    /**
     * Parse a float rating into an integer
     *
     * The rating should be between 0 and 10 inclusive.<br/>
     * Invalid values or values less than 0 will return -1
     *
     * @param rating the converted rating or -1 if there was an error
     * @return
     */
    public static int parseRating(float rating) {
        if (rating > 0.0f) {
            if (rating <= 10.0f) {
                return Math.round(rating * 10f);
            }
            return Math.round(rating * 1f);
        }
        return -1;
    }
    
    /**
     * Get the certification from the MPAA string
     *
     * @param mpaaCertification
     * @return
     */
    public static String processMpaaCertification(String mpaaCertification) {
        return processMpaaCertification(MPPA_RATED, mpaaCertification);
    }

    /**
     * Get the certification from the MPAA rating string
     *
     * @param mpaaRated
     * @param mpaaCertification
     * @return
     */
    public static String processMpaaCertification(String mpaaRated, String mpaaCertification) {
        // Strip out the "Rated " and extra words at the end of the MPAA certification
        Pattern mpaaPattern = Pattern.compile("(?:" + (StringUtils.isNotBlank(mpaaRated) ? mpaaRated : MPPA_RATED) + "\\s)?(.*?)(?:($|\\s).*?)");
        Matcher m = mpaaPattern.matcher(mpaaCertification);
        if (m.find()) {
            return m.group(1).trim();
        }
        return mpaaCertification.trim();
    }

    public static PersonName splitFullName(String fullName) {
        PersonName personName = new PersonName(fullName);
        
        try {
            String[] result = StringUtils.split(fullName, ' ');
            if (result == null || result.length == 0) {
                // nothing to do
            } else if (result.length == 1) {
                personName.setFirstName(result[0]);
            } else if (result.length == 2) {
                personName.setFirstName(result[0]);
                personName.setLastName(result[1]);
            } else {
                Matcher m = LASTNAME_PATTERN.matcher(fullName);
                if (m.matches()) {
                    personName.setFirstName(m.group(1));
                    personName.setLastName(m.group(2));
                }
            }
        } catch (Exception ex) {
            LOG.trace("Error splitting full person name: " + fullName, ex);
        }
        
        return personName;
    }
    
    public static String fixScannedValue(final String value) {
        return StringUtils.replace(value, "\"", "'");
    }

    /**
     * Remove unneeded text from the plot
     *
     * @param bio
     * @return
     */
    public static String cleanPlot(final String plot) {
        String newPlot = StringUtils.trimToNull(plot);
        if (newPlot == null) {
            return null;
        }

        return newPlot = newPlot.replaceAll("\\u00A0", " ").replaceAll("\\s+", " ").replaceAll("\"", "'");
    }
    
    /**
     * Remove unneeded text from the biography
     *
     * @param bio
     * @return
     */
    public static String cleanBiography(final String bio) {
        String newBio = StringUtils.trimToNull(bio);
        if (newBio == null) {
            return null;
        }

        newBio = newBio.replaceAll("\\u00A0", " ").replaceAll("\\s+", " ").replaceAll("\"", "'");
        
        int pos = StringUtils.indexOfIgnoreCase(newBio, FROM_WIKIPEDIA);
        if (pos >= 0) {
            newBio = newBio.substring(pos + FROM_WIKIPEDIA.length() + 1);
        }

        pos = StringUtils.indexOfIgnoreCase(newBio, WIKIPEDIA_DESCRIPTION_ABOVE);
        if (pos >= 0) {
            newBio = newBio.substring(0, pos);
        }
        
        return newBio;
    }

    /**
     * Determine from role if it is a voice role
     *
     * @param role
     * @return
     */
    public static boolean isVoiceRole(final String role) {
        return StringUtils.indexOfIgnoreCase(role, "(voice") != -1;
    }

    /**
     * Remove unneeded text from the role
     *
     * @param role
     * @return
     */
    public static String cleanRole(final String role) {
        String newRole = StringUtils.trimToNull(role);
        if (newRole == null) {
            return null;
        }
    
        // (voice)
        int idx = StringUtils.indexOfIgnoreCase(newRole, "(voice");
        if (idx > 0) {
            newRole = newRole.substring(0, idx);
        }
        
        // (as ... = alternate name
        idx = StringUtils.indexOfIgnoreCase(newRole, "(as ");
        if (idx > 0) {
            newRole = newRole.substring(0, idx);
        }
        
        // uncredited cast member
        idx = StringUtils.indexOfIgnoreCase(newRole, "(uncredit");
        if (idx > 0) {
            newRole = newRole.substring(0, idx);
        }
        
        // season marker
        idx = StringUtils.indexOfIgnoreCase(newRole, "(Season");
        if (idx > 0) {
            newRole = newRole.substring(0, idx);
        }
    
        // double characters
        idx = StringUtils.indexOf(newRole, "/");
        if (idx > 0) {
            List<String> characters = Arrays.asList(newRole.split("/"));
            newRole = StringUtils.join(characters.toArray(), " / ");
        }
        
        newRole = fixScannedValue(newRole);
        newRole = newRole.replaceAll("( )+", " ").trim();
        
        return newRole;
    }
        
    public static boolean isOriginalTitleScannable(String title, String originalTitle) {
        if (StringUtils.isBlank(originalTitle)) {
            return false;
        }
        return !StringUtils.equalsIgnoreCase(title, originalTitle);
    }
}
