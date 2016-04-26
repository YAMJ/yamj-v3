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
import org.yamj.plugin.api.model.ISeason;
import org.yamj.plugin.api.model.ISeries;

public class SeriesMock implements ISeries {

    private final Map<String, String> ids;
    private String title;
    private String originalTitle;
    private int startYear;
    private int endYear;
    private String plot;
    private String outline;
    private int rating = -1;
    private Collection<String> genres;
    private Collection<String> studios;
    private Collection<String> countries;
    private Map<String,String> certifications;
    private Collection<ISeason> seasons;
    
    public SeriesMock() {
        this.ids = new HashMap<>(1);
    }

    public SeriesMock(Map<String, String> ids) {
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
    public int getStartYear() {
        return startYear;
    }

    @Override
    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    @Override
    public int getEndYear() {
        return endYear;
    }

    @Override
    public void setEndYear(int endYear) {
        this.endYear = endYear;
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
    public void addAward(String event, String category, int year) {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void addAward(String event, String category, int year, boolean won, boolean nominated) {
        // TODO Auto-generated method stub
    }    

    public void addSeason(ISeason season) {
        if (this.seasons == null) {
            this.seasons = new ArrayList<>();
        }
        this.seasons.add(season);
    }
    
    @Override
    public Collection<ISeason> getSeasons() {
        return this.seasons;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}