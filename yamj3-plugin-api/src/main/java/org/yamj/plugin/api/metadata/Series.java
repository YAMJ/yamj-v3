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
package org.yamj.plugin.api.metadata;

import java.util.*;

public class Series {

    private Map<String, String> ids = new HashMap<>();
    private String title;
    private String originalTitle;
    private int startYear;
    private int endYear;
    private String plot;
    private String outline;
    private int rating = -1;
    private Collection<String> genres = new HashSet<>();
    private Collection<String> studios = new HashSet<>();
    private Collection<String> countries = new HashSet<>();
    private Collection<Season> seasons = new ArrayList<>();
    
    public Map<String, String> getIds() {
        return ids;
    }

    public Series setIds(Map<String, String> ids) {
        this.ids = ids;
        return this;
    }

    public Series addId(String source, String id) {
        if (id != null && id.length() > 0) {
            this.ids.put(source, id);
        }
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Series setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public Series setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public int getStartYear() {
        return startYear;
    }

    public Series setStartYear(int startYear) {
        this.startYear = startYear;
        return this;
    }

    public int getEndYear() {
        return endYear;
    }

    public Series setEndYear(int endYear) {
        this.endYear = endYear;
        return this;
    }

    public String getPlot() {
        return plot;
    }

    public Series setPlot(String plot) {
        this.plot = plot;
        return this;
    }

    public String getOutline() {
        return outline;
    }

    public Series setOutline(String outline) {
        this.outline = outline;
        return this;
    }

    public int getRating() {
        return rating;
    }

    public Series setRating(int rating) {
        this.rating = rating;
        return this;
    }

    public Collection<String> getStudios() {
        return studios;
    }

    public Series setStudios(Collection<String> studios) {
        this.studios = studios;
        return this;
    }

    public Series addStudio(String studio) {
        this.studios.add(studio);
        return this;
    }

    public Collection<String> getGenres() {
        return genres;
    }

    public Series setGenres(Collection<String> genres) {
        this.genres = genres;
        return this;
    }
    
    public Series addGenre(String genre) {
        this.genres.add(genre);
        return this;
    }

    public Collection<String> getCountries() {
        return countries;
    }

    public Series setCountries(Collection<String> countries) {
        this.countries = countries;
        return this;
    }

    public Series addCountry(String country) {
        this.countries.add(country);
        return this;
    }

    public Collection<Season> getSeasons() {
        return seasons;
    }

    public Series setSeasons(Collection<Season> seasons) {
        this.seasons = seasons;
        return this;
    }
    
    public Series addSeason(Season season) {
        if (season != null) {
            this.seasons.add(season);
        }
        return this;
    }
    
    public Season getSeason(int seasonNumber) {
        for (Season season : this.seasons) {
            if (season.getSeasonNumber() == seasonNumber) {
                return season;
            }
        }
        return null;
    }
}