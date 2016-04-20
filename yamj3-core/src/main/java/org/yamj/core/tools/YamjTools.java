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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.AbstractMetadata;
import org.yamj.core.database.model.MediaFile;
import org.yamj.core.database.model.VideoData;

public final class YamjTools {

    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * KB * KB;
    private static final Map<Character, Character> CHAR_REPLACEMENT_MAP = new HashMap<>();

    private static final DecimalFormat FILESIZE_FORMAT_0;
    private static final DecimalFormat FILESIZE_FORMAT_1;
    private static final DecimalFormat FILESIZE_FORMAT_2;

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

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        // Use the "." as a decimal format separator, ignoring localisation
        symbols.setDecimalSeparator('.');
        FILESIZE_FORMAT_0 = new DecimalFormat("0", symbols);
        FILESIZE_FORMAT_1 = new DecimalFormat("0.#", symbols);
        FILESIZE_FORMAT_2 = new DecimalFormat("0.##", symbols);
    }

    private YamjTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
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
        sortTitle = stringMapReplacement(sortTitle.substring(idx));
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
    
    public static <T extends Object> T getEqualObject(Collection<T> coll, T object) {
        if (CollectionUtils.isEmpty(coll)) {
            return null;
        }
        for (T col : coll) {
            if (col.equals(object)) {
                return col;
            }
        }
        return null;
    }
    
    public static <T extends Object> List<List<T>> split(List<T> list, int targetSize) {
        List<List<T>> lists = new ArrayList<>();
        for (int i = 0; i < list.size(); i += targetSize) {
            lists.add(list.subList(i, Math.min(i + targetSize, list.size())));
        }
        return lists;
    }
}
