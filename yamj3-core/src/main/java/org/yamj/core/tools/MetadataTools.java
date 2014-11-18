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
package org.yamj.core.tools;

import com.ibm.icu.text.Transliterator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.pojava.datetime.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.tools.PropertyTools;
import org.yamj.common.tools.StringTools;
import org.yamj.core.database.model.AbstractMetadata;
import org.yamj.core.database.model.MediaFile;
import org.yamj.core.database.model.VideoData;

public final class MetadataTools {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataTools.class);
    
    private static final Pattern CLEAN_STRING_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-\\(\\)]");
    private static final char[] CLEAN_DELIMITERS = new char[]{'.',' ','_','-'};
    private static final Pattern DATE_COUNTRY = Pattern.compile("(.*)(\\s*?\\(\\w*\\))");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(?:.*?)(\\d{4})(?:.*?)");
    private static final String MPPA_RATED = "Rated";
    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * KB * KB;
    private static final Map<Character, Character> CHAR_REPLACEMENT_MAP = new HashMap<Character, Character>();
    
    private static final DecimalFormat FILESIZE_FORMAT_0;
    private static final DecimalFormat FILESIZE_FORMAT_1;
    private static final DecimalFormat FILESIZE_FORMAT_2;
    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT_LONG;
    private static final boolean IDENT_TRANSLITERATE;
    private static final boolean IDENT_CLEAN;
    private static final Transliterator TRANSLITERATOR;

    private MetadataTools() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    static {
        // identifier cleaning
        IDENT_TRANSLITERATE = PropertyTools.getBooleanProperty("yamj3.identifier.transliterate", Boolean.FALSE);
        IDENT_CLEAN = PropertyTools.getBooleanProperty("yamj3.identifier.clean", Boolean.TRUE);
        
        // Populate the charReplacementMap
        String temp = PropertyTools.getProperty("indexing.character.replacement", "");
        StringTokenizer tokenizer = new StringTokenizer(temp, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            int idx = token.indexOf('-');
            if (idx > 0) {
                String key = token.substring(0, idx).trim();
                String value = token.substring(idx + 1).trim();
                if (key.length() == 1 && value.length() == 1) {
                    CHAR_REPLACEMENT_MAP.put(Character.valueOf(key.charAt(0)), Character.valueOf(value.charAt(0)));
                }
            }
        }
        
        String dateFormat = PropertyTools.getProperty("yamj3.date.format", "yyyy-MM-dd");
        
        // short date format
        SimpleDateFormat sdf;
        try {
            sdf = new SimpleDateFormat(dateFormat);
        } catch (Exception ignore) {
            sdf = new SimpleDateFormat("yyyy-MM-dd");
        }
        DATE_FORMAT = sdf;
        try {
            sdf = new SimpleDateFormat(dateFormat + " HH:mm:ss");
        } catch (Exception ignore) {
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        DATE_FORMAT_LONG = sdf;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        // Use the "." as a decimal format separator, ignoring localisation
        symbols.setDecimalSeparator('.');
        FILESIZE_FORMAT_0 = new DecimalFormat("0", symbols);
        FILESIZE_FORMAT_1 = new DecimalFormat("0.#", symbols);
        FILESIZE_FORMAT_2 = new DecimalFormat("0.##", symbols);

        // create a new transliterator
        TRANSLITERATOR = Transliterator.getInstance("NFD; Any-Latin; NFC");
    }
    
    /**
     * Check the passed character against the replacement list.
     *
     * @param charToReplace
     * @return
     */
    public static String characterMapReplacement(Character charToReplace) {
        Character tempC = CHAR_REPLACEMENT_MAP.get(charToReplace);
        if (tempC == null) {
            return charToReplace.toString();
        } else {
            return tempC.toString();
        }
    }

    /**
     * Change all the characters in a string to the safe replacements
     *
     * @param stringToReplace
     * @return
     */
    public static String stringMapReplacement(String input) {
        if (input == null || CHAR_REPLACEMENT_MAP.isEmpty()) {
            return input;
        }
        
        Character tempC;
        StringBuilder sb = new StringBuilder();

        for (Character c : input.toCharArray()) {
            tempC = CHAR_REPLACEMENT_MAP.get(c);
            if (tempC == null) {
                sb.append(c);
            } else {
                sb.append(tempC);
            }
        }
        return sb.toString();
    }

    /**
     * Format the date into short format
     * 
     * @param date
     */
    public static String formatDateShort(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }

    /**
     * Format the date into short format
     * 
     * @param date
     */
    public static String formatDateLong(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT_LONG.format(date);
    }
    
    /**
     * Format the file size
     *
     * @param fileSize
     * @return
     */
    public static String formatFileSize(long fileSize) {
        String returnSize;
        if (fileSize < 0 ) {
           returnSize = null;
        } else if (fileSize < KB) {
            returnSize = fileSize + " Bytes";
        } else {
            String appendText;
            long divider;

            // resolve text to append and divider
            if (fileSize < MB) {
                appendText = " KB";
                divider = KB;
            } else if (fileSize < GB) {
                appendText = " MB";
                divider = MB;
            } else {
                appendText = " GB";
                divider = GB;
            }

            // resolve decimal format
            DecimalFormat df;
            long checker = (fileSize / divider);
            if (checker < 10) {
                df = FILESIZE_FORMAT_2;
            } else if (checker < 100) {
                df = FILESIZE_FORMAT_1;
            } else {
                df = FILESIZE_FORMAT_0;
            }

            // build string
            returnSize = df.format((float) ((float) fileSize / (float) divider)) + appendText;
        }

        return returnSize;
    }

    /**
     * Format the duration passed as ?h ?m format
     *
     * @param duration duration in seconds
     * @return
     */
    public static String formatRuntime(final int runtime) {
        if (runtime < 0) {
            return null;
        }
        int fixed = runtime / 1000;
        StringBuilder returnString = new StringBuilder();

        int nbHours = fixed / 3600;
        if (nbHours != 0) {
            returnString.append(nbHours).append("h");
        }

        int nbMinutes = (fixed - (nbHours * 3600)) / 60;
        if (nbMinutes != 0) {
            if (nbHours != 0) {
                returnString.append(" ");
            }
            returnString.append(nbMinutes).append("m");
        }
        
        return returnString.toString();
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
    public static int extractYearAsInt(String date) {
        if (StringUtils.isBlank(date))  {
            return -1;
        }
        if (StringUtils.isNumeric(date) && (date.length()==4)) {
            try {
                return Integer.parseInt(date);
            } catch (Exception ignore) {}
        }

        int year = -1;
        Matcher m = YEAR_PATTERN.matcher(date);
        if (m.find()) {
            year = Integer.valueOf(m.group(1)).intValue();
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
            } else {
                return Math.round(rating * 1f);
            }
        } else {
            return -1;
        }
    }
    
    /**
     * Checks if all media files have been watched.
     * 
     * @param videoData
     * @param apiCall
     * @return
     */
    public static boolean allMediaFilesWatched(VideoData videoData, boolean apiCall) {
        boolean onlyExtras = true;
        for (MediaFile stored : videoData.getMediaFiles()) {
            if (stored.isExtra()) {
                continue;
            }
            onlyExtras = false;
            if (apiCall) {
                if (!stored.isWatchedApi()) {
                    return false;
                }
            } else if (!stored.isWatchedFile()) {
                return false;
            }
        }

        if (onlyExtras) {
            return false;
        }
        return true;
    }
    
    /**
     * Remove alternate name from role
     * 
     * @param role
     * @return
     */
    public static String fixActorRole(final String role) {
        if (role == null) {
            return null;
        }
        String fixed = role;
        
        // (as = alternate name)
        int idx = StringUtils.indexOfIgnoreCase(fixed, "(as ");
        if (idx > 0) {
            fixed = fixed.substring(0, idx);
        }
        
        // double characters
        idx = StringUtils.indexOf(fixed, "/");
        if (idx > 0) {
            List<String> characters = StringTools.splitList(fixed, "/");
            fixed = StringUtils.join(characters.toArray(), " / ");
        }
        
        return fixed;
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
        } else {
            return mpaaCertification.trim();
        }
    }
    
    public static String cleanIdentifier(final String identifier) {
        String result = identifier;
        if (IDENT_TRANSLITERATE) {
            result = TRANSLITERATOR.transliterate(result); 
        }
        if (IDENT_CLEAN) {
            // format ß to ss
            result = result.replaceAll("ß", "ss");
            // remove all accents from letters
            result = StringUtils.stripAccents(result);
            // capitalize first letter
            result = WordUtils.capitalize(result, CLEAN_DELIMITERS);
            // remove punctuation and symbols
            result = result.replaceAll("[\\p{Po}|\\p{S}]", "");
            // just leave characters and digits
            result = CLEAN_STRING_PATTERN.matcher(result).replaceAll(" ").trim();
            // remove double whitespaces
            result = result.replaceAll("^ +| +$|( ){2,}", "$1");
        }
        return result;
    }

    /**
     * Set the sort title.
     * 
     * @param metadata the scanned metadata
     * @param prefixes a list with prefixed to strip
     */
    public static void setSortTitle(AbstractMetadata metadata, List<String> prefixes) {
        String sortTitle;
        if (StringUtils.isBlank(metadata.getTitleSort())) {
            sortTitle = StringUtils.stripAccents(metadata.getTitle());

            // strip prefix
            for (String prefix : prefixes) {
                String check = prefix.trim()+" ";
                if (StringUtils.startsWithIgnoreCase(sortTitle, check)) {
                    sortTitle = sortTitle.substring(check.length());
                    break;
                }
            }
        } else {
            sortTitle = StringUtils.stripAccents(metadata.getTitleSort());
        }

        // first char must be a letter or digit
        int idx = 0;
        while (idx < sortTitle.length() && !Character.isLetterOrDigit(sortTitle.charAt(idx))) {
            idx++;
        }
        
        // replace all non-standard characters in the title sort
        sortTitle = MetadataTools.stringMapReplacement(sortTitle.substring(idx));
        metadata.setTitleSort(sortTitle);
    }
    
    public static String getExternalSubtitleFormat(String extension) {
        if ("srt".equalsIgnoreCase(extension)) return "SubRip";
        if ("ssa".equalsIgnoreCase(extension)) return "SubStation Alpha";
        if ("ass".equalsIgnoreCase(extension)) return "Advanced SubStation Alpha";
        if ("pgs".equalsIgnoreCase(extension)) return "Presentation Grapic Stream";
        if ("sup".equalsIgnoreCase(extension)) return "Presentation Grapic Stream";
        if ("smi".equalsIgnoreCase(extension)) return "Synchronized Accessible Media Interchange";
        if ("sami".equalsIgnoreCase(extension)) return "Synchronized Accessible Media Interchange";
        return extension.toUpperCase();
    }
}