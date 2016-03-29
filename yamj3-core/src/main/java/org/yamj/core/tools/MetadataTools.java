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
import org.pojava.datetime.*;
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
    private static final char[] CLEAN_DELIMITERS = new char[]{'.', ' ', '_', '-'};
    private static final Pattern DATE_COUNTRY = Pattern.compile("(.*)(\\s*?\\(\\w*\\))");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(?:.*?)(\\d{4})(?:.*?)");
    private static final Pattern LASTNAME_PATTERN = Pattern.compile("((?:(?:d[aeiu]|de la|mac|zu|v[ao]n(?: de[nr])?) *)?[^ ]+) *(.*)");
    private static final String FROM_WIKIPEDIA = "From Wikipedia, the free encyclopedia";
    private static final String WIKIPEDIA_DESCRIPTION_ABOVE = "Description above from the Wikipedia";

    private static final String MPPA_RATED = "Rated";
    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * KB * KB;
    private static final Map<Character, Character> CHAR_REPLACEMENT_MAP = new HashMap<>();

    private static final DecimalFormat FILESIZE_FORMAT_0;
    private static final DecimalFormat FILESIZE_FORMAT_1;
    private static final DecimalFormat FILESIZE_FORMAT_2;
    private static final boolean IDENT_TRANSLITERATE;
    private static final Transliterator TRANSLITERATOR;

    private static final String DATE_FORMAT = PropertyTools.getProperty("yamj3.date.format", "yyyy-MM-dd");
    private static final IDateTimeConfig DATETIME_CONFIG_DEFAULT;
    private static final IDateTimeConfig DATETIME_CONFIG_FALLBACK;
    
    private MetadataTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static {
        IDENT_TRANSLITERATE = PropertyTools.getBooleanProperty("yamj3.identifier.transliterate", false);

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

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        // Use the "." as a decimal format separator, ignoring localisation
        symbols.setDecimalSeparator('.');
        FILESIZE_FORMAT_0 = new DecimalFormat("0", symbols);
        FILESIZE_FORMAT_1 = new DecimalFormat("0.#", symbols);
        FILESIZE_FORMAT_2 = new DecimalFormat("0.##", symbols);

        // default date and time configuration
        DATETIME_CONFIG_DEFAULT = DateTimeConfig.getGlobalDefault();
        // fall-back configuration
        DateTimeConfigBuilder builder = DateTimeConfigBuilder.newInstance();
        builder.setDmyOrder(!DATETIME_CONFIG_DEFAULT.isDmyOrder());
        DATETIME_CONFIG_FALLBACK = DateTimeConfig.fromBuilder(builder);

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
        }
        return tempC.toString();
    }

    /**
     * Change all the characters in a string to the safe replacements
     *
     * @param input
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

    /**
     * Format the file size
     *
     * @param fileSize
     * @return
     */
    public static String formatFileSize(long fileSize) {
        String returnSize;
        if (fileSize < 0) {
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
            returnSize = df.format((float) fileSize / (float) divider) + appendText;
        }

        return returnSize;
    }

    /**
     * Format the duration passed as ?h ?m format
     *
     * @param runtime duration in seconds
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

    public static WatchedDTO getWatchedDTO(VideoData videoData) {
        WatchedDTO watched = new WatchedDTO();
        watched.watchedVideo(videoData.isWatchedNfo(), videoData.getWatchedNfoLastDate());
        watched.watchedVideo(videoData.isWatchedApi(), videoData.getWatchedApiLastDate());
        watched.watchedVideo(videoData.isWatchedTraktTv(), videoData.getWatchedTraktTvLastDate());
        
        for (MediaFile mediaFile : videoData.getMediaFiles()) {
            if (mediaFile.isExtra()) {
                continue;
            }
            watched.watchedMediaFile(mediaFile.isWatchedFile(), mediaFile.getWatchedFileLastDate());
            watched.watchedMediaApi(mediaFile.isWatchedApi(), mediaFile.getWatchedApiLastDate());
        }

        return watched;
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

    public static String cleanIdentifier(final String identifier) {
        String result = identifier;
        if (IDENT_TRANSLITERATE) {
            result = TRANSLITERATOR.transliterate(result);
        }
        
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
        result = result.replaceAll("( )+", " ").trim();
        
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
                String check = prefix.trim() + " ";
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
        if (extension == null) {
            return null;
        }
        
        String format;
        switch (extension.toLowerCase()) {
            case "ass":
                format = "Advanced SubStation Alpha";
                break;
            case "pgs":
            case "sup":
                format = "Presentation Grapic Stream";
                break;
            case "smi":
            case "sami":
                format = "Synchronized Accessible Media Interchange";
                break;
            case "srt":
                format = "SubRip";
                break;
            case "ssa":
                format = "SubStation Alpha";
                break;
            case "ssf":
                format = "Structured Subtitle Format";
                break;
            case "sub":
                format = "MicroDVD";
                break;
            case "usf":
                format = "Universal Subtitle Format";
                break;
            default:
                format = extension.toUpperCase();
                break;
        }
        return format;
    }

    public static PersonNameDTO splitFullName(String fullName) {
        PersonNameDTO dto = new PersonNameDTO(fullName);
        
        try {
            String[] result = StringUtils.split(fullName, ' ');
            if (result == null || result.length == 0) {
                // nothing to do
            } else if (result.length == 1) {
                dto.setFirstName(result[0]);
            } else if (result.length == 2) {
                dto.setFirstName(result[0]);
                dto.setLastName(result[1]);
            } else {
                Matcher m = LASTNAME_PATTERN.matcher(fullName);
                if (m.matches()) {
                    dto.setFirstName(m.group(1));
                    dto.setLastName(m.group(2));
                }
            }
        } catch (Exception ex) {
            LOG.trace("Error splitting full person name: " + fullName, ex);
        }
        
        return dto;
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
            List<String> characters = StringTools.splitList(newRole, "/");
            newRole = StringUtils.join(characters.toArray(), " / ");
        }
        
        newRole = fixScannedValue(newRole);
        newRole = newRole.replaceAll("( )+", " ").trim();
        
        return newRole;
    }
}
