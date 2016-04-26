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
import org.yamj.plugin.api.model.IMovie;
import org.yamj.plugin.api.model.type.JobType;

public class MovieMock implements IMovie {

    private final Map<String, String> ids;
    private String title;
    private String originalTitle;
    private int year;
    private String plot;
    private String outline;
    private String tagline;
    private String quote;
    private String releaseCountry;
    private Date releaseDate;
    private int rating = -1;
    private Collection<String> genres;
    private Collection<String> studios;
    private Collection<String> countries;
    private Map<String,String> certifications;
    private Map<String, JobType> credits;
    private Map<String,String> collections;
    
    public MovieMock() {
        this.ids = new HashMap<>(1);
    }

    public MovieMock(Map<String, String> ids) {
        this.ids = ids;
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

    public Collection<String> getStudios() {
        return studios;
    }

    @Override
    public void setStudios(Collection<String> studios) {
        this.studios = studios;
    }

    public Collection<String> getGenres() {
        return genres;
    }

    @Override
    public void setGenres(Collection<String> genres) {
        this.genres = genres;
    }
    
    public Collection<String> getCountries() {
        return countries;
    }

    @Override
    public void setCountries(Collection<String> countries) {
        this.countries = countries;
    }

    public Map<String, String> getCertifications() {
        return certifications;
    }

    @Override
    public void addCertification(String country, String certificate) {
        if (StringUtils.isNotBlank(country) && StringUtils.isNotBlank(certificate)) {
            if (this.certifications == null) {
                this.certifications = new HashMap<>(1);
            }
            this.certifications.put(country, certificate);
        }
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
    public void addCollection(String name, String id) {
        if (StringUtils.isNotBlank(name)) {
            if (this.collections == null) {
                this.collections = new HashMap<>(1);
            }
            this.collections.put(name,  id);
        }
    }

    public Map<String,JobType> getCredits() {
        return credits;
    }

    @Override
    public void addAward(String event, String category, int year) {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void addAward(String event, String category, int year, boolean won, boolean nominated) {
        // TODO Auto-generated method stub
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}