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
package org.yamj.plugin.api.metadata.mock;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yamj.plugin.api.metadata.IEpisode;
import org.yamj.plugin.api.metadata.ISeason;
import org.yamj.plugin.api.type.JobType;

public class EpisodeMock implements IEpisode {

    private final int number;
    private final Map<String, String> ids;
    private String title;
    private String originalTitle;
    private String plot;
    private String outline;
    private String tagline;
    private String quote;
    private String releaseCountry;
    private Date releaseDate;
    private int rating = -1;
    private Map<String, JobType> credits;
    private boolean done = false;
    private ISeason season;
    
    public EpisodeMock(int number) {
        this.number = number;
        this.ids = new HashMap<>(1);
    }

    public EpisodeMock(int number, Map<String, String> ids) {
        this.number = number;
        this.ids = ids;
    }

    @Override
    public int getNumber() {
        return this.number;
    }
    
    @Override
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

    public String getTagline() {
        return tagline;
    }

    @Override
    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getQuote() {
        return quote;
    }

    @Override
    public void setQuote(String quote) {
        this.quote = quote;
    }

    @Override
    public void setRelease(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Override
    public void setRelease(String country, Date releaseDate) {
        this.releaseCountry = country;
        this.releaseDate = releaseDate;
    }

    public String getReleaseCountry() {
        return releaseCountry;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public int getRating() {
        return rating;
    }

    @Override
    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public void addCredit(JobType jobType, String name) {
        addCredit(null, jobType, name);
    }

    @Override
    public void addCredit(JobType jobType, String name, String role) {
        addCredit(null, jobType, name, role);
    }

    @Override
    public void addCredit(JobType jobType, String name, String role, boolean voiceRole) {
        addCredit(null, jobType, name, role, voiceRole);
    }

    @Override
    public void addCredit(JobType jobType, String name, String role, String photoUrl) {
        addCredit(null, jobType, name, role, photoUrl);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name) {
        if (this.credits == null) {
            this.credits = new HashMap<>();
        }
        this.credits.put(name, jobType);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role) {
        if (this.credits == null) {
            this.credits = new HashMap<>();
        }
        this.credits.put(name, jobType);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role, boolean voiceRole) {
        if (this.credits == null) {
            this.credits = new HashMap<>();
        }
        this.credits.put(name, jobType);
    }

    @Override
    public void addCredit(String id, JobType jobType, String name, String role, String photoUrl) {
        if (this.credits == null) {
            this.credits = new HashMap<>();
        }
        this.credits.put(name, jobType);
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void setDone() {
        this.done = true;
    }

    @Override
    public void setNotFound() {
        this.done = false;
    }
    
    public void setSeason(ISeason season) {
        this.season = season;
    }
    
    @Override
    public ISeason getSeason() {
        return this.season;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }


}