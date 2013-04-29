/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.core.tools;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("subtitleTools")
public final class SubtitleTools implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(SubtitleTools.class);
    private static final String SPLIT_PATTERN = "\\||,|/";
    @Autowired
    private LanguageTools languageTools;
    private String subtitleDelimiter;
    private boolean subtitleUnique;
    private List<String> skippedSubtitles;

    @Override
    public void afterPropertiesSet() throws Exception {
        subtitleDelimiter = PropertyTools.getProperty("yamj3.subtitle.delimiter", Constants.SPACE_SLASH_SPACE);
        subtitleUnique = PropertyTools.getBooleanProperty("yamj3.subtitle.unique", Boolean.TRUE);

        // process skipped subtitles
        skippedSubtitles = new ArrayList<String>();
        List<String> types = Arrays.asList(PropertyTools.getProperty("yamj3.subtitle.skip", "").split(","));
        for (String type : types) {
            String determined = languageTools.determineLanguage(type.trim());
            if (StringUtils.isNotBlank(determined)) {
                skippedSubtitles.add(determined.toUpperCase());
            }
        }
    }

    /**
     * TODO
     *
     * Set subtitles in the movie. Note: overrides the actual subtitles in movie.
     *
     * @param movie
     * @param parsedSubtitles public static void setMovieSubtitles(VideoData movie, Collection<String> subtitles) { if
     * (!subtitles.isEmpty()) {
     *
     * // holds the subtitles for the movie String movieSubtitles = "";
     *
     * for (String subtitle : subtitles) { movieSubtitles = addMovieSubtitle(movieSubtitles, subtitle); }
     *
     * // set valid subtitles in movie; overwrites existing subtitles if (StringTools.isValidString(movieSubtitles)) {
     * movie.setSubtitles(movieSubtitles); } } }
     */
    /**
     * TODO Adds a subtitle to the subtitles in the movie.
     *
     * @param movie
     * @param subtitle public static void addMovieSubtitle(Movie movie, String subtitle) { String newSubtitles =
     * addMovieSubtitle(movie.getSubtitles(), subtitle); movie.setSubtitles(newSubtitles); }
     */
    /**
     * Adds a new subtitle to the actual list of subtitles.
     *
     * @param subtitles
     * @param newSubtitle public void addSubtitle(Collection subtitles, String newSubtitle) { // determine the language String
     * language = languageTools.determineLanguage(newSubtitle);
     *
     * if (StringUtils.isNotBlank(language) && !isSkippedSubtitle(language)) { if (subtitles.isEmpty()) { subtitles.add(e) } if
     * (StringTools.isNotValidString(actualSubtitles) || actualSubtitles.equalsIgnoreCase("NO") ) { // Overwrite existing sub titles
     * newMovieSubtitles = infoLanguage; } else if ("YES".equalsIgnoreCase(newSubtitle)) { // Nothing to change, cause there are
     * already valid subtitle languages present // TODO Inspect if UNKNOWN should be added add the end of the subtitles list } else
     * if ("YES".equalsIgnoreCase(actualSubtitles)) { // override with subtitle language newMovieSubtitles = infoLanguage; // TODO
     * Inspect if UNKNOWN should be added add the end of the subtitles list } else if (!subtitleUnique ||
     * !actualSubtitles.contains(infoLanguage)) { // Add subtitle to subtitles list newMovieSubtitles = actualSubtitles +
     * subtitleDelimiter + infoLanguage; } }
     *
     * return newMovieSubtitles; }
     */
    private boolean isSkippedSubtitle(String language) {
        if (skippedSubtitles.isEmpty()) {
            // not skipped if list is empty
            return false;
        }

        boolean skipped = skippedSubtitles.contains(language.toUpperCase());
        if (skipped) {
            LOG.debug("Skipping subtitle '{}'", language);
        }
        return skipped;
    }
}
