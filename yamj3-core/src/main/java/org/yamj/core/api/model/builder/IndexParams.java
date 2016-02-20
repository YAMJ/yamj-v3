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
package org.yamj.core.api.model.builder;

import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.yamj.common.type.MetaDataType;
import org.yamj.core.api.options.OptionsIndexVideo;
import org.yamj.core.database.model.type.ResolutionType;

/**
 * @author modmax
 */
public class IndexParams {

    private static final String YEAR = "year";
    private static final String GENRE = "genre";
    private static final String STUDIO = "studio";
    private static final String COUNTRY = "country";
    private static final String AWARD = "award";
    private static final String CERTIFICATION = "certification";
    private static final String VIDEOSOURCE = "videosource";
    private static final String RESOLUTION = "resolution";
    private static final String RATING = "rating";
    private static final String NEWEST = "newest";
    private static final String BOXSET = "boxset";

    private final OptionsIndexVideo options;
    private final Map<String, String> includes;
    private final Map<String, String> excludes;
    private final List<DataItem> dataItems;
    private final Map<String, Object> parameters = new HashMap<>();

    private String ratingSource;
    private int ratingValue;
    private String newestSource;
    private Date newestDate;
    
    public IndexParams(OptionsIndexVideo options) {
        this.options = options;
        this.includes = options.splitIncludes();
        this.excludes = options.splitExcludes();
        this.dataItems = options.splitDataItems();
    }

    public Set<MetaDataType> getMetaDataTypes() {
        return options.splitTypes();
    }

    public Long getId() {
        return options.getId();
    }

    public Boolean getWatched() {
        return options.getWatched();
    }

    public String getSearchString(boolean addWhere) {
        return options.getSearchString(addWhere);
    }

    public String getSortString() {
        return options.getSortString();
    }

    public List<DataItem> getDataItems() {
        return dataItems;
    }

    public boolean hasDataItem(DataItem dataItem) {
        return dataItems.contains(dataItem);
    }

    // year check
    public boolean includeYear() {
        return includes.containsKey(YEAR);
    }

    public boolean excludeYear() {
        return excludes.containsKey(YEAR);
    }

    public String getYear() {
        if (includeYear()) {
            return includes.get(YEAR);
        }
        return excludes.get(YEAR);
    }

    public int getYearStart() {
        return (options.getYearStart() == null) ? -1 : options.getYearStart().intValue();
    }

    public int getYearEnd() {
        if (options.getYearEnd() != null && options.getYearEnd().intValue() < getYearStart()) {
            return getYearStart();
        }
        return (options.getYearEnd() == null) ? -1 : options.getYearEnd().intValue();
    }

    // genre check
    public boolean includeGenre() {
        return includes.containsKey(GENRE);
    }

    public boolean excludeGenre() {
        return excludes.containsKey(GENRE);
    }

    public String getGenreName() {
        if (includeGenre()) {
            return includes.get(GENRE);
        }
        return excludes.get(GENRE);
    }

    // studio check
    public boolean includeStudio() {
        return includes.containsKey(STUDIO);
    }

    public boolean excludeStudio() {
        return excludes.containsKey(STUDIO);
    }

    public String getStudioName() {
        if (includeStudio()) {
            return includes.get(STUDIO);
        }
        return excludes.get(STUDIO);
    }

    // country check
    public boolean includeCountry() {
        return includes.containsKey(COUNTRY);
    }

    public boolean excludeCountry() {
        return excludes.containsKey(COUNTRY);
    }

    public String getCountryCode() {
        if (includeCountry()) {
            return includes.get(COUNTRY);
        }
        return excludes.get(COUNTRY);
    }

    // award check
    public boolean includeAward() {
        return includes.containsKey(AWARD);
    }

    public boolean excludeAward() {
        return excludes.containsKey(AWARD);
    }

    public String getAwardName() {
        if (includeAward()) {
            return includes.get(AWARD);
        }
        return excludes.get(AWARD);
    }
    
    // certification check
    public boolean includeCertification() {
        return includes.containsKey(CERTIFICATION);
    }

    public boolean excludeCertification() {
        return excludes.containsKey(CERTIFICATION);
    }

    public int getCertificationId() {
        if (includeCertification()) {
            return NumberUtils.toInt(includes.get(CERTIFICATION), -1);
        }
        return NumberUtils.toInt(excludes.get(CERTIFICATION), -1);
    }

    // video source check
    public boolean includeVideoSource() {
        return includes.containsKey(VIDEOSOURCE);
    }

    public boolean excludeVideoSource() {
        return excludes.containsKey(VIDEOSOURCE);
    }

    public String getVideoSource() {
        if (includeVideoSource()) {
            return includes.get(VIDEOSOURCE);
        }
        return excludes.get(VIDEOSOURCE);
    }

    // resolution check
    public boolean includeResolution() {
        return includes.containsKey(RESOLUTION);
    }

    public boolean excludeResolution() {
        return excludes.containsKey(RESOLUTION);
    }

    public ResolutionType getResolution() {
        String synonym;
        if (includeResolution()) {
            synonym = includes.get(RESOLUTION);
        } else {
            synonym = excludes.get(RESOLUTION);
        }
        return ResolutionType.fromString(synonym);
    }

    // boxed set check
    public boolean includeBoxedSet() {
        return includes.containsKey(BOXSET);
    }

    public boolean excludeBoxedSet() {
        return excludes.containsKey(BOXSET);
    }

    public int getBoxSetId() {
        if (includeBoxedSet()) {
            return NumberUtils.toInt(includes.get(BOXSET), -1);
        }
        return NumberUtils.toInt(excludes.get(BOXSET), -1);
    }

    // rating check
    public boolean includeRating() {
        return includes.containsKey(RATING);
    }

    public boolean excludeRating() {
        return excludes.containsKey(RATING);
    }

    public String getRatingSource() {
        if (ratingSource == null) {
            if (includeRating()) {
                this.parseRating(includes.get(RATING));
            } else {
                this.parseRating(excludes.get(RATING));
            }
        }
        return ratingSource;
    }

    public int getRating() {
        return this.ratingValue;
    }

    private void parseRating(final String value) {
        String[] result = StringUtils.split(value, '-');
        if (result == null || result.length == 0) {
            return;
        }

        ratingValue = NumberUtils.toInt(result[0]);

        if (ratingValue < 0) {
            ratingValue = 0;
        } else if (ratingValue > 10) {
            ratingValue = 10;
        }

        if (result.length > 1) {
            ratingSource = result[1];
        } else {
            ratingSource = "combined";
        }
    }

    // newest check
    public boolean includeNewest() {
        return includes.containsKey(NEWEST);
    }

    public boolean excludeNewest() {
        return excludes.containsKey(NEWEST);
    }

    public String getNewestSource() {
        if (newestSource == null) {
            if (includeNewest()) {
                this.parseNewest(includes.get(NEWEST));
            } else {
                this.parseNewest(excludes.get(NEWEST));
            }
        }
        return newestSource;
    }

    public Date getNewestDate() {
        return newestDate;
    }

    private void parseNewest(final String value) {
        String[] result = StringUtils.split(value, '-');
        if (result == null || result.length == 0) {
            return;
        }

        // Set the default to 30 if the conversion fails
        int maxDays = NumberUtils.toInt(result[0], 30);
        if (maxDays < 0) {
            maxDays = 30;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -maxDays);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        newestDate = cal.getTime();

        if (result.length > 1) {
            this.newestSource = result[1];
        } else {
            this.newestSource = "file";
        }
    }

    public void addParameter(String name, Object value) {
        this.parameters.put(name, value);
    }

    public void addScalarParameters(SqlScalars sqlScalars) {
        for (Entry<String, Object> entry : parameters.entrySet()) {
            sqlScalars.addParameter(entry.getKey(), entry.getValue());
        }
    }
}
