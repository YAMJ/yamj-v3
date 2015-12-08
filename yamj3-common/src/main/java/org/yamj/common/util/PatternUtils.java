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
package org.yamj.common.util;

import java.util.regex.Pattern;

public final class PatternUtils {

    public static final String TOKEN_DELIMITERS_STRING = ".[]()";
    public static final String NOTOKEN_DELIMITERS_STRING = " _-,";
    public static final String WORD_DELIMITERS_STRING = NOTOKEN_DELIMITERS_STRING + TOKEN_DELIMITERS_STRING;
    public static final Pattern TOKEN_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(TOKEN_DELIMITERS_STRING) + "]|$|^)");
    public static final Pattern NOTOKEN_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(NOTOKEN_DELIMITERS_STRING) + "])");
    public static final Pattern WORD_DELIMITERS_MATCH_PATTERN = patt("(?:[" + Pattern.quote(WORD_DELIMITERS_STRING) + "]|$|^)");
    public static final String SPACE_SLASH_SPACE = " / ";

    private PatternUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * @param regex
     * @return Exact pattern
     */
    public static Pattern patt(String regex) {
        return Pattern.compile(regex);
    }

    /**
     * @param regex
     * @return Case insensitive pattern
     */
    public static Pattern ipatt(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param regex
     * @return Case insensitive pattern matched somewhere in square brackets
     */
    public static Pattern pattInSBrackets(String regex) {
        return ipatt("\\[([^\\[\\]]*" + regex + "[^\\[]*)\\]");
    }

    /**
     * @param regex
     * @return Case insensitive pattern with word delimiters around
     */
    public static Pattern iwpatt(String regex) {
        return Pattern.compile("(?<=" + WORD_DELIMITERS_MATCH_PATTERN
                + ")(?:" + regex + ")(?="
                + WORD_DELIMITERS_MATCH_PATTERN + ")", Pattern.CASE_INSENSITIVE);
    }

    /**
     * @param regex
     * @return Case sensitive pattern with word delimiters around
     */
    public static Pattern wpatt(String regex) {
        return Pattern.compile("(?<=" + WORD_DELIMITERS_MATCH_PATTERN
                + ")(?:" + regex + ")(?="
                + WORD_DELIMITERS_MATCH_PATTERN + ")");
    }

    /**
     * @param regex
     * @return Case sensitive pattern with token delimiters around
     */
    public static Pattern tpatt(String regex) {
        return Pattern.compile(TOKEN_DELIMITERS_MATCH_PATTERN + "(?:" + NOTOKEN_DELIMITERS_MATCH_PATTERN + "*)" + "(?:" + regex + ")" + "(?:"
                + NOTOKEN_DELIMITERS_MATCH_PATTERN + "*)" + TOKEN_DELIMITERS_MATCH_PATTERN);
    }
}
