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
import java.io.File;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.yamj.common.tools.PropertyTools;

public final class StringTools {

    private static final Pattern CLEAN_STRING_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-\\(\\)]");
    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * KB * KB;
    private static final DecimalFormat FILESIZE_FORMAT_0;
    private static final DecimalFormat FILESIZE_FORMAT_1;
    private static final DecimalFormat FILESIZE_FORMAT_2;
    private static final Map<Character, Character> CHAR_REPLACEMENT_MAP = new HashMap<Character, Character>();
    private static final String MPPA_RATED = "Rated";
    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT_LONG;
    private static final Transliterator TRANSLITERATOR;

    private StringTools() {
        throw new UnsupportedOperationException("Utility class");
    }

    static {
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
    public static String stringMapReplacement(String stringToReplace) {
        Character tempC;
        StringBuilder sb = new StringBuilder();

        for (Character c : stringToReplace.toCharArray()) {
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
     * Append a string to the end of a path ensuring that there are the correct number of File.separators
     *
     * @param basePath
     * @param additionalPath
     * @return
     */
    public static String appendToPath(String basePath, String additionalPath) {
        StringBuilder newPath = new StringBuilder(basePath.trim());
        newPath.append((basePath.trim().endsWith(File.separator) ? "" : File.separator));
        newPath.append(additionalPath.trim());
        return newPath.toString();
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
    public static String formatDuration(final int duration) {
        if (duration < 0) {
            return null;
        }
        int fixed = duration / 1000;
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

    /**
     * Check that the passed string is not longer than the required length and trim it if necessary.
     *
     * @param sourceString
     * @param requiredLength
     * @return
     */
    public static String trimToLength(String sourceString, int requiredLength) {
        return trimToLength(sourceString, requiredLength, Boolean.TRUE, "...");
    }

    /**
     * Check that the passed string is not longer than the required length and trim it if necessary
     *
     * @param sourceString The string to check
     * @param requiredLength The required length (Maximum)
     * @param trimToWord Trim the source string to the last space to avoid partial words
     * @param endingSuffix The ending to append if the string is longer than the required length
     * @return
     */
    public static String trimToLength(String sourceString, int requiredLength, boolean trimToWord, String endingSuffix) {
        String changedString = sourceString.trim();

        if (StringUtils.isNotBlank(changedString)) {
            if (changedString.length() <= requiredLength) {
                // No need to do anything
                return changedString;
            } else {
                if (trimToWord) {
                    BreakIterator bi = BreakIterator.getWordInstance();
                    bi.setText(changedString);
                    int biLength = bi.preceding(requiredLength - endingSuffix.length());
                    return changedString.substring(0, biLength).trim() + endingSuffix;
                } else {
                    // We know that the source string is longer that the required length, so trim it to size
                    return changedString.substring(0, requiredLength - endingSuffix.length()).trim() + endingSuffix;
                }
            }
        }

        return changedString;
    }

    /**
     * Cast a generic list to a specfic class See:
     * http://stackoverflow.com/questions/367626/how-do-i-fix-the-expression-of-type-list-needs-unchecked-conversion
     *
     * @param <T>
     * @param objClass
     * @param c
     * @return
     */
    public static <T> List<T> castList(Class<? extends T> objClass, Collection<?> c) {
        List<T> r = new ArrayList<T>(c.size());
        for (Object o : c) {
            r.add(objClass.cast(o));
        }
        return r;
    }

    /**
     * Split a list using a regex and return a list of trimmed strings
     *
     * @param stringToSplit
     * @param regexDelim
     * @return
     */
    public static List<String> splitList(String stringToSplit, String regexDelim) {
        List<String> finalValues = new ArrayList<String>();

        for (String output : stringToSplit.split(regexDelim)) {
            finalValues.add(output.trim());
        }

        return finalValues;
    }

    public static Collection<String> tokenize(String sourceString, String delimiter) {
        StringTokenizer st = new StringTokenizer(sourceString, delimiter);
        Collection<String> keywords = new HashSet<String>();
        while (st.hasMoreTokens()) {
            keywords.add(st.nextToken());
        }
        return keywords;
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

    public static String cleanString(final String input) {
        String output = input.replaceAll("ß", "ss");
        output = StringUtils.stripAccents(output);
        output = CLEAN_STRING_PATTERN.matcher(output).replaceAll(" ").trim();
        output = output.replaceAll("^ +| +$|( ){2,}", "$1");
        return output;
    }

    public static String transliterate(final String input) {
        return TRANSLITERATOR.transliterate(input); 
    }
}
