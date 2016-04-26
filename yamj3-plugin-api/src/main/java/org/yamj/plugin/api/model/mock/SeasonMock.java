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
package org.yamj.plugin.api.model.mock;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.plugin.api.model.IEpisode;
import org.yamj.plugin.api.model.ISeason;
import org.yamj.plugin.api.model.ISeries;

public class SeasonMock implements ISeason {

    private final int number;
    private final Map<String, String> ids;
    private String title;
    private String originalTitle;
    private int year;
    private String plot;
    private String outline;
    private int rating = -1;
    private boolean done = false;
    private ISeries series;
    private Collection<IEpisode> episodes;
    
    public SeasonMock(int number) {
        this.number = number;
        this.ids = new HashMap<>(1);
    }

    public SeasonMock(int number, Map<String, String> ids) {
        this.number = number;
        this.ids = ids;
    }

    @Override
    public int getNumber() {
        return number;
    }

    public Map<String,String> getIds() {
        return ids;
    }

    @Override
    public String getId(String source) {
        return ids.get(source);
    }
    
    @Override
    public void addId(String source, String id) {
        if (StringUtils.isNotBlank(id)) {
            this.ids.put(source, id);
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getOriginalTitle() {
        return originalTitle;
    }

    @Override
    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    @Override
    public int getYear() {
        return year;
    }

    @Override
    public void setYear(int year) {
        this.year = year;
    }

    public String getPlot() {
        return plot;
    }

    @Override
    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getOutline() {
        return outline;
    }

    @Override
    public void setOutline(String outline) {
        this.outline = outline;
    }

    public int getRating() {
        return rating;
    }

    @Override
    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void setNotFound() {
        done = false;
    }

    @Override
    public void setDone() {
        this.done = true;
    }

    @Override
    public ISeries getSeries() {
        return series;
    }

    public void setSeries(ISeries series) {
        this.series = series;
    }

    public void addEpisode(IEpisode episode) {
        if (this.episodes == null) {
            this.episodes = new ArrayList<>();
        }
        this.episodes.add(episode);
    }
    
    @Override
    public Collection<IEpisode> getEpisodes() {
        return this.episodes;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}