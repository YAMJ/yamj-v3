/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.common.tools.PropertyTools;
import org.yamj.core.database.model.AbstractMetadata;
import org.yamj.core.database.model.Season;
import org.yamj.core.database.model.Series;
import org.yamj.core.database.model.VideoData;
import org.yamj.core.database.model.type.OverrideFlag;
import org.yamj.core.service.plugin.PluginMetadataService;

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
    private static final String TYPE_ALTERNATE_MOVIE = "alternate_movie";
    private static final String TYPE_ALTERNATE_SERIES = "alternate_series";
    private static final String PLUGIN_MOVIE = PluginMetadataService.MOVIE_SCANNER;
    private static final String PLUGIN_MOVIE_ALT = PluginMetadataService.MOVIE_SCANNER_ALT;
    private static final String PLUGIN_SERIES = PluginMetadataService.SERIES_SCANNER;
    private static final String PLUGIN_SERIES_ALT = PluginMetadataService.SERIES_SCANNER_ALT;
    private static final Map<OverrideFlag, List<String>> VIDEODATA_PRIORITIES = new EnumMap<OverrideFlag, List<String>>(OverrideFlag.class);
    private static final Map<OverrideFlag, List<String>> SEASON_PRIORITIES = new EnumMap<OverrideFlag, List<String>>(OverrideFlag.class);
    private static final Map<OverrideFlag, List<String>> SERIES_PRIORITIES = new EnumMap<OverrideFlag, List<String>>(OverrideFlag.class);

    static {
        String sources;

        // country
        sources = PropertyTools.getProperty("priority.videodata.country", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.COUNTRY, sources);
        // genres
        sources = PropertyTools.getProperty("priority.videodata.genres", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.GENRES, sources);
        sources = PropertyTools.getProperty("priority.series.genres", "nfo,plugin_series,plugin_movie,alternate_series,alternate_movie");
        putSeriesPriorities(OverrideFlag.GENRES, sources);
        // original title
        sources = PropertyTools.getProperty("priority.videodata.originaltitle", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.ORIGINALTITLE, sources);
        sources = PropertyTools.getProperty("priority.season.originaltitle", "nfo,plugin_series,alternate_series");
        putSeasonPriorities(OverrideFlag.ORIGINALTITLE, sources);
        sources = PropertyTools.getProperty("priority.series.originaltitle", "nfo,plugin_series,alternate_series");
        putSeriesPriorities(OverrideFlag.ORIGINALTITLE, sources);
        // outline
        sources = PropertyTools.getProperty("priority.videodata.outline", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.OUTLINE, sources);
        sources = PropertyTools.getProperty("priority.season.outline", "nfo,plugin_series,alternate_series");
        putSeasonPriorities(OverrideFlag.OUTLINE, sources);
        sources = PropertyTools.getProperty("priority.series.outline", "nfo,plugin_series,alternate_series");
        putSeriesPriorities(OverrideFlag.OUTLINE, sources);
        // plot
        sources = PropertyTools.getProperty("priority.videodata.plot", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.PLOT, sources);
        sources = PropertyTools.getProperty("priority.season.plot", "nfo,plugin_series,alternate_series");
        putSeasonPriorities(OverrideFlag.PLOT, sources);
        sources = PropertyTools.getProperty("priority.series.plot", "nfo,plugin_series,alternate_series");
        putSeriesPriorities(OverrideFlag.PLOT, sources);
        // quote
        sources = PropertyTools.getProperty("priority.videodata.quote", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.QUOTE, sources);
        // releasedate
        sources = PropertyTools.getProperty("priority.videodata.releasedate", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.RELEASEDATE, sources);
        // tagline
        sources = PropertyTools.getProperty("priority.videodata.tagline", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.TAGLINE, sources);
        // title
        sources = PropertyTools.getProperty("priority.videodata.title", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series,filename");
        putVideodataPriorities(OverrideFlag.TITLE, sources);
        sources = PropertyTools.getProperty("priority.season.title", "nfo,plugin_series,alternate_series,filename");
        putSeasonPriorities(OverrideFlag.TITLE, sources);
        sources = PropertyTools.getProperty("priority.series.title", "nfo,plugin_series,alternate_series,filename");
        putSeriesPriorities(OverrideFlag.TITLE, sources);
        // year
        sources = PropertyTools.getProperty("priority.videodata.year", "nfo,plugin_movie,plugin_series,alternate_movie,alternate_series");
        putVideodataPriorities(OverrideFlag.YEAR, sources);
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

    private static List<String> resolvePriorities(String sources) {
        List<String> priorities;
        if (StringUtils.isBlank(sources)) {
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

            if (newSources.contains(TYPE_ALTERNATE_MOVIE) && (StringUtils.isNotBlank(PLUGIN_MOVIE_ALT) && !newSources.contains(PLUGIN_MOVIE_ALT))) {
                // replace pattern with alternate video plugin
                newSources = newSources.replace(TYPE_ALTERNATE_MOVIE, PLUGIN_MOVIE_ALT);
            }

            if (newSources.contains(TYPE_ALTERNATE_SERIES) && (StringUtils.isNotBlank(PLUGIN_SERIES_ALT) && !newSources.contains(PLUGIN_SERIES_ALT))) {
                // replace pattern with alternate series plugin
                newSources = newSources.replace(TYPE_ALTERNATE_SERIES, PLUGIN_SERIES_ALT);
            }

            priorities = new ArrayList<String>(Arrays.asList(newSources.split(",")));
            priorities.remove(TYPE_PLUGIN_MOVIE);
            priorities.remove(TYPE_PLUGIN_SERIES);
            priorities.remove(TYPE_ALTERNATE_MOVIE);
            priorities.remove(TYPE_ALTERNATE_SERIES);
        }
        return priorities;
    }

    private static boolean skipCheck(AbstractMetadata metadata, OverrideFlag overrideFlag, String source) {
        if (SKIP_NOT_IN_LIST) {

            int index = -1;
            try {
                if (metadata instanceof VideoData) {
                    index = VIDEODATA_PRIORITIES.get(overrideFlag).indexOf(source.toLowerCase());
                } else if (metadata instanceof Season) {
                    index = SEASON_PRIORITIES.get(overrideFlag).indexOf(source.toLowerCase());
                } else if (metadata instanceof Series) {
                    index = SERIES_PRIORITIES.get(overrideFlag).indexOf(source.toLowerCase());
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
     * @return true, if new source has higher property than actual source, else false
     */
    private static boolean hasHigherPriority(final OverrideFlag overrideFlag, final String actualSource, final String newSource, final AbstractMetadata metadata) {
        // check sources
        if (StringUtils.isBlank(newSource)) {
            // new source is not valid
            // -> actual source has higher priority
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(actualSource)) {
            // actual source is not valid
            // -> new source has higher priority
            return Boolean.TRUE;
        } else if (actualSource.equalsIgnoreCase(newSource)) {
            // same source may override itself
            return Boolean.TRUE;
        }

        // both sources are valid so get priorities
        List<String> priorities;
        if (metadata instanceof VideoData) {
            priorities = VIDEODATA_PRIORITIES.get(overrideFlag);
        } else if (metadata instanceof Season) {
            priorities = SEASON_PRIORITIES.get(overrideFlag);
        } else if (metadata instanceof Series) {
            priorities = SERIES_PRIORITIES.get(overrideFlag);
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
        if ((actualPrio == -1) || (newPrio <= actualPrio)) {
            // -> new source has higher priority
            return Boolean.TRUE;
        }

        // -> actual source has higher priority
        return Boolean.FALSE;
    }

    private static boolean checkOverwrite(AbstractMetadata metadata, OverrideFlag overrideFlag, String source) {
        String actualSource = metadata.getOverrideSource(overrideFlag);
        return OverrideTools.hasHigherPriority(overrideFlag, actualSource, source, metadata);
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
                default:
                    check = checkOverwrite(metadata, overrideFlag, source);
                    break;

                // TODO until now these checks are enough
            }
            if (check) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkOverwriteGenres(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.COUNTRY, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (CollectionUtils.isEmpty(videoData.getGenres())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(videoData, OverrideFlag.GENRES, source);
    }

    public static boolean checkOverwriteCountry(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.COUNTRY, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(videoData.getCountry())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(videoData, OverrideFlag.COUNTRY, source);
    }

    public static boolean checkOverwriteOriginalTitle(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.ORIGINALTITLE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(metadata.getTitleOriginal())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(metadata, OverrideFlag.ORIGINALTITLE, source);
    }

    public static boolean checkOverwriteOutline(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.OUTLINE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(metadata.getOutline())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(metadata, OverrideFlag.OUTLINE, source);
    }

    public static boolean checkOverwritePlot(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.PLOT, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(metadata.getPlot())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(metadata, OverrideFlag.PLOT, source);
    }

    public static boolean checkOverwriteQuote(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.QUOTE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(videoData.getQuote())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(videoData, OverrideFlag.QUOTE, source);
    }

    public static boolean checkOverwriteReleaseDate(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.RELEASEDATE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(videoData.getReleaseDate())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(videoData, OverrideFlag.RELEASEDATE, source);
    }

    public static boolean checkOverwriteTagline(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.TAGLINE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(videoData.getTagline())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(videoData, OverrideFlag.TAGLINE, source);
    }

    public static boolean checkOverwriteTitle(AbstractMetadata metadata, String source) {
        if (skipCheck(metadata, OverrideFlag.TITLE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringUtils.isBlank(metadata.getTitle())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(metadata, OverrideFlag.TITLE, source);
    }

    public static boolean checkOverwriteYear(VideoData videoData, String source) {
        if (skipCheck(videoData, OverrideFlag.YEAR, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (0 <= videoData.getPublicationYear()) {
            return Boolean.TRUE;
        }
        return checkOverwrite(videoData, OverrideFlag.YEAR, source);
    }
}
