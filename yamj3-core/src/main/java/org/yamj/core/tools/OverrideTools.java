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

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.OverrideFlag;
import org.yamj.core.service.metadata.online.OnlineScannerService;

/**
 * Holds some override tools.
 */
public final class OverrideTools {

    private static final Logger LOG = LoggerFactory.getLogger(OverrideTools.class);
    // check skip if not in priority list
    private static final boolean SKIP_NOT_IN_LIST = PropertyTools.getBooleanProperty("priority.checks.skipNotInList", Boolean.FALSE);
    // handling for set default plugins
    private static final String TYPE_PLUGIN_MOVIE = "plugin_movie";
    private static final String TYPE_PLUGIN_SERIES = "plugin_series";
    private static final String TYPE_PLUGIN_PERSON = "plugin_person";
    private static final String TYPE_ALTERNATE_MOVIE = "alternate_movie";
    private static final String TYPE_ALTERNATE_SERIES = "alternate_series";
    private static final String TYPE_ALTERNATE_PERSON = "alternate_person";
    private static final String PLUGIN_MOVIE = OnlineScannerService.MOVIE_SCANNER;
    private static final String PLUGIN_MOVIE_ALT = OnlineScannerService.MOVIE_SCANNER_ALT;
    private static final String PLUGIN_SERIES = OnlineScannerService.SERIES_SCANNER;
    private static final String PLUGIN_SERIES_ALT = OnlineScannerService.SERIES_SCANNER_ALT;
    private static final String PLUGIN_PERSON = OnlineScannerService.PERSON_SCANNER;
    private static final String PLUGIN_PERSON_ALT = OnlineScannerService.PERSON_SCANNER_ALT;
    private static final Map<OverrideFlag, List<String>> VIDEODATA_PRIORITIES = new EnumMap<>(OverrideFlag.class);
    private static final Map<OverrideFlag, List<String>> SEASON_PRIORITIES = new EnumMap<>(OverrideFlag.class);
    private static final Map<OverrideFlag, List<String>> SERIES_PRIORITIES = new EnumMap<>(OverrideFlag.class);
    private static final Map<OverrideFlag, List<String>> PERSON_PRIORITIES = new EnumMap<>(OverrideFlag.class);
    private static final String DEFAULT_PLUGIN_MOVIE_SERIES = "api,nfo,plugin_movie,plugin_series,alternate_movie,alternate_series";
    private static final String DEFAULT_PLUGIN_SERIES = "api,nfo,plugin_series,alternate_series";
    private static final String DEFAULT_PLUGIN_PERSON = "api,nfo,plugin_person,alternate_person";
    private static final String APPEND_FILENAME = ",filename";
    
    static {
        String sources;

        // countries
        sources = PropertyTools.getProperty("priority.videodata.countries", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.COUNTRIES, sources);
        sources = PropertyTools.getProperty("priority.series.countries", DEFAULT_PLUGIN_SERIES);
        putSeriesPriorities(OverrideFlag.COUNTRIES, sources);
        // genres
        sources = PropertyTools.getProperty("priority.videodata.genres", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.GENRES, sources);
        sources = PropertyTools.getProperty("priority.series.genres", DEFAULT_PLUGIN_SERIES);
        putSeriesPriorities(OverrideFlag.GENRES, sources);
        // studios
        sources = PropertyTools.getProperty("priority.videodata.studios", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.STUDIOS, sources);
        sources = PropertyTools.getProperty("priority.series.studios", DEFAULT_PLUGIN_SERIES);
        putSeriesPriorities(OverrideFlag.STUDIOS, sources);
        // original title
        sources = PropertyTools.getProperty("priority.videodata.originaltitle", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.ORIGINALTITLE, sources);
        sources = PropertyTools.getProperty("priority.series.originaltitle", DEFAULT_PLUGIN_SERIES);
        putSeriesPriorities(OverrideFlag.ORIGINALTITLE, sources);
        sources = PropertyTools.getProperty("priority.season.originaltitle", DEFAULT_PLUGIN_SERIES);
        putSeasonPriorities(OverrideFlag.ORIGINALTITLE, sources);
        // outline
        sources = PropertyTools.getProperty("priority.videodata.outline", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.OUTLINE, sources);
        sources = PropertyTools.getProperty("priority.series.outline", DEFAULT_PLUGIN_SERIES);
        putSeriesPriorities(OverrideFlag.OUTLINE, sources);
        sources = PropertyTools.getProperty("priority.season.outline", DEFAULT_PLUGIN_SERIES);
        putSeasonPriorities(OverrideFlag.OUTLINE, sources);
        // plot
        sources = PropertyTools.getProperty("priority.videodata.plot", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.PLOT, sources);
        sources = PropertyTools.getProperty("priority.series.plot", DEFAULT_PLUGIN_SERIES);
        putSeriesPriorities(OverrideFlag.PLOT, sources);
        sources = PropertyTools.getProperty("priority.season.plot", DEFAULT_PLUGIN_SERIES);
        putSeasonPriorities(OverrideFlag.PLOT, sources);
        // quote
        sources = PropertyTools.getProperty("priority.videodata.quote", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.QUOTE, sources);
        // releasedate
        sources = PropertyTools.getProperty("priority.videodata.releasedate", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.RELEASEDATE, sources);
        // tagline
        sources = PropertyTools.getProperty("priority.videodata.tagline", DEFAULT_PLUGIN_MOVIE_SERIES);
        putVideodataPriorities(OverrideFlag.TAGLINE, sources);
        // title
        sources = PropertyTools.getProperty("priority.videodata.title", DEFAULT_PLUGIN_MOVIE_SERIES + APPEND_FILENAME);
        putVideodataPriorities(OverrideFlag.TITLE, sources);
        sources = PropertyTools.getProperty("priority.series.title", DEFAULT_PLUGIN_SERIES + APPEND_FILENAME);
        putSeriesPriorities(OverrideFlag.TITLE, sources);
        sources = PropertyTools.getProperty("priority.season.title", DEFAULT_PLUGIN_SERIES + APPEND_FILENAME);
        putSeasonPriorities(OverrideFlag.TITLE, sources);
        // year
        sources = PropertyTools.getProperty("priority.videodata.year", DEFAULT_PLUGIN_MOVIE_SERIES + APPEND_FILENAME);
        putVideodataPriorities(OverrideFlag.YEAR, sources);
        sources = PropertyTools.getProperty("priority.series.year", DEFAULT_PLUGIN_SERIES + APPEND_FILENAME);
        putSeriesPriorities(OverrideFlag.YEAR, sources);
        sources = PropertyTools.getProperty("priority.season.year", DEFAULT_PLUGIN_SERIES + APPEND_FILENAME);
        putSeasonPriorities(OverrideFlag.YEAR, sources);

        // person priorities
        sources = PropertyTools.getProperty("priority.person.name", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.NAME, sources);
        sources = PropertyTools.getProperty("priority.person.firstname", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.FIRSTNAME, sources);
        sources = PropertyTools.getProperty("priority.person.lastname", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.LASTNAME, sources);
        sources = PropertyTools.getProperty("priority.person.birtday", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.BIRTHDAY, sources);
        sources = PropertyTools.getProperty("priority.person.birtplace", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.BIRTHPLACE, sources);
        sources = PropertyTools.getProperty("priority.person.birthname", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.BIRTHNAME, sources);
        sources = PropertyTools.getProperty("priority.person.deathday", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.DEATHDAY, sources);
        sources = PropertyTools.getProperty("priority.person.deathplace", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.DEATHDAY, sources);
        sources = PropertyTools.getProperty("priority.person.biography", DEFAULT_PLUGIN_PERSON);
        putPersonPriorities(OverrideFlag.BIOGRAPHY, sources);
    }

    private OverrideTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Put video data priorities into map.
     *
     * @param overrideFlag
     * @param sources
     */
    private static void putVideodataPriorities(OverrideFlag overrideFlag, String sources) {
        List<String> priorities = resolvePriorities(sources);
        LOG.debug(overrideFlag.name() + " (VideoData) priorities " + priorities.toString());
        VIDEODATA_PRIORITIES.put(overrideFlag, priorities);
    }

    /**
     * Put season priorities into map.
     *
     * @param overrideFlag
     * @param sources
     */
    private static void putSeasonPriorities(OverrideFlag overrideFlag, String sources) {
        List<String> priorities = resolvePriorities(sources);
        LOG.debug(overrideFlag.name() + " (Season) priorities " + priorities.toString());
        SEASON_PRIORITIES.put(overrideFlag, priorities);
    }

    /**
     * Put series priorities into map.
     *
     * @param overrideFlag
     * @param sources
     */
    private static void putSeriesPriorities(OverrideFlag overrideFlag, String sources) {
        List<String> priorities = resolvePriorities(sources);
        LOG.debug(overrideFlag.name() + " (Series) priorities " + priorities.toString());
        SERIES_PRIORITIES.put(overrideFlag, priorities);
    }

    /**
     * Put person priorities into map.
     *
     * @param overrideFlag
     * @param sources
     */
    private static void putPersonPriorities(OverrideFlag overrideFlag, String sources) {
        List<String> priorities = resolvePriorities(sources);
        LOG.debug(overrideFlag.name() + " (Person) priorities " + priorities.toString());
        PERSON_PRIORITIES.put(overrideFlag, priorities);
    }

    private static List<String> resolvePriorities(String sources) {
        List<String> priorities;
        if (StringUtils.isEmpty(sources)) {
            priorities = Collections.emptyList();
        } else {
            String newSources = sources.toLowerCase();
            if (newSources.contains(TYPE_PLUGIN_MOVIE) && !newSources.contains(PLUGIN_MOVIE)) {
                // replace pattern with video plugin
                newSources = newSources.replace(TYPE_PLUGIN_MOVIE, PLUGIN_MOVIE);
            }
            if (newSources.contains(TYPE_PLUGIN_SERIES) && !newSources.contains(PLUGIN_SERIES)) {
                // replace pattern with series plugin
                newSources = newSources.replace(TYPE_PLUGIN_SERIES, PLUGIN_SERIES);
            }
            if (newSources.contains(TYPE_PLUGIN_PERSON) && !newSources.contains(PLUGIN_PERSON)) {
                // replace pattern with person plugin
                newSources = newSources.replace(TYPE_PLUGIN_PERSON, PLUGIN_PERSON);
            }

            if (newSources.contains(TYPE_ALTERNATE_MOVIE) && (StringUtils.isNotBlank(PLUGIN_MOVIE_ALT) && !newSources.contains(PLUGIN_MOVIE_ALT))) {
                // replace pattern with alternate video plugin
                newSources = newSources.replace(TYPE_ALTERNATE_MOVIE, PLUGIN_MOVIE_ALT);
            }

            if (newSources.contains(TYPE_ALTERNATE_SERIES) && (StringUtils.isNotBlank(PLUGIN_SERIES_ALT) && !newSources.contains(PLUGIN_SERIES_ALT))) {
                // replace pattern with alternate series plugin
                newSources = newSources.replace(TYPE_ALTERNATE_SERIES, PLUGIN_SERIES_ALT);
            }
            if (newSources.contains(TYPE_ALTERNATE_PERSON) && !newSources.contains(PLUGIN_PERSON_ALT)) {
                // replace pattern with alternate person plugin
                newSources = newSources.replace(TYPE_ALTERNATE_PERSON, PLUGIN_PERSON_ALT);
            }

            priorities = new ArrayList<>(Arrays.asList(newSources.split(",")));
            priorities.remove(TYPE_PLUGIN_MOVIE);
            priorities.remove(TYPE_PLUGIN_SERIES);
            priorities.remove(TYPE_PLUGIN_PERSON);
            priorities.remove(TYPE_ALTERNATE_MOVIE);
            priorities.remove(TYPE_ALTERNATE_SERIES);
            priorities.remove(TYPE_ALTERNATE_PERSON);
        }
        return priorities;
    }

    private static boolean skipCheck(IScannable scannable, OverrideFlag overrideFlag, String source) {
        if (SKIP_NOT_IN_LIST) {

            int index = -1;
            try {
                if (scannable instanceof VideoData) {
                    index = VIDEODATA_PRIORITIES.get(overrideFlag).indexOf(source.toLowerCase());
                } else if (scannable instanceof Season) {
                    index = SEASON_PRIORITIES.get(overrideFlag).indexOf(source.toLowerCase());
                } else if (scannable instanceof Series) {
                    index = SERIES_PRIORITIES.get(overrideFlag).indexOf(source.toLowerCase());
                } else if (scannable instanceof Person) {
                    index = PERSON_PRIORITIES.get(overrideFlag).indexOf(source.toLowerCase());
                }
            } catch (Exception ignore) {
                // ignore this error
            }

            // index < 0 means: not in list, so skip the check
            return (index < 0);
        }

        // no skip
        return Boolean.FALSE;
    }

    /**
     * Check the priority of a property to set.
     *
     * @param property the property to test
     * @param actualSource the actual source
     * @param newSource the new source
     * @param metadata the metadata object
     * @return true, if new source has higher property than actual source, else
     * false
     */
    private static boolean hasHigherPriority(final OverrideFlag overrideFlag, final String actualSource, final String newSource, final IScannable scannable) {
        // check sources
        if (StringUtils.isEmpty(newSource)) {
            // new source is not valid
            // -> actual source has higher priority
            return Boolean.FALSE;
        } else if (StringUtils.isEmpty(actualSource)) {
            // actual source is not valid
            // -> new source has higher priority
            return Boolean.TRUE;
        } else if (actualSource.equalsIgnoreCase(newSource)) {
            // same source may override itself
            return Boolean.TRUE;
        }

        // both sources are valid so get priorities
        List<String> priorities;
        if (scannable instanceof VideoData) {
            priorities = VIDEODATA_PRIORITIES.get(overrideFlag);
        } else if (scannable instanceof Season) {
            priorities = SEASON_PRIORITIES.get(overrideFlag);
        } else if (scannable instanceof Series) {
            priorities = SERIES_PRIORITIES.get(overrideFlag);
        } else if (scannable instanceof Person) {
            priorities = PERSON_PRIORITIES.get(overrideFlag);
        } else {
            priorities = Collections.emptyList();
        }

        // get and check new priority
        int newPrio = priorities.indexOf(newSource.toLowerCase());
        if (newPrio == -1) {
            // priority for new source not found
            // -> actual source has higher priority
            return Boolean.FALSE;
        }

        // check actual priority
        int actualPrio = priorities.indexOf(actualSource.toLowerCase());
        if ((actualPrio == -1) || (newPrio <= actualPrio) || scannable.isSkippedScan(actualSource)) {
            // -> new source has higher priority
            return Boolean.TRUE;
        }

        // -> actual source has higher priority
        return Boolean.FALSE;
    }

    private static boolean checkOverwrite(IScannable scannable, OverrideFlag overrideFlag, String source) {
        String actualSource = scannable.getOverrideSource(overrideFlag);
        return OverrideTools.hasHigherPriority(overrideFlag, actualSource, source, scannable);
    }

    public static boolean checkOneOverwrite(AbstractMetadata metadata, String source, OverrideFlag... overrideFlags) {
        for (OverrideFlag overrideFlag : overrideFlags) {
            boolean check;
            switch (overrideFlag) {
                case OUTLINE:
                    check = checkOverwriteOutline(metadata, source);
                    break;
                case PLOT:
                    check = checkOverwritePlot(metadata, source);
                    break;
                case TITLE:
                    check = checkOverwriteTitle(metadata, source);
                    break;
                case YEAR:
                    check = checkOverwriteYear(metadata, source);
                    break;
                default:
                    check = checkOverwrite(metadata, overrideFlag, source);
                    break;

                // until now these checks are enough
            }
            if (check) return true;
        }
        return false;
    }

    public static boolean checkOverwriteGenres(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.GENRES, source)) return Boolean.FALSE;
        return checkOverwrite(metadata, OverrideFlag.GENRES, source);
    }

    public static boolean checkOverwriteStudios(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.STUDIOS, source)) return Boolean.FALSE;
        return checkOverwrite(metadata, OverrideFlag.STUDIOS, source);
    }

    public static boolean checkOverwriteCountries(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.COUNTRIES, source)) return Boolean.FALSE;
        return checkOverwrite(metadata, OverrideFlag.COUNTRIES, source);
    }

    public static boolean checkOverwriteOriginalTitle(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.ORIGINALTITLE, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(metadata.getTitleOriginal())) return Boolean.TRUE;
        return checkOverwrite(metadata, OverrideFlag.ORIGINALTITLE, source);
    }

    public static boolean checkOverwriteOutline(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.OUTLINE, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(metadata.getOutline())) return Boolean.TRUE;
        return checkOverwrite(metadata, OverrideFlag.OUTLINE, source);
    }

    public static boolean checkOverwritePlot(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.PLOT, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(metadata.getPlot())) return Boolean.TRUE;
        return checkOverwrite(metadata, OverrideFlag.PLOT, source);
    }

    public static boolean checkOverwriteQuote(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.QUOTE, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(videoData.getQuote())) return Boolean.TRUE;
        return checkOverwrite(videoData, OverrideFlag.QUOTE, source);
    }

    public static boolean checkOverwriteReleaseDate(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.RELEASEDATE, source)) return Boolean.FALSE;
        if (videoData.getReleaseDate() == null) return Boolean.TRUE;
        return checkOverwrite(videoData, OverrideFlag.RELEASEDATE, source);
    }

    public static boolean checkOverwriteTagline(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.TAGLINE, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(videoData.getTagline())) return Boolean.TRUE;
        return checkOverwrite(videoData, OverrideFlag.TAGLINE, source);
    }

    public static boolean checkOverwriteTitle(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.TITLE, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(metadata.getTitle())) return Boolean.TRUE;
        return checkOverwrite(metadata, OverrideFlag.TITLE, source);
    }

    public static boolean checkOverwriteYear(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.YEAR, source)) return Boolean.FALSE;
        if (metadata.getYear() <= 0) return Boolean.TRUE;
        return checkOverwrite(metadata, OverrideFlag.YEAR, source);
    }

    // PERSON OVERRIDE
    public static boolean checkOverwritePersonNames(Person person, String source) {
        if (checkOverwriteName(person, source)) return true;
        if (checkOverwriteFirstName(person, source)) return true;
        if (checkOverwriteLastName(person, source)) return true;
        return false;
    }
    
    public static boolean checkOverwriteName(Person person, String source) {
        if (skipCheck(person, OverrideFlag.NAME, source)) return Boolean.FALSE;
        if (person.getName() == null) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.NAME, source);
    }

    public static boolean checkOverwriteFirstName(Person person, String source) {
        if (skipCheck(person, OverrideFlag.FIRSTNAME, source)) return Boolean.FALSE;
        if (StringUtils.isBlank(person.getFirstName())) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.FIRSTNAME, source);
    }

    public static boolean checkOverwriteLastName(Person person, String source) {
        if (skipCheck(person, OverrideFlag.LASTNAME, source)) return Boolean.FALSE;
        if (StringUtils.isBlank(person.getLastName())) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.LASTNAME, source);
    }

    public static boolean checkOverwriteBirthDay(Person person, String source) {
        if (skipCheck(person, OverrideFlag.BIRTHDAY, source)) return Boolean.FALSE;
        if (person.getBirthDay() == null) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.BIRTHDAY, source);
    }

    public static boolean checkOverwriteBirthPlace(Person person, String source) {
        if (skipCheck(person, OverrideFlag.BIRTHPLACE, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(person.getBirthPlace())) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.BIRTHPLACE, source);
    }

    public static boolean checkOverwriteBirthName(Person person, String source) {
        if (skipCheck(person, OverrideFlag.BIRTHNAME, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(person.getBirthName())) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.BIRTHNAME, source);
    }

    public static boolean checkOverwriteDeathDay(Person person, String source) {
        if (skipCheck(person, OverrideFlag.DEATHDAY, source)) return Boolean.FALSE;
        if (person.getDeathDay() == null) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.DEATHDAY, source);
    }

    public static boolean checkOverwriteDeathPlace(Person person, String source) {
        if (skipCheck(person, OverrideFlag.DEATHPLACE, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(person.getDeathPlace())) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.DEATHPLACE, source);
    }

    public static boolean checkOverwriteBiography(Person person, String source) {
        if (skipCheck(person, OverrideFlag.BIOGRAPHY, source)) return Boolean.FALSE;
        if (StringUtils.isEmpty(person.getBiography())) return Boolean.TRUE;
        return checkOverwrite(person, OverrideFlag.BIOGRAPHY, source);
    }
}
